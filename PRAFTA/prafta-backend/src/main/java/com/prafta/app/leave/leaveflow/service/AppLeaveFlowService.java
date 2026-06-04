package com.prafta.app.leave.leaveflow.service;

import com.prafta.app.leave.leaveflow.application.param.LeaveApplyMetaParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApplyParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApproverSearchParam;
import com.prafta.app.leave.leaveflow.dto.response.ApprovalPresetListResponse;
import com.prafta.app.leave.leaveflow.dto.response.ApproverSearchResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveApplyMetaResponse;

/**
 * prafta-app-018-A/B: 앱 연차 신청 폼 메타 조회 + 신청 쓰기 서비스.
 */
public interface AppLeaveFlowService {

    /** 신청 가능 연차종류 + 허용 사용단위(D2-a 계층) + 잔여 조회. */
    LeaveApplyMetaResponse selectApplyMeta(LeaveApplyMetaParam param);

    /** 본인 소유 결재선 프리셋 목록(mypage01 재사용). */
    ApprovalPresetListResponse selectApprovalPresets(LeaveApplyMetaParam param);

    /** 결재자 후보 검색(사업장 스코프, LIMIT/OFFSET, hasNext). */
    ApproverSearchResponse searchApprovers(LeaveApproverSearchParam param);

    /**
     * prafta-app-018-B: 연차 신청 1건 처리(요청 INSERT + 결재선 + 사용기록 + 부여 재계산).
     * 단위 게이팅(D2) + 구조검증/차감 + 사후마감 + 잔여검증을 모두 통과한 후에만 INSERT 한다.
     */
    void submitLeave(LeaveApplyParam param);
}
