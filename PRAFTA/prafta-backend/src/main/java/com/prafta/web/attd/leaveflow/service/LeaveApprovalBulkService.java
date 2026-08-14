package com.prafta.web.attd.leaveflow.service;

import com.prafta.common.dto.TokenInfo;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApprovalBulkRequest;
import com.prafta.web.attd.leaveflow.dto.response.LeaveApprovalBulkResponse;

/**
 * prafta-leavemulti: 연차 결재 일괄 승인/반려 (묶음 처리용).
 *
 * <p><b>★ 반드시 {@link LeaveFlowService} 와 별도 빈이어야 한다.</b>
 * 같은 빈에서 {@code approveStep} 을 호출하면 self-invocation 으로 프록시를 타지 않아
 * 건별 트랜잭션 분리가 되지 않는다(1건 실패 = 전건 롤백).
 *
 * <p>또한 <b>이 서비스의 메서드에는 {@code @Transactional} 을 걸지 않는다.</b>
 * 걸어버리면 건별 {@code approveStep}(REQUIRED)이 바깥 트랜잭션에 합류해 역시 전건 롤백이 된다.
 * 트랜잭션 없이 호출해야 각 {@code approveStep} 이 자기 트랜잭션을 열고 닫아 부분 성공이 성립한다.
 *
 * <p>판정·권한·마감·상태 가드는 전부 기존 단건 {@code approveStep}/{@code rejectStep} 이 그대로 수행한다
 * (신규 검증 로직 없음). 본 서비스는 반복과 결과 수집만 담당한다.
 */
public interface LeaveApprovalBulkService {

    /** 일괄 승인. 건별 독립 트랜잭션 — 일부 실패해도 나머지는 확정된다. */
    LeaveApprovalBulkResponse approveBulk(TokenInfo tokenInfo, LeaveApprovalBulkRequest request);

    /** 일괄 반려. 규약은 승인과 동일. */
    LeaveApprovalBulkResponse rejectBulk(TokenInfo tokenInfo, LeaveApprovalBulkRequest request);
}
