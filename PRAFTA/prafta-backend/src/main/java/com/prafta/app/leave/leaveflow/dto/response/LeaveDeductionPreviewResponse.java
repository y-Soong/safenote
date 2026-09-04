package com.prafta.app.leave.leaveflow.dto.response;

import java.math.BigDecimal;

/**
 * 앱 예상 차감액 미리보기 응답 (연차 시간차 환산 개편 LC-07 — T3).
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.dto.response.LeaveDeductionPreviewResponse} 미러(키명 동일).
 * preview 값 == 실제 신청 확정값(코어 산식 단일 출처). 시간차는 lock 없는 추정치라 동시 신청 직후엔
 * 제출 시점 재계산 값과 다를 수 있다(서버가 최종 판정). 9h "반차 유리" 플래그는 없다(결정 ④).
 *
 * @param chargeDays          예상 차감 일수(이번 신청 건 부과액 — 시간차=그날 누적 기준 차액, 고정단위=1.0/0.5/0.25)
 * @param floorApplied        하한 가드(R3) 발동 여부
 * @param capApplied          상한 캡(R4, 1.0일) 발동 여부
 * @param insufficientBalance 잔여 부족 예상 여부(true 면 이대로 신청 시 ATTD_400_051 거부될 값)
 * @param convMinutes         적용된 1일 환산시간(분, 신청 대상일 기준 F4) — FE "N일 H시간 M분" 조립용
 * @param floorDays           발동한 마일스톤 요금(0.25=반반차/0.5=반차/1.0=종일) — FE 하한 안내 단위 분기용(additive).
 *                            하한 미발동/고정단위 신청이면 {@code null}
 * @param remnantTriggered    PC-05(D6): 짜투리 보전 발동 예상 여부 — true 면 잔여 전액 차감 + 회사 부담.
 *                            발동 예상 시 insufficientBalance=false 로 내린다(신청 성공 예정 — UI-D)
 * @param remnantDays         발동 시 실제 차감될 잔여 전액(일). 미발동 {@code null}
 * @param companyCoverMinutes 회사 부담분(분 — coverDays × 본인 분모, DOWN 절사). 미발동 {@code null}
 * @param halfDayBoundaryTime 반차 경계 시각(HHMM) — 반차 단위 preview 에서만 채워진다. 그 외 {@code null}(HB-03, additive)
 * @param halfStartPartRange  시작기준(늦게 출근) 반차가 쉬는 구간 "HHMM~HHMM". 반차 외/산출 불가면 {@code null}
 * @param halfEndPartRange    종료기준(일찍 퇴근) 반차가 쉬는 구간 "HHMM~HHMM". 반차 외/산출 불가면 {@code null}
 * @param noGrantOnDate       2026-08-17: 잔여 부족의 원인이 "대상 날짜 기준 유효한 부여 0건"인 경우 true —
 *                            일반 잔여 부족과 구분해 FE 가 날짜 안내 문구를 띄운다(부여 시작 전 날짜 신청 혼란 방지).
 *                            true 면 insufficientBalance 도 항상 true 다(신청 시 ATTD_400_204 거부될 값). additive
 * @param grantAvailFromDate  noGrantOnDate=true 일 때 안내용: 대상 날짜 이후 가장 이른 부여 시작일(YYYYMMDD).
 *                            이후 부여가 없으면 {@code null}
 * @param brkWaiveAppliedYn   BW-04: 휴게 무시 체크 요청이 저장 시각에 실제 반영됐는지('Y'/'N'). 미체크 요청은 'N'
 * @param brkWaiveRecordOnlyYn BW-04: 체크했지만 시각 불변·요청 기록만('Y'/'N') — 휴게가 쉬는 구간 안 / 분만 타입(G-2) / 붙은 휴게 없음
 * @param brkWaiveExemptRange BW-04: 시간차 체크 시 합친 쉬는 구간 "HHMM~HHMM"(반차는 day-schedule 값 사용). 그 외 {@code null}
 * @param brkWaivedMinutes    BW-04: 쉬는 구간에 편입된 휴게 분(시간차 체크 시). 그 외 {@code null}
 * @param brkChargeMinutes    BW-04: 시간차 차감 분 = 신청 − 휴게 겹침(체크 시). 미체크 시간차는 신청 분. 그 외 {@code null}
 * @param brkLegalWarnYn      BW-06: 법정 휴게 하한 경고('Y'/'N', §1-3 — 차단 없음)
 * @param brkLegalWarnMsg     BW-06: 경고 문구(서버 생성 — 클라 재계산 금지). 경고 없으면 {@code null}
 */
public record LeaveDeductionPreviewResponse(
      BigDecimal chargeDays
    , boolean floorApplied
    , boolean capApplied
    , boolean insufficientBalance
    , int convMinutes
    , BigDecimal floorDays
    , boolean remnantTriggered
    , BigDecimal remnantDays
    , Integer companyCoverMinutes
    , String halfDayBoundaryTime
    , String halfStartPartRange
    , String halfEndPartRange
    , boolean noGrantOnDate
    , String grantAvailFromDate
    , String brkWaiveAppliedYn
    , String brkWaiveRecordOnlyYn
    , String brkWaiveExemptRange
    , Integer brkWaivedMinutes
    , Integer brkChargeMinutes
    , String brkLegalWarnYn
    , String brkLegalWarnMsg
) {
}
