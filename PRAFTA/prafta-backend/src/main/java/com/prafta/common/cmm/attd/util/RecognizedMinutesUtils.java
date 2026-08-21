package com.prafta.common.cmm.attd.util;

import java.util.List;

/**
 * PRAFTA-SUBCON-T8-1: 정상근무 <b>인정시간(분)</b> 파생 계산 — 웹 Attd_08 {@code recognizedMin} 산식의
 * 백엔드 미러(완전 동일 답 — <b>파리티 계약</b>. 산식 변경 시 Attd_08.vue L1068~1099 와 양쪽 동시 수정).
 *
 * <p>산식: 인정시간(분) = (실제 출퇴근 구간 ∩ 해당 차수 스케줄 구간) − 스케줄 휴게(분)
 * − (확정 시각연차 겹침 분, <b>교집합 구간 내로 한정</b> — 과차감 방지), 음수는 0 클램프.
 *
 * <p>절대분 좌표계는 {@link FixedOtMinutesUtils#dayAnchorMinutes} 재사용(일 anchor —
 * 일자 우선, 없으면 근무일 폴백). 단 +1440(자정 넘김) wrap 3종은 본 유틸이 직접 적용한다:
 * <ul>
 *   <li>wrap ① 실근태: 퇴근 &#x3C; 출근 → 퇴근 +1440 (일자 기재 여부와 무관 — Attd_08 미러)</li>
 *   <li>wrap ② 스케줄: 종료 &#x3C; 시작 → 종료 +1440 (야간 스케줄)</li>
 *   <li>wrap ③ 연차창: 종료 &#x3C;= 시작 → 종료 +1440 (★등호 포함 — 실근태/스케줄과 다름, 저장 규약 미러)</li>
 * </ul>
 * ★{@link FixedOtMinutesUtils#actualSegment} 는 사용 금지 — 퇴근&#x3C;=출근을 null 처리하는
 * 고정연장 슬롯 규약이라 wrap 파리티와 의미가 충돌한다(plan D-3).
 *
 * <p>null 계약: 산출 불가(미출근·미퇴근·스케줄 시각 결손·파싱 실패) = <b>null (0 아님)</b>.
 * 시각연차 창 없음 = 차감 0 (null 아님). 빈 문자열('')은 null 과 동일 취급.
 *
 * <p>소비처: 하도급 공유 스냅샷(Subcon03 — RECOG_MINUTES 시점 고정 저장). 순수 함수(상태 없음).
 */
public final class RecognizedMinutesUtils {

