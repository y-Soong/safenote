package com.prafta.common.cmm.consent;

/**
 * 약관 동의(Consent) 공통 상수 — PRAFTA-SUBCON-T4-02.
 *
 * <p>앱(app.terms.terms01)과 웹(web.subcon.subcon03)이 공유한다.
 * <p>약관ID/응답 경로 코드를 각 모듈에 흩뿌리지 않고 여기 한 곳에서만 정의한다
 *    (클라가 termsId 를 보내지 못하도록 서버 상수로 고정 — 임의 약관 토글 주입면 제거).
 */
public final class ConsentConst {

    private ConsentConst() {
    }

    /** 연동 회사 제3자 제공 동의 약관ID(SYS008 '006'). 004(필수, 개인정보 제3자 제공동의서)와 별개 약관이다. */
    public static final String THIRD_PARTY_CONSENT_TERMS_ID = "006";

    /** 응답 경로: 로그인 게이트(필수약관 일괄동의 / 제3자 제공 동의 게이트). */
    public static final String SOURCE_GATE = "GATE";

    /** 응답 경로: 마이페이지 선택약관 토글(동의/철회). */
    public static final String SOURCE_MYPAGE = "MYPAGE";

    /** 응답 경로: 가입 시(현재 미기록 — 확장 예약. 가입 경로는 필수약관만 취급하므로 006 과 무관). */
    public static final String SOURCE_JOIN = "JOIN";

    /** 동의 필터 IN 절 청크 크기(대량 기간 요청 시 바인딩 파라미터 폭주 방지). */
    public static final int USER_CD_CHUNK_SIZE = 500;
}
