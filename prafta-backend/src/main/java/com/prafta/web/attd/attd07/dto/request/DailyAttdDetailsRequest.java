package com.prafta.web.attd.attd07.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyAttdDetailsRequest {
	private String attdId;
    private String siteCd;
    private String userCd;
    private String userId;
    private String workYmd;
    private String nodeCd;
}
