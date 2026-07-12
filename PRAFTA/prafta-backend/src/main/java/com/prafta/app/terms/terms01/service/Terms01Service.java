package com.prafta.app.terms.terms01.service;

import com.prafta.app.terms.terms01.application.param.OptionalTermsAgreeParam;
import com.prafta.app.terms.terms01.dto.response.OptionalTermsResponse;
import com.prafta.app.terms.terms01.dto.response.PendingTermsResponse;
import com.prafta.app.terms.terms01.dto.response.TermsAgreeResponse;

/**
 * 앱 약관(Terms) 서비스.
 *
 * <p>로그인 게이트(필수약관 미동의 조회/일괄 동의) + 마이페이지 선택약관(목록/토글).
 *    모든 식별은 USER_CD(JWT)로만 수행한다(IDOR 차단).
 */
public interface Terms01Service {

    /** 미동의 필수약관 목록 조회(빈 목록이면 게이트 불필요). cmpnyCd/userCd 는 JWT 도출 회사 스코프. */
    PendingTermsResponse selectPendingRequiredTerms(String cmpnyCd, String userCd);

    /**
     * 미동의 필수약관 일괄 동의(AGR_YN='Y' upsert).
     * 서버가 미동의 목록을 직접 재산출하여 전부 동의 처리한다(클라 목록 불신, 멱등).
     */
    TermsAgreeResponse agreeRequiredTerms(String cmpnyCd, String userCd);

    /** 선택약관 목록 조회(현재버전 + 사용자 동의여부). cmpnyCd/userCd 는 JWT 도출 회사 스코프. */
    OptionalTermsResponse selectOptionalTerms(String cmpnyCd, String userCd);

    /**
     * 선택약관 토글(AGR_YN='Y'|'N' upsert).
     * 대상이 선택약관(REQUIRED_YN='N' AND USE_YN='Y')인지 검증 후 현재버전에 적용한다.
     */
    TermsAgreeResponse toggleOptionalTerms(OptionalTermsAgreeParam param);
}
