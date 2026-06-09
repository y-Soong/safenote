package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * E11 일용직 QR 입실 요청 바디(prafta-051 R-D).
 *
 * <p>Flutter SCAN_QR 브리지가 반환한 QR raw 문자열(JSON {cmpnyCd,siteCd,userCd,qrTitle})을 가공 없이
 * qrPayload 로 전달받는다(§6 계약). 파싱/검증은 서버가 수행하며, 식별키 userCd 만 신뢰한다. 회사/사업장은
 * QR 값을 신뢰하지 않고 토큰 CMPNY + 세션 SITE 로 재검증한다(#DF-1).
 */
@Getter
@Setter
public class AdminQrScanRequest {
    private String qrPayload;
}
