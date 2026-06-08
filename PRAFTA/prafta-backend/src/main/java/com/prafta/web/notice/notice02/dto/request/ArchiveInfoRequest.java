package com.prafta.web.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자료실 단건 상세 조회 요청(상세/수정 진입).
 */
@Getter
@Setter
@NoArgsConstructor
public class ArchiveInfoRequest {
    private String noticeId;
}
