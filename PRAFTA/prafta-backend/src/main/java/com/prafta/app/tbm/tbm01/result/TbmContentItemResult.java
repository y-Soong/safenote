package com.prafta.app.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-tbm: 교육 콘텐츠 세부항목(A6 items) 조회 결과.
 *
 * <p>출처 테이블 TB_TBM_EDU_MTRL_ITEM(DDL 확정 매핑):
 *   <ul>
 *     <li>itemType  = MTRL_ITEM_TYPE</li>
 *     <li>fileMgmtCd = FILE_MGMT_CD (계약 초안 filePath → DDL 에 직접 파일경로 컬럼 없음, 파일코드로 대체)</li>
 *     <li>url       = URL</li>
 *     <li>itemDesc  = MTRL_DESC (계약 초안 desc)</li>
 *     <li>sortIdx   = SORT_IDX</li>
 *   </ul>
 */
@Getter
@Setter
public class TbmContentItemResult {
    private String mtrlCd;        // 묶음 매핑용(서비스에서 그룹핑)
    private String mtrlItemCd;
    private String itemType;      // MTRL_ITEM_TYPE
    private String fileMgmtCd;    // FILE_MGMT_CD
    private String url;           // URL
    private String itemDesc;      // MTRL_DESC
    private Integer sortIdx;      // SORT_IDX
    private String filePath;      // TB_FILE_INFO.FILE_PATH (서명 relPath 조립용, 파일 없으면 NULL)
    private String fileExt;       // TB_FILE_INFO.FILE_EXT  (확장자, 점 포함)
}
