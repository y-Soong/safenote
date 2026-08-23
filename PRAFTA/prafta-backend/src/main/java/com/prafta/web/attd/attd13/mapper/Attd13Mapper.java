package com.prafta.web.attd.attd13.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

import com.prafta.web.attd.attd13.application.command.LeaveChangeRequestInsertCommand;
import com.prafta.web.attd.attd13.application.command.MovedLeaveUseInsertCommand;
import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;
import com.prafta.web.attd.attd13.result.LeaveUseTargetResult;
import com.prafta.web.attd.attd13.result.MovableLeaveResult;

/**
 * 연차 변경/삭제 동의·거부(attd13 / leavechange) 전용 Mapper (PRAFTA-COM-008-C).
 *
 * <p>정책서: {@code prafta-com-008-C-use-source-consent.md} §2/§3.
 * 모든 조회/쓰기는 CMPNY_CD 스코프로 격리하며, 식별값은 호출부(서비스)가 JWT 에서 도출한 값만 신뢰한다.
 *
 * <p>채번/부여 재계산/알림 적재는 본 매퍼가 중복 구현하지 않고 기존 공통 매퍼를 재사용한다
 * ({@code LeaveFlowMapper.recomputeGrantUsedDays}, {@code LeaveDashboardMapper.selectNextNotiId/insertNotiOutbox}).
 */
@Mapper
public interface Attd13Mapper {

    // ============================================================
    // 채번
    // ============================================================

    /**
     * CHANGE_REQ_ID 채번. 'LC' + YYYYMMDD + 시퀀스(FNC_CMM_SEQ_NEXTVAL, SEQ_KEY='LEAVE_CHANGE_REQ_ID').
     */
    String selectNextChangeReqId(@Param("cmpnyCd") String cmpnyCd);

    // ============================================================
    // 대상 연차/검증
    // ============================================================

    /**
     * 변경/삭제 대상 연차 사용행 단건 조회(회사 스코프). 없거나 스코프 밖/취소/삭제면 null.
     * IDOR/스코프 재검증 + 마감 가드(출발일)·이동 충돌 판정에 필요한 최소 컬럼만.
     */
    LeaveUseTargetResult selectLeaveUseTarget(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("leaveId") String leaveId);

    /**
     * (2026-08-18 완화) 이동 대상일의 기존 CONFIRMED 점유 일수 합 — 신청 3-B
     * ({@code LeaveFlowMapper.selectOccupiedLeaveDaysOnDate}) 산식 미러(종일=1.0 / 그 외=LEAVE_DAYS)에
     * 자기 행({@code excludeLeaveId}) + 자기 REQ 묶음({@code excludeReqId}, nullable) 제외를 더한 것.
     * 호출부(validateMove)는 (점유 + 이동해 올 분량) &gt; 1.0 이면 ATTD_400_209 로 거부한다.
     */
    BigDecimal sumOccupiedLeaveDaysOnDateExcludingSelf(@Param("cmpnyCd") String cmpnyCd,
                                                       @Param("userCd") String userCd,
                                                       @Param("targetDate") String targetDate,
                                                       @Param("excludeLeaveId") String excludeLeaveId,
                                                       @Param("excludeReqId") String excludeReqId);

    /**
     * (2026-08-18 완화) 직접사용 실충돌 사전 검증 — 이동 원본이 직접사용(REQ_ID NULL)일 때만 호출.
     * applyMove 재INSERT 가 REQ_ID 를 승계하므로 생성컬럼 키는 직접사용 원본에서만 활성이다:
     * 대상일의 동일 종류({@code leaveCd}) 직접사용 CONFIRMED 행(UK_LEAVE_USE_DIRECT, 단위 무관)과,
     * 종일 원본({@code fullDayYn}='Y')이면 종일 직접사용 행(UK_LEAVE_USE_DIRECT_CELL, 종류 무관)까지 센다.
     * 자기 자신({@code excludeLeaveId}) 제외는 방어적 유지. 초과 시 호출부가 ATTD_400_126 거부.
     */
    int countDirectUseConflictOnDate(@Param("cmpnyCd") String cmpnyCd,
                                     @Param("userCd") String userCd,
                                     @Param("leaveCd") String leaveCd,
                                     @Param("targetDate") String targetDate,
                                     @Param("excludeLeaveId") String excludeLeaveId,
                                     @Param("fullDayYn") String fullDayYn);

    /**
     * (2026-08-18 완화) 자기 묶음의 CONFIRMED 차감 일수 합 — 시간차 이동분 근사(원일자 분모 기준).
     * {@code reqId} 연결 건은 REQ 스코프, 직접사용은 {@code leaveId} 단건 스코프
     * ({@link #sumSelfRestorableDaysOnDate} 스코프 분기 관례 — 부여 유효 필터 없음).
     */
    BigDecimal sumTargetLeaveDays(@Param("cmpnyCd") String cmpnyCd,
                                  @Param("leaveId") String leaveId,
                                  @Param("reqId") String reqId);

