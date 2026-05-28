package com.prafta.web.user.user01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.model.UserInfoModel;
import com.prafta.web.user.user01.dto.request.UserInfoRequest;

public record UserInfoParam(
	List<UserInfoModel> userInfoModelList
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
) {
    public static UserInfoParam from(List<UserInfoRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트/토큰 검증
        if (requests == null || tokenInfo == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        // prafta-019-B 보안: 권한/회사 스코프 강제용 클레임 필수
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        //    회사 스코프(CMPNY_CD)는 요청 body가 아니라 토큰값으로 강제한다 (cross-tenant IDOR 차단).
        List<UserInfoModel> models = requests.stream()
            .map(req -> {
                return new UserInfoModel(
                    tokenInfo.gv_cmpnyCd(),
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
                    req.getRankCd(),
                    req.getUseYn(),
                    tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new UserInfoParam(models, tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd(), tokenInfo.gv_userCd());
    }
}
