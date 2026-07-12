package com.prafta.web.baim.baim07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim07.dto.request.LeaveConversionSaveRequest;

/**
 * 시간차 1일 환산시간 저장 Param (LC-02).
 *
 * <p>회사 스코프는 JWT 의 CMPNY_CD 로만 도출하고(body 신뢰 금지 — 타 회사 설정 변경 차단),
 * 권한 판정(AUTH_MASTER OR AUTH_HR_MANAGER)은 JWT 의 AUTH_CD 로 서비스 진입부에서 수행한다.
 */
public record LeaveConversionSaveParam(
      String applyFromDate
    , Integer dailyConvMinutes
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
) {

    public static LeaveConversionSaveParam from(LeaveConversionSaveRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new LeaveConversionSaveParam(
                request.getApplyFromDate(),
                request.getDailyConvMinutes(),
                tokenInfo.gv_cmpnyCd(),
                tokenInfo.gv_authCd(),
                tokenInfo.gv_userCd());
    }
}
