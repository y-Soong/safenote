package com.prafta.web.user.user01.application.command;

import java.math.BigDecimal;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.UserHireDateParam;

/**
 * 입사일 변경 이력(tb_user_hire_date_history) INSERT 커맨드 (PRAFTA-017-4 → prafta-032).
 * HIST_ID는 서비스가 selectNextHireHistId로 미리 채번해 전달한다(연차 조정 멱등키 _HD{histId}에 사용).
 * CHANGED_BY 컬럼이 없으므로 변경자는 INSERT_NO(=gvUserCd)로 기록한다.
 * AFFECTED_GRANT_SNAPSHOT은 영향 분석 JSON을 그대로 저장한다(nullable).
 *
 * <p>prafta-032(D1/D5/D7): HANDLING_TYPE은 신규분 'MANUAL' 고정(NOT NULL 유지). 수동 연차 조정의
 * 전/후 법정 총량(oldGrantTotal/newGrantTotal)과 회수 사유(withdrawReason)를 함께 기록한다(nullable).
 */
public record UserHireDateHistoryCommand(
    String histId           /** 미리 채번한 HIST_ID (PK) */
    , String cmpnyCd
    , String userCd
    , String prevHireDate   /** YYYYMMDD */
    , String newHireDate    /** YYYYMMDD */
    , String changeReason
    , String handlingType
    , String affectedGrantSnapshot   /** JSON 문자열 (nullable) */
    , BigDecimal oldGrantTotal       /** 변경 전 법정 부여 총량 (nullable) */
    , BigDecimal newGrantTotal       /** 변경 후 목표 법정 부여 총량 (nullable) */
    , String withdrawReason          /** 회수 사유 (nullable) */
    , String gvUserCd
) {
    /** prafta-032: HANDLING_TYPE='MANUAL' 고정 + 수동 조정 추적 컬럼. */
    public static final String HANDLING_TYPE_MANUAL = "MANUAL";

    public static UserHireDateHistoryCommand of(
        UserHireDateParam param
        , String histId
        , String prevHireDate
        , String affectedGrantSnapshot
        , BigDecimal oldGrantTotal
        , BigDecimal newGrantTotal
        , String withdrawReason
    ) {
        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UserHireDateHistoryCommand(
            histId
            , param.gvCmpnyCd()
            , param.userCd()
            , prevHireDate
            , param.newHireDate()
            , param.changeReason()
            , HANDLING_TYPE_MANUAL
            , affectedGrantSnapshot
            , oldGrantTotal
            , newGrantTotal
            , withdrawReason
            , param.gvUserCd()
        );
    }
}
