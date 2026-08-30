package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * tbm04-manager-sign: 종료 세션 사후 주관자 서명 응답.
 *
 * <p>managerSignedAt 은 표시용('yyyy-MM-dd HH:mm') — UPDATE 후 매퍼 재조회 값.
 * 파일코드(MANAGER_SIGN_FILE_MGMT_CD)는 앱 응답에 노출하지 않는다(클라 파일코드 신뢰 금지 원칙).
 */
@Getter
@Builder
public class AdminManagerSignResponse {
    private String sessionCd;
    private String managerSignedAt;
}
