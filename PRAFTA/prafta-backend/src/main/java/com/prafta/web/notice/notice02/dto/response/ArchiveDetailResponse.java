package com.prafta.web.notice.notice02.dto.response;

import java.util.List;

import com.prafta.web.notice.notice02.result.ArchiveFileResult;
import com.prafta.web.notice.notice02.result.ArchiveResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 자료실 단건 상세 응답(마스터 + 첨부 list). 대상/확인이력 없음.
 */
@Getter
@Builder
public class ArchiveDetailResponse {
    private ArchiveResult archiveInfo;
    private List<ArchiveFileResult> fileList;
}
