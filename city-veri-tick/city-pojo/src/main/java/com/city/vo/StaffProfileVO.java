package com.city.vo;
import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;
@Data
public class StaffProfileVO {
    private Long id;
    private String username;
    private String name;
    private String phone;
    private String role;
    private Boolean enabled;
    private List<OrganizerAccountVO> organizers;
    private LocalDateTime createdAt;
}
