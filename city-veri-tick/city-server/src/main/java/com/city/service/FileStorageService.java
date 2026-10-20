package com.city.service;

import com.city.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileUploadVO upload(Long userId, MultipartFile file);

    String getUrl(Long fileId);
}
