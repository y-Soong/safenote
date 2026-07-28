package com.prafta.common.cmm.consent.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 선택약관 토글 요청 본문(JSON @RequestBody) — 웹 내 정보 팝업 경로.
 *
 * <p>식별자(CMPNY_CD/USER_CD)는 본문에서 받지 않고 JWT 클레임에서만 도출한다(IDOR 차단).
 * <p>termsVersion 도 받지 않는다 — 서버가 TB_TERMS 현재버전을 resolve 한다(클라 버전 위조 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class ConsentOptionalTermsAgreeRequest {

    /** 선택약관 ID(SYS008). */
    private String termsId;

    /** 'Y'(동의) | 'N'(철회). 화이트리스트는 Param 에서 강제. */
    private String agrYn;
}
