package com.prafta.web.user.user01.application.param;

import java.math.BigDecimal;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UserCreditRequest;

/**
 * 경력 인정 저장 파라미터 (PRAFTA-017-4).
 * 회사 스코프/권한/입력자는 토큰에서만 가져온다(IDOR 방지).
 */
public record UserCreditParam(
    String userCd
    , List<CreditItem> creditList
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public record CreditItem(
        Integer creditMonths
        , String reasonType
        , String reasonDetail
        // 경력인정 이원화(2026-08-21) — 원문 그대로 보존, 정규화/검증은 서비스 레이어(User01ServiceImpl)에서 수행.
        , String leaveCalcYn
        , BigDecimal extraLeaveDays
    ) {}

    public static UserCreditParam from(UserCreditRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        List<CreditItem> items = (request.getCreditList() == null)
            ? List.of()
            : request.getCreditList().stream()
                .map(it -> new CreditItem(
                    it.getCreditMonths()
                    , it.getReasonType()
                    , it.getReasonDetail()
                    , it.getLeaveCalcYn()
                    , it.getExtraLeaveDays()
                ))
                .toList();

        return new UserCreditParam(
            request.getUserCd()
            , items
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
