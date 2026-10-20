package com.city.vo;
import lombok.Data;
import java.util.List;
@Data
public class StaffAccountViewVO {
    private Long id;
    private String username;
    private String nickname;
    private String name;
    private String phone;
    private Boolean enabled;
    private String role;
    private List<Long> activityIds;
}

