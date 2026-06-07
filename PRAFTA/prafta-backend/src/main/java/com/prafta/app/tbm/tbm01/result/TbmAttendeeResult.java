package com.prafta.app.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-tbm: 참석자 리스트(A4) 조회 결과.
 *
 * <p>PII 최소: 이름 + 입실시각만 노출한다(연락처/부서/끝4자리 미포함).
 */
@Getter
@Setter
public class TbmAttendeeResult {
    private String userNm;
    private String entryAt;   // yyyy-MM-dd HH:mm
}
