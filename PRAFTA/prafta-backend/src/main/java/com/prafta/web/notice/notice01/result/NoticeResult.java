package com.prafta.web.notice.notice01.result;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 공지 단건/목록 결과 VO (tb_notice 기준, camelCase 매핑).
 *
 * <p>isUnread/isUpdated 는 조회 본인 기준 계산값(요청서 §7) — boolean is- 접두 Jackson 탈락
 * 방지를 위해 @JsonProperty 로 직렬화 키를 고정한다.
 * targetSummary 는 대상 요약 문자열(전사 / 사업장명 / 노드명 외 N건) — service 에서 가공.
 * fileCnt 는 첨부 개수(목록 첨부 아이콘 표시용).
 *
 * <p>⚠️ MyBatis record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 * 보안상 EDIT_PWD(해시)는 결과 VO 에 절대 포함하지 않는다.
 */
public record NoticeResult(
    String cmpnyCd
    , String noticeId
    , String title
    , String content
    , String targetScope
    , String includeDailyYn
    , String popupYn
    , String popupFromYmd
    , String popupToYmd
    , String pinYn
    , Integer pinOrder
    , String insertNo
    , String insertDate
    , String insertUserNm
    , String updateDate
    , Integer fileCnt
    , @JsonProperty("isUnread") boolean isUnread
    , @JsonProperty("isUpdated") boolean isUpdated
    , String targetSummary
){
}
