package com.prafta.web.user.user01.dto.request;

import java.math.BigDecimal;
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
     * 경력인정 이원화(2026-08-21, 지시서 §1-1) — 연차 산정 반영 여부.
     * 'Y'(반영 모드, 기본) / 'N'(일수 모드). 미전송 시 서버가 'Y'로 기본 처리(하위호환).
     */
    private String creditLeaveCalcYn;

    /** 일수 모드(creditLeaveCalcYn='N') 전용 연간 추가 부여 일수. 반영 모드에서는 서버가 무시(NULL 강제)한다. */
    private BigDecimal creditExtraLeaveDays;

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

    // ===== 소정-03 : 계정 생성 시 소정근로시간 필수 입력 =====
    // (지시서 §0단계 "계정별 필수 입력(08-11 확정)" / plan §4 소정-03)

    /**
     * 소정근로 입력 유형 (<b>필수</b>).
     * <ul>
     *   <li>{@code FULL} — 풀타임. 주 소정근로분을 회사 통상 기준값(TB_CMPNY_STD_WORK_POLICY,
     *       행 부재 시 2400분)으로 서버가 채운다. 화면/엑셀에서 시간을 직접 받지 않는다
     *       (기준값 하드코딩 금지 — 지시서 B-1).</li>
     *   <li>{@code DIRECT} — 주 소정근로분 직접 입력. 단건 폼의 "단시간" 선택과 엑셀 업로드가 쓴다.</li>
     * </ul>
     */
    private String stdWorkType;

    /** 주 소정근로 분 — stdWorkType=DIRECT 일 때 필수(2400 = 주 40시간). FULL 이면 무시된다. */
    private Integer stdWorkWeekMinutes;

    /**
     * 소정근로 사유코드 [SYS083] — stdWorkType=DIRECT 일 때만 의미 있다.
     *
     * <p>미입력이면 서버가 회사 통상 기준값과 비교해 판정한다(본인 주 소정이 통상 기준보다
     * 짧으면 PART_TIME, 아니면 NORMAL — 지시서 B-2 단시간 파생 판정). 엑셀 업로드는 사유
     * 컬럼이 없으므로 항상 이 자동 판정을 탄다.
     *
     * <p>단축 사유(육아기·임신기·가족돌봄)는 적용 종료일이 필수라 계정 생성 경로에서는
     * 등록할 수 없다(소정근로시간 관리 화면에서 기간과 함께 등록).
     */
    private String stdWorkReasonCd;
}
