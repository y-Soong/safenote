package com.prafta.common.cmm.consent.service;

import java.util.List;

import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;
import com.prafta.common.cmm.consent.mapper.result.OptionalTermsResult;

/**
 * 선택약관/연동 회사 제3자 제공 동의(006) 공용 서비스.
 *
 * <p>앱(app.terms.terms01)과 웹(common.cmm.consent 컨트롤러 → 내 정보 팝업 / 로그인 게이트)이
 *    <b>동일한 판정과 저장 경로</b>를 쓰게 하기 위한 단일 출처다. 원래 Terms01ServiceImpl 안에만
 *    있던 로직을 이관했고, 앱 서비스는 본 서비스에 위임한다(앱 응답 스펙 불변).
 *
 * <p>채널별로 술어가 갈리면 "앱에서는 뜨는데 웹에서는 안 뜨는" 상태가 생기고, 그 차이가 곧
 *    하도급 공유 스냅샷의 포함/제외 차이로 이어진다(미동의자 상세행 전량 제외).
 *
 * <p>동의 변경은 모두 {@link ConsentHistoryRecorder} 를 경유한다 — 이력 없는 동의를 만들지 않는다.
 */
public interface ConsentTermsService {

    /** 선택약관 목록(현재버전 + 사용자 동의여부). cmpnyCd/userCd 는 호출측이 JWT 로 확정한 값. */
    List<OptionalTermsResult> listOptionalTerms(String cmpnyCd, String userCd);

    /**
     * 선택약관 토글(AGR_YN='Y'|'N').
     *
     * <p>대상이 선택약관(REQUIRED_YN='N' AND USE_YN='Y')인지 서버가 검증하고 현재버전을 resolve 한다
     *    (필수약관을 'N' 으로 토글해 로그인 게이트를 우회하려는 시도 차단 → TERMS_403_001).
     *
     * @param source ConsentConst.SOURCE_* (웹/앱 마이페이지=MYPAGE, 로그인 게이트=GATE)
     * @return upsert 영향행(전이 없음=0)
     */
    int toggleOptionalTerms(String cmpnyCd, String userCd, String termsId, String agrYn, String source);

    /**
     * 연동 회사 제3자 제공 동의(006) 게이트 판정.
     *
     * <p>게이트 필요 조건(모두 충족): ① 006 약관 배포(USE_YN='Y') ② 006 이 선택약관 ③ 사용자 소속
     *    사업장이 활성 연동 링크 참여(SRC 또는 DST) ④ 현재버전 <b>미응답</b>('N' 응답도 해제 대상).
     *
     * @return 게이트 노출이 필요하면 약관 메타, 불필요하면 null
     */
    ConsentTermsResult resolveSubconConsentGate(String cmpnyCd, String userCd);

    /**
     * 연동 회사 제3자 제공 동의(006) 응답 저장. 동의('Y')/미동의('N') 모두 행을 저장한다
     * (행 존재 = 응답 완료 = 게이트 해제 → 재노출 없음).
     *
     * <p>철회는 소급되지 않는다 — 기존 스냅샷(tb_cmpny_share_snapshot*)은 조회조차 하지 않는다.
     *
     * @param source ConsentConst.SOURCE_* (로그인 게이트=GATE)
     * @return upsert 영향행(전이 없음=0)
     */
    int respondSubconConsent(String cmpnyCd, String userCd, String agrYn, String source);
}
