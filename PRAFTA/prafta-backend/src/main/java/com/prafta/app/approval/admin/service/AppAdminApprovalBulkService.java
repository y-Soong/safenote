package com.prafta.app.approval.admin.service;

import com.prafta.app.approval.admin.dto.request.ApprovalBulkProcessRequest;
import com.prafta.app.approval.admin.dto.response.ApprovalBulkProcessResponse;
import com.prafta.common.dto.TokenInfo;

/**
 * prafta-leavemulti: 앱 관리자 승인 <b>일괄</b> 처리(연차 기간신청 묶음).
 *
 * <p>판정/권한/마감 로직을 한 줄도 새로 만들지 않는다. 기존 앱 단건
 * {@link AppAdminApprovalService#process} 를 items 수만큼 그대로 호출하는 얇은 어댑터다
 * — 일괄 경로가 단건 경로의 가드를 우회하지 못하게 하는 것이 이 설계의 목적이다.
 */
public interface AppAdminApprovalBulkService {

    /**
     * 일괄 승인/반려. 건별로 독립 트랜잭션이며 실패 건은 사유와 함께 응답에 담아 계속 진행한다(부분 성공).
     *
     * @throws com.prafta.common.exception.ApiException 요청 자체가 부적합할 때(그룹/결정 미지원, 반려사유 누락,
     *         items 상한 초과 등) — 이 경우 한 건도 처리하지 않는다.
     */
    ApprovalBulkProcessResponse bulkProcess(ApprovalBulkProcessRequest request, TokenInfo token);
}
