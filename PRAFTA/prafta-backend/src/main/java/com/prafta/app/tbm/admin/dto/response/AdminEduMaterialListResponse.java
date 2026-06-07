package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminEduMaterialListResult;

import lombok.Builder;
import lombok.Getter;

/** R5 교육자료 리스트 응답(탭3). 응답 키 'materials' 통일(프론트 폴백 제거). */
@Getter
@Builder
public class AdminEduMaterialListResponse {
    private List<AdminEduMaterialListResult> materials;
    private int totalCount;
    private int page;
    private int pageSize;
}
