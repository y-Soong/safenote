package com.prafta.app.tbm.admin.service;

import com.prafta.app.tbm.admin.application.param.AdminAttendeeListParam;
import com.prafta.app.tbm.admin.application.param.AdminCancelEntryParam;
import com.prafta.app.tbm.admin.application.param.AdminCompletionParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialListParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminEligibleRegularParam;
import com.prafta.app.tbm.admin.application.param.AdminForceExitParam;
import com.prafta.app.tbm.admin.application.param.AdminManagerDirectParam;
import com.prafta.app.tbm.admin.application.param.AdminQrScanParam;
import com.prafta.app.tbm.admin.application.param.AdminHistoryListParam;
import com.prafta.app.tbm.admin.application.param.AdminLiveTransitionParam;
import com.prafta.app.tbm.admin.application.param.AdminManagerSignParam;
import com.prafta.app.tbm.admin.application.param.AdminOptionParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionCancelParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionListParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionPrepareParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionPwdParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionUpdateParam;
import com.prafta.app.tbm.admin.dto.response.AdminAttendeeListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminCompletionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminContentOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialDetailResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialSaveResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEligibleRegularResponse;
import com.prafta.app.tbm.admin.dto.response.AdminForceExitResponse;
import com.prafta.app.tbm.admin.dto.response.AdminManagerDirectResponse;
import com.prafta.app.tbm.admin.dto.response.AdminQrScanResponse;
import com.prafta.app.tbm.admin.dto.response.AdminHistoryListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminLiveTransitionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminManagerSignResponse;
import com.prafta.app.tbm.admin.dto.response.AdminMaterialTypeOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminRiskOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionContentsResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionDetailResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionPwdResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionSaveResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSiteOptionResponse;
import com.prafta.common.dto.TokenInfo;

/** 001-P5: 앱 관리자 TBM 관리 서비스(R1+R2). */
public interface AppAdminTbmService {

    AdminSessionListResponse selectSessionList(AdminSessionListParam param);

    AdminSessionDetailResponse selectSessionDetail(AdminSessionDetailParam param);

    AdminSessionSaveResponse saveSession(AdminSessionSaveParam param);

    void updateSession(AdminSessionUpdateParam param);

    void cancelSession(AdminSessionCancelParam param);

    /* ===== prafta-051 R-A 상태머신 재정렬 ===== */
    /** E2 교육준비(OPENED) 전이(DRAFT→OPENED). 입실비번/관리자좌표/PREP_START_AT 확정. 개설자만. */
    AdminLiveTransitionResponse prepareSession(AdminSessionPrepareParam param);

    /** E3 교육준비 연장(PREP_START_AT 리셋). 개설자만. */
    void extendPrep(AdminLiveTransitionParam param);

    /** E6 입실비번 전용 재발급(OPENED 한정). */
    AdminSessionPwdResponse regenerateEntryPassword(AdminSessionPwdParam param);

    /** E7 종료비번 전용 재발급(COMPLETED 한정). */
    AdminSessionPwdResponse regenerateExitPassword(AdminSessionPwdParam param);

    AdminContentOptionResponse selectContentOptions(AdminOptionParam param);

    AdminRiskOptionResponse selectRiskOptions(AdminOptionParam param);

    AdminSiteOptionResponse selectSiteOptions(TokenInfo tokenInfo);

    /* ===== R3 라이브 제어 ===== */
    AdminLiveTransitionResponse startSession(AdminLiveTransitionParam param);

    /** T1 교육 종료(IN_PROGRESS→COMPLETED). tbm04-manager-sign: 주관자 서명(multipart 'item') 필수 — 서버 강제. */
    AdminLiveTransitionResponse endSession(AdminManagerSignParam param);

    /** tbm04-manager-sign: 종료(COMPLETED) 세션 사후 주관자 서명 등록(개설자 본인·서명 NULL·재서명 불가). */
    AdminManagerSignResponse signCompletedSession(AdminManagerSignParam param);

    /**
     * tbm04-manager-sign: 주관자 서명 이미지 스트림(앱 관리자 이력 상세 열람용).
     * <p>열람 권한 = 이력 상세에 진입 가능한 관리자(스코프 내) 전원. 등록(사후서명)과 달리 개설자 한정이 아니다.
     */
    com.prafta.common.cmm.file.application.model.FileBytesResult loadManagerSignImage(AdminSessionDetailParam param);

    AdminAttendeeListResponse selectAttendees(AdminAttendeeListParam param);

    AdminForceExitResponse forceExitAttendee(AdminForceExitParam param);

    AdminCompletionResponse updateAttendeeCompletion(AdminCompletionParam param);

    AdminSessionContentsResponse selectSessionContents(AdminSessionDetailParam param);

    /* ===== prafta-051 R-B 입실경로(정규직 대리입실) ===== */
    /** E9 정규직 대리입실 후보 검색(세션 사업장/노드 스코프 내 정규직, 이름/사번). */
    AdminEligibleRegularResponse selectEligibleRegulars(AdminEligibleRegularParam param);

    /** E10 정규직 관리자 대리입실(MANAGER_DIRECT). 세션 OPENED + 스코프 재검증 + UK 멱등. */
    AdminManagerDirectResponse managerDirectEnter(AdminManagerDirectParam param);

    /* ===== prafta-051 R-D 입실경로(일용직 QR 입실) ===== */
    /** E11 일용직 QR 입실(MANAGER_QR_SCAN). QR 페이로드 파싱(userCd) + 세션 OPENED + 일용직 유효성 재검증 + UK 멱등. */
    AdminQrScanResponse qrScanEnter(AdminQrScanParam param);

    /* ===== prafta-051 R-C 이탈자 내보내기(입실취소) ===== */
    /** E13 입실취소(GPS 이탈자 내보내기). 세션 OPENED + 스코프 재검증 + 물리삭제(#D-RE2). 재입실 자유. */
    void cancelEntry(AdminCancelEntryParam param);

    /* ===== R5 교육자료 관리 ===== */
    AdminEduMaterialListResponse selectEduMaterials(AdminEduMaterialListParam param);

    AdminEduMaterialDetailResponse selectEduMaterialDetail(AdminEduMaterialDetailParam param);

    AdminEduMaterialSaveResponse saveEduMaterial(AdminEduMaterialSaveParam param);

    AdminEduMaterialSaveResponse updateEduMaterial(AdminEduMaterialSaveParam param);

    void deleteEduMaterial(AdminEduMaterialDetailParam param);

    AdminMaterialTypeOptionResponse selectMaterialTypeOptions(TokenInfo tokenInfo);

    /* ===== R6 이력 ===== */
    AdminHistoryListResponse selectHistory(AdminHistoryListParam param);
}
