package com.prafta.web.user.user09.application.query;

import com.prafta.web.user.user09.application.param.SelfJoinListParam;

/**
 * 소정-09: 셀프가입 신청 목록 조회 쿼리 (매퍼 입력).
 *
 * <p>회사 스코프(gvCmpnyCd)는 반드시 SQL 술어로 들어간다(멀티테넌시).
 */
public record SelfJoinListQuery(
        String gvCmpnyCd
        , String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String accountStatus
        , String userKeyword
) {
    public static SelfJoinListQuery from(SelfJoinListParam param) {
        return new SelfJoinListQuery(
                param.gvCmpnyCd()
                , param.siteCd()
                , (param.nodeCd() == null || param.nodeCd().isBlank()) ? null : param.nodeCd()
                , param.incSubNodeYn()
                , param.accountStatus()
                , (param.userKeyword() == null || param.userKeyword().isBlank()) ? null : param.userKeyword().trim()
        );
    }
}
