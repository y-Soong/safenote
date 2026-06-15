package com.prafta.app.notice.notice02.dto.response;

import java.util.List;

import com.prafta.app.notice.notice02.result.AppArchiveFileResult;
import com.prafta.app.notice.notice02.result.AppArchiveResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 자료실 단건 상세 응답(마스터 + 첨부 list). 대상/확인이력 없음.
 */
@Getter
@Builder
public class AppArchiveDetailResponse {
    private AppArchiveResult archiveInfo;
    private List<AppArchiveFileResult> fileList;
}
