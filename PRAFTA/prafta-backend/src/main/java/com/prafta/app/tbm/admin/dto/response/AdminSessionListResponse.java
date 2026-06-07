package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminSessionListResult;

import lombok.Builder;
import lombok.Getter;

/** T-A1 교육관리 리스트 응답. */
@Getter
@Builder
public class AdminSessionListResponse {
    private List<AdminSessionListResult> sessions;
    private int totalCount;
    private int page;
    private int pageSize;
}
