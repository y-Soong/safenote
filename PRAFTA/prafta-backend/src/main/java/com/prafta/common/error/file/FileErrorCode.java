package com.prafta.common.error.file;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 공통 파일 업로드 관련 에러코드.
 *
 * <p>규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum FileErrorCode implements ApiErrorCode {

    // 허용 확장자(텍스트/이미지/동영상/음성) 외 파일 업로드 차단.
    FILE_400_001(HttpStatus.BAD_REQUEST,
            "허용되지 않은 파일 형식입니다.\n텍스트/이미지/동영상/음성 파일만 업로드할 수 있습니다.")
    // 암호(사용자/소유자 암호)가 설정된 PDF — 페이지 수 조회/렌더 불가.
    //   도메인 계층은 자기 맥락 코드로 remap 한다(예: DAILYCONTRACT_400_006).
    , FILE_400_002(HttpStatus.BAD_REQUEST, "암호가 설정된 PDF는 처리할 수 없습니다.")
    // PDF 파싱/렌더 실패(손상 파일 등).
    , FILE_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 처리에 실패했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    FileErrorCode(HttpStatus httpStatus, String message) {
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
