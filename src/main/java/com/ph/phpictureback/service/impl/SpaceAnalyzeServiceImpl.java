package com.ph.phpictureback.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.mapper.PictureMapper;
import com.ph.phpictureback.model.dto.analyze.*;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.space.analyze.*;
import com.ph.phpictureback.service.SpaceAnalyzeService;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.SpaceService;
import com.ph.phpictureback.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 杨志亮
 * @description 针对表【picture(分析表)】的数据库操作Service实现
 * @createDate 2025-03-06 22:04:22
 */
@Service
@Slf4j
public class SpaceAnalyzeServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements SpaceAnalyzeService {


    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;

    /**
     * 空间使用情况分析
     *
     * @param spaceUsageAnalyzeDto
     * @param loginUser
     * @return
     */
    public SpaceUsageAnalyzeVo spaceUsageAnalyze(SpaceUsageAnalyzeDto spaceUsageAnalyzeDto, User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeDto == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceUsageAnalyzeDto, loginUser);
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        qw.select("picSize");
        //填充校验参数，判断是查询全部空间，公共空间，个人空间的是使用情况
        fillSpaceAnalyze(spaceUsageAnalyzeDto, qw);
        //查询图片数据
        List<Object> pictureList = pictureService.getBaseMapper().selectObjs(qw);
        //得到图片的大小和数量
        long picSize = pictureList.stream().mapToLong(res -> res instanceof Long ? (Long) res : 0).sum();
        long picCount = pictureList.size();
        //设置返回值
        SpaceUsageAnalyzeVo spaceUsageAnalyzeVo = new SpaceUsageAnalyzeVo();
        spaceUsageAnalyzeVo.setUsedSize(picSize);
        spaceUsageAnalyzeVo.setUsedCount(picCount);
        //查询公共空间和全部空间时，不用设置总大小和总数量
        if (spaceUsageAnalyzeDto.isQueryAll() || spaceUsageAnalyzeDto.isQueryPublic()) {
            spaceUsageAnalyzeVo.setMaxCount(null);
            spaceUsageAnalyzeVo.setMaxSize(null);
        } else {
            //查询个人空间，按照字段查询
            Long spaceId = spaceUsageAnalyzeDto.getSpaceId();
            Space space = spaceService.lambdaQuery()
                    .select(Space::getMaxCount, Space::getMaxSize)
                    .eq(Space::getId, spaceId)
                    .one();
            spaceUsageAnalyzeVo.setMaxCount(space.getMaxCount());
            spaceUsageAnalyzeVo.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeVo.setSizeUsageRatio(NumberUtil.round(picSize * 1.0 / space.getMaxSize(), 2).doubleValue());
            spaceUsageAnalyzeVo.setCountUsageRatio(NumberUtil.round(picCount * 1.0 / space.getMaxCount(), 2).doubleValue());
        }

