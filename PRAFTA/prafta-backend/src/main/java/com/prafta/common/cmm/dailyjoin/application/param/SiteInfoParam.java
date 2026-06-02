package com.prafta.common.cmm.dailyjoin.application.param;

import com.prafta.common.cmm.dailyjoin.dto.request.SiteInfoRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 일일사용자 회원가입 - 회사/사업장 정보 조회 파라미터.
 * 비로그인 화면이므로 토큰 클레임을 사용하지 않고 요청 본문(쿼리)만 사용한다.
 */
public record SiteInfoParam(
    String cmpnyCd
    , String siteCd
) {
    public static SiteInfoParam from(SiteInfoRequest request) {

        if (request == null
                || request.getCmpnyCd() == null || request.getCmpnyCd().isBlank()
                || request.getSiteCd() == null || request.getSiteCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 형식/길이 서버측 검증
        DailyJoinValidators.validateCmpnyCd(request.getCmpnyCd());
        DailyJoinValidators.validateSiteCd(request.getSiteCd());

        return new SiteInfoParam(
            request.getCmpnyCd()
            , request.getSiteCd()
        );
    }
}
