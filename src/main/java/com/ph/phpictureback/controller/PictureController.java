package com.ph.phpictureback.controller;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ph.phpictureback.annotation.AuthCheck;
import com.ph.phpictureback.api.aliyun.AliyunApi;
import com.ph.phpictureback.api.aliyun.model.CreateOutPaintingTaskVo;
import com.ph.phpictureback.api.aliyun.model.GetOutPaintingTaskVo;
import com.ph.phpictureback.api.imageSearch.imageSearchFacade;
import com.ph.phpictureback.api.imageSearch.model.ImageSearchDto;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.constant.UserConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.LimitManager;
import com.ph.phpictureback.manager.ai.aiPicture.AiPicture;
import com.ph.phpictureback.manager.ai.aiPicture.AiPictureProducer;
import com.ph.phpictureback.manager.auth.StpKit;
import com.ph.phpictureback.manager.auth.annotation.SaSpaceCheckPermission;
import com.ph.phpictureback.manager.auth.model.SpaceUserPermissionConstant;
import com.ph.phpictureback.model.dto.picture.*;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.ReviewStatusEnum;
import com.ph.phpictureback.model.enums.SpaceTypeEnum;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.SpaceService;
import com.ph.phpictureback.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LimitManager limitManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AiPictureProducer aiPictureProducer;

    @Resource
    private AliyunApi aliyunApi;

    /**
     * caffeine  本地缓存的设置
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10000L)
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build();


    /**
     * 上传图片
     *
     * @param multipartFile
     * @param pictureUploadDto
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadDto pictureUploadDto,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadDto == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadDto, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传图片 （Url）
     *
     * @param pictureUploadDto
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadDto pictureUploadDto,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadDto == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(pictureUploadDto.getFileUrl(), pictureUploadDto, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * ai创建图片
     * @param pictureAiDto
     * @return
     */
    @PostMapping("/aipicture")
    @AuthCheck(mustRole = UserConstant.ADMIN)
    public BaseResponse<PictureVO> uploadAiPicture(@RequestBody PictureAiDto pictureAiDto,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(pictureAiDto == null, ErrorCode.PARAMS_ERROR);
        String content = pictureAiDto.getContent();
        User loginUser = userService.getLoginUser(request);
        if(StrUtil.isBlank(content)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容不能为空");
        }
        Picture picture = new Picture();
        picture.setName(pictureAiDto.getName());
        picture.setUrl("null");
        picture.setUserId(loginUser.getId());
        //获取当前登入用户的空间
        Space one = spaceService.lambdaQuery()
                .select(Space::getId)
                .eq(Space::getUserId, loginUser.getId())
                .eq(Space::getSpaceType, SpaceTypeEnum.PRIVATE.getValue())
                .one();
        ThrowUtils.throwIf(one == null, ErrorCode.PARAMS_ERROR, "用户没有空间，无法创建ai图片");
        picture.setSpaceId(one.getId());
        boolean save = pictureService.save(picture);
        ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "创建失败");
        aiPictureProducer.sendAiMessage(picture.getId().toString());
        return ResultUtils.success(pictureService.getPictureVo(picture.getId(), loginUser));
    }


    /**
     * 分页获取图片信息
     *
     * @param pictureQueryDto
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN)
    @PostMapping("/list/picture")
    public BaseResponse<Page<Picture>> getListPicture(@RequestBody PictureQueryDto pictureQueryDto) {
        ThrowUtils.throwIf(pictureQueryDto == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryDto.getCurrent();
        int pageSize = pictureQueryDto.getPageSize();
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryDto));
        return ResultUtils.success(page);
    }

    /**
     * 根据id获取图片信息
     *
     * @param id
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN)
    @GetMapping("/get")
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片信息为空");
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片信息(脱敏)
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureByIdVo(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVo = pictureService.getPictureVo(id, loginUser);

        return ResultUtils.success(pictureVo);
    }

    /**
     * 分页获取图片信息(脱敏)
     *
     * @param pictureQueryDto
     * @return
     */
    @PostMapping("/list/picture/vo")
    public BaseResponse<Page<PictureVO>> getListPictureByVo(@RequestBody PictureQueryDto pictureQueryDto,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryDto == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryDto.getCurrent();
        int pageSize = pictureQueryDto.getPageSize();
        //防止爬虫
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "图片请求过多");
        }
        Long spaceId = pictureQueryDto.getSpaceId();
        if (spaceId == null) {
            //查询公共图库 只查询审核通过的数据
            pictureQueryDto.setReviewStatus(ReviewStatusEnum.PASS.getValue());
            pictureQueryDto.setQueryPublic(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
//              查询个人图库 只查询自己的数据
           /* Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            User loginUser = userService.getLoginUser(request);
            //只有空间的创建人才可以查询自己的图库
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "没有权限");
            }*/
        }

        Page<Picture> page = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryDto));

        Page<PictureVO> pageVoList = pictureService.listPictureVo(page, request);
        return ResultUtils.success(pageVoList);
    }



    /**
     * 分页获取图片信息(脱敏)  使用缓存
     *
     * @param pictureQueryDto
     * @return
     */
    @PostMapping("/list/picture/vo/redis")
    public BaseResponse<Page<PictureVO>> getListPictureByVoRedis(@RequestBody PictureQueryDto pictureQueryDto,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryDto == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryDto.getCurrent();
        int pageSize = pictureQueryDto.getPageSize();
        //防止爬虫
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "图片请求过多");
        }

        Long spaceId = pictureQueryDto.getSpaceId();
        if (spaceId == null) {
            //查询公共图库 只查询审核通过的数据
            pictureQueryDto.setReviewStatus(ReviewStatusEnum.PASS.getValue());
            pictureQueryDto.setQueryPublic(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
//              查询个人图库 只查询自己的数据
           /* Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            User loginUser = userService.getLoginUser(request);
            //只有空间的创建人才可以查询自己的图库
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "没有权限");
            }*/
        }

        //定义redis和 caffeine的key
        String jsonStr = JSONUtil.toJsonStr(pictureQueryDto);
        String hashKey = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String redisKey = String.format("ph:getListPictureByVo:%s", hashKey);


        //从caffeine 本地缓存中取值
        String caffeineValue = LOCAL_CACHE.getIfPresent(redisKey);
        //如果值存在就直接返回
        if (StrUtil.isNotBlank(caffeineValue)) {
            Page<PictureVO> pageVoList = JSONUtil.toBean(caffeineValue, Page.class);
            return ResultUtils.success(pageVoList);
        }


        //获取redis中 key的值
        ValueOperations<String, String> strOpsForValue = stringRedisTemplate.opsForValue();
        String redisValue = strOpsForValue.get(redisKey);
        //如果值存在，就直接返回
        if (StrUtil.isNotBlank(redisValue)) {
            //取到值 放到本地缓存中
            LOCAL_CACHE.put(redisKey, redisValue);
            //redis的返回
            Page<PictureVO> pageListVo = JSONUtil.toBean(redisValue, Page.class);
            return ResultUtils.success(pageListVo);
        }


        pictureQueryDto.setReviewStatus(ReviewStatusEnum.PASS.getValue());
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryDto));

        Page<PictureVO> pageVoList = pictureService.listPictureVo(page, request);
        //设置国企时间，防止雪崩
        int timeout = 200 + RandomUtil.randomInt(100);
        //将获取到的值 设置到redis中
        String pageVoListStr = JSONUtil.toJsonStr(pageVoList);
        strOpsForValue.set(redisKey, pageVoListStr, timeout, TimeUnit.SECONDS);
        //本地缓存的设置值
        LOCAL_CACHE.put(redisKey, pageVoListStr);

        return ResultUtils.success(pageVoList);
    }


    /**
     * 更新图片信息
     *
     * @param pictureUpdateDto
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDto pictureUpdateDto) {
        ThrowUtils.throwIf(pictureUpdateDto == null, ErrorCode.PARAMS_ERROR);
        boolean update = pictureService.updatePicture(pictureUpdateDto);
        return ResultUtils.success(update);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        boolean delete = pictureService.deletePicture(deleteRequest);
        return ResultUtils.success(delete);
    }


    /**
     * 编辑图片
     *
     * @param pictureEditDto
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDto pictureEditDto, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditDto == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean edit = pictureService.editPicture(pictureEditDto, loginUser);
        return ResultUtils.success(edit);
    }

    /**
     * 标签信息
     *
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("风景", "生活", "高清", "艺术", "动物", "抽象", "城市", "游戏卡通", "创意");
        List<String> categoryList = Arrays.asList("手机壁纸", "电脑壁纸", "头像", "人文艺术", "素材", "其他");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }


    /**
     * 图片审核
     *
     * @param pictureReviewDto
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN)
    public BaseResponse<Boolean> pictureReview(@RequestBody PictureReviewDto pictureReviewDto, HttpServletRequest request) {
        if (pictureReviewDto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.pictureReview(pictureReviewDto, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量获取图片 （爬虫）
     *
     * @param pictureUploadByBatchDto
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchDto pictureUploadByBatchDto, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchDto == null, ErrorCode.PARAMS_ERROR, "获取图片条件为空");
        User loginUser = userService.getLoginUser(request);
        //限流
        limitManager.RateLimit(loginUser.getId().toString());
        Integer count = pictureService.uploadPictureByBatch(pictureUploadByBatchDto, loginUser);
        return ResultUtils.success(count);
    }


    /**
     * 以图搜图（爬取百度以图搜图）
     *
     * @param searchPictureByPictureDto
     * @param request
     * @return
     */
    @PostMapping("/image/search")
    public BaseResponse<List<ImageSearchDto>> imageSearch(@RequestBody SearchPictureByPictureDto searchPictureByPictureDto,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByPictureDto == null, ErrorCode.PARAMS_ERROR, "获取图片条件为空");
        Long pictureId = searchPictureByPictureDto.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId < 0, ErrorCode.PARAMS_ERROR, "图片id为空");
        //限流
        limitManager.RateLimit(pictureId.toString());
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片不存在");
        List<ImageSearchDto> imageSearchList = imageSearchFacade.getImageSearchList(picture.getUrl());
        return ResultUtils.success(imageSearchList);

    }

    /**
     * 根据颜色搜索图片
     *
     * @param searchPictureByColorDto
     * @param request
     * @return
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorDto searchPictureByColorDto,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorDto == null, ErrorCode.PARAMS_ERROR, "获取图片条件为空");
        Long spaceId = searchPictureByColorDto.getSpaceId();
        String color = searchPictureByColorDto.getPicColor();
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, color, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchDto
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchDto pictureEditByBatchDto, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchDto == null, ErrorCode.PARAMS_ERROR, "获取图片条件为空");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        pictureService.editPictureByBatch(pictureEditByBatchDto, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建图片出图任务 ai扩图
     *
     * @param createPictureOutPaintingTaskDto
     * @param request
     * @return
     */
    @PostMapping("/create/task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskVo> createOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskDto createPictureOutPaintingTaskDto,
                                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskDto == null, ErrorCode.PARAMS_ERROR, "获取图片条件为空");
        User loginUser = userService.getLoginUser(request);
        //限流
        limitManager.RateLimit(loginUser.getId().toString());
        CreateOutPaintingTaskVo createOutPaintingTaskVo = pictureService.createTask(createPictureOutPaintingTaskDto, loginUser);
        return ResultUtils.success(createOutPaintingTaskVo);
    }

    /**
     * 获取图片出图任务 ai扩图
     *
     * @param taskId
     * @return
     */
    @GetMapping("/get/task")
    public BaseResponse<GetOutPaintingTaskVo> getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务id为空");
        GetOutPaintingTaskVo getOutPaintingTaskVo = aliyunApi.getOutPaintingTask(taskId);
        return ResultUtils.success(getOutPaintingTaskVo);
    }


    /**
     * 分页获取个人发布的图片信息(脱敏)
     *
     * @param pictureQueryDto
     * @return
     */
    @PostMapping("/list/mypicture/vo")
    public BaseResponse<Page<PictureVO>> getMyListPictureByVo(@RequestBody PictureQueryDto pictureQueryDto,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryDto == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryDto.getCurrent();
        int pageSize = pictureQueryDto.getPageSize();
        //防止爬虫
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "图片请求过多");
        }
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.getId().equals(pictureQueryDto.getUserId()), ErrorCode.NO_AUTH_ERROR);

        Page<Picture> page = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryDto));

        Page<PictureVO> pageVoList = pictureService.listPictureVo(page, request);
        return ResultUtils.success(pageVoList);
    }

}
