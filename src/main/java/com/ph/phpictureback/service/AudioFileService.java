package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.dto.audioFile.AudioFileAddDto;
import com.ph.phpictureback.model.dto.audioFile.AudioFileQueryDto;
import com.ph.phpictureback.model.dto.audioFile.AudioFileUpdateDto;
import com.ph.phpictureback.model.entry.AudioFile;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.AudioFileVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 杨志亮
* @description 针对表【audio_file(视频文件表)】的数据库操作Service
* @createDate 2025-07-16 21:41:23
*/
public interface AudioFileService extends IService<AudioFile> {

    /**
     * 新增文件
     * @param multipartFile
     * @param audioFileAddDto
     * @param loginUser
     * @return
     */
    Long addAudioFile(MultipartFile multipartFile, AudioFileAddDto audioFileAddDto, User loginUser);

    /**
     * 更新文件
     * @param audioFileUpdateDto
     * @param loginUser
     * @return
     */
    Boolean updateAudioFile(AudioFileUpdateDto audioFileUpdateDto, User loginUser);


    Boolean deleteFile(Long id, User loginUser);

    QueryWrapper<AudioFile> getQueryWrapper(AudioFileQueryDto audioFileQueryDto);

    Page<AudioFileVO> listVO(Page<AudioFile> page);
}
