package com.prafta.web.risk.riskai01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.riskai01.dto.request.RiskAiRequest;

/**
 * 위험성평가 AI 도출 파라미터. 식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출한다(IDOR 차단).
 * 평가건 스코프 키(siteCd/processCd/assessmentCd)는 바디에서 받아 서비스에서 사업장 권한·존재를 검증한다.
 *
 * <p>v3: {@code kickoff}(자동 첫 질의 플래그), {@code adminImages}(관리자 추가 이미지, 요청단발) 추가.
 */
public record RiskAiParam(
    String siteCd
    , String processCd
    , String assessmentCd
    , String userMessage
    , String suppDesc
    , Boolean kickoff
    , List<RiskAiRequest.AdminImageIn> adminImages
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static RiskAiParam from(RiskAiRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new RiskAiParam(
            request.getSiteCd()
            , request.getProcessCd()
            , request.getAssessmentCd()
            , request.getUserMessage()
            , request.getSuppDesc()
            , request.getKickoff()
            , request.getAdminImages()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
