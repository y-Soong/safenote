package com.prafta.app.mypage.mypage01.application.param;

import com.prafta.app.mypage.mypage01.dto.request.UpdateDefaultSchRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * F-8-2: 본인 기본 근무타입 자기변경 Param(앱 마이페이지).
 *
 * <p>식별자(cmpnyCd/userCd)는 TokenInfo 출처만 사용한다(IDOR 차단). 사업장(SITE_CD)은 파라미터로 받지
 * 않는다 — 본인 사업장 변경은 이 흐름의 범위가 아니다.
 *
 * <p>PRAFTA-002(기본근무타입-승인제, 2026-08-26): 즉시 반영 → 요청 등록 전환에 따라
 * {@code reqReason}(변경 사유)을 운반한다. 필수 여부 검증은 Service 가 수행한다(req07 패턴 동일).
 */
public record UpdateDefaultSchParam(
      String defaultSchCd
    , String reqReason
    , TokenInfo tokenInfo
) {
    public static UpdateDefaultSchParam from(UpdateDefaultSchRequest request, TokenInfo tokenInfo) {
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_userCd() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return new UpdateDefaultSchParam(
              request.getDefaultSchCd()
            , request.getReqReason()
            , tokenInfo
        );
    }
}
