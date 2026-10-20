package com.city.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.city.dto.UpdateProfileDTO;
import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.UserProfileService;
import com.city.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户资料接口")
@RestController
@RequestMapping("/controller/user/profile")
public class UserProfileController {
    @Autowired
    private UserProfileService profileService;

    @Operation(summary = "查询当前用户资料")
    @GetMapping
    public Result<UserProfileVO> get(@AuthenticationPrincipal LoginPrincipal user) {
        return Result.success(profileService.get(user.userId()));
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping
    public Result<UserProfileVO> update(@AuthenticationPrincipal LoginPrincipal user,
                                        @Valid @RequestBody UpdateProfileDTO request) {
        return Result.success(profileService.update(user.userId(), request));
    }
}
