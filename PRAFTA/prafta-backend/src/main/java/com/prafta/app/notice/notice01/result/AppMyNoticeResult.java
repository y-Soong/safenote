package com.prafta.app.notice.notice01.result;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 앱 "내 공지 목록"(카드/전체목록) 1건 결과 VO (신규 쿼리 selectMyNoticeList).
 * isUnread = 미확인(하이브리드, prafta-app-025): 팝업 활성 공지는 CONFIRMED 미존재,
 *   그 외(비팝업/팝업기간 종료)는 LAST_READ_DATE IS NULL. 종배지 countMyUnreadNotice 와 동일 정의.
 * isImportant = (PIN_YN='Y').
 * boolean is- 접두 Jackson 탈락 방지로 @JsonProperty 고정.
 * ⚠️ MyBatis record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 */
public record AppMyNoticeResult(
    String noticeId
    , String title
    , String pinYn
    , String insertDate
    , Integer fileCnt
    , @JsonProperty("isUnread") boolean isUnread
    , @JsonProperty("isImportant") boolean isImportant
){
}
