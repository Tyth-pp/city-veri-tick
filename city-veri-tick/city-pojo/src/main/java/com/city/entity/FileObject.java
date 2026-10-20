package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("file_object")
public class FileObject implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private String storageProvider;
    private String bucketName;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
