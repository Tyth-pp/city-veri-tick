package com.city.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileVO {
    private String nickname;
    private String phone;
    private Long avatarFileId;
    private String avatarUrl;
    private String realName;
    private String idCardMasked;
    private boolean realNameVerified;
}
