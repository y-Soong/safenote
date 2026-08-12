package com.prafta.web.user.user10.application.query;

import com.prafta.web.user.user10.application.param.StdWorkUserListParam;

/**
 * 소정-10: 소정근로시간 관리 대상 목록 조회 쿼리 (매퍼 입력).
 *
 * <p>기준일(오늘)은 파라미터로 받지 않고 SQL 안에서 DB NOW() 로 만든다(JVM 시계 스큐 방지).
 */
public record StdWorkUserListQuery(
        String gvCmpnyCd
        , String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String userKeyword
) {
    public static StdWorkUserListQuery from(StdWorkUserListParam param) {
        return new StdWorkUserListQuery(
                param.gvCmpnyCd()
                , param.siteCd()
                , (param.nodeCd() == null || param.nodeCd().isBlank()) ? null : param.nodeCd()
                , param.incSubNodeYn()
                , (param.userKeyword() == null || param.userKeyword().isBlank()) ? null : param.userKeyword().trim()
        );
    }
}
