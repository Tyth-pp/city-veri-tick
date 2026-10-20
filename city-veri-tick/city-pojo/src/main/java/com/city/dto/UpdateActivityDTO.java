package com.city.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.city.enums.ActivityCategory;

import java.io.Serializable;

@Data
public class UpdateActivityDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String title;
    @NotNull
    private ActivityCategory category;
    @NotBlank
    private String content;
    @NotBlank
    private String highlights;
    @NotBlank
    private String notices;
    private String description;
    private Long coverFileId;
    private String venueName;
    private String venueAddress;
}
