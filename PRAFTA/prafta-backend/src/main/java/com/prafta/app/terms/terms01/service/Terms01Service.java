package com.prafta.app.terms.terms01.service;

import com.prafta.app.terms.terms01.application.param.OptionalTermsAgreeParam;
import com.prafta.app.terms.terms01.application.param.SubconConsentRespondParam;
import com.prafta.app.terms.terms01.dto.response.OptionalTermsResponse;
import com.prafta.app.terms.terms01.dto.response.PendingTermsResponse;
import com.prafta.app.terms.terms01.dto.response.SubconConsentGateResponse;
import com.prafta.app.terms.terms01.dto.response.SubconConsentRespondResponse;
import com.prafta.app.terms.terms01.dto.response.TermsAgreeResponse;

/**
 * 앱 약관(Terms) 서비스.
 *
 * <p>로그인 게이트(필수약관 미동의 조회/일괄 동의) + 마이페이지 선택약관(목록/토글)
 *    + 연동 회사 제3자 제공 동의 게이트(SUBCON-T4: 판정/응답).
 *    모든 식별은 USER_CD(JWT)로만 수행한다(IDOR 차단).
 * <p>동의 변경은 전부 ConsentHistoryRecorder 를 경유해 전이 이력을 남긴다(요청서 §3).
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

    /**
     * 연동 회사 제3자 제공 동의(006) 로그인 게이트 판정(SUBCON-T4).
     *
     * <p>게이트 필요 조건(모두 충족): ① 006 약관 배포(USE_YN='Y') ② 사용자 소속 사업장이
     *    활성 연동 링크 참여(SRC 또는 DST) ③ 현재버전 <b>미응답</b>(행 부재 — 'N' 응답도 해제 대상).
     */
    SubconConsentGateResponse selectSubconConsentGate(String cmpnyCd, String userCd);

    /**
     * 연동 회사 제3자 제공 동의(006) 응답 저장(SUBCON-T4). 동의('Y')/미동의('N') 모두 행을 저장한다
     * (행 존재 = 응답 완료 = 게이트 해제 → 재노출 없음).
     *
     * <p>철회는 소급되지 않는다 — 기존 스냅샷은 <b>조회조차 하지 않는다</b>(아무것도 하지 않음).
     */
    SubconConsentRespondResponse respondSubconConsent(SubconConsentRespondParam param);
}
