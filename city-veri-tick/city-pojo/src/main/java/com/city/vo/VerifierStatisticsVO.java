package com.city.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class VerifierStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long totalCheckedIn;
    private Long todayCheckedIn;
}
