package com.prafta.web.notice.notice02.dto.response;

import java.util.List;

import com.prafta.web.notice.notice02.result.ArchiveTypeResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 자료타입 드롭다운 응답. 코드그룹 상수 미주입 시 빈 목록(정상).
 */
@Getter
@Builder
public class ArchiveTypeResponse {
    private List<ArchiveTypeResult> typeList;
}
