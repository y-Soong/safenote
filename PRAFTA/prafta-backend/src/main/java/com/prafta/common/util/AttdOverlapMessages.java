package com.prafta.common.util;

import com.prafta.common.util.AttdOverlapUtils.Segment;
import com.prafta.common.util.AttdOverlapUtils.SegmentKind;

/**
 * 근무 구간 겹침(정책서 attd §7.6) 관련 <b>사용자 노출 문구</b> 생성기.
 *
 * <p>담당 범위 2가지.
 * <ol>
 *   <li>겹침 차단({@code ATTD_400_113}) 시 <b>원인별 안내 문구</b> — 4경로(앱 출근/앱 퇴근/웹 직접수정/웹 요청승인) 공용.</li>
 *   <li>승인·상세 화면의 <b>앞뒤 근무일 구간 표시 라벨</b> — 표시 문자열을 서버가 완성해 내려주기 위한 포맷터
 *       (웹 {@code daily-attd-details} / 앱 {@code admin/approval/detail} 공용).</li>
 * </ol>
 *
 * <p><b>문구 규약(2026-08-06 확정)</b>
 * <ul>
 *   <li>원인 특정에 필요한 <b>날짜 + 차수 + 시각</b>만 담는다.</li>
 *   <li><b>USER_CD / ATTD_ID / 이름 등 식별자·PII 는 절대 넣지 않는다</b>(관리자 화면이라도 토스트·로그 유출 표면이 된다).</li>
 *   <li>clamp 내부 규칙(근무일 경계·24시간 등)을 문구로 노출하지 않는다. 관리자에게 필요한 정보는
 *       "어느 날 몇 차 근무의 퇴근이 비어 있는가" 뿐이다.</li>
 * </ul>
 */
public final class AttdOverlapMessages {

    /** 원인을 특정하지 못했을 때의 기본 문구(= {@code ATTD_400_113} enum 기본 메시지와 동일). */
    private static final String FALLBACK = "다른 근무 구간과 시간이 겹칩니다.";

    private AttdOverlapMessages() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 겹침 차단 안내 문구를 원인별로 생성한다.
     *
     * <table border="1">
     *   <caption>케이스</caption>
     *   <tr><th>케이스</th><th>조건</th></tr>
     *   <tr><td>① 이웃날 미마감</td><td>상대가 OPEN 이고 dayOffset != 0</td></tr>
     *   <tr><td>② 당일 미마감</td><td>상대가 OPEN 이고 dayOffset == 0</td></tr>
     *   <tr><td>④ 실제 겹침</td><td>상대가 CLOSED</td></tr>
     *   <tr><td>④' 배치 내 겹침</td><td>상대가 아직 저장되지 않은 배치 내 다른 구간</td></tr>
     * </table>
     *
     * <p>※ 손상 구간(CORRUPT)은 판정에서 제외되므로 상대로 등장하지 않는다(§0-3 D-1).
     *
     * @param baseWorkYmd 판정 대상 근무일(yyyyMMdd)
     * @param other       충돌 상대 구간(null 이면 기본 문구)
     */
    public static String overlapMessage(String baseWorkYmd, Segment other) {
        if (other == null) {
            return FALLBACK;
        }

        // ④' 배치 내 겹침 — 아직 저장되지 않은 같은 요청 안의 다른 근무 구간.
        if (other.batchPending()) {
            String range = rangeText(other);
            return (range == null)
                    ? "이번 저장 요청의 다른 근무 구간과 시간이 겹칩니다."
                    : "이번 저장 요청의 다른 근무 구간(" + range + ")과 시간이 겹칩니다.";
        }

        String otherDay = monthDayText(other.workYmd());
        if (otherDay == null) {
            // 날짜 문구를 못 만들면 "null 1차 근무…" 처럼 새어나가므로 기본 문구로 폴백한다.
            return FALLBACK;
        }
        String otherSeq = seqLabel(other.workSeq());
        String seqPhrase = (otherSeq == null) ? "근무" : otherSeq + " 근무";

        if (other.kind() == SegmentKind.OPEN) {
            String inText = hhmmText(other.checkInTime());
            String inPhrase = (inText == null) ? "" : "(출근 " + inText + ")";

            if (other.dayOffset() != 0) {
                // ① 이웃날 미마감 — 이번 운영 장애의 주 케이스.
                String baseDay = monthDayText(baseWorkYmd);
                if (baseDay == null) {
                    return FALLBACK;
                }
                return otherDay + " " + seqPhrase + inPhrase
                        + "의 퇴근이 기록되지 않아 " + baseDay + " 근무 시간과 겹칩니다. "
                        + otherDay + " 퇴근 시각을 먼저 입력해 주세요.";
            }
            // ② 당일 미마감.
            return "같은 날 " + seqPhrase + inPhrase
                    + "의 퇴근이 기록되지 않았습니다. 퇴근 시각을 먼저 입력한 뒤 다시 시도해 주세요.";
        }

        if (other.kind() == SegmentKind.CLOSED) {
            // ④ 실제 시간 겹침.
            String range = rangeText(other);
            String rangePhrase = (range == null) ? "" : "(" + range + ")";
            return otherDay + " " + seqPhrase + rangePhrase
                    + "와 시간이 겹칩니다. 겹치지 않는 시간으로 조정해 주세요.";
        }

        return FALLBACK;
    }

