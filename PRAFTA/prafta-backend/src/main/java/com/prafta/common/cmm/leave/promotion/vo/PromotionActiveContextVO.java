package com.prafta.common.cmm.leave.promotion.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 앱 1차 계획서 진행 컨텍스트 1행 운반체 (PRAFTA-COM-008-A-3).
 *
 * <p>{@code LeavePromotionMapper.selectActiveFirstContext} 결과 — 진행 중 1차 촉진이 있는 사용자의
 * 보유/잔여/기준 만료일/안내노출 플래그/기준 grant 메타. 미해당이면 매퍼가 null(0행)을 반환한다.
 *
 * <p>잔여(remainingDays) = 본연차(STATUTORY_ANNUAL)+근속가산(STATUTORY_TENURE_BONUS) ACTIVE
 * (GRANT_DAYS-USED_DAYS) 합(확정-1). 미래 등록분도 USED_DAYS 에 즉시 반영되어 빠진다(미지정 잔여).
 */
@Getter
@Setter
public class PromotionActiveContextVO {

    /** 회사 코드 */
    private String cmpnyCd;

    /** 사업장 코드 (사용자 소속) */
    private String siteCd;

    /** 사용자 코드 */
    private String userCd;

    /** 역산 기준 본연차 부여 ID */
    private String baseGrantId;

    /** 역산 기준 본연차 사용가능 종료일 (YYYYMMDD) */
    private String baseAvailToDate;

    /** 본연차 총 부여 일수(보유) — 본연차+근속가산 ACTIVE GRANT_DAYS 합 */
    private BigDecimal grantedDays;

    /** 미지정(미사용) 잔여 일수 — 본연차+근속가산 ACTIVE (GRANT_DAYS-USED_DAYS) 합 */
    private BigDecimal remainingDays;

    /** 앱 로그인 안내 1회 노출 완료 여부 (Y/N) */
    private String loginNotifiedYn;
}
