package com.prafta.app.chkLst.chkLst01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-011: 점검결과 저장 응답 — 화면 C(저장 완료) 요약 표시용.
 * <p>기존 200 빈 바디 → 요약 정보 포함으로 보강.
 */
@Getter
@Builder
public class SaveInspectResultResponse {

    /** 체크포인트명 (TB_CHKPT_TYPE_MGMT.CHKPT_NM) */
    private final String chkptName;

    /** 양호 항목 수 (inspectValue='Y') */
    private final int okCount;

    /** 불량 항목 수 (inspectValue='N') */
    private final int badCount;

    /** 실제 저장 항목 수 */
    private final int savedCount;

    /** 작업일자 (요청 workDate 그대로) */
    private final String workDate;
}
