package com.prafta.app.notice.notice01.result;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 앱 공지 단건 상세 결과 VO (tb_notice 기준, camelCase 매핑).
 * isUnread 는 조회 본인 기준 계산값 — boolean is- 접두 Jackson 탈락 방지로 @JsonProperty 고정.
 * 보안상 EDIT_PWD(해시)는 결과 VO 에 절대 포함하지 않는다.
 * ⚠️ MyBatis record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 */
public record AppNoticeResult(
    String noticeId
    , String title
    , String content
    , String pinYn
    , String insertDate
    , String insertUserNm
    , Integer fileCnt
    , @JsonProperty("isUnread") boolean isUnread
){
}
