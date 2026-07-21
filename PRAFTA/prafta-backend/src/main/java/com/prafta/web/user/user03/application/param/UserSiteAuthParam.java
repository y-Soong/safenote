package com.prafta.web.user.user03.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.user.user03.application.model.UserSiteAuthModel;
import com.prafta.web.user.user03.dto.request.UserSiteAuthRequest;

public record UserSiteAuthParam(
	List<UserSiteAuthModel> userSiteAuthModelList
) {
	public static UserSiteAuthParam from(List<UserSiteAuthRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트/토큰 자체 검증
        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_003);

        // [보안 H-1] 사업장 권한 원장(TB_USER_SITE_AUTH)은 SiteAccessService 인가 판정의 원천이므로
        //   쓰기는 관리자 역할(master/hr/safe/system)만 허용한다. 무게이트 시 일반 사원이 자기부여로
        //   타 사업장 인가를 획득할 수 있다(권한상승).
        if (!AuthRoleUtils.canManageSite(tokenInfo.gv_authCd()))
        	throw new ApiException(UserErrorCode.USER_403_002);

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<UserSiteAuthModel> models = requests.stream()
            .map(request -> {
                // [보안 M-1] allocYn allow-list — USE_YN 에 그대로 저장되므로 'Y'/'N' 외 값(소문자 'n' 등)으로
                //   회수 차단 가드를 우회하거나 원장을 오염시키지 못하게 여기서 차단한다.
                String allocYn = request.getAllocYn();
                if (!"Y".equals(allocYn) && !"N".equals(allocYn))
                	throw new ApiException(CommonErrorCode.COMMON_400_002);
                return new UserSiteAuthModel(
            		request.getChk()
            		, request.getCmpnyCd()
            		, request.getUserCd()
            		, request.getSiteCd()
            		, allocYn
            		, request.getUseYn()

            		, tokenInfo.gv_cmpnyCd()
            		, tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new UserSiteAuthParam(models);
    }
}
