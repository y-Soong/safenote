package com.prafta.web.chkLst.chkLst02.dto.request;

import java.util.List;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PRAFTA-SUBCON-T0-04: 타 사업장 점검문항 가져오기 요청.
 * 하우스 준거: Baim06 CopySiteNodeRequest.
 */
@Getter
@Setter
@NoArgsConstructor
public class CopyChkptInspectItemRequest {

	@FieldLabel("원본 사업장코드")
	@NotBlank
	private String srcSiteCd;

	@FieldLabel("대상 사업장코드")
	@NotBlank
	private String dstSiteCd;

	@FieldLabel("점검구분")
	@NotBlank
	private String chkLstType;

	@FieldLabel("가져올 문항코드 목록")
	@NotEmpty
	private List<String> inspectItemCdList;
}
