package com.city.vo;
import java.util.List;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PlatformUserViewVO implements Serializable {
    private Long id;
    private String username;
    private String phone;
    private String nickname;
    private String name;
    private Boolean enabled;
    private String roleCodes;
    private List<OrganizerAccountVO> organizers;
    private LocalDateTime createdAt;
}
