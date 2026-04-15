package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleWithdrawalRequest {
    private String cmpnyCd;
    private String userCd;
    /** YYYY-MM-DD (HTML date input format) */
    private String withdrawalDate;
}
