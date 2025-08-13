package com.ph.phpictureback.mapper;

import com.ph.phpictureback.model.entry.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;

/**
 * @author 杨志亮
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2025-03-10 22:21:48
 * @Entity com.ph.phpictureback.model.entry.Picture
 */
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 判断是否存在
     * @param id
     * @return
     */
    @Select("select exists(select 1 from ph_picture.picture where id=#{id})")
    boolean exists(Long id);

    /**
     * 根据id获取图片
     * @param id
     * @return
     */
    @Select("select * from ph_picture.picture where id=#{id}")
    Picture getById(Long id);

    @Insert("insert into  ph_picture.picture( url, name, introduction, category, tags, picSize, picWidth,picHeight, picScale, picFormat," +
            " userId, reviewStatus, reviewMessage, reviewerId, reviewTime,thumbnailUrl,spaceId,picColor) values " +
            "(#{url},#{name},#{introduction},#{category},#{tags},#{picSize},#{picWidth},#{picHeight},#{picScale},#{picFormat},#{userId}" +
            ",#{reviewStatus},#{reviewMessage},#{reviewerId},#{reviewTime},#{thumbnailUrl},#{spaceId},#{picColor})")
    Boolean savePicture(Picture picture);


    int updatePictureLike(@Param("map") HashMap<Long, Long> map);
}




