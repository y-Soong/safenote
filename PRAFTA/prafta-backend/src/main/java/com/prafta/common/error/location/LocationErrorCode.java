package com.prafta.common.error.location;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 위치정보 동의(005) 관련 오류코드 — 위치정보 동의철회·중지 S3.
 *
 * <p>★{@code LOCATION_403_001} 은 클라이언트가 <b>재동의 화면으로 분기하는 신호</b>다.
 * 앱은 이 코드를 받으면 안내 팝업을 띄우고 위치정보 동의 화면으로 유도한다.
 * 다른 403 과 섞이지 않도록 전용 코드를 둔다.
 */
public enum LocationErrorCode implements ApiErrorCode {

    // 규칙: {MODULE}_{HTTP}_{SEQ}
    LOCATION_400_001(HttpStatus.BAD_REQUEST, "요청한 동의 상태가 올바르지 않습니다.")
    , LOCATION_400_002(HttpStatus.BAD_REQUEST, "이미 동의를 철회한 상태입니다.\n중지로 되돌릴 수 없으며, 다시 이용하려면 재동의가 필요합니다.")
    , LOCATION_403_001(HttpStatus.FORBIDDEN, "위치정보 제공 및 이용에 동의해야 이용할 수 있는 기능입니다.\n위치정보 동의 후 다시 시도해 주세요.")
    , LOCATION_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "위치기반서비스 약관 정보를 확인할 수 없습니다.\n관리자에게 문의해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    LocationErrorCode(HttpStatus httpStatus, String message) {
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
