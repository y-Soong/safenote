package com.prafta.web.notice.notice02.dto.response;

import java.util.List;

import com.prafta.web.notice.notice02.result.ArchiveResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 자료실 목록 응답.
 */
@Getter
@Builder
public class ArchiveListResponse {
    private List<ArchiveResult> archiveList;
}
