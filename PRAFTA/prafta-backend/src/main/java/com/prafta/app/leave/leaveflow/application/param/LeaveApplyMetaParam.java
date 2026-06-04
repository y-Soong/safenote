package com.prafta.app.leave.leaveflow.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-018-A: 연차 신청 폼 메타(apply-meta) / 본인 프리셋(approval-presets) 조회 Param.
 *
 * <p>식별값(cmpnyCd/userCd)은 JWT 토큰에서만 도출한다(클라 입력 무시 — IDOR 차단).
 *   leave01 {@code MyLeaveSummaryParam.from(TokenInfo)} 패턴을 동일하게 따른다.
 */
public record LeaveApplyMetaParam(
      String cmpnyCd
    , String userCd
    , TokenInfo tokenInfo
) {
    public static LeaveApplyMetaParam from(TokenInfo tokenInfo) {

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

        return new LeaveApplyMetaParam(cmpnyCd, userCd, tokenInfo);
    }
}
