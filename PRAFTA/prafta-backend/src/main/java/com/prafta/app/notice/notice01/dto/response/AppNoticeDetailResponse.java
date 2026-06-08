package com.prafta.app.notice.notice01.dto.response;

import java.util.List;

import com.prafta.app.notice.notice01.result.AppNoticeFileResult;
import com.prafta.app.notice.notice01.result.AppNoticeResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 공지 단건 상세 응답(마스터 + 첨부 list).
 * 대상 list 는 앱 표시에 불필요하므로 미포함(읽기 전용 본문/첨부만).
 */
@Getter
@Builder
public class AppNoticeDetailResponse {
    private AppNoticeResult noticeInfo;
    private List<AppNoticeFileResult> fileList;
}
