package com.prafta.web.user.user09.application.query;

import com.prafta.web.user.user09.application.param.SelfJoinHistoryListParam;

/**
 * 소정-09: 셀프가입 처리 이력 목록 조회 쿼리 (매퍼 입력).
 *
 * <p>회사 스코프(gvCmpnyCd)는 반드시 SQL 술어로 들어간다(멀티테넌시).
 *
 * <p>처리기간은 <b>여기서 datetime 문자열로 정규화</b>한다(시작 00:00:00 / 종료 23:59:59).
 * 매퍼에서 {@code DATE(INSERT_DATE)} 같은 함수 술어를 쓰면 감사 로그 인덱스
 * (CMPNY_CD, RESOURCE_TYPE, INSERT_DATE)를 못 타기 때문이다.
 */
public record SelfJoinHistoryListQuery(
        String gvCmpnyCd
        , String siteCd
        , String nodeCd
        , String incSubNodeYn
        , String userKeyword
        , String actionType
        , String startDtime
        , String endDtime
        , int offset
        , int pageSize
) {
    public static SelfJoinHistoryListQuery from(SelfJoinHistoryListParam param) {

        return new SelfJoinHistoryListQuery(
                param.gvCmpnyCd()
                , param.siteCd()
                , (param.nodeCd() == null || param.nodeCd().isBlank()) ? null : param.nodeCd()
                , param.incSubNodeYn()
                , (param.userKeyword() == null || param.userKeyword().isBlank()) ? null : param.userKeyword().trim()
                , param.actionType()
                , param.startDate() == null ? null : param.startDate() + " 00:00:00"
                , param.endDate() == null ? null : param.endDate() + " 23:59:59"
                , (param.page() - 1) * param.pageSize()
                , param.pageSize()
        );
    }
}
