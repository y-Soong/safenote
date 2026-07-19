package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** E11 일용직 QR 입실 응답(prafta-051 R-D). userTypeCd='DAILY', userCd=QR 일용직 USER_CD. */
@Getter
@Builder
public class AdminQrScanResponse {
    private String sessionCd;
    private String userTypeCd;
    private String userCd;
    /**
     * PRAFTA-SUBCON-T5: 스캔 대상자의 소속 표시명(스캔 주체=개설사 기준 1차 relabel).
     * 데이터는 실제 CMPNY_CD 로 기록하되 화면 표시만 접는다(요청서 §3.2 ①). 자사 대상은 자사명.
     */
    private String affilCmpnyNm;
}
