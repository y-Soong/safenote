package com.prafta.app.mypage.mypage01.application.param;

import com.prafta.app.mypage.mypage01.dto.request.MobileSendRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-010-03a: 휴대폰 변경 인증번호 발송 Param.
 */
public record MobileSendParam(
      String mblNo
    , TokenInfo tokenInfo
    // SMS2-B4: 요청 IP 해시(IP 축 상한 재료). 컨트롤러가 SmsClientIpResolver 로 해석해 넣는다.
    // ★확정하지 못하면 null 이며 그때는 IP 축을 판정하지 않는다(fail-open).
    , String ipHash
) {
    public static MobileSendParam from(MobileSendRequest request, TokenInfo tokenInfo, String ipHash) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new MobileSendParam(request.getMblNo(), tokenInfo, ipHash);
    }
}
