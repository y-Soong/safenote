package com.prafta.platform.company.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신규 고객사 프로비저닝 요청 DTO (POST /platformApi/company).
 *
 * <p>회사코드(CMPNY_CD)는 서버에서 추측 불가한 20자 랜덤값으로 발급한다(요청에 포함하지 않음).
 * 운영자 인가/식별은 토큰 + 플랫폼 게이트가 강제하므로 본 요청에 운영자 정보는 포함하지 않는다.
 *
 * <p>필수: cmpnyNm / bsnsLcnNo / adminNm / adminId / adminMbl
 * <p>선택: contractEndDate(YYYYMMDD, 무기한/미정이면 미입력)
 */
@Getter
@Setter
@NoArgsConstructor
public class CompanyProvisionRequest {

    /** 회사명(TB_CMPNY.CMPNY_NM). */
    private String cmpnyNm;

    /** 사업자번호(TB_CMPNY.BSNS_LCN_NO) — 숫자 10자리(하이픈 입력 허용, 정규화 후 검증). */
    private String bsnsLcnNo;

    /** 계약 종료일 YYYYMMDD (선택). 무기한/미정이면 미입력. */
    private String contractEndDate;

    /** 최초 master 계정 관리자명(TB_USER.USER_NM). */
    private String adminNm;

    /** 최초 master 계정 로그인 ID(TB_USER.USER_ID) — 관리자가 지정, 회사 내 유일. */
    private String adminId;

    /** 최초 master 계정 관리자 휴대폰 — 초기 비밀번호 원천, AES-GCM 암호화 후 저장. */
    private String adminMbl;
}
