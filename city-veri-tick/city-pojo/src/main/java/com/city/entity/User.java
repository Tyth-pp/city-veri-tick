package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.city.enums.UserRole;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@TableName("city_user")
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private String username;
    private String passwordHash;
    private String phone;
    private String nickname;
    private String staffName;
    private String realName;
    private String idCardHash;
    private String idCardMasked;
    private Long avatarFileId;
    private Boolean enabled;
    private Boolean mustChangePassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableField(exist = false)
    private UserRole role;
    @TableField(exist = false)
    private List<String> roleCodes;


}
