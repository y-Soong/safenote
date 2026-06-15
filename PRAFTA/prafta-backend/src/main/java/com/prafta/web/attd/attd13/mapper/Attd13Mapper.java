package com.prafta.web.attd.attd13.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd13.application.command.LeaveChangeRequestInsertCommand;
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
     * 이동 대상일에 동일 직원·동일 연차코드의 확정(CONFIRMED) 사용행이 이미 있는지 카운트
     * (DIRECT_USE_KEY 충돌 사전 검증). 자기 자신(leaveId)은 제외한다.
     */
    int countLeaveUseOnDate(@Param("cmpnyCd") String cmpnyCd,
                            @Param("userCd") String userCd,
                            @Param("leaveCd") String leaveCd,
                            @Param("targetDate") String targetDate,
                            @Param("excludeLeaveId") String excludeLeaveId);

    /**
     * 대상 연차가 차감한 부여(GRANT)의 만료일(AVAIL_TO_DATE, YYYYMMDD) 단건. 없으면 null.
     * 이동 대상일 만료 초과 검증용.
     */
    String selectGrantAvailToDate(@Param("cmpnyCd") String cmpnyCd,
                                  @Param("grantId") String grantId);

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
     * 변경 요청 목록 조회(관리자) — 역할 기반 스코프(작업1 D1+D3).
     *
     * <ul>
     *   <li>{@code siteWide='Y'}(master/hr): 회사 전사. siteCd 가 지정되면 해당 사업장으로 선택 필터,
     *       미지정이면 전체 사업장.</li>
     *   <li>{@code siteWide='N'}(노드 정·부 관리자): siteCd + nodeCd(+하위) 강제 한정. 호출부에서
     *       권한 검증을 통과한 nodeCd 만 넘긴다(노드 미지정 진입 불가).</li>
     * </ul>
     */
    List<LeaveChangeRequestRowResult> selectChangeRequests(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("siteWide") String siteWide,
                                                           @Param("siteCd") String siteCd,
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
     * MOVE 확정: 대상 연차 사용행의 START_DATE/END_DATE 를 새 일자로 갱신.
     * 최초 지정일(ORIG_DESIGNATED_DATE)이 비어 있으면 기존 START_DATE 로 보존 세팅한다.
     * WHERE 를 CONFIRMED + 미삭제로 못박는다.
     *
     * @return 갱신 행 수
     */
    int moveLeaveUseDate(@Param("cmpnyCd") String cmpnyCd,
                         @Param("leaveId") String leaveId,
                         @Param("newDate") String newDate,
                         @Param("updateNo") String updateNo);

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
