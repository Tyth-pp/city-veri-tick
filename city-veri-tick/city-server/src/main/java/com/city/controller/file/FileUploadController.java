package com.city.controller.file;

import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.FileStorageService;
import com.city.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/files")
public class FileUploadController {
    @Autowired
    private FileStorageService fileStorageService;

    @Operation(summary = "上传图片")
    @PostMapping(value = "/images", consumes = "multipart/form-data")
    public Result<FileUploadVO> uploadImage(@AuthenticationPrincipal LoginPrincipal user, @RequestPart("file") MultipartFile file) {
        return Result.success(fileStorageService.upload(user.userId(), file));
    }
}
