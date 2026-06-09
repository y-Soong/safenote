package com.prafta.app.tbm.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.tbm.admin.application.command.AdminCancelEntryCommand;
import com.prafta.app.tbm.admin.application.command.AdminCompletionCommand;
import com.prafta.app.tbm.admin.application.command.AdminEduMaterialCommand;
import com.prafta.app.tbm.admin.application.command.AdminEduMaterialItemCommand;
import com.prafta.app.tbm.admin.application.command.AdminForceExitCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionCancelCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionContentCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionPrepareCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionRiskCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionSinglePwdCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionStateCommand;
import com.prafta.app.tbm.admin.application.command.AdminManagerEnterCommand;
import com.prafta.app.tbm.admin.application.command.AdminSessionTransitionCommand;
import com.prafta.app.tbm.admin.application.query.AdminAttendeeListQuery;
import com.prafta.app.tbm.admin.application.query.AdminEligibleRegularQuery;
import com.prafta.app.tbm.admin.application.query.AdminEntryTargetQuery;
import com.prafta.app.tbm.admin.application.query.AdminEduMaterialDetailQuery;
import com.prafta.app.tbm.admin.application.query.AdminEduMaterialListQuery;
import com.prafta.app.tbm.admin.application.query.AdminHistoryListQuery;
import com.prafta.app.tbm.admin.application.query.AdminOptionQuery;
import com.prafta.app.tbm.admin.application.query.AdminSessionDetailQuery;
import com.prafta.app.tbm.admin.application.query.AdminSessionListQuery;
import com.prafta.app.tbm.admin.result.AdminAttendeeResult;
import com.prafta.app.tbm.admin.result.AdminCancelEntrySnapshotResult;
import com.prafta.app.tbm.admin.result.AdminContentItemResult;
import com.prafta.app.tbm.admin.result.AdminContentOptionResult;
import com.prafta.app.tbm.admin.result.AdminEligibleRegularResult;
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

    /* ===== prafta-051 R-A 상태머신 재정렬 ===== */
    /** E2 교육준비 전이(DRAFT→OPENED). WHERE STATUS_CD='DRAFT' 가드. 영향 행수 반환. */
    int prepareSession(AdminSessionPrepareCommand command);

    /** E3 교육준비 연장(PREP_START_AT=NOW() 리셋). WHERE STATUS_CD='OPENED' 가드. 영향 행수 반환. */
    int extendPrep(AdminSessionTransitionCommand command);

    /** E6 입실비번 전용 재발급(OPENED 한정 ENTRY_PWD). 영향 행수 반환. */
    int updateEntryPwd(AdminSessionSinglePwdCommand command);

    /** E7 종료비번 전용 재발급(COMPLETED 한정 EXIT_PWD). 영향 행수 반환. */
    int updateExitPwd(AdminSessionSinglePwdCommand command);

    /**
     * 15분 자동 교육시작 지연평가(OPENED + PREP_START_AT 만료 → IN_PROGRESS). 멱등 UPDATE.
     * WHERE STATUS_CD='OPENED' AND PREP_START_AT 만료 가드. 영향 행수 반환(0=no-op).
     */
    int evaluateAutoStart(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("sessionCd") String sessionCd, @Param("minutes") int minutes);

    /* ===== T6: 개설 사업장 서버 검증(접근가능 사업장 USE_YN='Y') ===== */
    int existsAccessibleSite(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("gvUserCd") String gvUserCd, @Param("siteCd") String siteCd);

    /* ===== T-K 보조 옵션 ===== */
    List<AdminContentOptionResult> selectContentOptions(AdminOptionQuery query);

    List<AdminRiskOptionResult> selectRiskOptions(AdminOptionQuery query);

    /* ===== R3 라이브 제어(교육 시작/종료/강제퇴실/개별 미이수/슬라이드) ===== */
    /** T1 교육 시작(OPENED→IN_PROGRESS). WHERE STATUS_CD='OPENED' 가드. 영향 행수 반환. */
    int startSession(AdminSessionTransitionCommand command);

    /**
     * T1 교육 종료(IN_PROGRESS→COMPLETED) + 종료비번(EXIT_PWD) 최초 발급(prafta-051 E5).
     * WHERE STATUS_CD='IN_PROGRESS' 가드. 영향 행수 반환.
     */
    int endSession(AdminSessionSinglePwdCommand command);

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

    /* ===== prafta-051 R-B 입실경로(정규직 대리입실) ===== */
    /**
     * E9 정규직 대리입실 후보 검색(세션 사업장 활성 정규직 + 노드 스코프). 이름/사번 LIKE, 미입실 우선.
     * web Tbm02.selectRegularCandidates 포팅 + 노드 스코프(scopedNodeCds) 결합. LIMIT 페이징.
     */
    List<AdminEligibleRegularResult> selectEligibleRegulars(AdminEligibleRegularQuery query);

    /**
     * E10/E11 대리입실 대상 유효성. web Tbm02.countEntryTarget 포팅(userTypeCd 분기). 1=유효, 0=무효.
     * REGULAR=TB_USER(사업장+노드 스코프), DAILY=TB_DAILY_USER(사업장 단위, NODE_CD 없음 — 노드 스코프 미적용).
     */
    int countEntryTarget(AdminEntryTargetQuery query);

    /**
     * E10/E11 관리자 직접 입실 INSERT. ENTRY_TYPE_CD 는 호출부 분기(정규직 대리=MANAGER_DIRECT / 일용직 QR=MANAGER_QR_SCAN).
     * ENTRY_GPS_LAT/LON 은 세션 MANAGER_GPS_LAT/LON 복사(INSERT...SELECT, 감사용). UK 충돌 시 DuplicateKeyException.
     * INSERT...SELECT 가드(STATUS_CD='OPENED')로 영향 행수 반환(0=TOCTOU 전이로 OPENED 이탈, 멱등 거부).
     */
    int insertManagerDirectEntry(AdminManagerEnterCommand command);

    /* ===== prafta-051 R-C 이탈자 내보내기(입실취소) ===== */
    /**
     * E13 입실취소 물리삭제 직전 감사 스냅샷 조회. 대상 출결의 USER_CD/USER_TYPE_CD 코드값만 반환한다
     * (물리 DELETE 후 추적 흔적 보강용). DELETE 와 동일 @Transactional 안에서 일관 조회. 없으면 null.
     */
    AdminCancelEntrySnapshotResult selectCancelEntrySnapshot(AdminCancelEntryCommand command);

    /**
     * E13 입실취소 물리삭제(#D-RE2). 본 세션+attendanceCd+토큰 CMPNY + 미종료 + DEL_YN='N' + 세션 OPENED
     * (서브쿼리 가드 — TOCTOU 자동전이 차단). 영향 행수 반환(0=이미 취소/없음 또는 OPENED 이탈 → 멱등).
     */
    int deleteCancelEntry(AdminCancelEntryCommand command);

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
