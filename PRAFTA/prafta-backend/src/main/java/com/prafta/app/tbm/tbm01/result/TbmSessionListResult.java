package com.prafta.app.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-tbm: 사용자 앱 TBM 세션 리스트(탭별 A1/A2/A3) 조회 결과.
 *
 * <p>탭에 따라 일부 필드만 채워진다.
 *   <ul>
 *     <li>A1(참석가능): sessionCd/title/managerUserNm/openedAt</li>
 *     <li>A2(교육중): + startedAt</li>
 *     <li>A3(교육완료): + startedAt/endedAt/completionStatusCd</li>
 *   </ul>
 * <p>일시는 SQL DATE_FORMAT 으로 문자열 가공한다(클라이언트 표시 그대로).
 */
@Getter
@Setter
public class TbmSessionListResult {
    private String sessionCd;
    private String title;
    private String managerUserNm;
    private String openedAt;             // yyyy-MM-dd HH:mm
    private String startedAt;            // yyyy-MM-dd HH:mm (A2/A3)
    private String endedAt;              // yyyy-MM-dd HH:mm (A3, IFNULL(S.ENDED_AT, AT.EXIT_AT))
    private String completionStatusCd;   // SYS053 COMPLETED/NOT_COMPLETED (A3)
}
