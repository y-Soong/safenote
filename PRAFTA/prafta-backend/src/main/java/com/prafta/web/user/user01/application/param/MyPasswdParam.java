package com.prafta.web.user.user01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.MyPasswdRequest;

public record MyPasswdParam(
    String cmpnyCd,
    String userCd,
    String currentPw,
    String newPw
) {
    public static MyPasswdParam from(MyPasswdRequest request, TokenInfo tokenInfo) {
        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        // 유효 로그인 토큰 필수화: 토큰 미존재/무효 시 거부한다.
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 본인 비밀번호 변경: 대상 회사/사용자 식별자는 request를 무시하고 토큰 값으로 강제한다 (IDOR 방지).
        return new MyPasswdParam(
            tokenInfo.gv_cmpnyCd(),
            tokenInfo.gv_userCd(),
            request.getCurrentPw(),
            request.getNewPw()
        );
    }
}
