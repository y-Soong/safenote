package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** E10 정규직 관리자 대리입실 응답(prafta-051 R-B). */
@Getter
@Builder
public class AdminManagerDirectResponse {
    private String sessionCd;
    private String userTypeCd;
    private String userCd;
}
