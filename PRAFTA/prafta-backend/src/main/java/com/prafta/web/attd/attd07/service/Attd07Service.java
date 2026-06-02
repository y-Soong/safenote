package com.prafta.web.attd.attd07.service;

import com.prafta.web.attd.attd07.application.param.DailyAttdDetailDeleteParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailsParam;
import com.prafta.web.attd.attd07.application.param.MonthlyAttdListParam;
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
}
