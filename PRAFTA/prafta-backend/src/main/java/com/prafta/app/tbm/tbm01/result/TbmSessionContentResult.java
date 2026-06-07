package com.prafta.app.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-tbm: 교육 콘텐츠 묶음(A6) 조회 결과(자료 헤더).
 *
 * <p>tb_tbm_session_content ⨝ TB_TBM_EDU_MTRL. 세부 항목(items)은 별도 조회로 채운다.
 */
@Getter
@Setter
public class TbmSessionContentResult {
    private String mtrlCd;
    private String title;
    private Integer displayOrder;
    private String overrideDesc;
}
