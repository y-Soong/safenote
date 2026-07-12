package com.prafta.web.attd.leaveflow.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 예상 차감액 미리보기 응답 (연차 시간차 환산 개편 LC-07 — T3).
 *
 * <p>preview 값 == 실제 신청 확정값(동일 입력 기준 — 코어 산식 {@code HourlyLeaveChargeUtils} 단일 출처).
 * 단, 시간차는 advisory lock 없이 산출한 추정치라 동시 신청 직후에는 제출 시점 재계산 값과
 * 다를 수 있다(서버가 최종 판정 — UI 명세 §5-C). 9h "반차 유리" 안내 플래그는 넣지 않는다(결정 ④).
 */
@Getter
@Builder
public class LeaveDeductionPreviewResponse {

    /** 예상 차감 일수 — 이번 신청 건 부과액(시간차=그날 누적 기준 차액, 고정단위=1.0/0.5/0.25). */
    private final BigDecimal chargeDays;

    /** 하한 가드(R3) 발동 여부 — 누적이 마일스톤(D/4·D/2·D)에 도달해 고정단위 요금이 적용됨. */
    private final boolean floorApplied;

    /** 상한 캡(R4, 1.0일) 발동 여부 — D&gt;환산시간 스케줄 보호. */
    private final boolean capApplied;

    /** 잔여 부족 예상 여부 — true 면 이대로 신청 시 잔여 부족(ATTD_400_051)으로 거부될 값. */
    private final boolean insufficientBalance;

    /** 적용된 1일 환산시간(분) — 신청 대상일(WORK_YMD) 기준 분모(F4). FE "N일 H시간 M분" 조립용. */
    private final int convMinutes;

    /**
     * 발동한 마일스톤 요금(0.25=반반차/0.5=반차/1.0=종일) — FE 하한 안내 단위 분기용(additive).
     * 하한 미발동({@code floorApplied=false})/고정단위 신청이면 {@code null}.
     */
    private final BigDecimal floorDays;
}
