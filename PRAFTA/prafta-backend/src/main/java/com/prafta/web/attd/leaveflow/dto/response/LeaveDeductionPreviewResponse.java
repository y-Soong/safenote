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

    /**
     * PC-05(D6): 짜투리 보전 발동 예상 여부 — true 면 이대로 신청 시 잔여 전액({@code remnantDays})이
     * 차감되고 부족분({@code companyCoverMinutes})은 회사 부담으로 기록된다.
     * 발동 예상 시 {@code insufficientBalance}=false 로 내린다(신청은 성공하므로 — FE 는 부족 경고
     * 대신 발동 안내를 표시, UI-C).
     */
    private final boolean remnantTriggered;

    /** 발동 시 실제 차감될 잔여 전액(일). 미발동이면 {@code null}. */
    private final BigDecimal remnantDays;

    /** 회사 부담분(분 — coverDays × 본인 분모, DOWN 절사). 미발동이면 {@code null}. */
    private final Integer companyCoverMinutes;

    /**
     * HB-03: 반차 경계 시각(HHMM) — 근로를 절반으로 나누는 시각(휴게 제외 누적 기준).
     * 반차('01') preview 에서만 채워지며 그 외 단위/산출 불가면 {@code null}(additive).
     */
    private final String halfDayBoundaryTime;

    /** HB-03: 시작기준(늦게 출근) 반차가 쉬는 구간 "HHMM~HHMM". 반차 외/산출 불가면 {@code null}. */
    private final String halfStartPartRange;

    /** HB-03: 종료기준(일찍 퇴근) 반차가 쉬는 구간 "HHMM~HHMM". 반차 외/산출 불가면 {@code null}. */
    private final String halfEndPartRange;

    // ===== BW-04 휴게 무시(앱 LeaveDeductionPreviewResponse 미러) =====
    /** 체크 요청이 저장 시각에 반영됐는지 'Y'/'N'. 미체크 요청은 'N'. */
    private final String brkWaiveAppliedYn;
    /** 체크했지만 시각 불변·요청 기록만 'Y'/'N'(휴게가 쉬는 구간 안 / 분만 타입 G-2 / 붙은 휴게 없음). */
    private final String brkWaiveRecordOnlyYn;
    /** 시간차 체크 시 합친 쉬는 구간 "HHMM~HHMM"(반차는 day-schedule 값 사용). 그 외 null. */
    private final String brkWaiveExemptRange;
    /** 쉬는 구간에 편입된 휴게 분(시간차 체크 시). 그 외 null. */
    private final Integer brkWaivedMinutes;
    /** 시간차 차감 분 = 신청 − 휴게 겹침(체크 시). 미체크 시간차는 신청 분. 그 외 null. */
    private final Integer brkChargeMinutes;
    /** BW-06: 법정 휴게 하한 경고 'Y'/'N'(차단 없음). */
    private final String brkLegalWarnYn;
    /** BW-06: 경고 문구(서버 생성). 경고 없으면 null. */
    private final String brkLegalWarnMsg;
}
