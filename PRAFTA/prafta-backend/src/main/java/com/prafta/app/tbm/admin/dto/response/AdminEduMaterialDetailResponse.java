package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminEduMaterialResult;

import lombok.Builder;
import lombok.Getter;

/** R5 교육자료 상세 응답(묶음 + 항목). 항목 previewUrl 은 서명 절대 URL. */
@Getter
@Builder
public class AdminEduMaterialDetailResponse {
    private AdminEduMaterialResult material;
    private List<AdminEduMaterialItemResponse> items;
}
