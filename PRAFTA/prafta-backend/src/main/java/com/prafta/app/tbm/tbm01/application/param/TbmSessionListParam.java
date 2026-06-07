package com.prafta.app.tbm.tbm01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.app.tbm.tbm01.dto.request.TbmSessionListRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-tbm: 탭별 세션 리스트(A1/A2/A3) Param.
 * <p>tab(요청) + tokenInfo 를 service 단일 인자로 정리한다.
 * <p>tab 은 AVAILABLE/IN_PROGRESS/COMPLETED 만 허용(허용값 외 거부).
 */
public record TbmSessionListParam(
    String tab
    , TokenInfo tokenInfo
) {
    public static final String TAB_AVAILABLE = "AVAILABLE";
    public static final String TAB_IN_PROGRESS = "IN_PROGRESS";
    public static final String TAB_COMPLETED = "COMPLETED";

    public static TbmSessionListParam from(TbmSessionListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String tab = request.getTab();
        if (!StringUtils.hasText(tab))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!TAB_AVAILABLE.equals(tab) && !TAB_IN_PROGRESS.equals(tab) && !TAB_COMPLETED.equals(tab))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (!StringUtils.hasText(tokenInfo.gv_cmpnyCd())
                || !StringUtils.hasText(tokenInfo.gv_siteCd())
                || !StringUtils.hasText(tokenInfo.gv_userCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TbmSessionListParam(tab, tokenInfo);
    }
}
