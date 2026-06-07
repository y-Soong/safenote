package com.prafta.web.notice.notice01.application.model;

/**
 * 공지 첨부 1건 운반 모델(다건 리스트 요소).
 */
public record NoticeFileModel(
    String fileMgmtCd
    , Integer sortIdx
){
}
