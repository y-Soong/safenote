package com.prafta.common.cmm.baseinfo.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.cmm.baseinfo.dto.request.SiteNodeListRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 회원가입(비로그인) 단계 사업장 소속 부서 조회 Param.
 *
 * <p>{@link SiteNodeListParam} 의 NoAuth 변형. cmpnyCd 와 siteCd 가 모두 필수.
 * 사업장 선택이 선행된 다음에만 호출되는 흐름이므로 두 식별값을 모두 클라이언트가 제공한다.
 */
public record JoinSiteNodeListParam(
        String cmpnyCd
        , String siteCd
        , String nodeCd
        , String nodeType
        , String nodeNm
        , String parentNodeNm
) {
    public static JoinSiteNodeListParam from(SiteNodeListRequest request) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        String cmpnyCd = request.getCmpnyCd();
        String siteCd = request.getSiteCd();
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd))
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new JoinSiteNodeListParam(
                cmpnyCd
                , siteCd
                , request.getNodeCd()
                , request.getNodeType()
                , request.getNodeNm()
                , request.getParentNodeNm()
        );
    }
}
