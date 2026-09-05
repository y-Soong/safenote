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
     * v2(BW2-13, plan §7 Q1): "그날 반차 휴게 넘김 분량" 이 <b>전부</b>(v1 행 — {@code BRK_WAIVE_MIN NULL} = B 해석)임을 나타내는 센티널.
     * {@link #breakDeduction} 이 {@code max(0, 공제 − W)} 를 취하므로 어떤 공제도 0 이 된다.
     */
    public static final int WAIVE_ALL = Integer.MAX_VALUE;

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
        // BW-05: 휴게 시각 없음·반차 체크 없음 → 종전 산식(휴게 분 공제)과 바이트 동일.
        return recognizedMinutes(workYmd, checkInDate, checkInTime, checkOutDate, checkOutTime,
                planStrTime, planEndTime, planBrkMin, null, null, leaveWindows, false);
    }

    /**
     * BW-05(2026-09-04, 요청서 §1-4 / plan §7 Q-3): "실제로 쉰 휴게만 공제" 확장 산식.
     *
     * <pre>
     * 인정 = (실근태 ∩ 스케줄) − 휴게 공제 − (확정 시각연차 겹침, 교집합 구간 내 한정)
     * 휴게 공제 =
     *   ① 그날 반차(01) 휴게 무시 체크 행이 있으면 0 (근무 구간 안 휴게 전부 근로로 전환 — 시뮬 rest_left=0)
     *   ② 휴게 시각이 있으면 Σ(휴게 시각 구간 ∩ 실근태∩스케줄 구간 − 연차창)   ← 유효 소정 안에서 실제로 쉰 휴게만
     *   ③ 휴게 시각이 없으면(분만 등록) 종전대로 planBrkMin 전액 공제(0 클램프)
     * </pre>
     * 시간차(02~04) 체크는 추가 면제 없음 — 편입된 휴게는 연차창(쉬는 구간) 안에 들어가므로 ②가 자동 제외한다.
     *
     * <p>휴게 시각 파싱은 반차 경계 산식(ScheduleWorkMinutesUtils.breakInterval — G-1)과 동일 규칙:
     * 종료는 {@code "2400"}=1440 인정, 종료 &lt; 시작이면 +1440 wrap, 종료 == 시작은 0폭(=시각 없음 → ③ 폴백),
     * 스케줄 구간 프레임으로 이동({@code while (bs < schStart) +1440}) 후 스케줄 구간과 클램프.
     *
     * @param planBrkStrTime       해당 차수 스케줄 휴게 시작 'HHmm'(null/blank = 시각 없음)
     * @param planBrkEndTime       해당 차수 스케줄 휴게 종료 'HHmm' 또는 '2400'(null/blank = 시각 없음)
     * @param leaveWindows         그날 확정 시각연차 창. 요소 = {START, END[, USE_UNIT_TYPE, BRK_WAIVE_YN]}
     *                             (3·4번째는 선택 — {@link #halfDayBreakWaived(List)} 가 읽는다)
     * @param halfDayBreakWaived   그날 반차(01) 휴게 무시 체크 행 존재 여부(①). v1 호환 — {@code true} = 전부 넘김
     *                             ({@link #WAIVE_ALL}) 로 위임한다. v2 호출부는 {@link #halfDayWaiveMinutes(List)} 결과를
     *                             int 오버로드로 넘긴다.
     * @return 인정시간(분, 0 이상 클램프). 산출 불가(입력 결손·파싱 실패) 시 null — 0 아님.
     */
    public static Integer recognizedMinutes(
            String workYmd,
            String checkInDate, String checkInTime,
            String checkOutDate, String checkOutTime,
            String planStrTime, String planEndTime,
            Integer planBrkMin,
            String planBrkStrTime, String planBrkEndTime,
            List<String[]> leaveWindows,
            boolean halfDayBreakWaived) {
        return recognizedMinutes(workYmd, checkInDate, checkInTime, checkOutDate, checkOutTime,
                planStrTime, planEndTime, planBrkMin, planBrkStrTime, planBrkEndTime, leaveWindows,
                halfDayBreakWaived ? WAIVE_ALL : 0);
    }

    /**
     * v2(BW2-13, plan §7 Q1): 반차 휴게 넘김 <b>분량 W</b> 반영 산식.
     *
     * <pre>
     * 휴게 공제 = max(0, (휴게 시각 ∩ 실근태∩스케줄 − 연차창) − W)     ← W = 그날 반차(01)+Y 행의 BRK_WAIVE_MIN 합
     *   · W = B(전부, v1 NULL 행 = {@link #WAIVE_ALL}) 이면 종전 ① 과 동일(공제 0)
     *   · W &lt; B 면 안 넘긴 휴게만 공제 (W = 0 기록 전용 = 미체크와 동일)
     *   · 휴게 시각 없음(분만 등록) ③ 폴백도 max(0, planBrkMin − W)
     * </pre>
     * 웹 {@code Attd_08.vue schedBreakDeduct} · Subcon03 스냅샷과 3중 파리티(산식 변경 시 동시 수정).
     *
     * @param halfDayWaiveMin 그날 반차(01)+'Y' 행의 넘긴 휴게 분 합(0 = 없음/기록 전용, {@link #WAIVE_ALL} = 전부).
     *                        호출부가 {@link #halfDayWaiveMinutes(List)} 로 산출
     */
    public static Integer recognizedMinutes(
            String workYmd,
            String checkInDate, String checkInTime,
            String checkOutDate, String checkOutTime,
            String planStrTime, String planEndTime,
            Integer planBrkMin,
            String planBrkStrTime, String planBrkEndTime,
            List<String[]> leaveWindows,
            int halfDayWaiveMin) {

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
        // BW-05: 휴게 공제 ②에서 "연차창 안의 휴게"를 제외하기 위해 절대분 창을 모아 둔다(같은 날 창끼리는 겹치지 않음 — ATTD_400_112).
        java.util.List<int[]> absWins = new java.util.ArrayList<>();
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
                absWins.add(new int[] { ws, we });
            }
        }

        // 7. 휴게 공제(BW-05 ①②③) 후 연차 차감 → 0 클램프(음수 금지).
        int brkDeduct = breakDeduction(planBrkMin, planBrkStrTime, planBrkEndTime,
                schStartM, schEndM, sM, eM, absWins, halfDayWaiveMin);
        return Math.max(0, overlap - brkDeduct - overlapLeave);
    }

    /**
     * v2(BW2-13): 그날 확정 시각연차 창 중 <b>반차(01) + 'Y'</b> 행의 넘긴 휴게 분량 W 합.
     * 요소 = {START, END, USE_UNIT_TYPE, BRK_WAIVE_YN, BRK_WAIVE_MIN}(5번째는 nullable 문자열 — 없거나 null 이면 v1 행 = 전부).
     *
     * @return 0 = 해당 행 없음(또는 전부 기록 전용) / {@link #WAIVE_ALL} = 어느 한 행이라도 v1(NULL) / 그 외 합(음수·파싱 실패는 전부로 본다)
     */
    public static int halfDayWaiveMinutes(List<String[]> leaveWindows) {
        if (leaveWindows == null) {
            return 0;
        }
        int sum = 0;
        for (String[] win : leaveWindows) {
            if (win == null || win.length < 4 || !"01".equals(win[2]) || !"Y".equals(win[3])) {
                continue;
            }
            if (win.length < 5 || win[4] == null || win[4].isBlank()) {
                return WAIVE_ALL; // v1 행(NULL) = 전부
            }
            try {
                int w = Integer.parseInt(win[4].trim());
                if (w < 0) {
                    return WAIVE_ALL;
                }
                sum += w;
            } catch (NumberFormatException e) {
                return WAIVE_ALL;
            }
        }
        return sum;
    }

    /**
     * BW-05: 그날 확정 시각연차 창 중 <b>반차(01) + 휴게 무시 체크('Y')</b> 행이 있는지.
     * 요소 = {START, END, USE_UNIT_TYPE, BRK_WAIVE_YN} (3·4번째가 없는 구 형식 요소는 false).
     */
    public static boolean halfDayBreakWaived(List<String[]> leaveWindows) {
        if (leaveWindows == null) {
            return false;
        }
        for (String[] win : leaveWindows) {
            if (win != null && win.length >= 4 && "01".equals(win[2]) && "Y".equals(win[3])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 휴게 공제 분(BW-05 §1-4 → v2 BW2-13 W 반영).
     * ② 휴게 시각 있음 → max(0, Σ(휴게 ∩ [sM,eM] − 연차창) − W). ③ 시각 없음/0폭/파싱 실패 → max(0, planBrkMin − W).
     * W = {@link #WAIVE_ALL}(v1 전부) 이면 종전 ①(공제 0)과 동일. 스케줄 구간 프레임 = 근무일 00:00 기준 분.
     */
    private static int breakDeduction(Integer planBrkMin, String brkStrTime, String brkEndTime,
                                      int schStartM, int schEndM, int sM, int eM,
                                      List<int[]> absWins, int halfDayWaiveMin) {
        int w = Math.max(0, halfDayWaiveMin);
        int brkMin = (planBrkMin == null) ? 0 : planBrkMin;
        Integer rbs = com.prafta.common.util.DateTimeUtils.hhmmToMinutes(blankToNull(brkStrTime));
        Integer rbe = com.prafta.common.util.DateTimeUtils.brkEndToMinutes(blankToNull(brkEndTime));
        if (rbs == null || rbe == null) {
            return Math.max(0, brkMin - w); // ③ 분만 등록(시각 없음/파싱 실패) — 종전 공제 − W
        }
        int bs = rbs;
        int be = rbe;
        if (be < bs) {
            be += 1440; // 자정 넘김 휴게(G-1 wrap)
        }
        if (be == bs) {
            return Math.max(0, brkMin - w); // 0폭 = 시각 없음 취급 — 종전 공제 − W
        }
        // 스케줄 구간 프레임으로 이동(야간 스케줄: 휴게 01:00~02:00 이 22:00 시작 구간보다 앞이면 +1일).
        while (bs < schStartM) {
            bs += 1440;
            be += 1440;
        }
        // 스케줄 구간 ∩ 실근태 = [sM, eM] 과 클램프.
        int cs = Math.max(bs, sM);
        int ce = Math.min(be, eM);
        if (ce <= cs) {
            return 0; // 휴게 시각이 실근태 밖(예: 반차 미체크 늦게 출근 후 14:00 출근, 휴게 12~13) — 쉬지 않았으니 공제 없음
        }
        int deduct = ce - cs;
        // ② 연차창(쉬는 구간) 안에 든 휴게는 이미 연차 겹침으로 빠지므로 이중 공제하지 않는다(유효 소정 밖 = 편입 휴게 자동 제외).
        if (absWins != null) {
            for (int[] win : absWins) {
                deduct -= Math.max(0, Math.min(ce, win[1]) - Math.max(cs, win[0]));
            }
        }
        // v2(BW2-13): 넘긴 분량 W 만큼은 근로로 전환됐으므로 공제에서 뺀다(W=전부 → 0).
        return Math.max(0, Math.max(0, deduct) - w);
    }

    /** 빈 문자열('') 방어 — 스케줄/근태 시각의 '' 는 null 과 동일하게 산출 불가로 취급한다(개발 DB 실측 존재). */
    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
