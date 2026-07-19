package com.prafta.common.error.chkLst;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum ChkLstErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    CHKLST_400_001(HttpStatus.BAD_REQUEST, "등록된 점검항목이 없습니다.")
    , CHKLST_403_001(HttpStatus.FORBIDDEN, "현재 소속 사업장과 다른 QR 코드입니다.")
    , CHKLST_404_001(HttpStatus.NOT_FOUND, "등록되지 않은 체크포인트입니다.")
    , CHKLST_403_002(HttpStatus.FORBIDDEN, "접근 권한이 없는 사업장이거나 존재하지 않는 불량 항목입니다.")
    , CHKLST_403_003(HttpStatus.FORBIDDEN, "접근 권한이 없는 사업장입니다.")	// PRAFTA-SUBCON-T0-02: 문항관리 사업장 인가 가드
    // PRAFTA-SUBCON-T6 보안검토
    , CHKLST_400_002(HttpStatus.BAD_REQUEST, "점검 답변값이 올바르지 않습니다.")	// M3/L4: 답변타입 화이트리스트 / 문항 실재·활성 검증
    , CHKLST_400_003(HttpStatus.BAD_REQUEST, "입력 내용이 너무 깁니다. (최대 1000자)")	// M3: 전파되는 서술 필드 길이 상한
    , CHKLST_403_004(HttpStatus.FORBIDDEN, "해당 기능에 대한 권한이 없습니다.")	// High-2: 점검대상 저장/삭제 메뉴·버튼 권한 게이트
    ;

    private final HttpStatus httpStatus;
    private final String message;

    ChkLstErrorCode(HttpStatus httpStatus, String message) {
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