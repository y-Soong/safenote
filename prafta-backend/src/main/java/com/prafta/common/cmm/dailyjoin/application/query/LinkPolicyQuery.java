package com.prafta.common.cmm.dailyjoin.application.query;

/**
 * 일일사용자 회원가입 - 사업장별 계정등록 정책(TB_DAILY_USER_LINK_POLICY) 사용여부 조회 쿼리.
 */
public record LinkPolicyQuery(
    String cmpnyCd
    , String siteCd
) {
    public static LinkPolicyQuery of(String cmpnyCd, String siteCd) {
        return new LinkPolicyQuery(cmpnyCd, siteCd);
    }
}
