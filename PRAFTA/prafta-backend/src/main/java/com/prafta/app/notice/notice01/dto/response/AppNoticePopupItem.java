package com.prafta.app.notice.notice01.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prafta.app.notice.notice01.result.AppNoticeFileResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 로그인 팝업 노출 공지 1건(첨부 목록 포함) 응답 아이템.
 * 매퍼 결과(AppNoticePopupResult)에 첨부 list(AppNoticeFileResult)를 서비스에서 합성한다.
 * boolean is- 접두 Jackson 탈락 방지로 @JsonProperty 고정.
 */
@Getter
@Builder
public class AppNoticePopupItem {
    private String noticeId;
    private String title;
    private String content;
    private String pinYn;
    private String popupFromYmd;
    private String popupToYmd;
    private String insertDate;
    private Integer fileCnt;
    @JsonProperty("isDaily")
    private boolean isDaily;
    private List<AppNoticeFileResult> fileList;
}
