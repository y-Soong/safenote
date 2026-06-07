package com.prafta.web.notice.notice01.result;

/**
 * 공지 대상 1건 결과 VO (tb_notice_target 기준).
 * siteNm/nodeNm 은 join 으로 해석한 표시명.
 */
public record NoticeTargetResult(
    Integer targetSeq
    , String siteCd
    , String siteNm
    , String nodeCd
    , String nodeNm
    , String includeDescendantsYn
){
}
