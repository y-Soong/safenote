package com.prafta.app.notice.notice01.result;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 앱 로그인 팝업 노출 공지 1건 결과 VO (웹 NoticePopupResult 미러).
 * pinYn/isDaily 로 팝업 버튼 셋을 프론트가 분기한다(§6-6).
 * boolean is- 접두 Jackson 탈락 방지로 @JsonProperty 고정.
 * ⚠️ MyBatis record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 */
public record AppNoticePopupResult(
    String noticeId
    , String title
    , String content
    , String pinYn
    , String popupFromYmd
    , String popupToYmd
    , String insertDate
    , Integer fileCnt
    , @JsonProperty("isDaily") boolean isDaily
){
}
