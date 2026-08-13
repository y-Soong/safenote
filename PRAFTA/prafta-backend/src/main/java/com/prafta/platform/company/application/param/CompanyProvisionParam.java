package com.prafta.platform.company.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.company.dto.request.CompanyProvisionRequest;

/**
 * 신규 고객사 프로비저닝 파라미터.
 *
 * <p>식별자(운영자 USER_CD)는 토큰에서만 도출한다(클라 바디 신뢰 금지). 신규 회사코드는
 * 서비스 계층에서 발급하므로 본 파라미터에 포함하지 않는다. gvUserCd 는 생성 레코드의 INSERT_NO 로 쓴다.
 */
public record CompanyProvisionParam(
    String cmpnyNm
    , String bsnsLcnNo
    , String contractEndDate
    // 통상근로자 주 소정근로 분(선택). null 이면 기준값 행을 만들지 않고 코드 폴백 2400분을 쓴다.
    , Integer weekStdMinutes
    , String adminNm
    , String adminId
    , String adminMbl
    , String gvUserCd
) {
    public static CompanyProvisionParam from(CompanyProvisionRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new CompanyProvisionParam(
            request.getCmpnyNm()
            , request.getBsnsLcnNo()
            , request.getContractEndDate()
            , request.getWeekStdMinutes()
            , request.getAdminNm()
            , request.getAdminId()
            , request.getAdminMbl()
            , tokenInfo.gv_userCd()
        );
    }
}
