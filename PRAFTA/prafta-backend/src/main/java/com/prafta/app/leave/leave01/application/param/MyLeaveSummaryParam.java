package com.prafta.app.leave.leave01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-005: 앱 "연차 현황"(본인 잔여연차 상세) 조회 Param.
 * <p>식별값(cmpnyCd/userCd)은 JWT 토큰에서만 도출한다(클라 입력 무시).
 * 본인 자기조회만 허용 — 타인 userCd 주입 불가(파라미터 미수신),
 * cross-company 차단(cmpnyCd 클레임 강제). home01 의 from(TokenInfo) 패턴을 동일하게 따른다.
 */
public record MyLeaveSummaryParam(
    String cmpnyCd
    , String userCd
    , TokenInfo tokenInfo
) {
    public static MyLeaveSummaryParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();

        // 회사/사용자 식별값이 토큰에 없으면 명확한 에러
        if (!StringUtils.hasText(cmpnyCd)
                || !StringUtils.hasText(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new MyLeaveSummaryParam(cmpnyCd, userCd, tokenInfo);
    }
}
