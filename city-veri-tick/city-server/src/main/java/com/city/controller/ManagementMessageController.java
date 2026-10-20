package com.city.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.ManagementMessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端消息接口")
@RestController
@RequestMapping("/controller/management/messages")
public class ManagementMessageController {
    private final ManagementMessageService service;

    public ManagementMessageController(ManagementMessageService service) {
        this.service = service;
    }

    @Operation(summary = "分页查询管理端消息")
    @GetMapping
    public Result<Map<String, Object>> list(@AuthenticationPrincipal LoginPrincipal user, @RequestParam(value = "page", defaultValue = "1") int page) {
        return Result.success(service.inbox(user, page));
    }

    @Operation(summary = "标记管理端消息为已读")
    @PostMapping("/{id}/read")
    public Result<Void> read(@AuthenticationPrincipal LoginPrincipal user, @PathVariable("id") Long id) {
        service.read(user, id);
        return Result.success();
    }
}
