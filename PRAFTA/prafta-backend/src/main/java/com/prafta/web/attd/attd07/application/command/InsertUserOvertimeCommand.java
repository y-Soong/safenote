package com.prafta.web.attd.attd07.application.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.model.OvertimeItemModel;
import com.prafta.web.attd.attd07.application.param.UpdateUserOvertimeRequestParam;

/**
 * INSERT payload for TB_USER_OVERTIME_MGMT.
 *
 * Built once per OT segment from {@link UpdateUserOvertimeRequestParam} +
 * {@link OvertimeItemModel} + a sequence-issued OT_ID.
 *
 * Conventions matching the table:
 *   - PLAN_* and ACTUAL_* are seeded with the same client-supplied values
 *     because this endpoint records *completed* overtime; the worker has
 *     already performed it and the admin is confirming it.
 *   - OT_STATUS is always 'COMPLETED' for this endpoint.
 *   - BREAK_MINUTES defaults to 0 (no break logged at this point).
 *   - WORK_MINUTES is the minute span between start and end stamps.
 *
 * <p>소정-07 M-4: 단축근무자(육아기·가족돌봄) 연장근로의 <b>근로자 명시 청구 확인 기록</b>
 * ({@code REDUCED_CLAIM_YN} / {@code REDUCED_CLAIM_BY})을 함께 적재한다. 위반 시 1천만원 이하
 * 벌금이 따르는 법정 요건이라 판정만 하고 버리면 증빙이 남지 않는다(정책 §11.3).
 * 단축 대상이 아닌 대다수 OT 는 두 값이 <b>null</b> 이며, 확인 일시는 SQL 의 NOW() 로 채운다.
 * ★단축 사유코드는 저장하지 않는다(M-3 규약 — 건강정보·가족관계 정보화 방지).
 */
public record InsertUserOvertimeCommand(
      String otId
    , String gvCmpnyCd
    , String siteCd
    , String userCd

    , String attdId
    , String reqId

    , String workYmd
    , String nodeCd

    , String planStartDate
    , String planStartTime
    , String planEndDate
    , String planEndTime

    , String actualStartDate
    , String actualStartTime
    , String actualEndDate
    , String actualEndTime

    , Integer workMinutes
    , Integer breakMinutes

    , String otStatus

    /** 소정-07 M-4: 단축근무 연장근로 명시 청구 확인 여부('Y' 또는 null=해당없음). */
    , String reducedClaimYn
    /** 소정-07 M-4: 명시 청구 확인 주체 USER_CD (REQ 경유=신청 근로자 본인 / 직접등록=확인 관리자). */
    , String reducedClaimBy

    , String gvUserCd
) {

    private static final Logger log = LoggerFactory.getLogger(InsertUserOvertimeCommand.class);

    /**
     * 소정-07 M-4: 명시 청구 확인 기록을 함께 실어 OT 행을 만든다.
     *
     * <p>★확인 기록 없는 4-인자 오버로드는 의도적으로 두지 않는다. 그런 편의 생성자가 있으면
     * 단축 대상 근로자의 OT 를 만들면서 감사 컬럼을 조용히 NULL 로 남기는 실수가 가능해지고,
     * 그 순간 법정 요건의 증빙이 사라진다. 호출부가 항상 게이트 판정 결과를 명시하게 강제한다.
     *
     * @param reducedClaimYn 단축 대상이면 "Y", 아니면 null (게이트 판정 결과로 결정)
     * @param reducedClaimBy 확인 주체 USER_CD. reducedClaimYn 이 null 이면 함께 null
     */
    public static InsertUserOvertimeCommand from(String otId,
                                                 UpdateUserOvertimeRequestParam param,
                                                 OvertimeItemModel ot,
                                                 int workMinutes,
                                                 String reducedClaimYn,
                                                 String reducedClaimBy) {

        // SEC-019 - do not leak the precise missing field name to the client.
        // Log the internal field name server-side and surface a generic 400.
        if (otId == null || otId.isEmpty()) {
            log.warn("InsertUserOvertimeCommand.from - required param missing: otId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (param == null) {
            log.warn("InsertUserOvertimeCommand.from - required param missing: UpdateUserOvertimeRequestParam");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (ot == null) {
            log.warn("InsertUserOvertimeCommand.from - required param missing: OvertimeItemModel");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new InsertUserOvertimeCommand(
              otId
            , param.gvCmpnyCd()
            , param.siteCd()
            , param.userCd()

            , param.attdId()
            , param.reqId()

            , param.workYmd()
            , param.nodeCd()

            , ot.startDate()
            , ot.startTime()
            , ot.endDate()
            , ot.endTime()

            , ot.startDate()
            , ot.startTime()
            , ot.endDate()
            , ot.endTime()

            , Integer.valueOf(workMinutes)
            , Integer.valueOf(0)

            , "COMPLETED"

            , reducedClaimYn
            , reducedClaimBy

            , param.gvUserCd()
        );
    }
}
