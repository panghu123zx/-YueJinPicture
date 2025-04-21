package com.ph.phpictureback.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.api.aliyun.AliyunApi;
import com.ph.phpictureback.api.aliyun.model.CreateOutPaintingTaskDto;
import com.ph.phpictureback.api.aliyun.model.CreateOutPaintingTaskVo;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.constant.RedisCacheConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.CosManager;
import com.ph.phpictureback.manager.DownloadFileTemplate;
import com.ph.phpictureback.manager.auth.SpaceUserAuthManager;
import com.ph.phpictureback.manager.auth.StpKit;
import com.ph.phpictureback.manager.auth.model.SpaceUserPermissionConstant;
import com.ph.phpictureback.manager.fileUpload.FilePictureUpload;
import com.ph.phpictureback.manager.fileUpload.UrlPictureUpload;
import com.ph.phpictureback.manager.redisCache.PictureViewCache;
import com.ph.phpictureback.mapper.PictureMapper;
import com.ph.phpictureback.model.dto.picture.*;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.ReviewStatusEnum;
import com.ph.phpictureback.model.enums.SpaceTypeEnum;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.model.vo.UserVO;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.SpaceService;
import com.ph.phpictureback.service.UserService;
import com.ph.phpictureback.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 杨志亮
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-03-10 22:21:48
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {


    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManager cosManager;

    @Resource
    private AliyunApi aliyunApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private PictureViewCache pictureViewCache;


    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 上传图片
     *
     * @param inputSource      文件
     * @param pictureUploadDto 图片id
     * @param loginuser        登入用户
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadDto pictureUploadDto, User loginuser) {
        ThrowUtils.throwIf(loginuser == null, ErrorCode.NO_AUTH_ERROR, "无权限");

        Boolean admin = userService.isAdmin(loginuser);
        //判断是新增还是更新图片
        Long pictureId = pictureUploadDto.getId();

        //更新图片，判断图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        //得到空间id
        Long spaceId = pictureUploadDto.getSpaceId();
        //只有空间管理员才可以上传图片,团队空间的编辑者也可以上传
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            //私有空间，空间的管理员才可以上传
            if(space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue())){
                ThrowUtils.throwIf(!space.getUserId().equals(loginuser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限");
            }
            //上传时，校验空间的大小和数量 是否达到上限
            if (space.getTotalCount() > space.getMaxCount()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间图片数量已达到上限");
            }
            if (space.getTotalSize() > space.getMaxSize()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间图片大小已达到上限");
            }
        }
        String uploadPicturePath = null;
        //文件存放位置
        if (spaceId == null) {
            uploadPicturePath = String.format("public/%s", loginuser.getId());
        } else {
            uploadPicturePath = String.format("/space/%s", spaceId);
        }
        //上传图片
        DownloadFileTemplate pictureUpload = filePictureUpload;
        //根据类型判断是 url 还是文件上传
        if (inputSource instanceof String) {
            pictureUpload = urlPictureUpload;
        }
        //上传图片
        UploadPictureDto uploadPicture = pictureUpload.uploadPicture(inputSource, uploadPicturePath);
        Picture picture = new Picture();
        picture.setUrl(uploadPicture.getUrl());
        picture.setPicSize(uploadPicture.getPicSize());
        picture.setPicWidth(uploadPicture.getPicWidth());
        picture.setPicHeight(uploadPicture.getPicHeight());
        picture.setSpaceId(spaceId);
        picture.setPicScale(uploadPicture.getPicScale());
        picture.setPicFormat(uploadPicture.getPicFormat());
        picture.setUserId(loginuser.getId());
        picture.setPicColor(uploadPicture.getPicColor());
        //设置缩略图的地址
        picture.setThumbnailUrl(uploadPicture.getThumbnailUrl());
        //批量获取图片时，根据关键词设置名字
        if (StrUtil.isNotBlank(pictureUploadDto.getPicName())) {
            picture.setName(pictureUploadDto.getPicName());
        } else {
            picture.setName(uploadPicture.getPicName());
        }
        //公共空间的图片上传时审核，但是个人图库的图片上传时，不对其审核
        if (spaceId == null) {
            //审核信息
            sendReviewMessage(picture, loginuser);
        }


        //id不为空表示更新
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR);
            //更新时需要设置编辑时间，并且使更新的图片依然保留在原来的位置上
            picture.setId(pictureId);
            picture.setEditTime(new Date());
            if (spaceId == null) {
                //没有传递空间id，就复用原来的空间id
                picture.setSpaceId(oldPicture.getSpaceId());
            } else {
                //如果传递了空间id，就判断是否和原来的空间id一样
                if (!spaceId.equals(oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
                }
            }

        }

        //加锁
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            //更新/保存
            boolean update = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新图片失败");
            if (finalSpaceId != null) {
                //上传之后更新空间的使用图片的数据和大小
                boolean updateSpace = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize=totalSize+" + picture.getPicSize())
                        .setSql("totalCount=totalCount+1")
                        .update();
                ThrowUtils.throwIf(!updateSpace, ErrorCode.SYSTEM_ERROR, "更新空间失败");
            }
            return picture;
        });
        return PictureVO.objToVo(picture);
    }


    /**
     * 分页获取图片列表
     *
     * @param page
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> listPictureVo(Page<Picture> page, HttpServletRequest request) {


        List<Picture> pictureList = page.getRecords();
        //获取Vo类型的page类
        Page<PictureVO> pictureVOPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //vo类型的变换
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //获取用户信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());

        //让用户的id和用户一一对应，组成map集合
        Map<Long, List<User>> userIdListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        //将用户信息设置到图片中
        pictureVOList.forEach(pictureVO -> {
            //取出分页的每一个Id
            Long userId = pictureVO.getUserId();
            User user = null;
            //判断集合中是否存在
            if (userIdListMap.containsKey(userId)) {
                //根据userId查询第一个的用户
                user = userIdListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVo(user));


            HashOperations ops = redisTemplate.opsForHash();
            if(ops.hasKey(RedisCacheConstant.PICTURE_VIEW, pictureVO.getId())){
                Object viewSum = ops.get(RedisCacheConstant.PICTURE_VIEW, pictureVO.getId());
                long viewCount = ((Number) viewSum).longValue();
                pictureVO.setViewCount(pictureVO.getViewCount() + viewCount);
            }

            if(ops.hasKey(RedisCacheConstant.PICTURE_LIKE, pictureVO.getId())){
                Object likeSum = ops.get(RedisCacheConstant.PICTURE_LIKE, pictureVO.getId());
                long likeCount = ((Number) likeSum).longValue();
                pictureVO.setLikeCount(pictureVO.getLikeCount() + likeCount);
            }

            if(ops.hasKey(RedisCacheConstant.PICTURE_SHARE, pictureVO.getId())){
                Object shareSum = ops.get(RedisCacheConstant.PICTURE_SHARE, pictureVO.getId());
                long shareCount = ((Number) shareSum).longValue();
                pictureVO.setShareCount(pictureVO.getShareCount() + shareCount);
            }

        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    /**
     * 条件查询
     *
     * @param pictureQueryDto
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryDto pictureQueryDto) {
        if (pictureQueryDto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = pictureQueryDto.getId();
        String name = pictureQueryDto.getName();
        String introduction = pictureQueryDto.getIntroduction();
        String category = pictureQueryDto.getCategory();
        List<String> tags = pictureQueryDto.getTags();
        Long picSize = pictureQueryDto.getPicSize();
        String picFormat = pictureQueryDto.getPicFormat();
        String searchText = pictureQueryDto.getSearchText();
        Long userId = pictureQueryDto.getUserId();
        Long spaceId = pictureQueryDto.getSpaceId();
        Long commentCount = pictureQueryDto.getCommentCount();
        Long likeCount = pictureQueryDto.getLikeCount();
        Long shareCount = pictureQueryDto.getShareCount();
        Long viewCount = pictureQueryDto.getViewCount();
        boolean queryPublic = pictureQueryDto.isQueryPublic();
        Integer reviewStatus = pictureQueryDto.getReviewStatus();
        Long reviewerId = pictureQueryDto.getReviewerId();
        String reviewMessage = pictureQueryDto.getReviewMessage();
        String sortField = pictureQueryDto.getSortField();
        String sortOrder = pictureQueryDto.getSortOrder();
        Date startEditTime = pictureQueryDto.getStartEditTime();
        Date endEditTime = pictureQueryDto.getEndEditTime();
        String homeShow = pictureQueryDto.getHomeShow();

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);

        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText));
        }
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "createTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "createTime", endEditTime);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotNull(commentCount), "commentCount", commentCount);
        queryWrapper.eq(ObjUtil.isNotNull(likeCount), "likeCount", likeCount);
        queryWrapper.eq(ObjUtil.isNotNull(shareCount), "shareCount", shareCount);
        queryWrapper.eq(ObjUtil.isNotNull(viewCount), "viewCount", viewCount);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(queryPublic, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotNull(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotNull(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotNull(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        if(StrUtil.isEmpty(homeShow)){
            queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        }else{
            queryWrapper.orderBy(StrUtil.isNotEmpty(homeShow), sortOrder.equals("ascend"), "viewCount");
            queryWrapper.orderBy(StrUtil.isNotEmpty(homeShow), sortOrder.equals("ascend"), "likeCount");
            queryWrapper.orderBy(StrUtil.isNotEmpty(homeShow), sortOrder.equals("ascend"),"createTime");
        }
        return queryWrapper;
    }

    /**
     * 更新图片信息
     *
     * @param pictureUpdateDto
     * @return
     */
    @Override
    public boolean updatePicture(PictureUpdateDto pictureUpdateDto) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateDto, picture);
        //将List类型的Tag转化为String类型
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateDto.getTags()));
        this.validPicture(picture);

        //观察图片是否为空
        Long pictureId = pictureUpdateDto.getId();
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "图片为空");

        boolean update = this.updateById(picture);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新失败");
        return true;
    }

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @return
     */
    @Override
    public boolean deletePicture(DeleteRequest deleteRequest) {
        Long pictureId = deleteRequest.getId();
        Picture picture = this.getById(pictureId);
        //检验图片权限
//        pictureService.checkPictureAuth(picture, loginUser);

        //更新空间大小
        transactionTemplate.execute(status -> {
            if(picture.getSpaceId()!=null){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, picture.getSpaceId())
                        .setSql("totalSize = totalSize - " + picture.getPicSize())
                        .setSql("totalCount=totalCount-1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新空间大小失败");
            }


            boolean delete = this.removeById(pictureId);

            ThrowUtils.throwIf(!delete, ErrorCode.SYSTEM_ERROR, "删除失败");
            return true;
        });
        //释放cos资源
        this.cosDeletePicture(picture);
        return true;
    }

    /**
     * 编辑图片
     * @param pictureEditDto
     * @param loginUser
     * @return
     */
    @Override
    public boolean editPicture(PictureEditDto pictureEditDto, User loginUser) {
        //检验图片是否存在
        Picture oldPicture = this.getById(pictureEditDto.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR);

        //设置图片信息
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditDto, picture);
        String tags = JSONUtil.toJsonStr(pictureEditDto.getTags());
        picture.setTags(tags);
        picture.setUserId(oldPicture.getUserId());
        picture.setEditTime(new Date());

        //检验图片信息
        this.validPicture(picture);

        //图片权限的校验
