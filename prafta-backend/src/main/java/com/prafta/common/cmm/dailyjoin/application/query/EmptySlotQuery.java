package com.prafta.common.cmm.dailyjoin.application.query;

/**
 * 일일사용자 회원가입 - 사업장의 첫 빈 슬롯 조회 쿼리.
 */
public record EmptySlotQuery(
    String cmpnyCd
    , String siteCd
) {
    public static EmptySlotQuery of(String cmpnyCd, String siteCd) {
        return new EmptySlotQuery(cmpnyCd, siteCd);
    }
}
