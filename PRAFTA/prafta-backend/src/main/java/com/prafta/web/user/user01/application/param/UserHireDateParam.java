package com.prafta.web.user.user01.application.param;

import java.math.BigDecimal;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UserHireDateRequest;

/**
 * 입사일 변경 파라미터 (PRAFTA-017-4 → prafta-032 수동 연차 조정 전환).
 * 회사 스코프/권한/변경자는 토큰에서만 가져온다(IDOR 방지).
 * newHireDate는 YYYYMMDD로 정규화한다.
 *
 * <p>prafta-032(D1): handlingType(처리방식 자동계산) 폐기. targetStatutoryGrantDays(목표 법정 부여량,
 * nullable)와 withdrawReason(회수 사유, nullable)을 받는다.
 */
public record UserHireDateParam(
    String userCd
    , String newHireDate   /** YYYYMMDD */
    , BigDecimal targetStatutoryGrantDays  /** 목표 법정 부여량 (nullable, null이면 조정 없음) */
    , String changeReason
    , String withdrawReason  /** 회수 사유 (nullable, 회수 발생 시 필수) */
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static UserHireDateParam from(UserHireDateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // YYYY-MM-DD -> YYYYMMDD 정규화
        String date = request.getNewHireDate();
        if (date != null) {
            date = date.replace("-", "");
        }

        return new UserHireDateParam(
            request.getUserCd()
            , date
            , request.getTargetStatutoryGrantDays()
            , request.getChangeReason()
            , request.getWithdrawReason()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
