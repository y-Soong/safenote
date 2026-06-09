package com.prafta.app.leave.approval.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.leave.approval.result.LeaveBalanceRow;
import com.prafta.app.leave.approval.result.LeaveDetailBodyRow;
import com.prafta.app.leave.approval.result.LeaveProcessedHistoryRow;
import com.prafta.app.leave.approval.result.LeaveReqMetaRow;

/**
 * 사용자연차결재-01 Mapper.
 *
 * <p>상세 읽기 SQL(meta/body/balance)은 AppAdminApprovalMapper 연차 쿼리를 본 매퍼로 포팅했다(F-DETAIL —
 * 관리자 모듈 변경 영향 차단). 이력은 "내 단계 행동 기준" 신규 쿼리다(F-H1). 대기·처리는 SQL 신규 없음
 * (LeaveFlowService 위임). 식별자(cmpny/user)는 서비스가 토큰값으로만 전달한다(IDOR 차단).
 */
@Mapper
public interface AppLeaveApprovalMapper {

    // ============================ 3-B 상세 (읽기 SQL 포팅) ============================

    /** 연차 요청 메타 1행(회사 스코프). 없으면 null. */
    LeaveReqMetaRow selectLeaveReqMeta(@Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId);

    /** 연차 본문(05=요청 연결 사용기록 / 06=수정대상 사용기록). 유급여부(paidYn) 포함. 없으면 null. */
    LeaveDetailBodyRow selectLeaveBody(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("reqId") String reqId,
                                       @Param("targetId") String targetId);

    /** 연차 잔여(활성 부여 합계: 부여/사용/잔여). */
    LeaveBalanceRow selectLeaveBalance(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd,
                                       @Param("leaveCd") String leaveCd);

    // ============================ 3-C 이력 (신규 쿼리) ============================

    /**
     * 내가 처리(승인/반려)한 연차 요청 이력 목록(내 단계 행동 기준, F-H1).
     * {@code AP.APPROVER_USER_CD = approverUserCd AND AP.APPROVAL_STATUS IN ('02','03')}, AP.APPROVAL_DATE DESC.
     */
    List<LeaveProcessedHistoryRow> selectMyProcessedLeaveHistory(@Param("cmpnyCd") String cmpnyCd,
                                                                 @Param("approverUserCd") String approverUserCd,
                                                                 @Param("keyword") String keyword,
                                                                 @Param("startYmd") String startYmd,
                                                                 @Param("endYmd") String endYmd,
                                                                 @Param("offset") int offset,
                                                                 @Param("limit") int limit);

    /** 내가 처리한 연차 요청 이력 건수. */
    int countMyProcessedLeaveHistory(@Param("cmpnyCd") String cmpnyCd,
                                     @Param("approverUserCd") String approverUserCd,
                                     @Param("keyword") String keyword,
                                     @Param("startYmd") String startYmd,
                                     @Param("endYmd") String endYmd);
}
