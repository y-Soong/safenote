package com.prafta.web.user.user01.application.param;

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
        , String reasonDetail
    ) {}

    public static UserCreditParam from(UserCreditRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        List<CreditItem> items = (request.getCreditList() == null)
            ? List.of()
            : request.getCreditList().stream()
                .map(it -> new CreditItem(it.getCreditMonths(), it.getReasonDetail()))
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
