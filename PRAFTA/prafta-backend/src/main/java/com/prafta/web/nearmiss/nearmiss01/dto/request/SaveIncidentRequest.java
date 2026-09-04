package com.prafta.web.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E4 정밀조사 저장 요청 (원인/재발방지/즉시조치/임시조치).
 * 식별(cmpnyCd/reviewer)은 JWT 에서만 도출.
 */
@Getter
@Setter
@NoArgsConstructor
public class SaveIncidentRequest {
    private String siteCd;
    private String nearMissId;
    private String causeDesc;
    private String preventionDesc;
    private String immediateActionDesc;
    /**
     * 관리자 임시조치 메모(ADMIN_TEMP_ACTION_DESC). null 이면 미변경, 빈 문자열이면 삭제.
     * 종결(완료 300 / 미처리대상 400) 건은 서버가 무시한다(읽기전용).
     */
    private String adminTempActionDesc;
}
