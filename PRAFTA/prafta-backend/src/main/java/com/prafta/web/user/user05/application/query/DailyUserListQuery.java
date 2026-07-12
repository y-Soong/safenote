package com.prafta.web.user.user05.application.query;

/**
 * 일일사용자 관리(조회) 쿼리.
 *
 * <p>전화 검색은 평문 대신 mblNoHmac(정확매칭)/mblNoLast4(부분매칭) 파생값만 전달한다.
 * managerYn='Y'(master/hr)면 사업장 스코프 미적용(전사), 그 외는 TB_USER_SITE_AUTH 매핑 사업장으로 제한.
 * occupyFrom/occupyTo 는 yyyy-MM-dd(포함). limit/offset 은 서버에서 산출(페이징).
 */
public record DailyUserListQuery(
    String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String userNm
    , String mblNoHmac
    , String mblNoLast4
    , String occupyFrom
    , String occupyTo
    , String managerYn
    , int limit
    , int offset
    , String gvCmpnyCd
    , String gvUserCd
){
}
