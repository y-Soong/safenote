package com.prafta.web.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeScopeParam;

/**
 * 발행자 대상선택 트리 조회 쿼리.
 * isCompanyWide(master 등 전사 권한) 일 때는 전사 사업장/노드를 반환,
 * 아니면 권한 보유 사업장(tb_user_site_auth) + 소속 사업장 자기노드+자손으로 제한한다.
 */
public record NoticeScopeQuery(
    String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvNodeCd
    , boolean isCompanyWide
){
    public static NoticeScopeQuery from(NoticeScopeParam param, boolean isCompanyWide) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeScopeQuery(
            param.gvCmpnyCd()
            , param.gvUserCd()
            , param.gvSiteCd()
            , param.gvNodeCd()
            , isCompanyWide
        );
    }
}
