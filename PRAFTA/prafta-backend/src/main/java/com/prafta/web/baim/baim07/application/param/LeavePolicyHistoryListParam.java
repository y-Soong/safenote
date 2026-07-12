package com.prafta.web.baim.baim07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim07.dto.request.LeavePolicyHistoryListRequest;

/**
 * 정책 변경 이력 페이징 조회 Param.
 *
 * <p>권한 가드는 정책서 §8.5.7에 따라 정책 변경 권한자(AUTH_MASTER OR AUTH_HR_MANAGER)로 제한한다.
 * 변경 이력에는 변경자 실명(USER_NM, 평문) 등 민감정보가 포함되므로 서비스 진입부 ensureManager로 강제한다.
 * 회사 스코프는 JWT의 CMPNY_CD로만 도출하고(body 신뢰 금지), 권한 판정은 JWT의 AUTH_CD로 수행한다.
 */
public record LeavePolicyHistoryListParam(
      int page
    , int size
    , String gvCmpnyCd
    , String gvAuthCd
) {

    public static LeavePolicyHistoryListParam from(LeavePolicyHistoryListRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
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

        return new LeavePolicyHistoryListParam(page, size, tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd());
    }
}