    /**
     * T3: 같은 REQ 분할 묶음의 대표행(MIN LEAVE_ID, CONFIRMED·미삭제) 단건. 없으면 null.
     * 발의/확정 시 비대표행 LEAVE_ID 입력을 대표행으로 정규화한다(ACTIVE_LEAVE_KEY 멱등의 REQ 단위 성립).
     */
    String selectRepresentativeLeaveId(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("reqId") String reqId);

    /**
     * T1 발의 soft 체크(§2-6): 자기 묶음(REQ 또는 단건)의 CONFIRMED 차감분 중 "이동 대상일 기준
     * 유효(ACTIVE, AVAIL_FROM~TO 포함) + 원 종류(leaveCd) 귀속" 부여에 걸린 일수 합.
     * 취소 시 복원되어 재차감에 쓸 수 있는 분량의 근사치(잠금 없음 — 확정 시 재검증이 단일 신뢰 지점).
     */
    BigDecimal sumSelfRestorableDaysOnDate(@Param("cmpnyCd") String cmpnyCd,
                                           @Param("reqId") String reqId,
                                           @Param("leaveId") String leaveId,
                                           @Param("leaveCd") String leaveCd,
                                           @Param("targetDate") String targetDate);

    /**
     * (F1 재활성) 대상 연차가 차감한 부여(GRANT)의 만료일(AVAIL_TO_DATE) 단건.
     * 만료일 이내 이동 제한 복원(ATTD_400_125, 2026-08-04 사용자 확정)의 직접사용(REQ_ID NULL)
     * 경로에서 재사용한다. REQ 묶음은 {@link #selectMinGrantAvailToDateByReq} 사용.
     */
    String selectGrantAvailToDate(@Param("cmpnyCd") String cmpnyCd,
                                  @Param("grantId") String grantId);

    /**
     * F1: REQ 묶음(CONFIRMED 잔존 행)이 차감한 부여들의 최소 만료일(min AVAIL_TO_DATE).
     * 이동 대상일이 이 값을 넘으면 ATTD_400_125 거부(validateMove — 발의·확정 재검증 공통).
     */
    String selectMinGrantAvailToDateByReq(@Param("cmpnyCd") String cmpnyCd,
                                          @Param("reqId") String reqId);

    /**
     * F9(qa D-4): 삭제 확정의 가불 회수({@code cancelBorrowGrantByReqId})에서 건너뛰어진
     * (USED_DAYS &gt; 0 잔존) 가불 GRANT 목록 — log.info 보고 전용 조회.
     * 기존 statement(LeaveDashboardMapper.selectBorrowGrantIdsForCancel)는 수정하지 않는다.
     */
    List<String> selectRemainingBorrowGrantIdsByReq(@Param("cmpnyCd") String cmpnyCd,
                                                    @Param("reqId") String reqId);

    /**
     * 근로자 본인의 이동 가능 연차일 목록(C-5a). 미래 확정 연차 중 본인 소유만.
     * 만료일/촉진단계/지정주체 포함(프론트 표시·서버 재검증 동일 소스).
     */
    List<MovableLeaveResult> selectMovableLeaves(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("userCd") String userCd,
                                                 @Param("todayYmd") String todayYmd);

    // ============================================================
    // 변경 요청 (CRUD)
    // ============================================================

    /**
     * 변경 요청 1건 INSERT (REQUESTED). ACTIVE_LEAVE_KEY UNIQUE 로 동시 활성요청 1건 강제.
     */
    int insertChangeRequest(LeaveChangeRequestInsertCommand cmd);

    /**
     * 변경 요청 목록 조회(관리자) — 접근 가능 사업장 목록 기반 스코프
     * (접수함연차변경다중사업장확장-001/002, ReqInboxMapper.selectPendingRequests 와 동형).
     *
     * <ul>
     *   <li>{@code siteCds}: 서비스({@code Attd13ServiceImpl.resolveSiteCds})가 확정한 접근 가능
     *       사업장 목록. 항상 1건 이상만 전달된다 — 0건이면 호출부가 매퍼 호출 자체를 생략한다.</li>
     *   <li>{@code nodeCd} 지정 시 부서(+하위) 서브필터를 적용한다. 부서 필터는 사업장이 정확히
     *       1건으로 좁혀졌을 때만 유효(§0-5 설계 결정)하며, 그 경우에 한해 서비스가
     *       {@code nodeSiteCd}(= {@code siteCds} 의 그 1건)를 채워 전달한다. 매퍼는 항상 유효한
     *       조합만 받는다고 가정한다(2건 이상/0건 + nodeCd 조합은 서비스 단계에서 거부).</li>
     * </ul>
     */
    List<LeaveChangeRequestRowResult> selectChangeRequests(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("siteCds") List<String> siteCds,
                                                           @Param("nodeSiteCd") String nodeSiteCd,
                                                           @Param("nodeCd") String nodeCd,
                                                           @Param("incSubNodeYn") String incSubNodeYn,
                                                           @Param("userNm") String userNm,
                                                           @Param("reqStatus") String reqStatus);

