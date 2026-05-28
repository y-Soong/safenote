package com.prafta.common.cmm.dailyjoin.application.query;

import com.prafta.common.cmm.dailyjoin.application.param.SiteInfoParam;

/**
 * 일일사용자 회원가입 - 회사/사업장 정보 조회 쿼리.
 */
public record SiteInfoQuery(
    String cmpnyCd
    , String siteCd
) {
    public static SiteInfoQuery from(SiteInfoParam param) {
        return new SiteInfoQuery(
            param.cmpnyCd()
            , param.siteCd()
        );
    }
}
