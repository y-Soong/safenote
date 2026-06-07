package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-A1 관리자 TBM 교육관리 리스트 조회 요청. */
@Getter
@Setter
@NoArgsConstructor
public class AdminSessionListRequest {
    private String statusCd;        // DRAFT / OPENED / IN_PROGRESS (빈 값=전체)
    private String keyword;         // 제목 검색(옵션)
    private Integer page;           // 1-base
    private Integer pageSize;
}