//        pictureService.checkPictureAuth(picture, loginUser);
        //填充审核信息
        this.sendReviewMessage(picture, loginUser);
        //编辑图片
        boolean update = this.updateById(picture);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR);
        return true;
    }

    /**
     * 获取单个图片的用户信息
     *
     * @param picture
     * @return
     */
    @Override
    public PictureVO getUserByPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user);
            pictureVO.setUser(userVo);
        }

        return pictureVO;
    }


    /**
     * 校验图片信息
     *
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 图片审核
     *
     * @param pictureReviewDto
     * @param loginUser
     */
    @Override
    public void pictureReview(PictureReviewDto pictureReviewDto, User loginUser) {
        if (pictureReviewDto == null || pictureReviewDto.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //得到审核状态的枚举
        Integer reviewStatus = pictureReviewDto.getReviewStatus();
        ReviewStatusEnum reviewStatusValue = ReviewStatusEnum.getReviewStatusValue(reviewStatus);
        //图片为 待审核 时不可以审核
        if (reviewStatusValue==null ||ReviewStatusEnum.REVIEWING.equals(reviewStatusValue)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "待审核，不可重复审核");
        }
        Picture picture = this.getById(pictureReviewDto.getId());
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片不存在");
        //不可以重复审核
        if (picture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "不可重复审核");
        }
        //图片审核
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewDto, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());

        boolean update = this.updateById(updatePicture);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 获取图片 （脱敏）
     *
     * @param id
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO getPictureVo(long id, User loginUser) {
        //浏览量+1
        pictureViewCache.addPictureViewCache(id);
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片信息为空");
        /*  if (picture.getSpaceId() != null) {
            pictureService.checkPictureAuth(picture, loginUser);
        }*/

        //搜索的是别人个人空间的图片，需要判断是否为空间管理员
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            //使用so-token校验
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        }
        //获取当前用户的权限
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = this.getUserByPicture(picture);
        pictureVO.setPermissionList(permissionList);
        return pictureVO;
    }

    /**
     * 审核信息的填写
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void sendReviewMessage(Picture picture, User loginUser) {
        Boolean admin = userService.isAdmin(loginUser);
        if (admin) {
            picture.setReviewStatus(ReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
        } else {
            picture.setReviewStatus(ReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量获取图片
     *
     * @param pictureUploadByBatchDto
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchDto pictureUploadByBatchDto, User loginUser) {
        //搜索的关键词
        String searchText = pictureUploadByBatchDto.getSearchText();
        //搜索的条数
        Integer count = pictureUploadByBatchDto.getCount();
        String namePrefix = pictureUploadByBatchDto.getNamePrefix();

        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多获取30条数据");
        //爬取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            //爬取图片
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取图片失败: {}", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片失败");
        }
        //获取包裹多个图片的外层元素 class=dgControl
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片盛失败");
        }
        //获取图片
//        Elements imageElement = div.select("img.mimg");
        Elements imageElement = div.select(".iusc");
        int uploadCount = 1;
        for (Element element : imageElement) {
            String dataM = element.attr("m");
            //获取到图片的地址
            String fileUrl;
            try{
                fileUrl = JSONUtil.parseObj(dataM).getStr("murl");
            }catch (Exception e){
                log.error("获取图片地址失败: {}", e);
                continue;
            }

            if (StrUtil.isBlank(fileUrl)) {
                log.info("图片地址为空,以跳过：{}", fileUrl);
                continue;
            }
            //处理图片的地址，获取到 ？ 的下标
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                //获取到 ？ 搜索之前的http链接，防止影响到fileUrl的关键词搜索
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            PictureUploadDto pictureUploadDto = new PictureUploadDto();
            namePrefix = searchText + uploadCount;
            pictureUploadDto.setPicName(namePrefix);
            try {
                //上传图片
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadDto, loginUser);
                log.info("上传图片成功，id={}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("上传图片失败:{}", e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传图片失败");
            }
            //观察爬取的数量是否到达
            if (uploadCount > count) {
                break;
            }

        }
        return uploadCount;
    }

    /**
     * 根据颜色搜索图片 (仅对空间图片开放)
     *
     * @param spaceId
     * @param color
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(long spaceId, String color, User loginUser) {
        ThrowUtils.throwIf(spaceId < 0 || StrUtil.isBlank(color), ErrorCode.PARAMS_ERROR);

        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //私有空间时，当前用户是否为空间的管理人
        if(space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue())){
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        }

        //获取当前空间所有的图片,主色调不能为空
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前空间没有图片");
        }

        //将目标颜色转化为color对象
        Color targetColor = Color.decode(color);

        return pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    //获取图片的主色调
                    String picColor = picture.getPicColor();
                    Color pictureColor = Color.decode(picColor);
                    //计算相似度
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .map(PictureVO::objToVo)
                .limit(12)
                .collect(Collectors.toList());
    }

    /**
     * 批量修改图片信息
     *
     * @param pictureEditByBatchDto
     * @param loginUser
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchDto pictureEditByBatchDto, User loginUser) {
        ThrowUtils.throwIf(pictureEditByBatchDto == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureEditByBatchDto.getSpaceId();
        String category = pictureEditByBatchDto.getCategory();
        List<String> tags = pictureEditByBatchDto.getTags();
        String nameRule = pictureEditByBatchDto.getNameRule();

        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //私有空间时，当前用户是否为空间的管理人
        if(space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue())){
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        }
        List<Long> pictureIdList = pictureEditByBatchDto.getPictureIdList();
        //只查询需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片列表为空");
        }
        //填充命名规则
        fillNameRole(pictureList, nameRule);

        //批量修改图片信息
        List<Picture> updatePictureList = pictureList.stream().peek(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        }).collect(Collectors.toList());

        boolean update = this.updateBatchById(updatePictureList);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);


    }

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskDto
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskVo createTask(CreatePictureOutPaintingTaskDto createPictureOutPaintingTaskDto, User loginUser) {
        Long pictureId = createPictureOutPaintingTaskDto.getPictureId();
        ThrowUtils.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR);
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //权限校验
//        checkPictureAuth(picture, loginUser);
        //创建扩图任务
        CreateOutPaintingTaskDto taskDto = new CreateOutPaintingTaskDto();
        CreateOutPaintingTaskDto.Input input = new CreateOutPaintingTaskDto.Input();
        //设置图片信息
        input.setImageUrl(picture.getUrl());
        taskDto.setInput(input);
        BeanUtils.copyProperties(createPictureOutPaintingTaskDto, taskDto);
        //更新图片信息
        return aliyunApi.createOutPaintingTask(taskDto);

    }

    /**
     * 删除COS文件 （异步处理）
     *
     * @param picture
     */
    @Async
    @Override
    public void cosDeletePicture(Picture picture) {
        String url = picture.getUrl();
        //观察 这个url 是否在多条记录中使用 ： 秒传等场景
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, url)
                .count();
        //如果被使用到了 ，直接返回
        if (count > 1) {
            return;
        }
        //没有被使用 则清理
        try {
            //提取url中的路径
            String path = new URL(url).getPath();
            cosManager.deleteObject(path);

            //清理压缩图
            String thumbnailUrl = picture.getThumbnailUrl();
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                String thumbUrl = new URL(thumbnailUrl).getPath();
                cosManager.deleteObject(thumbUrl);
            }
        } catch (MalformedURLException e) {
            log.error("删除COS文件错误:{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除COS文件错误");
        }

    }


    /**
     * 图片权限的检验,以替换为so-token统一校验
     *
     * @param picture
     * @param loginUser
     */
    public void checkPictureAuth(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            //公共图片，只有图片的创建人才可以个管理员才可以修改
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有权限");
            }
        } else {
            //个人图库,只有空间的管理员才可以上传图片
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "图库不存在");
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有权限");
            }
        }
    }

    /**
     * 填充命名规则
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillNameRole(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule)) {
            return;
        }
        int count = 1;
        for (Picture picture : pictureList) {
            picture.setName(nameRule + "_" + count);
            count++;
        }
    }
}




