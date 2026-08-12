package com.prafta.common.cmm.login.dto.response;

/**
 * 소정-04: 셀프가입(회원가입) 접수 결과 응답.
 *
 * <p>가입은 더 이상 "즉시 활성화"가 아니라 <b>관리자 승인 대기</b>로 끝난다. 클라이언트가
 * "로그인 해주세요" 안내 대신 승인대기 화면으로 분기할 수 있도록 상태 신호를 싣는다.
 *
 * <p><b>계약</b> — 앱({@code prafta-app-frontend/src/utils/joinApproval.js})은 다음 순서로 읽는다.
 * <ol>
 *   <li>{@code nextStep} = {@code JOIN_APPROVAL_PENDING} (게이트 관례 명명: PHONE_AUTH/DEFAULT_SCH 계열)</li>
 *   <li>{@code accountStatus} = {@code '06'}[SYS013] (DDL 로 확정된 값 — 가장 안정적인 신호)</li>
 * </ol>
 * 둘 다 실어 어느 쪽을 보더라도 같은 결론이 나오게 한다.
 *
 * <p>★PII 를 담지 않는다(이름·휴대폰·아이디 미포함). 비로그인 경로의 응답이다.
 */
public record UserJoinResponse(
        String accountStatus
        , String nextStep
) {
    /** 계정상태[SYS013] '06' 가입승인대기. */
    private static final String STATUS_JOIN_PENDING = "06";

    /** 클라이언트 분기 신호(게이트 관례 명명). */
    private static final String NEXT_STEP_JOIN_PENDING = "JOIN_APPROVAL_PENDING";

    /** 승인 대기로 접수됨. */
    public static UserJoinResponse pending() {
        return new UserJoinResponse(STATUS_JOIN_PENDING, NEXT_STEP_JOIN_PENDING);
    }
}
