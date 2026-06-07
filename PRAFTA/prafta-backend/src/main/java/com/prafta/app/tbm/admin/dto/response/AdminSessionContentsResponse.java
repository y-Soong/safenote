package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * R3 진행화면 슬라이드용 자료 항목 응답.
 *
 * <p>사용자 TBM TbmContentResponse 와 동일 형태(프론트 TbmMaterialSlider 재사용 호환).
 * materials=[{ mtrlCd, title, overrideDesc, displayOrder, items:[{ type, url, ... , sortIdx }] }].
 * <p>item: type=MTRL_ITEM_TYPE[SYS018], fileMgmtCd=FILE_MGMT_CD(파일경로 변환 미지원→코드), url=URL,
 *   itemDesc=MTRL_DESC, sortIdx=SORT_IDX.
 */
@Getter
@Builder
public class AdminSessionContentsResponse {

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
