package com.prafta.web.attd.attd07.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyAttdDetailDeleteRequest {
    private String siteCd;
    private String userCd;
    private String attdId;
    private String reason;
}
