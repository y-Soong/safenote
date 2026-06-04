package com.prafta.web.attd.attd12.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-003 C6 - 부정 출퇴근 의심 케이스 1행.
 *
 * <p>의심유형(suspectType):
 *   <ul>
 *     <li>RULE1 — 한 기기(deviceUuid) → 같은 날 서로 다른 2계정 이상 출퇴근(결정타). members 2명+.</li>
 *     <li>RULE2 — 본인 평소(baseline) 기기와 다른 기기에서 출근. members 1명.</li>
 *     <li>RULE3 — 로그인 이력에 한 번도 없던 신규 기기에서 출근. members 1명.</li>
 *   </ul>
 *   suspectKey 는 화면 v-for key(중복 없는 식별자).
 */
@Getter
@Builder
public class FraudSuspectRow {
    private final String suspectKey;
    private final String workYmd;        // YYYYMMDD
    private final String deviceUuid;     // 전체 기기 UUID(축약은 프론트에서)
    private final String suspectType;    // RULE1 / RULE2 / RULE3
    private final String nodeNm;
    private final String siteNm;
    private final List<FraudSuspectMember> members;
}
