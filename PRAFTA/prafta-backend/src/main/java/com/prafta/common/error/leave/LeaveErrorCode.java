package com.prafta.common.error.leave;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 연차(leave) 도메인 공통 에러코드.
 *
 * <p>소정-05(작업지시서_근로자별-소정근로시간-관리-도입 §연차 부여 on/off 토글)에서 신설.
 * 기존 연차 에러는 근태 화면 계열이라 {@code AttdErrorCode} 에 누적돼 왔으나, 본 코드들은
 * <b>회사 정책(TB_LEAVE_POLICY) 레벨 게이트</b>라 근태 화면에 종속되지 않아 도메인 enum 을 분리한다
 * (CLAUDE.md "신규 도메인: com.prafta.common.error.{module}/{Module}ErrorCode.java").
 *
 * <p>메시지는 관리자/근로자가 직접 읽는 안내문이다. 원인(회사 토글 off)과 대안(수동 약정 부여)을
 * 함께 제시해, 시스템 장애로 오인하지 않도록 한다.
 */
public enum LeaveErrorCode implements ApiErrorCode {

    /**
     * 법정 연차 자동 부여가 꺼진 회사에서 법정(정책 기준) 부여를 시도한 경우.
     *
     * <p>차단 지점: 부여 엔진 진입부({@code LeaveGrantEngineServiceImpl.prepareGrantContext})
     * → Attd_09 [정책 기준 부여] 미리보기/적용, 정기부여 배치의 방어선.
     * 관리자 수동(약정) 부여는 본 게이트 대상이 아니다.
     */
    LEAVE_400_001(HttpStatus.BAD_REQUEST,
            "법정 연차 자동 부여를 사용하지 않도록 설정된 회사입니다. (연차 정책 화면에서 변경)\n"
                    + "필요한 경우 [수동 부여]로 약정 연차를 부여해 주세요.")

    /**
     * 법정 연차 자동 부여가 꺼진 회사에서 가불(선차감)을 시도한 경우.
     *
     * <p>가불은 "차기 부여 예정 법정 연차를 미리 끌어오는" 동작이므로, 자동 부여가 없는 회사에서는
     * 상계될 부여가 발생하지 않아 영구 마이너스가 된다(plan §8 Q3 확정 = 차단).
     */
    , LEAVE_400_002(HttpStatus.BAD_REQUEST,
            "법정 연차 자동 부여를 사용하지 않는 회사에서는 가불을 사용할 수 없어요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    LeaveErrorCode(HttpStatus httpStatus, String message) {
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
