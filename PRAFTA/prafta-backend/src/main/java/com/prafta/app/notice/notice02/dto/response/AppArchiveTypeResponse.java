package com.prafta.app.notice.notice02.dto.response;

import java.util.List;

import com.prafta.app.notice.notice02.result.AppArchiveTypeResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 자료타입 드롭다운 응답. 코드그룹 상수 미주입 시 빈 목록(정상).
 */
@Getter
@Builder
public class AppArchiveTypeResponse {
    private List<AppArchiveTypeResult> typeList;
}
