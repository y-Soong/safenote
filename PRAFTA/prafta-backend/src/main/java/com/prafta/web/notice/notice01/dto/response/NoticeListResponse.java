package com.prafta.web.notice.notice01.dto.response;

import java.util.List;

import com.prafta.web.notice.notice01.result.NoticeResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 공지 관리 목록 응답.
 */
@Getter
@Builder
public class NoticeListResponse {
    private List<NoticeResult> noticeList;
}
