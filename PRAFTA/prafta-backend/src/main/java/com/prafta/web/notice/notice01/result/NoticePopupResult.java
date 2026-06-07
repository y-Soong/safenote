package com.prafta.web.notice.notice01.result;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 로그인 팝업 노출 공지 1건 결과 VO.
 * pinYn/isDaily 로 팝업 버튼 셋을 프론트가 분기한다(요청서 §6-6).
 * isDaily 는 수신자 일용직 여부(쿼리 파라미터 그대로 반사) — 캐러셀 버튼 결정용.
 * boolean is- 접두 Jackson 탈락 방지로 @JsonProperty 고정.
 */
public record NoticePopupResult(
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
