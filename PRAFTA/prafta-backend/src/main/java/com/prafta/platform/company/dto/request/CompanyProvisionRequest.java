package com.prafta.platform.company.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신규 고객사 프로비저닝 요청 DTO (POST /platformApi/company).
 *
 * <p>회사코드(CMPNY_CD)는 2026-08-16 부터 <b>운영자가 직접 입력</b>한다(종전 서버 랜덤 20자 발급).
 * 모든 사용자가 가입 시 관리자 승인을 받는 구조로 바뀌어, 코드 복잡도를 방어 수단으로 쓸 이유가 없어졌다.
 * 운영자 인가/식별은 토큰 + 플랫폼 게이트가 강제하므로 본 요청에 운영자 정보는 포함하지 않는다.
 *
 * <p>필수: cmpnyCd / cmpnyNm / bsnsLcnNo / adminNm / adminId / adminMbl
 * <p>선택: contractEndDate(YYYYMMDD, 무기한/미정이면 미입력) / weekStdMinutes(통상근로시간 기준값)
 */
@Getter
@Setter
@NoArgsConstructor
public class CompanyProvisionRequest {

    /**
     * 회사코드(TB_CMPNY.CMPNY_CD) — 운영자가 직접 입력. 영문·숫자 2~20자.
     *
     * <p>★서버가 대문자로 정규화해 저장한다. DB 콜레이션이 대소문자를 무시(utf8mb4_unicode_ci)해
     * 'parma' 와 'PARMA' 가 어차피 같은 값으로 충돌하므로, 표기를 하나로 고정해 혼동을 없앤다.
     *
     * <p>★한 번 저장되면 사실상 변경 불가다 — 22개 테이블의 복합 PK 선두 컬럼이다.
     */
    private String cmpnyCd;

    /** 회사명(TB_CMPNY.CMPNY_NM). */
    private String cmpnyNm;

    /** 사업자번호(TB_CMPNY.BSNS_LCN_NO) — 숫자 10자리(하이픈 입력 허용, 정규화 후 검증). */
    private String bsnsLcnNo;

    /** 계약 종료일 YYYYMMDD (선택). 무기한/미정이면 미입력. */
    private String contractEndDate;

    /**
     * 통상근로자 주 소정근로 분 (선택, 0 초과 ~ 2400분).
     *
     * <p>단시간 판정 분모·연차 비례부여 분모·소정 이력 미입력 계정 폴백에 쓰이는 회사 기본값이다.
     * <b>미입력이면 행을 만들지 않는다</b> — 코드 폴백 2400분(주 40시간)이 그대로 적용된다.
     * 사업장별로 다르면 사업장 관리(Baim_01)에서 오버라이드한다.
     */
    private Integer weekStdMinutes;

    /**
     * 기본 근무타입 근무 시작/종료 시각(HHMM, 선택 — 미입력 시 09:00~18:00).
     *
     * <p>프로비저닝이 시드하는 기본 근무타입(ST001)의 1구간 시각이다. 당일 주간 근무만
     * 허용(종료 &gt; 시작)하며, 야간 등 다른 형태는 등록 후 근무타입 관리(Attd_01)에서 만든다.
     */
    private String schStrTime;
    private String schEndTime;

    /** 기본 근무타입 휴게 시작/종료 시각(HHMM, 선택 쌍 — 미입력 시 휴게 없음. 근무구간 내부여야 함). */
    private String brkStrTime;
    private String brkEndTime;

    /** 최초 master 계정 관리자명(TB_USER.USER_NM). */
    private String adminNm;

    /** 최초 master 계정 로그인 ID(TB_USER.USER_ID) — 관리자가 지정, 회사 내 유일. */
    private String adminId;

    /** 최초 master 계정 관리자 휴대폰 — 초기 비밀번호 원천, AES-GCM 암호화 후 저장. */
    private String adminMbl;
}
