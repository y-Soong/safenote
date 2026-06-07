package com.prafta.app.tbm.admin.service;

import com.prafta.app.tbm.admin.application.param.AdminAttendeeListParam;
import com.prafta.app.tbm.admin.application.param.AdminCompletionParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialListParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminForceExitParam;
import com.prafta.app.tbm.admin.application.param.AdminHistoryListParam;
import com.prafta.app.tbm.admin.application.param.AdminLiveTransitionParam;
import com.prafta.app.tbm.admin.application.param.AdminOptionParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionCancelParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionListParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionPwdParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionUpdateParam;
import com.prafta.app.tbm.admin.dto.response.AdminAttendeeListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminCompletionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminContentOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialDetailResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialSaveResponse;
import com.prafta.app.tbm.admin.dto.response.AdminForceExitResponse;
import com.prafta.app.tbm.admin.dto.response.AdminHistoryListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminLiveTransitionResponse;
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

    AdminSessionPwdResponse regeneratePassword(AdminSessionPwdParam param);

    AdminContentOptionResponse selectContentOptions(AdminOptionParam param);

    AdminRiskOptionResponse selectRiskOptions(AdminOptionParam param);

    AdminSiteOptionResponse selectSiteOptions(TokenInfo tokenInfo);

    /* ===== R3 라이브 제어 ===== */
    AdminLiveTransitionResponse startSession(AdminLiveTransitionParam param);

    AdminLiveTransitionResponse endSession(AdminLiveTransitionParam param);

    AdminAttendeeListResponse selectAttendees(AdminAttendeeListParam param);

    AdminForceExitResponse forceExitAttendee(AdminForceExitParam param);

    AdminCompletionResponse updateAttendeeCompletion(AdminCompletionParam param);

    AdminSessionContentsResponse selectSessionContents(AdminSessionDetailParam param);

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
