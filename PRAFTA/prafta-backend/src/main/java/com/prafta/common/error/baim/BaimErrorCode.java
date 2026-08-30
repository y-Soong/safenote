package com.prafta.common.error.baim;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum BaimErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    BAIM_400_001(HttpStatus.BAD_REQUEST, "하위 부서가 존재하는 경우 삭제할 수 없습니다.")
    , BAIM_400_002(HttpStatus.BAD_REQUEST, "부서내 관리자가 ")
    , BAIM_400_003(HttpStatus.BAD_REQUEST, "동일한 핸드폰번호를 사용중인\n일일사용자 계정이 존재합니다.")
    , BAIM_400_004(HttpStatus.BAD_REQUEST, "계정 등록이 비활성화된 사업장입니다.\n계정 등록을 ON 으로 변경 후 발급하세요.")
    , BAIM_400_005(HttpStatus.BAD_REQUEST, "점유중인 슬롯은 구분을 변경할 수 없습니다.")
    , BAIM_400_006(HttpStatus.BAD_REQUEST, "유효하지 않은 슬롯 구분 값입니다.")
    , BAIM_400_007(HttpStatus.BAD_REQUEST, "슬롯에 소속부서가 지정되지 않아 계정을 매칭할 수 없습니다.\n소속부서를 먼저 지정해 주세요.")
    , BAIM_400_008(HttpStatus.BAD_REQUEST, "점유 중인 계정 슬롯이 있어 활성 계정 수를 줄일 수 없습니다.\n해당 슬롯을 먼저 비운 뒤 변경해 주세요.")
    , BAIM_400_009(HttpStatus.BAD_REQUEST, "해당 사업장에서 사용할 수 없는 근무타입입니다.\n근무타입을 다시 확인해 주세요.")
    , BAIM_403_001(HttpStatus.FORBIDDEN, "해당 사업장에 대한 접근 권한이 없습니다.")
    , BAIM_403_002(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.")
    , BAIM_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "약관 데이터 생성 오류 !\n관리자에게 문의해주세요.")
    , BAIM_500_002(HttpStatus.INTERNAL_SERVER_ERROR, "조직정보 삭제에 실패했습니다.\n관리자에게 문의해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    BaimErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return name(); // enum 이름을 그대로 코드로 쓰면 관리 쉬움
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