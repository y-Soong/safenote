package com.prafta.app.notice.notice02.dto.response;

import java.util.List;

import com.prafta.app.notice.notice02.result.AppArchiveResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 자료실 목록 응답.
 */
@Getter
@Builder
public class AppArchiveListResponse {
    private List<AppArchiveResult> archiveList;
}
