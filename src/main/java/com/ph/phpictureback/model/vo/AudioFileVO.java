package com.ph.phpictureback.model.vo;

import com.ph.phpictureback.model.entry.AudioFile;
import com.ph.phpictureback.model.entry.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 脱敏后的用户，用户用户搜索他人时展示
 */
@Data
public class AudioFileVO implements Serializable {

    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 存放地址
     */
    private String fileUrl;

    /**
     * 文件类型 0-图片，1-视频，2-音频
     */
    private Integer fileType;

    /**
     * 标题
     */
    private String title;

    /**
     * 大小
     */
    private Long size;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 文件的md5
     */
    private String md5;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private UserVO userVO;

    /**
     * 包装类转对象
     *
     * @param audioFileVO
     * @return
     */
    public static AudioFile voToObj(AudioFileVO audioFileVO) {
        if (audioFileVO == null) {
            return null;
        }
        AudioFile audioFile = new AudioFile();
        BeanUtils.copyProperties(audioFileVO, audioFile);

        return audioFile;
    }

    /**
     * 对象转包装类
     *
     * @param audioFile
     * @return
     */
    public static AudioFileVO objToVo(AudioFile audioFile) {
        if (audioFile == null) {
            return null;
        }
        AudioFileVO audioFileVO = new AudioFileVO();
        BeanUtils.copyProperties(audioFile, audioFileVO);
        return audioFileVO;
    }

    private static final long serialVersionUID = 1L;
}
