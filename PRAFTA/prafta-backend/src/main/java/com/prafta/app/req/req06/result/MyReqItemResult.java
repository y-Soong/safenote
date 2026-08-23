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
        , String leaveType
        , BigDecimal leaveDays
        , String schCd
        , String processComment
        , LocalDateTime processDate
        , LocalDateTime insertDate
        , String siteCd    // 작업지시서_소속이동-이력가시성-보정: 발의 당시 소속 사업장 코드. 위치매핑 14번째(마지막 직전).
        , String siteNm    // 사업장명(TB_SITE JOIN). 위치매핑 마지막(15번째).
) {
}
