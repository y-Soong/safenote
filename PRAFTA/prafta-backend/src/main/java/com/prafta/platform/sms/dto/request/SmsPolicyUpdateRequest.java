package com.prafta.platform.sms.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SMS 발송 임계값 수정 요청(Platform_05).
 *
 * <p>★운영자 식별자는 받지 않는다 — 토큰에서만 도출한다(IDOR 방지).
 * <p>★발송 게이트({@code prafta.sms.enabled})도 받지 않는다. 게이트는 서버 secrets 소관이며
 *    화면에서 토글하게 만들면 진실 원천이 secrets 와 DB 로 둘이 되어
 *    "코드 기본값만 보고 꺼진 줄 몰랐던" 07-31 PUSH_WORKER_ENABLED 사고가 재현된다.
 *
 * <p>Integer 로 받는 이유: 미전송(null)과 0(무제한)을 구분해야 한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SmsPolicyUpdateRequest {

    /** 번호별 연속 발송 최소 간격(초). 1~59 — 프론트 재발송 타이머 60초보다 짧아야 한다. */
    private Integer phoneWindowSec;

    private Integer phoneHourLimit;
    private Integer phoneDayLimit;

    /** IP축 실차단 여부(Y/N). ★운영 XFF 구조 계측 전에는 'N' 을 유지할 것. */
    private String ipAxisEnabledYn;

    private Integer ipHourLimit;
    private Integer ipDayLimit;
    private Integer userHourLimit;
    private Integer userDayLimit;

    /** 전역 시간당 상한. 초과 시 킬스위치 자동 발동. */
    private Integer globalHourLimit;
}
