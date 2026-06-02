package com.prafta.web.baim.baim07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim07.dto.request.LeavePolicyHistoryListRequest;

/**
 * 정책 변경 이력 페이징 조회 Param.
 *
 * <p>GET endpoint이므로 권한 가드는 정책서 §8.5.7에 따라 "인증 사용자 + 사업장 스코프" 수준.
 * 본 Param은 JWT의 CMPNY_CD만 사용하여 회사 스코프를 강제한다.
 */
public record LeavePolicyHistoryListParam(
      int page
    , int size
    , String gvCmpnyCd
) {

    public static LeavePolicyHistoryListParam from(LeavePolicyHistoryListRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // request가 null이거나 빈 ModelAttribute일 수 있음 → 기본값 적용
        int page = 1;
        int size = 20;
        if (request != null) {
            if (request.getPage() != null && request.getPage() > 0) {
                page = request.getPage();
            }
            if (request.getSize() != null && request.getSize() > 0) {
                size = request.getSize();
            }
        }

        return new LeavePolicyHistoryListParam(page, size, tokenInfo.gv_cmpnyCd());
    }
}
