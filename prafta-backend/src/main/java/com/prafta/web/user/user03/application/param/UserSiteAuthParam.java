package com.prafta.web.user.user03.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user03.application.model.UserSiteAuthModel;
import com.prafta.web.user.user03.dto.request.UserSiteAuthRequest;

public record UserSiteAuthParam(
	List<UserSiteAuthModel> userSiteAuthModelList
) {
	public static UserSiteAuthParam from(List<UserSiteAuthRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트 자체 검증
        if (requests == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - UserInfoRequest");
        
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<UserSiteAuthModel> models = requests.stream()
            .map(request -> {
                return new UserSiteAuthModel(
            		request.getChk()
            		, request.getCmpnyCd()
            		, request.getUserCd()
            		, request.getSiteCd()
            		, request.getAllocYn()
            		, request.getUseYn()
            		
            		, tokenInfo.gv_cmpnyCd()
            		, tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new UserSiteAuthParam(models);
    }
}
