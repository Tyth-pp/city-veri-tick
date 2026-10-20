package com.city.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrganizerVerificationSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long totalTickets;
    private Long usedTickets;
    private Long unusedTickets;
    private Long cancelledTickets;
    private Long totalCheckInRecords;
}
