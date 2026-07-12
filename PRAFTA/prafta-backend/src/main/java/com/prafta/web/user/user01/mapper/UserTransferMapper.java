package com.prafta.web.user.user01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user01.application.command.TransferReservationInsertCommand;
import com.prafta.web.user.user01.result.PartialLeaveTimeResult;
import com.prafta.web.user.user01.result.SchSegmentTimeResult;
import com.prafta.web.user.user01.result.TransferNoticeResult;
import com.prafta.web.user.user01.result.TransferReservationExecRow;
import com.prafta.web.user.user01.result.UserTransferBasicResult;

/**
 * 사용자 소속이동(Terminal A) 전용 Mapper — PRAFTA-WEB_001-1.
 *
 * <p>예약 등록(채번/INSERT/중복검사)과 5종 불가케이스 사전 검증 쿼리를 담는다.
 * 모든 조회는 회사 스코프(CMPNY_CD)를 강제한다(cross-tenant 방지).
 */
@Mapper
public interface UserTransferMapper {

    /** 대상 사용자 기본 정보(현재 사업장/부서/고용형태). 회사 스코프. 없으면 null. */
    UserTransferBasicResult selectUserBasic(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 이동 사업장 실재 검증: (CMPNY_CD, SITE_CD) 존재 카운트(회사 스코프). 1 이상이면 실재. */
    int selectSiteExists(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /** 이동 소속부서 실재 검증: (CMPNY_CD, SITE_CD, NODE_CD) 존재 카운트(회사 스코프). 1 이상이면 실재. */
    int selectNodeExists(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("nodeCd") String nodeCd);

    /** 불가① 대상자가 현재 사업장(siteCd)의 사업장 관리자(SITE_ADMIN_CD)인지. 1 이상이면 불가. */
    int selectIsSiteAdmin(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("userCd") String userCd);

    /**
     * 불가② 대상자가 "마지막 담당자"인 노드 수.
     * 대상자가 MAIN 또는 SUB 담당자이면서, 두 슬롯이 (비어있음 또는 대상자 본인) 뿐이라 제거 시 담당자 0명이 되는 노드. 1 이상이면 불가.
     */
    int selectLastAdminNodeCnt(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 불가③ 대상자가 순회점검(체크포인트) 담당자(MGMT_USER_CD, USE_YN='Y')인지. 1 이상이면 불가. */
    int selectIsChkptManager(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /**
     * 불가⑤ 대상자의 현재/미래 부분(시간차) 연차 목록.
     * LEAVE_STATUS='CONFIRMED' AND DEL_YN='N' AND USE_UNIT_TYPE != '00' AND START_TIME 보유 AND START_DATE >= moveDate.
     */
    List<PartialLeaveTimeResult> selectFuturePartialLeaves(@Param("cmpnyCd") String cmpnyCd,
            @Param("userCd") String userCd, @Param("moveDate") String moveDate);

    /**
     * 불가⑤ 기본 근무타입(schCd)의 baseDate 기준 effective 근무 구간 시각(1/2구간).
     * TB_SCH_MGMT(현재본) ∪ TB_SCH_MGMT_HIST(이력) 중 APPLY_DATE <= baseDate 최신본. 없으면 최이른 적용본 폴백. 미존재 시 null.
     */
    SchSegmentTimeResult selectEffectiveSchSegment(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("schCd") String schCd, @Param("baseDate") String baseDate);

    /** 동일 사용자 활성 예약(STATUS='RESERVED') 카운트(중복 사전검사). 1 이상이면 중복. */
    int selectActiveReservationCnt(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 예약 ID 채번('TR' + YYYYMMDD + 시퀀스). */
    String selectReservationId(@Param("cmpnyCd") String cmpnyCd);

    /** 소속이동 예약 INSERT(STATUS='RESERVED'/NOTICE_ACK_YN='N'/DEL_YN='N' 고정). */
    void insertReservation(TransferReservationInsertCommand command);

    // ====================================================================
    // PRAFTA-WEB_001-3 (Terminal C) — 등록 즉시 PUSH + 로그인 안내 조회/ack.
    //   안내 조회/ack 는 대상자 본인(JWT userCd) 스코프로만 동작한다(IDOR 방지).
    //   명칭(사업장/부서/기본근무타입)은 회사 스코프 조인으로 도출한다.
    // ====================================================================

    /**
     * 단건 예약의 안내 표시정보(PUSH 본문 구성용). 회사 + 대상자 + 예약 ID 스코프.
     * 사업장명/부서명/기본근무타입명을 조인해 반환한다(일용직 defaultSchNm=null). 없으면 null.
     */
    TransferNoticeResult selectTransferNoticeByReservation(@Param("cmpnyCd") String cmpnyCd,
            @Param("userCd") String userCd, @Param("reservationId") String reservationId);

    /**
     * 로그인 안내 — 대상자 본인의 미확인(NOTICE_ACK_YN='N') 비종결 예약(STATUS IN 'RESERVED','APPLIED') 1건.
     * 최신(INSERT_DATE DESC) 1건만 반환. 없으면 null. 회사 + 대상자 스코프(IDOR 방지).
     */
    TransferNoticeResult selectUnackedTransferNotice(@Param("cmpnyCd") String cmpnyCd,
            @Param("userCd") String userCd);

    /**
     * 안내 확인(ack): NOTICE_ACK_YN='Y' + NOTICE_ACK_DATE 기록. 멱등(재확인 시에도 1행).
     * WHERE 를 회사 + 예약 ID + 대상자(USER_CD)로 못박아 타인 예약 ack 를 차단한다(IDOR).
     *
     * @return 갱신된 행 수(소유 1, 미존재/타인 0)
     */
    int ackTransferNotice(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("reservationId") String reservationId);

    // ====================================================================
    // PRAFTA-WEB_001-2 (Terminal B) — 발효 실행기/자정 스케줄러 전용 쿼리.
    //   모든 변경은 예약 레코드의 CMPNY_CD/USER_CD 범위로만 수행한다(cross-tenant 방지).
    //   진행중 요청 반려/취소·TBM 종료/미이수는 기존 인터랙티브 반려 서비스가
    //   호출자(승인자) 권위·단건 REQ_ID 컨텍스트에 강결합되어 배치 재사용이 불가하므로,
    //   각 캐노니컬 상태 전이(컬럼 집합)를 동일하게 미러하는 회사+사용자 스코프 일괄 UPDATE 로 구현한다.
    // ====================================================================

    /**
     * 발효 대상 예약 조회: STATUS='RESERVED' AND DEL_YN='N' AND MOVE_DATE &lt;= today.
     * (이미 발효된 'APPLIED'/'FAILED'/'CANCELLED' 는 STATUS 필터로 제외 — 멱등.)
     */
    List<TransferReservationExecRow> selectDueReservations(@Param("today") String today);

    /**
     * 발효 선점 잠금: 예약 행을 {@code FOR UPDATE} 로 잠그고 현재 STATUS 를 반환한다.
     * 다중 인스턴스 동시 발효 시 패자는 잠금 해제 후 STATUS!='RESERVED' 를 보고 즉시 빠진다(이중작업 방지).
     * 행이 없으면 null.
     */
    String selectReservationStatusForUpdate(@Param("cmpnyCd") String cmpnyCd, @Param("reservationId") String reservationId);

    /** 발효 성공 마킹: STATUS='APPLIED' + EXECUTED_DATE=NOW(). RESERVED 가드(정확히 1행). 0행=이미 처리/경합. */
    int markReservationApplied(@Param("cmpnyCd") String cmpnyCd, @Param("reservationId") String reservationId,
            @Param("actor") String actor);

    /** 발효 실패 격리: STATUS='FAILED' + FAIL_REASON. RESERVED 가드. 다른 예약 무영향. */
    int markReservationFailed(@Param("cmpnyCd") String cmpnyCd, @Param("reservationId") String reservationId,
            @Param("failReason") String failReason, @Param("actor") String actor);

    /**
     * (a) 소속 발효: tb_user.SITE_CD/NODE_CD 변경. 정규직(applyDefaultSch=true)이면 DEFAULT_SCH_CD/SET_DATE 도 갱신.
     * mergeUserInfo 의 DEFAULT_SCH 조건부 갱신 패턴과 정합. 회사+사용자 스코프.
     */
    int updateUserSiteNodeForTransfer(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("toSiteCd") String toSiteCd, @Param("toNodeCd") String toNodeCd,
            @Param("toDefaultSchCd") String toDefaultSchCd, @Param("applyDefaultSch") boolean applyDefaultSch,
            @Param("actor") String actor);

    /**
     * (c) 담당 정 자동등록(정규직): 이동 노드의 MAIN_ADMIN_CD·SUB_ADMIN_CD 가 모두 비어있을 때만(정·부 무담당)
     * 대상자를 담당 정(正)으로 세팅. 정 또는 부 담당이 한 명이라도 있으면 0행(무변경).
     * 회사+사업장+노드 스코프.
     */
    int updateNodeMainAdminIfEmpty(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("nodeCd") String nodeCd, @Param("userCd") String userCd, @Param("actor") String actor);

    /**
     * (e) 진행중 근태/연차 요청(대상자=신청자) 취소.
     * 캐노니컬 반려(Attd07Mapper.updateUserAttdReqReject) 컬럼 집합 미러 — REQ_STATUS='04'(취소),
     * PROCESS_USER_CD/PROCESS_COMMENT/PROCESS_DATE 기록. WHERE USER_CD=대상자 AND REQ_STATUS='01' AND DEL_YN='N'.
     */
    int cancelActiveAttdReqByApplicant(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("reason") String reason, @Param("actor") String actor);

    /**
     * QT-11-7 — 발효로 취소될 대상자 본인의 진행중 "연차 사용('05')" 요청 REQ_ID 스냅샷.
     * 상태 UPDATE 이후에는 REQ_STATUS='01' 조건으로 다시 찾을 수 없으므로 UPDATE 전에 확보한다.
     */
    List<String> selectActiveLeaveReqIdsByApplicant(@Param("cmpnyCd") String cmpnyCd,
            @Param("userCd") String userCd);

    /**
     * QT-11-7 — 스냅샷된 REQ_ID 집합(대상자가 결재자인 건) 중 연차 사용('05')만 추린다.
     * 근태보정/OT/스케줄 요청은 연차 원장과 무관하므로 원복 대상에서 제외된다.
     */
    List<String> selectLeaveReqIdsIn(@Param("cmpnyCd") String cmpnyCd, @Param("reqIds") List<String> reqIds);

    /**
     * (e) 진행중 근태/연차 요청(대상자=승인대기 결재자) — 대상 REQ_ID 스냅샷 선조회.
     * R.REQ_STATUS='01' AND R.DEL_YN='N' AND 대상자에게 배정된 미처리(APPROVAL_STATUS IN '00','01') 결재단계 존재.
     *
     * <p>[수정5] 본 요청 반려와 결재단계 반려가 서로의 사전상태에 상호 의존하므로, 대상 집합을 먼저 스냅샷한 뒤
     * 두 UPDATE 가 동일 REQ_ID 집합을 키로 처리한다(순차 UPDATE 의 상호 무력화 방지, 활성 요청만 한정).
     */
    List<String> selectActiveApproverReqIds(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /**
     * (e) 스냅샷된 REQ_ID 들의 본 요청 반려 → REQ_STATUS='03'(반려).
     * Attd07Mapper.updateUserAttdReqReject 컬럼 집합 미러. REQ_STATUS='01' 가드(경합 방어).
     */
    int rejectAttdReqByReqIds(@Param("cmpnyCd") String cmpnyCd, @Param("reqIds") List<String> reqIds,
            @Param("reason") String reason, @Param("actor") String actor);

    /**
     * (e) 스냅샷된 REQ_ID 들에서 대상자(결재자)의 미처리(APPROVAL_STATUS IN '00','01') 결재단계 반려 → '03'(SYS044).
     * ApprovalLineMapper.updateStepStatus 컬럼 집합 미러.
     */
    int rejectApprovalStepsForApproverByReqIds(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("reqIds") List<String> reqIds, @Param("reason") String reason, @Param("actor") String actor);

    /**
     * (e) 진행중 초과근무 취소: TB_USER_OVERTIME_MGMT OT_STATUS='IN_PROGRESS' → 'CANCELLED'.
     * selectDailyOvertimeList 술어(OT_STATUS != 'CANCELLED')와 정합 — 취소분은 목록에서 제외.
     */
    int cancelActiveOvertimeByUser(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("actor") String actor);

    /**
     * (e) 진행중 연차변경요청 반려: TB_LEAVE_CHANGE_REQUEST REQ_STATUS IN ('REQUESTED','AGREED') → 'REJECTED'.
     * 대상자가 TARGET_USER_CD 또는 INITIATOR_USER_CD 인 비종결 건. Attd13Mapper.rejectChangeRequest 컬럼 집합 미러.
     */
    int rejectActiveLeaveChangeByUser(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("rejectReason") String rejectReason, @Param("actor") String actor);

    /**
     * (f-가) 대상자가 진행 관리자(MANAGER_USER_CD)인 시작·미종료 TBM 세션을 이동시점 종료처리.
     * STARTED_AT NOT NULL AND ENDED_AT IS NULL AND CANCELLED_AT IS NULL → STATUS_CD='COMPLETED' + ENDED_AT=NOW().
     * AppAdminTbmMapper.endSession 컬럼 집합 미러(EXIT_PWD 미발급 — 강제 종료).
     */
    int endActiveTbmSessionsAsManager(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("actor") String actor);

    /**
     * (f-나) 대상자가 참여(ENTRY_AT NOT NULL)했으나 정상종료(서명)前(COMPLETION_STATUS_CD != 'COMPLETED')인
     * TBM 참석 행을 미이수 처리. COMPLETION_STATUS_CD='NOT_COMPLETED' + NOT_COMPLETED_REASON + STATUS_UPDATED_BY/AT.
     * Tbm04Mapper.updateCompletion 컬럼 집합 미러(EXIT_AT 등 입퇴실 컬럼 불변).
     */
    int markTbmAttendanceNotCompleted(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("reason") String reason, @Param("actor") String actor);
}
