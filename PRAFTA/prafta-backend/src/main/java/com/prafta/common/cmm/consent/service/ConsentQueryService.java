package com.prafta.common.cmm.consent.service;

import java.util.Collection;
import java.util.Set;

import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;

/**
 * 약관 동의(Consent) 조회 서비스 — PRAFTA-SUBCON-T4-02.
 *
 * <p>레이어링: app.terms.terms01 → common.cmm.consent, web.subcon.subcon03 → common.cmm.consent (역참조 없음).
 * <p>모든 판정은 DB 실측 기준이다(토큰 gv_siteCd 불신 — 소속이동 발효 반영 지연 방지).
 */
public interface ConsentQueryService {

    /**
     * 활성 약관(USE_YN='Y') 메타 조회. 없으면 null(= 약관 미배포 = 제도 미가동).
     *
     * @param termsId 약관ID(SYS008)
     */
    ConsentTermsResult resolveActiveTerms(String termsId);

    /**
     * 약관 행이 <b>존재하는지</b>(USE_YN 무관).
     *
     * <p>resolveActiveTerms 의 null 은 두 상태가 겹쳐 있다 — ①제도 미도입(행 자체가 없음)과
     * ②배포 후 비활성화(USE_YN='N'). ②를 ①로 오인해 전원 포함하면 <b>명시적 미동의자의 PII 까지
     * 반출</b>된다(security M-1). 동의 필터는 이 값으로 두 상태를 구분해야 한다.
     */
    boolean existsTerms(String termsId);

    /**
     * 사용자의 소속 사업장이 활성(ACTIVE) 연동 링크에 참여 중인지(제공측 SRC 또는 수신측 DST).
     * 소속 사업장을 DB(tb_user / tb_daily_user)에서 실측하여 판정한다.
     *
     * @return 활성 링크 참여 사업장 소속이면 true(= 게이트 대상)
     */
    boolean isLinkedSiteMember(String cmpnyCd, String userCd);

    /**
     * 사용자의 특정 약관/버전 현재 동의값('Y'|'N'). 미응답이면 null.
     * ★ 게이트 해제 판정은 "응답 존재"(null 여부)로 한다 — 'N'(명시적 미동의)도 해제 대상이다(plan D2).
     */
    String selectUserAgrYn(String cmpnyCd, String userCd, String termsId, String termsVersion);

    /**
     * 후보 근로자 중 연동 회사 제3자 제공 동의(006) 현재버전에 동의('Y')한 USER_CD 집합만 반환한다(T3 스냅샷 필터).
     *
     * <p>정규/일용직 공통(USER_CD 는 회사 스코프 채번). 대량 후보는 500건 청크로 분할 조회한다.
     * <p><b>fail-closed</b>: 조회 예외를 삼켜 "전원 포함"으로 fallback 하지 않는다(예외 그대로 전파 → 승인 트랜잭션 롤백).
     */
    Set<String> filterConsentedUserCds(String cmpnyCd, Collection<String> userCds);
}
