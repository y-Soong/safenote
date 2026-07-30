package com.prafta.app.leave.leaveflow.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 앱 연차 신청 폼 일자별 스케줄(휴게 포함) 조회 Param.
 *
 * <p>시간차 신청 시 휴게 가로지름(ATTD_400_055) 사전 안내를 위해 신청 대상일의
 * 근무/휴게 시각을 조회한다. 식별값(cmpny/site/user)은 토큰에서만 강제(IDOR 차단).
 */
public record LeaveDayScheduleParam(
      String cmpnyCd
    , String siteCd
    , String userCd
    , String workYmd
) {
    public static LeaveDayScheduleParam from(TokenInfo tokenInfo, String workYmd) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (workYmd == null || workYmd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (!workYmd.matches("\\d{8}")) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }
        return new LeaveDayScheduleParam(
              tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_userCd()
            , workYmd
        );
    }
}
