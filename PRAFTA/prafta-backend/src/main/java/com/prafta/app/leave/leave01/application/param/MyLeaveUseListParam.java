package com.prafta.app.leave.leave01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 앱 "연차 현황" 사용 내역(연 단위) 조회 Param.
 * <p>식별값(cmpnyCd/userCd)은 JWT 토큰에서만 도출한다(본인 자기조회만, MyLeaveSummaryParam 패턴).
 * year 는 조회 연도(YYYY, 선택) — 미지정이면 서비스가 DB 기준 올해로 보정한다.
 */
public record MyLeaveUseListParam(
      String cmpnyCd
    , String userCd
    , String year
) {
    public static MyLeaveUseListParam from(TokenInfo tokenInfo, String year) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        // year 는 선택값 — 있으면 4자리 연도(1900~2999)만 허용(그 외 400).
        String normalizedYear = null;
        if (StringUtils.hasText(year)) {
            if (!year.matches("^(19|2\\d)\\d{2}$")) {
                throw new ApiException(CommonErrorCode.COMMON_400_002);
            }
            normalizedYear = year;
        }

        return new MyLeaveUseListParam(cmpnyCd, userCd, normalizedYear);
    }
}
