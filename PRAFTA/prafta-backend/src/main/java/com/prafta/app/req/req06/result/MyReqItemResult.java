package com.prafta.app.req.req06.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * prafta-app-006: tb_user_attd_req 행 매핑 결과.
 *
 * <p>mapper SELECT 컬럼명과 1:1 대응. Service 에서 가공해 응답 DTO 로 변환한다.
 */
public record MyReqItemResult(
        String reqId
        , String reqType
        , String reqStatus
        , String workYmd
        , String startDate
        , String startTime
        , String endDate
        , String endTime
        , String otType
        , String leaveType
        , BigDecimal leaveDays
        , String processComment
        , LocalDateTime processDate
        , LocalDateTime insertDate
) {
}
