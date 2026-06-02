package com.prafta.web.baim.baim01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim01.application.model.SiteInfoModel;
import com.prafta.web.baim.baim01.dto.request.SiteInfoRequest;

public record SiteInfoParam(
	List<SiteInfoModel> siteInfoModelList
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

        return new SiteInfoParam(models);
    }
}
