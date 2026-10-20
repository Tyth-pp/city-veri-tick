package com.city.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.city.entity.FileObject;
import com.city.exception.BusinessException;
import com.city.result.SystemResultCode;
import com.city.mapper.FileObjectMapper;
import com.city.properties.OssProperties;
import com.city.service.FileStorageService;
import com.city.vo.FileUploadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Date;

@Service
public class OssFileStorageServiceImpl implements FileStorageService {
    private static final long URL_TTL_MILLIS = 30 * 60 * 1000L;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    @Autowired
    private FileObjectMapper fileObjectMapper;
    @Autowired
    private OssProperties ossProperties;

    @Override
    public FileUploadVO upload(Long userId, MultipartFile file) {
        validateConfiguration();
        if (file == null || file.isEmpty()) throw new BusinessException(SystemResultCode.BAD_REQUEST);
        if (!IMAGE_TYPES.contains(file.getContentType())) throw new BusinessException(SystemResultCode.BAD_REQUEST);
        if (file.getSize() > 10 * 1024 * 1024) throw new BusinessException(SystemResultCode.BAD_REQUEST);
        String suffix = extension(file.getOriginalFilename(), file.getContentType());
        String objectKey = normalizePrefix() + LocalDate.now() + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        OSS client = new OSSClientBuilder().build(ossProperties.getEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
        try {
            client.putObject(ossProperties.getBucketName(), objectKey, file.getInputStream());
        } catch (IOException ex) {
            throw new BusinessException(SystemResultCode.BAD_REQUEST);
        } finally {
            client.shutdown();
        }
        FileObject entity = new FileObject();
        entity.setStorageProvider("OSS");
        entity.setBucketName(ossProperties.getBucketName());
        entity.setObjectKey(objectKey);
        entity.setOriginalName(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setUploadedBy(userId);
        fileObjectMapper.insert(entity);
        return new FileUploadVO(entity.getId(), entity.getOriginalName(), entity.getContentType(), entity.getSizeBytes(), buildUrl(objectKey));
    }

    @Override
    public String getUrl(Long fileId) {
        if (fileId == null) return null;
        FileObject file = fileObjectMapper.selectById(fileId);
        return file == null ? null : buildUrl(file.getObjectKey());
    }

    private void validateConfiguration() {
        if (blank(ossProperties.getEndpoint()) || blank(ossProperties.getBucketName()) || blank(ossProperties.getAccessKeyId()) || blank(ossProperties.getAccessKeySecret()))
            throw new BusinessException(SystemResultCode.BAD_REQUEST);
    }

    private String buildUrl(String key) {
        OSS client = new OSSClientBuilder().build(normalizeEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
        try {
            Date expiration = new Date(System.currentTimeMillis() + URL_TTL_MILLIS);
            return client.generatePresignedUrl(ossProperties.getBucketName(), key, expiration).toString();
        } finally {
            client.shutdown();
        }
    }

    private String normalizeEndpoint() {
        return ossProperties.getEndpoint().matches("^https?://.*") ? ossProperties.getEndpoint() : "https://" + ossProperties.getEndpoint();
    }

    private String normalizePrefix() {
        String p = ossProperties.getObjectPrefix();
        return p == null || p.isBlank() ? "" : p.replaceAll("^/+|/+$", "") + "/";
    }

    private String extension(String name, String type) {
        if (name != null && name.contains(".")) return name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return "." + type.substring(type.indexOf('/') + 1);
    }

    private boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private String trimEnd(String v) {
        return v.replaceAll("/+$", "");
    }
}
