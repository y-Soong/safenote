package com.prafta.web.baim.baim01.dto.request;

import java.math.BigDecimal;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SiteInfoRequest {
	private String cmpnyCd;
	private String siteCd;

	// PRAFTA-COM-001-T2-1: 사업장번호 기본값(siteCd) 세팅 제거 → SITE_NO NOT NULL 위반 방지 위해 필수화.
	@FieldLabel("사업장번호")
	@NotBlank
	private String siteNo;

	private String siteNm;

	// 주소 필수(prafta-038 D3): 기본주소 + 우편번호 필수, 상세주소(addr2)는 선택
	@FieldLabel("주소")
	@NotBlank
	private String addr1;

	private String addr2;

	@FieldLabel("우편번호")
	@NotBlank
	private String zipCode;

	private String strDate;
	private String endDate;
	private String useYn;
	private String siteAdminCd;
	private String telNo;
	private String gpsRange;
	private String siteDesc;

	// 위도/경도 (TB_SITE.LAT/LON decimal(10,7), nullable).
	// geocode 실패 시 빈 값 전송 가능하므로 좌표에는 @NotBlank 미적용(prafta-038 D4).
	// prafta-038 보안(Medium): DB decimal(10,7) 범위 초과(정수부 overflow)로 인한 truncation/500 방지를 위해
	// 위경도 "범위"만 검증한다. 소수 자릿수는 Kakao Geocoder 가 7자리를 초과해 반환할 수 있고
	// DB decimal(10,7) 저장 시 반올림되므로 정밀도(@Digits) 제약은 두지 않는다. null 은 허용(D4).
	@FieldLabel("위도")
	@DecimalMin(value = "-90.0")
	@DecimalMax(value = "90.0")
	private BigDecimal lat;

	@FieldLabel("경도")
	@DecimalMin(value = "-180.0")
	@DecimalMax(value = "180.0")
	private BigDecimal lon;
}
