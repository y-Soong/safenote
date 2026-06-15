package com.prafta.app.req.req07.service;

import com.prafta.app.req.req07.application.param.AttdCorrectionParam;
import com.prafta.app.req.req07.application.param.OvertimeParam;
import com.prafta.app.req.req07.application.param.SchedModifyParam;
import com.prafta.app.req.req07.dto.response.RegisterReqResponse;
import com.prafta.app.req.req07.dto.response.SchedOptionResponse;

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

    /**
     * 스케줄 선택 옵션 목록 조회 (prafta-app-007 F2).
     * 식별값(cmpnyCd/siteCd/userCd)은 JWT 도출값을 사용한다 (IDOR). 빈 결과는 빈 배열.
     * prafta-com-008-E-9a: 응답에 사용자 본인 기본 근무타입(userDefaultSchCd) 동반.
     * prafta-com-008-D-5: workYmd(대상 일자, optional)가 교대팀 소속 구간이면 응답 shiftLocked=true.
     *   판정은 공용 cmm ShiftMembershipService(D-1 술어) 재사용 — 신규 쿼리 신설 금지. workYmd null 이면 false.
     */
    SchedOptionResponse getSchedOptions(String cmpnyCd, String siteCd, String userCd, String workYmd);
}
