package com.prafta.common.cmm.leave.promotion.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 촉진 도래 판정용 후보 1행 운반체 (PRAFTA-COM-008-A-1).
 *
 * <p>{@code LeavePromotionMapper.selectPromotionCandidates} 결과 — AXIS7='Y' 회사의 사용자 중
 * <b>ACTIVE STATUTORY_ANNUAL grant 가 존재하는(=1년차 이상)</b> 사용자별 "역산 기준 본연차"
 * 메타다. 1년차 미만(STATUTORY_ANNUAL 미보유, 월차만)은 쿼리에서 자연 제외된다(확정-1).
 *
 * <p>{@code baseAvailToDate} = 본연차 grant 중 가장 임박한 AVAIL_TO_DATE(단일 역산 기준).
 * {@code remainingDays} = 본연차+근속가산 ACTIVE (GRANT_DAYS-USED_DAYS) 합(월차/약정 제외).
 */
@Getter
@Setter
public class PromotionCandidateVO {

    /** 회사 코드 */
    private String cmpnyCd;

    /** 사업장 코드 (사용자 소속) */
    private String siteCd;

    /** 사용자 코드 */
    private String userCd;

    /** 역산 기준 본연차 부여 ID (가장 임박한 STATUTORY_ANNUAL grant) */
    private String baseGrantId;

    /** 역산 기준 본연차 사용가능 종료일 (YYYYMMDD, 촉진 시기 역산 기준) */
    private String baseAvailToDate;

    /** 법정 본연차+근속가산 ACTIVE 잔여 합 (GRANT_DAYS-USED_DAYS) */
    private BigDecimal remainingDays;

    /**
     * 해당 회차(=BASE_AVAIL_TO_DATE 동일) FIRST 마스터 존재 여부 Y/N.
     *
     * <p>후보 쿼리에서 TB_LEAVE_PROMOTION_LOG(PROMO_STAGE='FIRST', DEL_YN='N') LEFT JOIN 1패스로
     * 싣는다(사용자 수 × 단건 조회 N+1 회피). 1차 촉구 선행 전제의 구간 판정 입력값.
     */
    private String firstMasterYn;

    /**
     * 해당 회차 FIRST 마스터의 최초 통지일 (YYYYMMDD, 마스터 부재 시 null).
     *
     * <p>2차 판정의 법정 기한(통지일 + 10일, 근로기준법 제61조) 산출 기준. 과거 데이터 방어로
     * 다건이면 MIN(NOTICED_DATE)(최초 통지일)을 채택한다.
     */
    private String firstNoticedDate;
}
