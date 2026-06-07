package com.prafta.app.tbm.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.tbm.admin.application.command.AdminCompletionCommand;
import com.prafta.app.tbm.admin.application.command.AdminEduMaterialCommand;
import com.prafta.app.tbm.admin.application.command.AdminEduMaterialItemCommand;
import com.prafta.app.tbm.admin.application.command.AdminForceExitCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionCancelCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionContentCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionPwdCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionRiskCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionStateCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionTransitionCommand;
import com.prafta.app.tbm.admin.application.query.AdminAttendeeListQuery;
import com.prafta.app.tbm.admin.application.query.AdminEduMaterialDetailQuery;
import com.prafta.app.tbm.admin.application.query.AdminEduMaterialListQuery;
import com.prafta.app.tbm.admin.application.query.AdminHistoryListQuery;
import com.prafta.app.tbm.admin.application.query.AdminOptionQuery;
import com.prafta.app.tbm.admin.application.query.AdminSessionDetailQuery;
import com.prafta.app.tbm.admin.application.query.AdminSessionListQuery;
import com.prafta.app.tbm.admin.result.AdminAttendeeResult;
import com.prafta.app.tbm.admin.result.AdminContentItemResult;
import com.prafta.app.tbm.admin.result.AdminContentOptionResult;
import com.prafta.app.tbm.admin.result.AdminEduMaterialItemResult;
import com.prafta.app.tbm.admin.result.AdminEduMaterialListResult;
import com.prafta.app.tbm.admin.result.AdminEduMaterialResult;
import com.prafta.app.tbm.admin.result.AdminHistoryListResult;
import com.prafta.app.tbm.admin.result.AdminHistoryStatResult;
import com.prafta.app.tbm.admin.result.AdminMaterialTypeOptionResult;
import com.prafta.app.tbm.admin.result.AdminRiskOptionResult;
import com.prafta.app.tbm.admin.result.AdminSessionContentResult;
import com.prafta.app.tbm.admin.result.AdminSessionGuardResult;
import com.prafta.app.tbm.admin.result.AdminSessionListResult;
import com.prafta.app.tbm.admin.result.AdminSessionResult;
import com.prafta.app.tbm.admin.result.AdminSessionRiskResult;

/**
 * 001-P5: 앱 관리자 TBM 관리 Mapper(web Tbm02Mapper SQL 포팅 + 토큰/노드 스코프).
 *
 * <p>식별자(cmpnyCd/siteCd/userCd)는 token 출처(IDOR 차단). leading 콤마, #{} 바인딩, SELECT * 금지.
 */
@Mapper
public interface AppAdminTbmMapper {

    /* ===== 채번 ===== */
    String selectSessionCd(@Param("gvCmpnyCd") String gvCmpnyCd);

    /* ===== T-A1 교육관리 리스트 ===== */
    List<AdminSessionListResult> selectManageSessions(AdminSessionListQuery query);

    int selectManageSessionsCount(AdminSessionListQuery query);

    /* ===== T-A2 상세 ===== */
    AdminSessionResult selectSessionDetail(AdminSessionDetailQuery query);

    List<AdminSessionContentResult> selectSessionContents(AdminSessionDetailQuery query);

    List<AdminSessionRiskResult> selectSessionRisks(AdminSessionDetailQuery query);

    /** 상태/스코프 게이트 검증용 경량 조회(개설자 NODE_CD 포함). */
    AdminSessionGuardResult selectSessionGuard(AdminSessionDetailQuery query);

    /* ===== T-A3/T-A4 쓰기 ===== */
    void insertSession(AdminSessionCommand command);

    void updateSession(AdminSessionCommand command);

    void insertSessionContent(AdminSessionContentCommand command);

    void deleteSessionContents(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("sessionCd") String sessionCd);

    void insertSessionRisk(AdminSessionRiskCommand command);

    void deleteSessionRisks(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("sessionCd") String sessionCd);

    void upsertSessionState(AdminSessionStateCommand command);

    void cancelSession(AdminSessionCancelCommand command);

    void updateSessionPwd(AdminSessionPwdCommand command);

    /* ===== T6: 개설 사업장 서버 검증(접근가능 사업장 USE_YN='Y') ===== */
    int existsAccessibleSite(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("gvUserCd") String gvUserCd, @Param("siteCd") String siteCd);

    /* ===== T-K 보조 옵션 ===== */
    List<AdminContentOptionResult> selectContentOptions(AdminOptionQuery query);

    List<AdminRiskOptionResult> selectRiskOptions(AdminOptionQuery query);

    /* ===== R3 라이브 제어(교육 시작/종료/강제퇴실/개별 미이수/슬라이드) ===== */
    /** T1 교육 시작(OPENED→IN_PROGRESS). WHERE STATUS_CD='OPENED' 가드. 영향 행수 반환. */
    int startSession(AdminSessionTransitionCommand command);

