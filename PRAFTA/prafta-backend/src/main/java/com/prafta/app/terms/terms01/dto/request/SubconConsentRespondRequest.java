package com.prafta.app.terms.terms01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연동 회사 제3자 제공 동의(006) 응답 본문(JSON @RequestBody) — PRAFTA-SUBCON-T4-03.
 *
 * <p>agrYn: 'Y'(동의) | 'N'(미동의/철회). 화이트리스트는 Param 에서 강제한다.
 * <p>★ termsId 를 받지 않는다 — 서버 상수(ConsentConst.THIRD_PARTY_CONSENT_TERMS_ID)로 고정하여
 *    본 경로로 임의 약관을 토글하려는 주입면을 제거한다.
 * <p>★ termsVersion 도 받지 않는다 — 서버가 TB_TERMS 현재버전을 resolve 한다(클라 버전 위조 차단).
 * <p>★ userCd/cmpnyCd 도 받지 않는다 — JWT 클레임에서만 도출한다(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class SubconConsentRespondRequest {
    private String agrYn;
}
