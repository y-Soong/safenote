package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** R5 교육자료 등록 응답(채번된 mtrlCd). 수정 시에도 동일 mtrlCd 반환. */
@Getter
@Builder
public class AdminEduMaterialSaveResponse {
    private String mtrlCd;
}
