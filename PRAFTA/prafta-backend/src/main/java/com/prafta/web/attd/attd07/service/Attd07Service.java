package com.prafta.web.attd.attd07.service;

import com.prafta.web.attd.attd07.application.param.ApproveDefaultSchChangeRequestParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailDeleteParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailsParam;
import com.prafta.web.attd.attd07.application.param.DeleteUserOvertimeParam;
import com.prafta.web.attd.attd07.application.param.ApproveSchedModifyRequestParam;
import com.prafta.web.attd.attd07.application.param.MonthlyAttdListParam;
import com.prafta.web.attd.attd07.application.param.RejectDefaultSchChangeRequestParam;
import com.prafta.web.attd.attd07.application.param.RejectUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.RejectUserOvertimeRequestParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdInfosParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserOvertimeRequestParam;
import com.prafta.web.attd.attd07.dto.response.AttdRecordListResponse;
import com.prafta.web.attd.attd07.dto.response.DailyAttdDetailsResponse;

public interface Attd07Service {

    AttdRecordListResponse getMonthlyAttdList(MonthlyAttdListParam param);

    void updateUserAttdInfos(UpdateUserAttdInfosParam param);

    DailyAttdDetailsResponse getDailyAttdDetails(DailyAttdDetailsParam param);

    void dailyAttdDetailDelete(DailyAttdDetailDeleteParam param);

    void updateUserAttdRequest(UpdateUserAttdRequestParam param);

    /**
     * PRAFTA-008 - 근태(REQ_TYPE='01' 근태생성 / '02' 근태수정) 요청을 반려한다.
     *
     * 승인과 동일한 권위 검증(REQ row 로더 / REQ_TYPE 가드 / 신청('01') 상태 가드 /
     * body-REQ 변조검증)을 거치되 출퇴근 값은 실제 반영하지 않는다.
     * TB_USER_ATTD_REQ 를 반려('03') 상태로 전이하고 TB_USER_ATTD_HIST 에
     * HIST_TYPE='07' 반려 이력 1행만 남긴다.
     */
    void rejectUserAttdRequest(RejectUserAttdRequestParam param);

    /**
     * PRAFTA-010 - 초과근무(REQ_TYPE='03', 초과근무생성) 요청을 반려한다.
     *
     * SEC-015~017 가드(매니저 게이트 / scope 검증)를 거친 뒤
     * TB_USER_ATTD_REQ 의 처리 컬럼만 갱신한다. 근태 반려와 달리
     * TB_USER_ATTD_HIST 에는 이력을 남기지 않는다.
     */
    void rejectUserOvertimeRequest(RejectUserOvertimeRequestParam param);

    /**
     * Registers one or more overtime rows into TB_USER_OVERTIME_MGMT for a worker.
     *
     * The service validates each requested OT segment against the
     * "allowed window" (standardized work time minus scheduled time) for the
     * specified work day. PRAFTA-003.
     */
    void updateUserOvertimeRequests(UpdateUserOvertimeRequestParam param);

    /**
     * com-013 #6 - 일자 상세 관리자 직접 등록 OT(OT_ID 보유)를 즉시 소프트삭제(DEL_YN='Y')한다.
     *
     * <p>매니저 게이트(canManageNode) + 마감 가드(ensureNotClosed) + 대상 사용자 scope 검증을 거친 뒤
     * (cmpny, site, user, otId) 일치 + 활성(DEL_YN='N') OT 1행을 삭제한다. 0행이면 스코프 밖이거나
     * 이미 삭제/취소된 OT 로 보고 {@code ATTD_404_012} 로 거부한다. 처리 이력(HIST)은 남기지 않는다
     * (기존 OT 연쇄 cascade 삭제와 정합).
     */
    void deleteUserOvertime(DeleteUserOvertimeParam param);

    /**
     * PRAFTA-APP-007 - 스케줄 수정 요청(REQ_TYPE='10')을 승인한다.
     *
     * 근태 승인(updateUserAttdRequest)과 동일한 권위 검증(REQ row 로더 / REQ_TYPE 가드 /
     * 매니저 게이트 / 마감 가드 / 신청('01') 상태 가드 / body-REQ 변조검증 / 대상 사용자 scope)을
     * 거친 뒤, REQ row 의 SCH_CD 를 권위 값으로 tb_user_work_plan 의 WORK_PLAN_CD 한 칸을
     * upsert 하고 TB_USER_ATTD_REQ 를 승인('02') 상태로 전이한다. 처리 이력(HIST)은 남기지 않는다(D3).
     */
    void approveSchedModifyRequest(ApproveSchedModifyRequestParam param);

    /**
     * PRAFTA-APP-007 - 스케줄 수정 요청(REQ_TYPE='10')을 반려한다.
     *
     * 승인과 동일한 권위 검증을 거치되 tb_user_work_plan 은 일절 건드리지 않고(D4),
     * TB_USER_ATTD_REQ 를 반려('03') 상태로 전이하며 처리자 / 반려사유(필수) / 처리일시를
     * 기록한다. 처리 이력(HIST)은 남기지 않는다(D3). 반려 DTO/Param/Command 는 근태 반려와
     * 공유한다(RejectUserAttdRequest*).
     */
    void rejectSchedModifyRequest(RejectUserAttdRequestParam param);

    /**
     * PRAFTA-003(기본근무타입-승인제) - 기본 근무타입 변경 요청(REQ_TYPE='14')을 승인한다.
     *
     * <p>스케줄 수정 승인(approveSchedModifyRequest)과 동일한 권위 검증(REQ row 로더 / REQ_TYPE 가드 /
     * 결재선 현재 단계 소유권 / 신청('01') 상태 가드 / body-REQ 변조검증 / 대상 사용자 scope)을 거치되,
     * 특정 근무일에 종속되지 않으므로 마감 가드 / 교차일 겹침 가드 / shift 잠금 가드는 적용하지 않는다
     * (E3 연차 잠금은 applyDefaultSchChange 내부 가드가 승인 반영 시점에 자동으로 보존 처리).
     *
     * <p>화이트리스트 재검증(DefaultSchOptionService.isValidDefaultSch) 후 결재선 단계 전진, 최종 단계일
     * 때만 {@code DefaultSchGenService.applyDefaultSchChange} 로 반영하고 REQ 를 승인('02') 상태로 전이한다.
     */
    void approveDefaultSchChangeRequest(ApproveDefaultSchChangeRequestParam param);

    /**
     * PRAFTA-003(기본근무타입-승인제) - 기본 근무타입 변경 요청(REQ_TYPE='14')을 반려한다.
     *
     * <p>승인과 동일한 권위 검증을 거치되 {@code TB_USER.DEFAULT_SCH_CD}/{@code TB_USER_WORK_PLAN} 은
     * 일절 건드리지 않고, TB_USER_ATTD_REQ 를 반려('03') 상태로 전이하며 처리자/반려사유(필수)/처리일시를
     * 기록한다. 처리 이력(HIST)은 남기지 않는다(스케줄 수정 반려와 동일 관례).
     */
    void rejectDefaultSchChangeRequest(RejectDefaultSchChangeRequestParam param);
}
