package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminMaterialTypeOptionResult;

import lombok.Builder;
import lombok.Getter;

/** R5 자료 타입(COM003) 옵션 응답. 프론트 타입 필터/셀렉트용. */
@Getter
@Builder
public class AdminMaterialTypeOptionResponse {
    private List<AdminMaterialTypeOptionResult> options;
}
