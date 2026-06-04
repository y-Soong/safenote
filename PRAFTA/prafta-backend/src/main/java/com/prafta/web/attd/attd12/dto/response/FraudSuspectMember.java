package com.prafta.web.attd.attd12.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-003 C6 - 의심 케이스에 묶인 관련 계정 1명(사번/이름 + 그 기기에서의 출퇴근 시각).
 */
@Getter
@Builder
public class FraudSuspectMember {
    private final String userCd;
    private final String userId;
    private final String userNm;
    private final String checkInTime;   // HHMM (해당 기기·날짜의 출근)
    private final String checkOutTime;  // HHMM (nullable)
}