    // ------------------------------------------------------------------
    // 표시 라벨 포맷터 (승인·상세 화면의 앞뒤 근무일 섹션 — 서버 완성 문자열)
    // ------------------------------------------------------------------

    /**
     * "8월 2일 (D-1)" 형태의 날짜 라벨. 대상 근무일과 같은 날이면 "(D)" 를 붙이지 않는다.
     */
    public static String dayLabel(String baseWorkYmd, String segWorkYmd) {
        String day = monthDayText(segWorkYmd);
        if (day == null) {
            return null;
        }
        Integer offset = safeDiffDays(baseWorkYmd, segWorkYmd);
        if (offset == null || offset.intValue() == 0) {
            return day;
        }
        return day + " (D" + (offset.intValue() > 0 ? "+" : "-") + Math.abs(offset.intValue()) + ")";
    }

    /** "8월 2일". 입력이 형식에 맞지 않으면 null. */
    public static String monthDayText(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) {
            return null;
        }
        try {
            int mm = Integer.parseInt(yyyymmdd.substring(4, 6));
            int dd = Integer.parseInt(yyyymmdd.substring(6, 8));
            return mm + "월 " + dd + "일";
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "1차". workSeq 가 비었으면 null. */
    public static String seqLabel(String workSeq) {
        if (workSeq == null || workSeq.isBlank()) {
            return null;
        }
        return workSeq.trim() + "차";
    }

    /** "22:01". HHmm 형식이 아니면 null. */
    public static String hhmmText(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) {
            return null;
        }
        return hhmm.substring(0, 2) + ":" + hhmm.substring(2, 4);
    }

    /**
     * 이웃날 표시용 <b>퇴근</b> 시각 문구. 상태가 {@code OPEN}(종료 시각 미상)이면 퇴근 시각 값이 남아 있어도
     * 노출하지 않는다(null).
     *
     * <p>퇴근 시각은 있는데 {@code CHECK_OUT_DATE} 가 NULL 인 손상 행은 {@code OPEN} 으로 분류되는데,
     *   {@link #stampText}는 일자가 없어도 시각만으로 문자열을 만들어 주므로 그대로 쓰면
     *   화면에 "… ~ 18:00 [퇴근 미입력]" 같은 모순 표기가 나온다. 그 조합을 막는다.
     */
    public static String checkOutTextFor(AttdOverlapUtils.SegmentKind kind,
                                         String workYmd, String outDate, String outTime) {
        if (kind == AttdOverlapUtils.SegmentKind.OPEN) {
            return null;
        }
        return stampText(workYmd, outDate, outTime);
    }

    /**
     * 출퇴근 시각 1건의 표시 문자열. 근무일과 같은 날이면 "22:01", 다른 날이면 "08-03 06:00".
     * 시각/일자가 없으면 null(= 미입력).
     */
    public static String stampText(String workYmd, String date, String time) {
        String hm = hhmmText(time);
        if (hm == null) {
            return null;
        }
        if (date == null || date.length() != 8) {
            return hm;
        }
        if (date.equals(workYmd)) {
            return hm;
        }
        return date.substring(4, 6) + "-" + date.substring(6, 8) + " " + hm;
    }

    /**
     * 확정 구간의 "09:30~18:00" 표기. 퇴근 일자가 출근 일자와 다르면 "22:00~익일 06:00".
     * 출근/퇴근 중 하나라도 표기 불가하면 null.
     */
    private static String rangeText(Segment seg) {
        String in = hhmmText(seg.checkInTime());
        String out = hhmmText(seg.checkOutTime());
        if (in == null || out == null) {
            return null;
        }
        boolean nextDay = seg.checkInDate() != null && seg.checkOutDate() != null
                && !seg.checkInDate().equals(seg.checkOutDate());
        return in + "~" + (nextDay ? "익일 " : "") + out;
    }

    /** (segWorkYmd - baseWorkYmd) 일수 차이. 산출 불가면 null. */
    private static Integer safeDiffDays(String baseWorkYmd, String segWorkYmd) {
        if (baseWorkYmd == null || baseWorkYmd.length() != 8) return null;
        if (segWorkYmd == null || segWorkYmd.length() != 8) return null;
        try {
            return Integer.valueOf(DateTimeUtils.diffDays(segWorkYmd, baseWorkYmd));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
