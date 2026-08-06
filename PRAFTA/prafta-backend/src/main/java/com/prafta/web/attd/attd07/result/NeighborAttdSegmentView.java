package com.prafta.web.attd.attd07.result;

/**
 * 앞뒤 근무일(D-1 / D+1) 근태 구간 1건 — 일자상세 팝업 표시 전용 뷰.
 *
 * <p>겹침 가드(정책서 attd §7.6)가 이웃 근무일의 미마감 근태 때문에 발동할 때, 관리자가 화면 이동 없이
 *   원인 행을 특정할 수 있게 한다(2026-08-04 운영 실증 대응).
 *
 * <p><b>표시 문자열은 서버가 완성해 내려준다</b>(프론트 재가공·재판정 금지). {@code status} 는 서버 단일 출처다.
 *   {@code ATTD_ID} 는 화면에서 쓰지 않고 노출 시 IDOR 표면만 늘어나므로 담지 않는다.
 *
 * @param workYmd      근무일(yyyyMMdd)
 * @param dayLabel     "8월 2일 (D-1)"
 * @param workSeq      근무 차수
 * @param seqLabel     "1차"
 * @param checkInText  "22:01" (근무일과 출근일이 다르면 "08-03 06:00")
 * @param checkOutText 퇴근 표기. 미입력이면 null
 * @param status       CLOSED | OPEN | CORRUPT ({@code AttdOverlapUtils.SegmentKind})
 */
public record NeighborAttdSegmentView(
      String workYmd
    , String dayLabel
    , String workSeq
    , String seqLabel
    , String checkInText
    , String checkOutText
    , String status
) {
}
