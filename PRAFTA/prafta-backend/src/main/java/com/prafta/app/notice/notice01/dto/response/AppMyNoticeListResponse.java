package com.prafta.app.notice.notice01.dto.response;

import java.util.List;

import com.prafta.app.notice.notice01.result.AppMyNoticeResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 "내 공지 목록"(카드/전체목록) 응답.
 * noticeList: 대상 공지 전체(정렬 PIN_YN DESC, PIN_ORDER ASC, INSERT_DATE DESC). 카드 slice 는 프론트 책임.
 * unreadCount: 동일 모수의 미열람(LAST_READ_DATE NULL) 개수(배지·별도 호출 절약).
 */
@Getter
@Builder
public class AppMyNoticeListResponse {
    private List<AppMyNoticeResult> noticeList;
    private int unreadCount;
}
