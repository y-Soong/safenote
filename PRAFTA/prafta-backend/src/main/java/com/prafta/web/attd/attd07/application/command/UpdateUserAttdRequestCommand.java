package com.prafta.web.attd.attd07.application.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdRequestParam;

/**
 * Command for {@code Attd07Mapper.updateUserAttdReqApprove}.
 *
 * PRAFTA-003: TB_USER_ATTD_REQ.ATTD_ID was renamed to TARGET_ID (now
 * generalised over attendance / overtime / leave). The {@code targetId} field
 * holds the resolved ATTD_ID for the attendance-modification path; future
 * request types will reuse the same column with different semantics.
 */
public record UpdateUserAttdRequestCommand(
      String reqId
    , String targetId
    , String siteCd
    , String processComment
    , String gvCmpnyCd
    , String gvUserCd
) {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserAttdRequestCommand.class);

    public static UpdateUserAttdRequestCommand from(String targetId, UpdateUserAttdRequestParam param) {

        if (targetId == null) {
            log.warn("UpdateUserAttdRequestCommand.from - required field missing: targetId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (param == null) {
            log.warn("UpdateUserAttdRequestCommand.from - param is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new UpdateUserAttdRequestCommand(
              param.reqId()
            , targetId
            , param.siteCd()
            , param.processComment()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
