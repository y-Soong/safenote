package com.prafta.web.user.user01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.dto.request.UserInfoRequest;

public record UserInfoParam(
	List<UserInfoModel> userInfoModelList
) {
    public static UserInfoParam from(List<UserInfoRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트 자체 검증
        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<UserInfoModel> models = requests.stream()
            .map(req -> {
                return new UserInfoModel(
                    req.getCmpnyCd(),
                    req.getUserCd(),
                    req.getUserId(),
                    req.getUserPw(),
                    req.getUserNm(),
                    req.getMblNo(),
                    req.getEmail(),
                    req.getGender(),
                    req.getBirthDt(),
                    req.getSiteCd(),
                    req.getNodeCd(),
                    req.getOriNodeCd(),
                    req.getAuthCd(),
                    req.getUseYn(),
                    tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new UserInfoParam(models);
    }
}
