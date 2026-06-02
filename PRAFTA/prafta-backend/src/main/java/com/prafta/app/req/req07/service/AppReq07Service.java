package com.prafta.app.req.req07.service;

import com.prafta.app.req.req07.application.param.AttdCorrectionParam;
import com.prafta.app.req.req07.application.param.OvertimeParam;
import com.prafta.app.req.req07.application.param.SchedModifyParam;
import com.prafta.app.req.req07.dto.response.RegisterReqResponse;

/**
 * prafta-app-007: 모바일 앱 근태 요청 등록 (스케줄 수정 / 근태 보정 / 초과근무) 서비스.
 *
 * <p>3 endpoint 모두 동일한 트랜잭션 모델 (REQ_ID 채번 → slots 개수만큼 INSERT) 을 사용한다.
 * 결재선 통합 / 알림 발송은 prafta-app-009 / outbox follow-up 으로 분리 (Q3, P4).
 */
public interface AppReq07Service {

    /** 스케줄 수정 요청 등록 (REQ_TYPE='10' 고정). */
    RegisterReqResponse registerSchedModify(SchedModifyParam param);

    /** 근태 보정 요청 등록 (REQ_TYPE 자동 분기 — '01' or '02' or 'MIXED'). */
    RegisterReqResponse registerAttdCorrection(AttdCorrectionParam param);

    /** 초과근무 신청 등록 (REQ_TYPE='03' 고정). */
    RegisterReqResponse registerOvertime(OvertimeParam param);
}
