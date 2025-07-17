package com.ph.phpictureback.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.fileUpload.FilePictureUpload;
import com.ph.phpictureback.manager.minio.MinioManage;
import com.ph.phpictureback.model.dto.audioFile.AudioFileAddDto;
import com.ph.phpictureback.model.dto.audioFile.AudioFileQueryDto;
import com.ph.phpictureback.model.dto.audioFile.AudioFileUpdateDto;
import com.ph.phpictureback.model.dto.picture.UploadPictureDto;
import com.ph.phpictureback.model.entry.AudioFile;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.AudioFileVO;
import com.ph.phpictureback.model.vo.SpaceVO;
import com.ph.phpictureback.service.AudioFileService;
import com.ph.phpictureback.mapper.AudioFileMapper;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 杨志亮
* @description 针对表【audio_file(视频文件表)】的数据库操作Service实现
* @createDate 2025-07-16 21:41:23
*/
@Service
public class AudioFileServiceImpl extends ServiceImpl<AudioFileMapper, AudioFile>
    implements AudioFileService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private MinioManage minioManage;
    /**
     * 新增文件
     * @param multipartFile
     * @param audioFileAddDto
     * @param loginUser
     * @return
     */
    @Override
    public Long addAudioFile(MultipartFile multipartFile, AudioFileAddDto audioFileAddDto, User loginUser) {
        Long userId = loginUser.getId();
        //上传路径
        String uploadPath=String.format("audio/%s",userId);
        //上传文件
        Integer fileType = audioFileAddDto.getFileType();
        AudioFile audioFile = new AudioFile();
        audioFile.setUserId(userId);
        audioFile.setFileType(fileType);
        audioFile.setIntroduction(audioFileAddDto.getIntroduction());
        //上传图片
        if (fileType.equals(0)){
            UploadPictureDto uploadPicture = filePictureUpload.uploadPicture(multipartFile, uploadPath);
            audioFile.setFileUrl(uploadPicture.getUrl());
            audioFile.setSize(uploadPicture.getPicSize());
            audioFile.setTitle(uploadPicture.getPicName());
        }else {
            //上传文件 / 音频
            //todo 音频的时长后端没法确定
            String url = minioManage.upload(multipartFile, uploadPath);
            long fileSize = multipartFile.getSize();
            audioFile.setFileUrl(url);
            audioFile.setSize(fileSize);
            audioFile.setTitle(multipartFile.getOriginalFilename());
        }
        boolean save = this.save(audioFile);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "文件上传失败");
        return audioFile.getId();
    }

    /**
     * 更新文件
     * @param audioFileUpdateDto
     * @param loginUser
     * @return
     */
    @Override
    public Boolean updateAudioFile(AudioFileUpdateDto audioFileUpdateDto, User loginUser) {
        Long id = audioFileUpdateDto.getId();
        AudioFile byId = this.getById(id);
        ThrowUtils.throwIf(byId == null, ErrorCode.SYSTEM_ERROR, "文件不存在");
        AudioFile audioFile = new AudioFile();
        BeanUtils.copyProperties(audioFileUpdateDto, audioFile);
        boolean update = this.updateById(audioFile);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "文件更新失败");
        return true;
    }

    /**
     * 删除文件
     * @param id
     * @param loginUser
     * @return
     */
    @Override
    public Boolean deleteFile(Long id, User loginUser) {
        AudioFile byId = this.getById(id);
        ThrowUtils.throwIf(byId == null, ErrorCode.SYSTEM_ERROR, "文件不存在");
        Long userId = byId.getUserId();
        if(!loginUser.getId().equals(userId) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"你无权删除");
        }
        boolean remove = this.removeById(id);
        ThrowUtils.throwIf(!remove, ErrorCode.SYSTEM_ERROR, "文件删除失败");
        return true;
    }

    @Override
    public QueryWrapper<AudioFile> getQueryWrapper(AudioFileQueryDto audioFileQueryDto) {
        ThrowUtils.throwIf(audioFileQueryDto==null,ErrorCode.PARAMS_ERROR,"参数为空");
        QueryWrapper<AudioFile> qw = new QueryWrapper<>();
        Long id = audioFileQueryDto.getId();
        Long userId = audioFileQueryDto.getUserId();
        Integer fileType = audioFileQueryDto.getFileType();
        String title = audioFileQueryDto.getTitle();
        Long size = audioFileQueryDto.getSize();
        String introduction = audioFileQueryDto.getIntroduction();
        String sortField = audioFileQueryDto.getSortField();
        String sortOrder = audioFileQueryDto.getSortOrder();
        qw.eq(ObjectUtil.isNotNull(id),"id",id);
        qw.eq(ObjectUtil.isNotNull(userId),"userId",userId);
        qw.eq(ObjectUtil.isNotNull(fileType),"fileType",fileType);
        qw.like(StrUtil.isNotEmpty(title),"title",title);
        qw.eq(ObjectUtil.isNotNull(size),"size",size);
        qw.like(StrUtil.isNotEmpty(introduction),"introduction",introduction);
        qw.orderBy(StrUtil.isNotEmpty(sortField),sortOrder.equals("ascend"),sortField);
        return qw;
    }

    @Override
    public Page<AudioFileVO> listVO(Page<AudioFile> page) {
        List<AudioFile> audioFileList = page.getRecords();
        Page<AudioFileVO> pageVO = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        //转化为vo类型
        List<AudioFileVO> audioFileVOList = audioFileList
                .stream()
                .map(AudioFileVO::objToVo)
                .collect(Collectors.toList());
        if (ObjUtil.isEmpty(audioFileList)) {
            return pageVO;
        }
        //获取用户id集合
        Set<Long> userIdList = audioFileList.stream().map(AudioFile::getUserId).collect(Collectors.toSet());

        Map<Long, List<User>> usermap = userService.listByIds(userIdList)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        audioFileVOList.forEach(audioFileVO -> {
            Long userId = audioFileVO.getUserId();
            User user = null;
            if (usermap.containsKey(userId)) {
                user = usermap.get(userId).get(0);
            }
            audioFileVO.setUserVO(userService.getUserVo(user));
        });
        pageVO.setRecords(audioFileVOList);
        return pageVO;
    }
}




