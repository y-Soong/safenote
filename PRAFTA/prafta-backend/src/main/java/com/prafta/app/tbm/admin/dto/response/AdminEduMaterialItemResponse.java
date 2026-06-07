package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * R5 교육자료 상세 항목 응답(서명 URL 전환).
 *
 * <p>{@code AdminEduMaterialItemResult}(원시 컬럼) → 응답 매핑. previewUrl 은 파일형 항목의
 * 서명 절대 URL({@code FileUrlSigner.sign})이며, 파일 없으면 NULL(프론트 graceful: previewUrl || url).
 */
@Getter
@Builder
public class AdminEduMaterialItemResponse {
    private final String mtrlItemCd;
    private final String mtrlItemType;   // SYS018
    private final String mtrlDesc;
    private final String fileMgmtCd;
    private final String thumbFileMgmtCd;
    private final Integer durationSec;
    private final String url;
    private final int sortIdx;
    private final String previewUrl;     // 서명 절대 URL(파일형) / NULL(외부링크·미첨부)
}
