package com.prafta.web.baim.baim01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;
import com.prafta.web.baim.baim01.dto.request.SiteInfoRequest;

public record SiteInfoParam(
	List<SiteInfoModel> siteInfoModelList
	// PRAFTA-COM-001-T2 보안 재작업: 사업장 저장 EP 역할 게이트용 JWT 도출 클레임(클라 바디 신뢰 금지).
	, String gvAuthCd
	, String gvUserCd
) {
    public static SiteInfoParam from(List<SiteInfoRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트 자체 검증
        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<SiteInfoModel> models = requests.stream()
            .map(req -> {
                return new SiteInfoModel(
                    req.getCmpnyCd()
                    , req.getSiteCd()
                    , req.getSiteNo()
                    , req.getSiteNm()
                    , req.getAddr1()
                    , req.getAddr2()
                    , req.getZipCode()
                    , req.getStrDate()
                    , req.getEndDate()
                    , req.getUseYn()
                    , req.getSiteAdminCd()
                    , req.getTelNo()
                    , req.getGpsRange()
                    , req.getSiteDesc()
                    , req.getLat()
                    , req.getLon()
                    , tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                );
            })
            .toList();

        // 역할 게이트는 JWT 도출 authCd 만 신뢰한다(바디 미신뢰). userCd 는 차단 로그 추적용.
        return new SiteInfoParam(models, tokenInfo.gv_authCd(), tokenInfo.gv_userCd());
    }
}
