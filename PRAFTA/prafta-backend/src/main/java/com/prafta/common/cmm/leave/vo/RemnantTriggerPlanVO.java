package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * PC-05(D5·D6): 짜투리 보전 발동 계획 — 판정({@code evaluateTrigger}) 결과 운반체.
 *
 * <p>발동 조건(D5 ⓐ~ⓔ) 전부 충족 시에만 생성된다(미충족이면 서비스가 null 반환).
 * preview(안내)와 submit(기록)이 같은 판정 산출물을 공유한다.
 *
 * @param chargeDays   최소단위 정상 요금(일) — 신청 시점 산출값
 * @param remnantDays  발동 시 전액 차감할 대상 5종 합산 잔여(일) — use 행 합과 일치(원장 음수 금지 D6)
 * @param coverDays    회사 부담분(일) = chargeDays − remnantDays
 * @param coverMinutes 회사 부담분(분) = coverDays × convMinutes (DOWN 절사 — 표기·preview 용)
 * @param convMinutes  발동 시 개인 분모(분, 산출 불가 사용자는 480 폴백 기록) — 회수 재계산 기준 고정
 * @param charges      잔여 전액의 부여별 분할 차감 계획(만료 임박순, 부여의 LEAVE_CD 귀속)
 */
public record RemnantTriggerPlanVO(
      BigDecimal chargeDays
    , BigDecimal remnantDays
    , BigDecimal coverDays
    , int coverMinutes
    , int convMinutes
    , List<RemnantGrantChargeVO> charges
) {

    /**
     * 부여 1건 차감 계획. use 행의 LEAVE_CD 는 차감 GRANT 의 코드로 귀속한다
     * (5종 GRANT 를 넘나드는 차감 — selectAutoDeductibleGrant 자동 차감 선례 미러).
     */
    public record RemnantGrantChargeVO(
          String grantId
        , String leaveCd
        , BigDecimal days
    ) {
    }
}
