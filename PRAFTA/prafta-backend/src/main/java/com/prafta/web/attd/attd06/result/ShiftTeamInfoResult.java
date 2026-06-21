package com.prafta.web.attd.attd06.result;

/**
 * prafta-com-016-D-2/D-3: 교대팀 1건의 기간(STR/END) + 팀명 조회 결과.
 *   - 조원 추가(D-3) 시 덮어쓰기 범위(합류일+1 ~ END_DATE) 산출 기준.
 *   - PUSH(D-2) 본문 치환용 팀명(shiftTeamNm) 공급.
 */
public record ShiftTeamInfoResult(
    String strDate
    , String endDate
    , String shiftTeamNm
) {
}