    private RecognizedMinutesUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 정상근무 인정시간(분) = (실제 출퇴근 ∩ 스케줄) − 스케줄 휴게 − (확정 시각연차 겹침, 교집합 구간 내로 한정).
     * 웹 Attd_08 recognizedMin 산식의 백엔드 미러(완전 동일 답 — 파리티 계약).
     *
     * @param workYmd      근무일 'yyyyMMdd' — null/길이 8 아님 → null 반환
     * @param checkInDate  출근 일자 'yyyyMMdd' — null/blank 면 workYmd 폴백
     * @param checkInTime  출근 시각 'HHmm'   — null/blank('' 포함) → null 반환(산출 불가)
     * @param checkOutDate 퇴근 일자 'yyyyMMdd' — null/blank 면 workYmd 폴백
     * @param checkOutTime 퇴근 시각 'HHmm'   — null/blank → null 반환
     * @param planStrTime  해당 차수 스케줄 시작 'HHmm' — null/blank → null 반환
     * @param planEndTime  해당 차수 스케줄 종료 'HHmm' — null/blank → null 반환
     * @param planBrkMin   해당 차수 스케줄 휴게(분) — null → 0 취급
     * @param leaveWindows 그날 확정 시각연차 창 목록. 각 요소 = {START_TIME, END_TIME}('HHmm').
     *                     null/빈 리스트 → 차감 0 (null 반환 아님). 파싱 불가 요소는 skip.
     * @return 인정시간(분, 0 이상 클램프). 산출 불가(입력 결손·파싱 실패) 시 null — 0 아님.
     */
    public static Integer recognizedMinutes(
            String workYmd,
            String checkInDate, String checkInTime,
            String checkOutDate, String checkOutTime,
            String planStrTime, String planEndTime,
            Integer planBrkMin,
            List<String[]> leaveWindows) {

        // 1. 실근태 절대분(일 anchor) — 어느 한쪽이라도 결손이면 산출 불가(null).
        Integer inBoxed = FixedOtMinutesUtils.dayAnchorMinutes(workYmd, checkInDate, blankToNull(checkInTime));
        Integer outBoxed = FixedOtMinutesUtils.dayAnchorMinutes(workYmd, checkOutDate, blankToNull(checkOutTime));
        if (inBoxed == null || outBoxed == null) {
            return null;
        }
        int inM = inBoxed;
        int outM = outBoxed;
        // 2. wrap ① 실근태: 퇴근 시각이 출근보다 이르면 자정 넘김으로 보고 +1일(일자 기재 여부 무관 — Attd_08 미러).
        if (outM < inM) {
            outM += 1440;
        }

        // 3. 스케줄 절대분 — 시각 결손('' 포함)이면 산출 불가(null). 스케줄 시각은 근무일 당일 anchor.
        Integer schStartBoxed = FixedOtMinutesUtils.dayAnchorMinutes(workYmd, null, blankToNull(planStrTime));
        Integer schEndBoxed = FixedOtMinutesUtils.dayAnchorMinutes(workYmd, null, blankToNull(planEndTime));
        if (schStartBoxed == null || schEndBoxed == null) {
            return null;
        }
        int schStartM = schStartBoxed;
        int schEndM = schEndBoxed;
        // 4. wrap ② 스케줄: 종료가 시작보다 이르면 익일 종료(야간 스케줄).
        if (schEndM < schStartM) {
            schEndM += 1440;
        }

        // 5. 교집합(실제 ∩ 스케줄) 길이.
        int overlap = Math.max(0, Math.min(outM, schEndM) - Math.max(inM, schStartM));

        // 6. 확정 시각연차 겹침 — 대상 구간은 교집합 [sM, eM] 로 한정한다(구간 밖 연차 과차감 방지 — Attd_08 미러).
        int sM = Math.max(inM, schStartM);
        int eM = Math.min(outM, schEndM);
        int overlapLeave = 0;
        if (eM > sM && leaveWindows != null) {
            for (String[] win : leaveWindows) {
                if (win == null || win.length < 2) {
                    continue;
                }
                Integer wsBoxed = FixedOtMinutesUtils.dayAnchorMinutes(workYmd, null, blankToNull(win[0]));
                Integer weBoxed = FixedOtMinutesUtils.dayAnchorMinutes(workYmd, null, blankToNull(win[1]));
                if (wsBoxed == null || weBoxed == null) {
                    // 파싱 불가 창은 skip(차감 제외 — 추정 금지).
                    continue;
                }
                int ws = wsBoxed;
                int we = weBoxed;
                // wrap ③ 연차창: 종료가 시작보다 이르거나 같으면 익일 종료(★등호 포함 — 저장 규약, Attd_08 L1046 미러).
                if (we <= ws) {
                    we += 1440;
                }
                overlapLeave += Math.max(0, Math.min(eM, we) - Math.max(sM, ws));
            }
        }

        // 7. 휴게·연차 차감 후 0 클램프(음수 금지).
        int brkMin = (planBrkMin == null) ? 0 : planBrkMin;
        return Math.max(0, overlap - brkMin - overlapLeave);
    }

    /** 빈 문자열('') 방어 — 스케줄/근태 시각의 '' 는 null 과 동일하게 산출 불가로 취급한다(개발 DB 실측 존재). */
    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