    /**
     * 변경 요청 단건 조회(회사 스코프). 없거나 스코프 밖/삭제면 null.
     */
    LeaveChangeRequestRowResult selectChangeRequest(@Param("cmpnyCd") String cmpnyCd,
                                                    @Param("changeReqId") String changeReqId);

    /**
     * 근로자 본인 대상 대기(REQUESTED) 요청 목록(C-2 앱). 스코프 = 회사 + 본인 TARGET_USER_CD.
     * 사업장 필터를 두지 않고 본인 대상만 노출한다(IDOR: targetUserCd 는 JWT 도출값).
     */
    List<LeaveChangeRequestRowResult> selectPendingConsents(@Param("cmpnyCd") String cmpnyCd,
                                                            @Param("targetUserCd") String targetUserCd);

    /**
     * 근로자 응답(AGREE/REJECT) 반영. WHERE 를 REQUESTED + 대상자 본인 + 미삭제로 못박아 멱등/동시성 방어.
     *
     * @return 갱신 행 수(정상 1, 경합/조건불일치 0)
     */
    int applyWorkerResponse(@Param("cmpnyCd") String cmpnyCd,
                            @Param("changeReqId") String changeReqId,
                            @Param("targetUserCd") String targetUserCd,
                            @Param("workerResponse") String workerResponse,
                            @Param("responseReason") String responseReason,
                            @Param("reqStatus") String reqStatus,
                            @Param("updateNo") String updateNo);

    /**
     * 관리자 최종 확인 반영(상태 CONFIRMED + 확인자/일시). WHERE 를 AGREED + 미삭제로 못박는다.
     *
     * @return 갱신 행 수(정상 1, 경합/조건불일치 0)
     */
    int confirmChangeRequest(@Param("cmpnyCd") String cmpnyCd,
                             @Param("changeReqId") String changeReqId,
                             @Param("confirmUserCd") String confirmUserCd);

    /**
     * 관리자 반려 반영(작업2): REQ_STATUS='REJECTED' + 반려자/일시/사유. WHERE 를 AGREED + 미삭제로 못박는다.
     * 원 연차는 불변(상태만 전이). 반려 사유는 REJECT_REASON 컬럼에 저장.
     *
     * @return 갱신 행 수(정상 1, 경합/조건불일치 0)
     */
    int rejectChangeRequest(@Param("cmpnyCd") String cmpnyCd,
                            @Param("changeReqId") String changeReqId,
                            @Param("rejectReason") String rejectReason,
                            @Param("confirmUserCd") String confirmUserCd);

    // ============================================================
    // 연차 반영 (MOVE / DELETE)
    // ============================================================

    /**
     * (T1 사장 — 참조 0건 유지) 구 MOVE 반영: START_DATE 단순 UPDATE.
     * 이동이 "원 차감 취소 + 대상일 재차감"으로 재정의되어 미사용(plan §2-2).
     */
    int moveLeaveUseDate(@Param("cmpnyCd") String cmpnyCd,
                         @Param("leaveId") String leaveId,
                         @Param("newDate") String newDate,
                         @Param("updateNo") String updateNo);

    /**
     * T1: 이동 재차감 use 행 INSERT — 속성 승계(PROMOTION_STAGE/DESIGNATOR_TYPE/ORIG_DESIGNATED_DATE)
     * 포함. {@code LeaveFlowMapper.insertLeaveUse} 는 승계 컬럼 미포함(§0-1-6)이라 전용 신설(불변 원칙).
     */
    int insertMovedLeaveUse(MovedLeaveUseInsertCommand cmd);

    /**
     * DELETE 확정: 대상 연차 사용행 soft cancel(LEAVE_STATUS='CANCELLED' + 사유/일시). WHERE 못박음.
     *
     * @return 갱신 행 수
     */
    int cancelLeaveUse(@Param("cmpnyCd") String cmpnyCd,
                       @Param("leaveId") String leaveId,
                       @Param("cancelReason") String cancelReason,
                       @Param("updateNo") String updateNo);

    /**
     * 출근 차단(일 단위) 블록 이동: 기존 일자 근무계획 연차블록 삭제.
     */
    int deleteWorkPlanLeave(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCd") String userCd,
                            @Param("workYmd") String workYmd,
                            @Param("leaveCd") String leaveCd);

    /**
     * 출근 차단(일 단위) 블록 설정: 새 일자 근무계획 연차블록 upsert.
     */
    int upsertWorkPlanLeave(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCd") String userCd,
                            @Param("workYmd") String workYmd,
                            @Param("leaveCd") String leaveCd,
                            @Param("updateNo") String updateNo);
}
