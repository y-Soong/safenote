package com.prafta.web.notice.notice01.dto.response;

import java.util.List;

import com.prafta.web.notice.notice01.result.NoticeFileResult;
import com.prafta.web.notice.notice01.result.NoticeResult;
import com.prafta.web.notice.notice01.result.NoticeTargetResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 공지 단건 상세 응답(마스터 + 첨부 list + 대상 list).
 */
@Getter
@Builder
public class NoticeDetailResponse {
    private NoticeResult noticeInfo;
    private List<NoticeFileResult> fileList;
    private List<NoticeTargetResult> targetList;
}
