package com.prafta.web.risk.riskai01.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.risk.riskai01.application.model.RiskAiDerivationRow;
import com.prafta.web.risk.riskai01.application.model.RiskAssessmentAiSource;

@Mapper
public interface RiskAi01Mapper {

    // 사업장 접근 권한 확인 (tb_user_site_auth 매핑, USE_YN='Y'). 1 이상이면 접근 가능 (risklink01 패턴 복제)
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );

    // AI 도출 입력 원천(구조화 값 + 사진코드). 대상 없으면 null → AI_404_001
    // (INIT_DESC/ASSESSMENT_DESC/INIT_FILE_MGMT_CD + 공정명/위험성분류/유해요인명 조인)
    RiskAssessmentAiSource selectAssessmentForAi(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
    );

    // 도출 결과/카운터 1행 조회. 행 없으면 null(초기 상태)
    RiskAiDerivationRow selectDerivation(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
    );

    // 도출 행 멱등 생성(이미 있으면 무시). 최초 호출 전 카운터 원자증가 대상 행 보장
    int insertDerivationIfAbsent(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
        , @Param("userCd") String userCd
    );

    // 이미지분석 대화이력(IMG_CHAT_JSON) 저장(v2 멀티턴 채팅). [{role,text}] 텍스트만 저장
    int updateImgChatJson(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
        , @Param("imgChatJson") String imgChatJson
        , @Param("userCd") String userCd
    );

    // 관리자 보완 설명(SUPP_DESC) 저장(v2.1). blur 자동저장 + AI 실행 시 동반저장
    int updateSuppDesc(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
        , @Param("suppDesc") String suppDesc
        , @Param("userCd") String userCd
    );

    // 이미지 이해 확정 여부(IMG_CONFIRMED) 저장(v3 confirm-image)
    int updateImgConfirmed(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
        , @Param("imgConfirmed") String imgConfirmed
        , @Param("userCd") String userCd
    );

    // 도출 행 삭제(초기화 — 대화이력/도출결과/보완설명 전체. 행 없으면 0건 삭제로 무해)
    int deleteDerivation(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
    );

    // 미저장(SAVED_YN='N') 도출 행 삭제(commit-on-save — 팝업 닫기/재오픈 시 미확정 작업분 정리).
    // 확정(Y) 행은 보존한다. 행 없으면 0건 삭제로 무해
    int deleteUnsavedDerivation(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
    );

    // 저장 확정 여부(SAVED_YN) 갱신(commit-on-save — 평가 저장 성공 시 'Y' 확정). 행 없으면 0건 갱신으로 무해
    int updateSavedYn(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
        , @Param("savedYn") String savedYn
        , @Param("userCd") String userCd
    );

    // 도출 결과 저장(v3: HAZARD_JSON 그룹 구조 + CITATION_JSON + VERBATIM_JSON + ABSTAINED.
    // MEASURE_JSON 은 NULL 클리어. VERBATIM_JSON 은 verbatim 참고 원문 패스스루 — 그라운딩 개선 C)
    int updateDeriveResult(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
        , @Param("hazardJson") String hazardJson
        , @Param("citationJson") String citationJson
        , @Param("verbatimJson") String verbatimJson
        , @Param("abstained") String abstained
        , @Param("userCd") String userCd
    );
}
