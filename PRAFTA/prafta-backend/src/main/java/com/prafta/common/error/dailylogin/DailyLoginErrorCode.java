package com.prafta.common.error.dailylogin;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 일용직 직접 로그인(dailylogin) 도메인 전용 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>정책서: {@code .claude/context/policies/common/03-account-auth.md} §3.5(차단), §3.2(로그인).
 * 보안: 비활성/만료/오ID/오비번/탈퇴를 사유별로 구분 노출하지 않고 통합 메시지(001)로 차단하여
 *       계정 존재 여부를 노출하지 않는다. 잠금(002)만 사용자 안내 목적상 별도 메시지를 둔다.
 *
 * <p>예외(003): 사용자 요청으로 "비활성화된 계정"은 별도 안내한다. 단, 사전 enumeration(비번 없이 ID
 *       실재 확인)을 막기 위해 <b>비밀번호 검증을 통과한 경우에만</b> 003 으로 사유를 노출한다(서비스에서 제어).
 * <p>게이트(403_001): 로그인 이후 관리자가 계정을 비활성화하면, 일용직 토큰의 후속 요청을 즉시 차단한다
 *       (DailyUserActiveGateInterceptor). 프론트는 이 코드를 받아 안내 후 강제 로그아웃한다.
 */
public enum DailyLoginErrorCode implements ApiErrorCode {

    // 통합 차단 메시지(아이디/비번/만료/탈퇴 — 계정 존재 비노출).
    DAILYLOGIN_400_001(HttpStatus.BAD_REQUEST, "아이디 혹은 비밀번호를 확인해주세요.")
    // 비밀번호 인증 실패 잠금.
    , DAILYLOGIN_400_002(HttpStatus.BAD_REQUEST, "비밀번호 인증 실패로 계정이 잠겨진 상태입니다.")
    // 비활성화된 계정(비밀번호 검증 통과 시에만 노출). 관리자 슬롯 비우기/만료 등으로 비활성.
    , DAILYLOGIN_400_003(HttpStatus.BAD_REQUEST, "비활성화된 계정입니다. 관리자에게 문의해 주세요.")
    // 로그인 이후 비활성화된 일용직 계정의 후속 요청 차단(게이트). 프론트: 안내 후 강제 로그아웃.
    , DAILYLOGIN_403_001(HttpStatus.FORBIDDEN, "관리자에 의해 계정이 비활성화되었습니다. 관리자에게 문의해 주세요.")
    // prafta-app-032 B: 로그인 자동 재활성 시 사업장 슬롯(정원) 부족으로 자리를 배정할 수 없음.
    //   토글 OFF(003)와 구분해 "오늘 배정 가능한 자리 없음"을 명확히 안내한다.
    , DAILYLOGIN_400_004(HttpStatus.BAD_REQUEST, "오늘 배정 가능한 자리가 없습니다. 관리자에게 문의해 주세요.")
    // prafta-daily-blacklist: 블랙리스트 등록 휴대폰의 자동 재활성 차단.
    //   사유(블랙리스트)는 직접 노출하지 않고 "이용 제한"으로만 안내(정보 노출 회피).
    , DAILYLOGIN_400_005(HttpStatus.BAD_REQUEST, "이용이 제한된 계정입니다. 관리자에게 문의해 주세요.")
    // 일용직 입장 승인제(D5): 관리자 승인 대기 중(요청 생성 직후 포함). R4 — 승인 후 재로그인 유도.
    //   반드시 비밀번호 검증을 통과한 경우에만 노출한다(003 전례 미러, enumeration 방지).
    , DAILYLOGIN_400_006(HttpStatus.BAD_REQUEST, "관리자 승인 대기 중입니다. 승인 후 다시 로그인해 주세요.")
    // 일용직 입장 승인제(D10): 당일 거부됨. 거부 사유 상세는 미노출(§4-1 통합 메시지).
    //   비밀번호 검증 통과 후에만 노출(006 과 동일 규칙).
    , DAILYLOGIN_400_007(HttpStatus.BAD_REQUEST, "입장이 승인되지 않았습니다. 관리자에게 문의해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    DailyLoginErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
