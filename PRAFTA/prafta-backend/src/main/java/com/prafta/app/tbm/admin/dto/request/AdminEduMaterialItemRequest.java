package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * R5 교육자료 항목(등록/수정 JSON 항목).
 *
 * <p>파일형(01 이미지 / 02 동영상 / 04 PDF):
 *   - 신규 업로드: {@code fileIndex} 가 멀티파트 files 배열의 인덱스를 가리킨다(0-base). 서버가 저장→FILE_MGMT_CD 발급.
 *   - 기존 유지: {@code fileMgmtCd} 보존(fileIndex 는 null).
 * <p>외부링크형(03 유튜브URL): {@code url} 필수.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminEduMaterialItemRequest {
    private String mtrlItemType;   // SYS018: 01/02/03/04
    private String url;            // 03(유튜브URL)만
    private String mtrlDesc;
    private Integer sortIdx;
    private String fileMgmtCd;     // 기존 파일 유지 시
    private Integer fileIndex;     // 신규 업로드 시 멀티파트 files 배열 인덱스(0-base)
}
