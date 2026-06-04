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

    , String gvUserCd
) {

    private static final Logger log = LoggerFactory.getLogger(InsertUserOvertimeCommand.class);

    public static InsertUserOvertimeCommand from(String otId,
                                                 UpdateUserOvertimeRequestParam param,
                                                 OvertimeItemModel ot,
                                                 int workMinutes) {

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

            , param.gvUserCd()
        );
    }
}
