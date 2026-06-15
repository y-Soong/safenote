package com.prafta.app.siteops.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * J1-7(prafta-app-025) 관리자 현장 일용직 QR 출퇴근 등록 요청 바디.
 *
 * <p>Flutter/Vue QR 스캐너가 읽은 QR raw 문자열(JSON {cmpnyCd,siteCd,userCd,...})을 가공 없이
 * qrPayload 로 전달받는다. 파싱/검증은 서버가 수행하며 식별키 userCd 만 신뢰한다(회사/사업장은
 * QR 값을 신뢰하지 않고 토큰 CMPNY + 현장전환 SITE 로 재검증).
 *
 * <p>siteCd 는 화면이 현재 선택한 현장전환 사업장(currentSiteCd)이다. 서버는 이 사업장이 토큰
 * 사용자의 접근가능 사업장(USE_YN='Y') 멤버십인지 재검증한 뒤 권위로 사용한다(IDOR 방어).
 */
@Getter
@Setter
public class SiteOpsQrRequest {
    private String qrPayload;
    private String siteCd;
}
