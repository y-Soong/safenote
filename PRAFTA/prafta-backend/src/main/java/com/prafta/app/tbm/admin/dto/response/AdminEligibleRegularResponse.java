package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminEligibleRegularResult;

import lombok.Builder;
import lombok.Getter;

/** E9 정규직 대리입실 후보 검색 응답(prafta-051 R-B). */
@Getter
@Builder
public class AdminEligibleRegularResponse {
    private List<AdminEligibleRegularResult> users;
    private int page;
    private int pageSize;
}
