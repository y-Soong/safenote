package com.prafta.web.attd.attd12.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-016-F 9-1 - 부정 출퇴근(기기 공유) 의심 케이스 1행(기기 중심).
 *
 * <p>표시 모델: 1 기기(deviceUuid) → N 사용자(members). 한 기기를 직전 7일 내에 서로 다른 사용자가
 *   APP 로그인한 "공유 의심 기기" 단위로 묶는다. members 에는 그 기기에 관여한 각 로그인(사용자·시각)이 담긴다.
 *
 * <p>의심유형(suspectType): SHARED_DEVICE(기기 공유 의심) 단일값.
 *   (기존 check-in 기반 RULE1/2/3 은 거의 미발화하여 폐기되었다.)
 *   suspectKey 는 화면 v-for key(중복 없는 식별자).
 *   nodeNm/siteNm 은 대표 멤버(첫 로그인)의 부서/사업장.
 */
@Getter
@Builder
public class FraudSuspectRow {
    private final String suspectKey;
    private final String deviceUuid;     // 전체 기기 UUID(축약은 프론트에서)
    private final String suspectType;    // SHARED_DEVICE
    private final String nodeNm;
    private final String siteNm;
    private final List<FraudSuspectMember> members;
}
