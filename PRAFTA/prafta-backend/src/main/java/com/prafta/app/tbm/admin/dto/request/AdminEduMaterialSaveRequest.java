package com.prafta.app.tbm.admin.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * R5 교육자료 등록/수정 요청(멀티파트 A안의 "data" JSON 파트).
 *
 * <p>멀티파트 구성: {@code @RequestPart("data") AdminEduMaterialSaveRequest}
 *   + {@code @RequestPart(value="files", required=false) List<MultipartFile>}.
 * <p>식별자(회사/사용자)는 JWT 클레임에서만 도출하며 본 바디에 포함하지 않는다(IDOR 차단).
 *   siteCd 는 자료 스코프(null=회사공통 / 값=사업장전용)이며 서버가 접근가능 사업장인지 검증한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminEduMaterialSaveRequest {
    private String title;
    private String mtrlType;       // COM003(TB_BAIM_VAL_D) 유효값 — 서버 검증
    private String siteCd;         // null/빈값 = 회사공통, 값 = 사업장전용
    private String contents;       // 설명(선택)
    private String useYn;          // 'Y'/'N' (미지정 시 'Y')
    private List<AdminEduMaterialItemRequest> items;
}