    /** T1 교육 종료(IN_PROGRESS→COMPLETED). WHERE STATUS_CD='IN_PROGRESS' 가드. 영향 행수 반환. */
    int endSession(AdminSessionTransitionCommand command);

    /** T2 종료 자동이수 일괄(EXIT_AT IS NULL 출결 → COMPLETED). 처리 인원 수 반환. */
    int autoCompleteOnEnd(AdminSessionTransitionCommand command);

    /** T1 시작 시 동기화 상태 PLAYING UPSERT(AdminSessionStateCommand 재사용). */
    void upsertSessionStatePlaying(AdminSessionStateCommand command);

    /** 출결 리스트(LIVE/COMPLETED). web Tbm04 출결 SQL 축약 포팅. */
    List<AdminAttendeeResult> selectSessionAttendeesAdmin(AdminAttendeeListQuery query);

    /** T3 강제 퇴실(멱등 가드 EXIT_AT IS NULL). 영향 행수 반환. */
    int forceExitAttendee(AdminForceExitCommand command);

    /** T4 개별 이수처리(이수↔미이수). 영향 행수 반환. */
    int updateAttendeeCompletion(AdminCompletionCommand command);

    /** 슬라이드용 자료 세부항목(사용자 TBM tbm01 selectSessionContentItems 포팅). */
    List<AdminContentItemResult> selectSessionContentItems(AdminSessionDetailQuery query);

    /* ===== R5 교육자료 관리(web Tbm01 SQL 포팅 + 토큰/사업장 스코프) ===== */
    /** 자료 채번(web Tbm01.selectMtrlCd 포팅). */
    String selectMtrlCd(@Param("gvCmpnyCd") String gvCmpnyCd);

    /** 자료 항목 채번(web Tbm01.selectMtrlItemCd 포팅). */
    String selectMtrlItemCd(@Param("gvCmpnyCd") String gvCmpnyCd);

    /** 자료 리스트(공통 OR 접근사업장). web Tbm01.selectTbmEduInfo 포팅. */
    List<AdminEduMaterialListResult> selectAdminEduMaterials(AdminEduMaterialListQuery query);

    /** 자료 리스트 총건수. */
    int selectAdminEduMaterialsCount(AdminEduMaterialListQuery query);

    /** 자료 상세 묶음(스코프 검증용 SITE_CD 포함). web Tbm01.selectTbmEduDetail 포팅. */
    AdminEduMaterialResult selectAdminEduMaterial(AdminEduMaterialDetailQuery query);

    /** 자료 상세 항목(previewUrl 조립 포함, USE_YN='Y'). web Tbm01.selectTbmEduDetailItems 포팅. */
    List<AdminEduMaterialItemResult> selectAdminEduMaterialItems(AdminEduMaterialDetailQuery query);

    /** 자료 묶음 INSERT. */
    void insertAdminEduMaterial(AdminEduMaterialCommand command);

    /** 자료 묶음 UPDATE(CMPNY 가드, SITE_CD 보존). 영향 행수 반환. */
    int updateAdminEduMaterial(AdminEduMaterialCommand command);

    /** 자료 항목 INSERT. */
    void insertAdminEduMaterialItem(AdminEduMaterialItemCommand command);

    /** 자료 항목 일괄 소프트삭제(USE_YN='N', 수정 시 재구성). */
    void softDeleteAdminEduMaterialItems(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("mtrlCd") String mtrlCd, @Param("gvUserCd") String gvUserCd);

    /** 자료 묶음 소프트삭제(USE_YN='N', CMPNY 가드). 영향 행수 반환. */
    int softDeleteAdminEduMaterial(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("mtrlCd") String mtrlCd, @Param("gvUserCd") String gvUserCd);

    /** 자료 타입(COM003, TB_BAIM_VAL_D) 옵션. */
    List<AdminMaterialTypeOptionResult> selectMaterialTypeOptions(@Param("gvCmpnyCd") String gvCmpnyCd);

    /* ===== R6 이력(web Tbm04 SQL 포팅 + R3 스코프) ===== */
    /** 이력 리스트(STATUS_CD IN COMPLETED/CANCELLED). web Tbm04.selectHistorySessionList 포팅. */
    List<AdminHistoryListResult> selectAdminHistory(AdminHistoryListQuery query);

    /** 이력 리스트 총건수. */
    int selectAdminHistoryCount(AdminHistoryListQuery query);

    /** 이력 상단 통계(스코프 적용 전체 집계). web Tbm04.selectHistoryStat 포팅. */
    AdminHistoryStatResult selectAdminHistoryStat(AdminHistoryListQuery query);
}
