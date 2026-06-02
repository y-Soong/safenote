package com.prafta.app.mypage.mypage01.application.param;

import com.prafta.app.mypage.mypage01.dto.request.ProfileUpdateRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-010-02: 프로필 저장 Param.
 * <p>식별자(cmpnyCd/userCd)는 TokenInfo 출처만 사용한다(IDOR 차단).
 */
public record ProfileUpdateParam(
      String userNm
    , String genderCode
    , String birthDate
    , String email
    , String mblNo
    , String mobileVerificationToken
    , TokenInfo tokenInfo
) {
    public static ProfileUpdateParam from(ProfileUpdateRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new ProfileUpdateParam(
              request.getUserNm()
            , request.getGenderCode()
            , request.getBirthDate()
            , request.getEmail()
            , request.getMblNo()
            , request.getMobileVerificationToken()
            , tokenInfo
        );
    }
}
