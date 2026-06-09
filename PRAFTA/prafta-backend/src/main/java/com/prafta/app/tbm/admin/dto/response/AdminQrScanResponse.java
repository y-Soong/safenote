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
}
