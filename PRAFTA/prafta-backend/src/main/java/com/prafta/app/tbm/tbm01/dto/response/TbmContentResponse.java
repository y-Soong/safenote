package com.prafta.app.tbm.tbm01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-tbm-A6: 교육 콘텐츠/자료 응답.
 *
 * <p>contentBody = tb_tbm_session.CONTENT_BODY(리치 HTML).
 * <p>materials = 자료 묶음(≤3) 각각의 헤더 + 세부항목(items).
 * <p>item 필드 매핑(TB_TBM_EDU_MTRL_ITEM DDL 확정):
 *   type=MTRL_ITEM_TYPE, fileMgmtCd=FILE_MGMT_CD(파일경로 변환 미지원→코드), url=URL,
 *   itemDesc=MTRL_DESC, sortIdx=SORT_IDX.
 */
@Getter
@Builder
public class TbmContentResponse {

    private final String contentBody;
    private final List<Material> materials;

    @Getter
    @Builder
    public static class Material {
        private final String mtrlCd;
        private final String title;
        private final String overrideDesc;
        private final Integer displayOrder;
        private final List<Item> items;
    }

    @Getter
    @Builder
    public static class Item {
        private final String mtrlItemCd;
        private final String type;          // MTRL_ITEM_TYPE
        private final String fileMgmtCd;    // FILE_MGMT_CD
        private final String url;           // URL
        private final String itemDesc;      // MTRL_DESC
        private final Integer sortIdx;      // SORT_IDX
        private final String previewUrl;    // 서명 절대 URL(파일형) / NULL(외부링크·미첨부)
    }
}
