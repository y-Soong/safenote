package com.prafta.app.mypage.mypage01.application.param;

import com.prafta.app.mypage.mypage01.dto.request.BrkWaiveStandingRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * BW-12(§7-1, 2026-09-04): 휴게 미이용 상시 요청 저장 Param(앱 마이페이지).
 *
 * <p>식별자(cmpnyCd/userCd)는 TokenInfo 출처만 사용한다(IDOR 차단·관리자 대리 불가).
 * 값 검증('Y'/'N')은 Service 가 수행한다(ATTD_400_220) — 여기서는 운반만 한다.
 */
public record BrkWaiveStandingUpdateParam(
      String standingYn
    , TokenInfo tokenInfo
) {
    public static BrkWaiveStandingUpdateParam from(BrkWaiveStandingRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String raw = request.getStandingYn();
        return new BrkWaiveStandingUpdateParam(raw == null ? null : raw.trim(), tokenInfo);
    }
}
