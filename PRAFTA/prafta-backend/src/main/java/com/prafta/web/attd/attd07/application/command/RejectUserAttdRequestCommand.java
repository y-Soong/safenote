package com.prafta.web.attd.attd07.application.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.param.RejectDefaultSchChangeRequestParam;
import com.prafta.web.attd.attd07.application.param.RejectUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.RejectUserOvertimeRequestParam;

/**
 * Command for {@code Attd07Mapper.updateUserAttdReqReject} (PRAFTA-008).
 *
 * TB_USER_ATTD_REQ 를 반려('03') 상태로 전이하면서 처리자 / 반려사유 / 처리일시를
 * 기록한다. UPDATE 는 REQ_STATUS='01'(신청) 가드로 정확히 1행만 영향을
 * 받아야 하며, 0행이면 동시 처리 충돌로 보고 롤백한다.
 *
 * PRAFTA-010 의 초과근무 요청 반려도 동일 command/mapper 를 재사용한다
 * (처리 컬럼 집합과 status 전이가 동일하기 때문).
 */
public record RejectUserAttdRequestCommand(
      String reqId
    , String siteCd
    , String rejectReason
    , String gvCmpnyCd
    , String gvUserCd
) {

    private static final Logger log = LoggerFactory.getLogger(RejectUserAttdRequestCommand.class);

    public static RejectUserAttdRequestCommand from(RejectUserAttdRequestParam param) {

        if (param == null) {
            log.warn("RejectUserAttdRequestCommand.from - param is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new RejectUserAttdRequestCommand(
              param.reqId()
            , param.siteCd()
            , param.rejectReason()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }

    /**
     * PRAFTA-010 - 초과근무 요청 반려용 팩토리.
     * 처리 컬럼 집합과 status 전이가 근태 반려와 동일하므로 동일 command 를 재사용한다.
     */
    public static RejectUserAttdRequestCommand from(RejectUserOvertimeRequestParam param) {

        if (param == null) {
            log.warn("RejectUserAttdRequestCommand.from - OT param is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new RejectUserAttdRequestCommand(
              param.reqId()
            , param.siteCd()
            , param.rejectReason()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }

    /**
     * PRAFTA-003(기본근무타입-승인제) - 기본 근무타입 변경 요청 반려용 팩토리.
     * 처리 컬럼 집합과 status 전이가 근태 반려와 동일하므로(REQ_TYPE 무관 UPDATE) 동일 command 를 재사용한다.
     */
    public static RejectUserAttdRequestCommand from(RejectDefaultSchChangeRequestParam param) {

        if (param == null) {
            log.warn("RejectUserAttdRequestCommand.from - DefaultSchChange param is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new RejectUserAttdRequestCommand(
              param.reqId()
            , param.siteCd()
            , param.rejectReason()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
