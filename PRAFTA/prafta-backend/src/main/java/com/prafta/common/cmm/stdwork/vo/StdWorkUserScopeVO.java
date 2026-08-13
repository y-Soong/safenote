package com.prafta.common.cmm.stdwork.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 소정-02: 소정근로 판정에 필요한 대상 계정의 서버 권위 스코프.
 *
 * <p>고용형태(일용직 제외 게이트)와 소속 사업장(사업장별 기준값 오버라이드 판정)을 한 번에
 * 읽는다. 두 값을 따로 조회하면 {@code resolveSummary} 의 쿼리 왕복이 4회로 늘어나므로
 * 한 행으로 묶는다.
 *
 * <p><b>★employmentType 의 빈 문자열 규약</b> — {@code TB_USER.EMPLOYMENT_TYPE} 은 NULL 허용
 * 컬럼이다. 매퍼가 COALESCE 로 미지정을 빈 문자열로 치환하므로, 본 VO 자체가 {@code null}
 * 이면 "계정 없음/탈퇴·사용중지"를 뜻하고 {@code employmentType} 이 빈 문자열이면
 * "고용형태 미지정"을 뜻한다.
 */
@Getter
@Setter
public class StdWorkUserScopeVO {

    /** 고용형태 (미지정이면 빈 문자열 — 매퍼 COALESCE). */
    private String employmentType;

    /** 소속 사업장 코드 (TB_USER.SITE_CD, NULL 허용 컬럼). */
    private String siteCd;
}
