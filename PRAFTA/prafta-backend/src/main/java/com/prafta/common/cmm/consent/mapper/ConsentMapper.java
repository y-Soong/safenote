package com.prafta.common.cmm.consent.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.consent.application.command.ConsentAgrUpsertCommand;
import com.prafta.common.cmm.consent.application.command.ConsentHistInsertCommand;
import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;

/**
 * 약관 동의(Consent) 공통 Mapper — PRAFTA-SUBCON-T4-02.
 *
 * <p>앱(terms01 게이트/토글)과 웹(subcon03 스냅샷 동의 필터)이 공유한다.
 * <p>USER_CD/CMPNY_CD 는 항상 호출 측이 JWT 또는 서버 산출값으로 확정해 전달한다(IDOR 차단).
 * <p>TB_TERMS_USER_AGR_HIST 는 append-only — 본 매퍼에 UPDATE/DELETE 구문을 두지 않는다(plan D8).
 */
@Mapper
public interface ConsentMapper {

    /**
     * 활성 약관(USE_YN='Y') 1건 조회 + SYS008 약관명. 없으면 null(= 약관 미배포 = 제도 미가동).
     * 게이트 판정/응답 저장 양쪽에서 현재버전 resolve 에 사용한다(클라 버전 위조 차단).
     */
    ConsentTermsResult selectActiveTerms(@Param("termsId") String termsId);

    /**
     * 약관 행의 <b>존재 여부</b>(USE_YN 무관, TB_TERMS 단독 — SYS008 조인 없음).
     *
     * <p>selectActiveTerms 의 null 은 "제도 미도입(행 자체 부재)"과 "배포 후 USE_YN='N' 으로 끔"을 구분하지
     * 못한다. 후자를 미도입으로 오인해 전원 포함하면, <b>명시적으로 미동의를 선택한 근로자의 PII 까지
     * 반출</b>된다(동의 필터 kill-switch — security M-1). 두 상태를 가르는 판정에 쓴다.
     *
     * <p>SYS008 코드 디테일 행이 지워져도 같은 fail-open 이 생기므로 조인을 두지 않는다.
     */
    int countTermsExists(@Param("termsId") String termsId);

    /**
     * 사용자 소속 사업장코드(정규 tb_user UNION ALL 일용직 tb_daily_user). 없으면 null.
     * ★ 토큰 gv_siteCd 는 로그인 시점 스냅샷이라 소속이동 발효가 즉시 반영되지 않는다 → DB 실측으로만 판정.
     */
    String selectUserSiteCd(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /**
     * 해당 (회사, 사업장) 이 활성 연동 링크에 참여 중인지(제공측 SRC 또는 수신측 DST) 건수.
     * ★ 미러(DST)만 보면 제공사(원본 사업장) 근로자가 게이트에서 누락되어 무동의 반출된다(plan §0-4/D3).
     */
    int countActiveSiteLinkMember(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /**
     * 사용자의 특정 약관/버전 현재 동의값(AGR_YN). 행이 없으면 null(= 미응답).
     * ★ 게이트 해제 판정은 "행 존재"(Y/N 무관)이므로 null 여부로 판단한다(plan D2).
     */
    String selectUserAgrYn(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("termsId") String termsId
            , @Param("termsVersion") String termsVersion);

    /**
     * 동의 현재값 조회 + <b>행 잠금</b>(FOR UPDATE) — 이력 기록 전용.
     *
     * <p>잠금 없이 읽으면 같은 사용자의 응답이 동시에 2건 들어올 때(더블탭·두 기기) 둘 다 before=null 을
     * 읽어 "최초 동의" 이력이 2행 남는다. 이력은 PII 반출의 법적 근거이므로 중복 전이는 증거 신뢰도를 해친다.
     * PK 미존재 행에 대한 FOR UPDATE 는 갭 락으로 동시 INSERT 를 직렬화한다.
     *
     * <p>★ 게이트 조회(selectUserAgrYn)에는 절대 쓰지 말 것 — 읽기 전용 경로에 잠금을 걸면 안 된다.
     */
    String selectUserAgrYnForUpdate(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("termsId") String termsId
            , @Param("termsVersion") String termsVersion);

    /**
     * 후보 사용자 중 해당 약관 <b>현재버전</b>에 AGR_YN='Y' 동의한 USER_CD 목록(T3 스냅샷 필터).
     * 정규/일용직 공통(USER_CD 는 회사 스코프 채번 → CMPNY_CD 조건으로 충분).
     */
    List<String> selectConsentedUserCds(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("termsId") String termsId
            , @Param("userCds") List<String> userCds);

    /**
     * 동의 현재상태 upsert(TB_TERMS_USER_AGR_MGMT). 영향행 반환.
     * ★ 반드시 ConsentHistoryRecorder 경유로만 호출한다(이력 우회 금지).
     */
    int upsertTermsAgr(ConsentAgrUpsertCommand command);

    /** 동의/철회 전이 이력 INSERT(TB_TERMS_USER_AGR_HIST, append-only). 영향행 반환. */
    int insertTermsAgrHist(ConsentHistInsertCommand command);
}
