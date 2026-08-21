package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 연차 회수(soft cancel) 대상 부여행 단건 조회 결과(attd09, PRAFTA-031).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.7 / §8.5.8
 *
 * <p>서버 재검증용 최소 컬럼만 담는다. 회수 가능 조건:
 * {@code GRANT_TYPE LIKE 'MANUAL_%' AND GRANT_BY_TYPE='02' AND STATUS='ACTIVE' AND USED_DAYS=0 AND DEL_YN='N'}.
 * (P-11, 2026-08-21 경력인정 이원화 D-2) 단, {@code GRANT_TYPE='MANUAL_CAREER'} 는
 * {@code GRANT_BY_TYPE='01'}(자동)이어도 회수 가능 — 오입력 복구 안전망 특례. 다른 자동 부여 타입은 미적용.
 * 회수는 STATUS='CANCELED' 소프트 처리이며 USED_DAYS는 절대 갱신하지 않는다(§8.5.8 #2).
 */
@Getter
@Setter
public class LeaveRecallTargetVO {

    /** 부여 ID (PK) */
    private String grantId;

    /** 부여 대상 사용자 코드 (회수 알림 수신자) */
    private String userCd;

    /** 연차 코드 */
    private String leaveCd;

    /** 부여 분류 (MANUAL_* / STATUTORY_*) */
    private String grantType;

    /** 부여 방식 [SYS043] 01:자동 / 02:관리자 수동 */
    private String grantByType;

    /** 부여 일수 */
    private BigDecimal grantDays;

    /** 사용 일수 캐시 */
    private BigDecimal usedDays;

    /** 상태 [SYS040] ACTIVE/EXHAUSTED/EXPIRED/CANCELED */
    private String status;

    /** 삭제 여부 */
    private String delYn;
}
