package com.city.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class FileUploadVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long fileId;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String url;
}