        return spaceUsageAnalyzeVo;

    }

    /**
     * 按照图片分类分析图片情况
     *
     * @param spaceCategoryAnalyzeDto
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeVo> spaceCategoryAnalyze(SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeDto == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeDto, loginUser);
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        //填充校验参数，判断是查询全部空间，公共空间，个人空间的是使用情况
        fillSpaceAnalyze(spaceCategoryAnalyzeDto, qw);
        qw.select("category as category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
        //查询图片数据,创建的是Map集合，key是字段如：category，count，size，value是数量或值
        return pictureService.getBaseMapper().selectMaps(qw)
                .stream()
                .map(res -> {
                    String category = res.get("category") != null ? res.get("category").toString() : "未分类";
                    long count = ((Number) res.get("count")).longValue();
                    long totalSize = ((Number) res.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeVo(category, count, totalSize);
                }).collect(Collectors.toList());
    }

    /**
     * 根据标签对图片分类
     *
     * @param spaceTagAnalyzeDto
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeVo> spaceTagAnalyze(SpaceTagAnalyzeDto spaceTagAnalyzeDto, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeDto == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeDto, loginUser);
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        //填充校验参数，判断是查询全部空间，公共空间，个人空间的是使用情况
        fillSpaceAnalyze(spaceTagAnalyzeDto, qw);

        qw.select("tags");
        //获取到tag列表
        List<String> tagList = pictureService.getBaseMapper().selectObjs(qw)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());


        //合并所有标签，并统计使用次数
        Map<String, Long> tagCountMap = tagList.stream()
                .flatMap(tag -> JSONUtil.toList(tag, String.class).stream()) //合并tag标签
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        //转化成为响应对象
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) //降序排列
                .map(entry -> new SpaceTagAnalyzeVo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 图片大小 空间分析
     *
     * @param spaceSizeAnalyzeDto
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeVo> spaceSizeAnalyze(SpaceSizeAnalyzeDto spaceSizeAnalyzeDto, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeDto == null, ErrorCode.PARAMS_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeDto, loginUser);
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        //填充校验参数，判断是查询全部空间，公共空间，个人空间的是使用情况
        fillSpaceAnalyze(spaceSizeAnalyzeDto, qw);
        qw.select("picSize");
        //获取到图片大小列表
        List<Long> pictureSize = pictureService.getBaseMapper().selectObjs(qw)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(res -> ((Number) res).longValue())
                .collect(Collectors.toList());

        //统计图片大小
        Map<String, Long> sizeCountMap = new HashMap<>();
        sizeCountMap.put("<100KB", pictureSize.stream().filter(pic -> pic < 100 * 1024).count());
        sizeCountMap.put(">100KB,<500KB", pictureSize.stream().filter(pic -> pic >= 100 * 1024 && pic < 500 * 1024).count());
        sizeCountMap.put(">500kb,<1M", pictureSize.stream().filter(pic -> pic < 1024 * 1024 && pic >= 5 * 100 * 1024).count());
        sizeCountMap.put(">1M", pictureSize.stream().filter(pic -> pic >= 1024 * 1024).count());
        //转化成为响应对象
        return sizeCountMap.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeVo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 用户上传图片空间分析
     *
     * @param spaceUserAnalyzeDto
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeVo> spaceUserAnalyze(SpaceUserAnalyzeDto spaceUserAnalyzeDto, User loginUser) {
        Long userId = spaceUserAnalyzeDto.getUserId();
        //权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeDto, loginUser);
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        //填充校验参数，判断是查询全部空间，公共空间，个人空间的是使用情况
        qw.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillSpaceAnalyze(spaceUserAnalyzeDto, qw);

        //获取到要搜素的区间
        String timeDimension = spaceUserAnalyzeDto.getTimeDimension();
        switch (timeDimension) {
            case "day":
                qw.select("DATE_FORMAT(createTime,'%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                qw.select("YEARWEEK(createTime) as period", "count(*) as count");
                break;
            case "month":
                qw.select("DATE_FORMAT(createTime,'%Y-%m') as period", "count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        //根据查询的时间进行排序
        qw.groupBy("period").orderByAsc("period");

        return pictureService.getBaseMapper().selectMaps(qw)
                .stream()
                .map(map -> new SpaceUserAnalyzeVo(map.get("period").toString(), Long.parseLong(map.get("count").toString())))
                .collect(Collectors.toList());
    }

    /**
     * 获取排名前N个空间
     *
     * @param spaceRankAnalyzeDto
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> spaceAnalyze(SpaceRankAnalyzeDto spaceRankAnalyzeDto, User loginUser) {
        Integer topN = spaceRankAnalyzeDto.getTopN();
        //权限校验，只有管理员才可以访问
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "没有权限");
        QueryWrapper<Space> qw = new QueryWrapper<>();
        qw.select("id", "spaceName", "totalSize", "userId");
        qw.orderByDesc("totalSize").last("limit " + topN);
        //查询结果
        return spaceService.list(qw);

    }

    /**
     * 校验空间分析权限
     *
     * @param spaceAnalyzeDto
     * @param loginUser
     */
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeDto spaceAnalyzeDto, User loginUser) {
        //只有管理员才可以分析 全部图库和公共图库
        if (spaceAnalyzeDto.isQueryAll() || spaceAnalyzeDto.isQueryPublic()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "没有权限");
        } else {
            //查询自己的图库
            Long spaceId = spaceAnalyzeDto.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId < 0, ErrorCode.PARAMS_ERROR, "空间为空");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间为空");
            //校验用户是否拥有该空间权限
            spaceService.checkSpaceAuth(space, loginUser);
        }
    }

    /**
     * 查询空间分析条件分析填充
     *
     * @param spaceAnalyzeDto
     * @param queryWrapper
     */
    private void fillSpaceAnalyze(SpaceAnalyzeDto spaceAnalyzeDto, QueryWrapper<Picture> queryWrapper) {
        Long spaceId = spaceAnalyzeDto.getSpaceId();
        boolean queryAll = spaceAnalyzeDto.isQueryAll();
        boolean queryPublic = spaceAnalyzeDto.isQueryPublic();
        if (queryAll) {
            //查询全部图库 ,不用添加查询条件
        } else if (queryPublic) {
            //查询公共图库
            queryWrapper.isNull("spaceId");
        } else if (spaceId != null) {
            //查询个人空间图库
            queryWrapper.eq("spaceId", spaceId);
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作失败");
        }
    }
}





