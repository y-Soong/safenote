package com.prafta.common.cmm.consent.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연동 회사 제3자 제공 동의(006) 응답 본문(JSON @RequestBody) — 웹 로그인 게이트 경로.
 *
 * <p>★ termsId 를 받지 않는다 — 서버 상수(ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID) 고정.
 * <p>★ termsVersion/userCd/cmpnyCd 도 받지 않는다 — 서버 resolve / JWT 클레임에서만 도출.
 */
@Getter
@Setter
@NoArgsConstructor
public class ConsentSubconRespondRequest {

    /** 'Y'(동의) | 'N'(미동의). 미동의도 정상 응답이며 저장 대상이다(= 게이트 해제). */
    private String agrYn;
}
