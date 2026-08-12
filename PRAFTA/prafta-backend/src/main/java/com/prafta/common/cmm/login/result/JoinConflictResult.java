package com.prafta.common.cmm.login.result;

/**
 * 소정-04: 셀프가입 시 아이디/휴대폰이 이미 점유되어 있을 때 그 점유 행의 최소 식별 정보.
 *
 * <p>재가입 재활용(plan §8 Q2) 판정에만 쓰는 운반체다. PII(이름/휴대폰/이메일)는 싣지 않는다
 * — 비로그인 경로(회원가입)에서 조회되므로 계정 정보를 응답에 노출할 여지를 아예 만들지 않는다.
 *
 * <p><b>판정 규칙</b>
 * <ul>
 *   <li>{@code accountStatus='07'}(가입거부) 이면서 <b>가입 요청과 같은 회사</b>인 행만 재활용 대상이다.</li>
 *   <li>그 외(활성·인증대기·탈퇴·타 회사)는 기존 중복 검사 그대로 차단한다.</li>
 * </ul>
 *
 * <p>★record 컬럼 순서 = 매퍼 SELECT 순서와 1:1 (MyBatis 위치 매핑 — feedback_mybatis_record_column_order).
 */
public record JoinConflictResult(
        String cmpnyCd
        , String userCd
        , String userId
        , String accountStatus
        , String useYn
) {
}
