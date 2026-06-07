package com.prafta.web.notice.notice01.application.model;

/**
 * 공지 대상 1건 운반 모델(다건 리스트 요소, ChkptInfoModel 패턴).
 * SITE 스코프 = nodeCd null. NODE 스코프 = nodeCd 지정.
 */
public record NoticeTargetModel(
    String siteCd
    , String nodeCd
    , String includeDescendantsYn
){
}
