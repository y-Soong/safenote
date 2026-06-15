package com.prafta.web.user.user01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 단건 사용자 생성 요청 DTO (PRAFTA-036).
 *
 * <p>회사 스코프(CMPNY_CD)는 토큰의 gv_cmpnyCd 로 강제하므로 본 요청에 포함되지 않는다.
 * 사용자코드(USER_CD)는 서버에서 채번한다.
 *
 * <p>필수: userId / userNm / authCd / siteNo / nodeCd / mblNo / birthDt
 * <p>선택: email / gender / rankCd / hireDate / employmentType / contractEndDate
 *         / creditMonths / creditReasonType / creditReasonDetail
 *
 * <p>단건 생성 흐름과 엑셀 업로드 행 단위 처리(B와 동일 로직 재사용)가 본 DTO를 공유한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserCreateRequest {

    private String userId;
    private String userNm;
    private String authCd;

    /** 사업장번호 (SITE_NO) — 서버에서 SITE_CD 로 매핑한다. */
    private String siteNo;
    /** 소속부서코드 (NODE_CD) — TB_SITE_NODE 존재 검증 대상. */
    private String nodeCd;

    /** 휴대폰번호 — 초기 비밀번호(BCrypt 해시) 원천. AES-GCM 암호화 후 저장. */
    private String mblNo;

    /** 이메일 (선택). AES-GCM 암호화 후 저장. */
    private String email;

    /** 성별 [SYS004] M/F (선택). */
    private String gender;

    /** 생년월일 YYMMDD 또는 YYYYMMDD. AES-GCM 암호화 후 저장. */
    private String birthDt;

    /** 직급코드 [COM007] (선택). */
    private String rankCd;

    /** 입사일 YYYYMMDD (선택). */
    private String hireDate;

    /** 고용형태 [SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE (선택). */
    private String employmentType;

    /** 계약 종료일 YYYYMMDD — employmentType=CONTRACT 일 때만 의미 있음. */
    private String contractEndDate;

    /** 경력 인정 개월 수 — 0 이상. null/0 이면 경력 인정 항목 생성 생략. */
    private Integer creditMonths;

    /** 경력 인정 사유 유형 [SYS042] — creditMonths>0 이고 미입력 시 'OTHER' 기본값. */
    private String creditReasonType;

    /** 경력 인정 상세 설명 (선택, 500자 이내). */
    private String creditReasonDetail;

    /**
     * PRAFTA-037-F7 — 추가 권한 사이트 코드 목록 (선택).
     * 기본 siteCd(=siteNo 매핑값) 외에 추가로 권한 부여할 사이트들의 SITE_CD.
     * - 동일 회사(cmpnyCd) 내 존재하는 SITE_CD 만 허용. 검증 실패 시 USER_400_055.
     * - 기본 siteCd 와 중복 입력은 무시(중복 INSERT 차단).
     * - 엑셀 업로드는 본 필드를 사용하지 않는다(1행=1사용자=1사이트 단순화 유지).
     */
    private List<String> additionalSiteCdList;

    /**
     * PRAFTA-COM-008-E-5 — 기본 근무타입(tb_sch_mgmt.SCH_CD, 선택).
     * 입력 시 대상 사업장(siteNo→SITE_CD) 활성 근무타입 화이트리스트 검증 후 저장하고,
     * 교대팀 비소속이면 저장 직후 당해 연말까지 평일 근무계획을 자동 생성한다.
     * 엑셀 업로드 행은 본 필드를 사용하지 않는다(null).
     */
    private String defaultSchCd;
}
