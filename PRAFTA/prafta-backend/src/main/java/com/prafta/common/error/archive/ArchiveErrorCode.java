package com.prafta.common.error.archive;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 자료실(Archive) 도메인 에러코드 (PRAFTA-053).
 * 규칙: {MODULE}_{HTTP}_{SEQ}. 공지(NoticeErrorCode)와 분리하여 타입 누수를 구조적으로 막는다.
 */
public enum ArchiveErrorCode implements ApiErrorCode {

    // 400: 비밀번호 불일치(수정/삭제 검증 실패)
    ARCHIVE_400_001(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.")
    // 400: 자료타입 코드가 유효하지 않음(존재하지 않거나, 신규 생성 시 사용중지된 코드)
    , ARCHIVE_400_002(HttpStatus.BAD_REQUEST, "유효하지 않은 자료타입입니다.")
    // 403: 다운로드 토큰 scope 불일치 / 만료
    , ARCHIVE_403_001(HttpStatus.FORBIDDEN, "유효하지 않은 다운로드 요청입니다.")
    // 403: 자료실 등록 권한 없음(앱 관리자 게시판 등록 — master/hr/safe 만). A-1 서버 강제.
    , ARCHIVE_403_002(HttpStatus.FORBIDDEN, "자료실에 등록할 수 있는 권한이 없습니다.")
    // 404: 대상 자료가 존재하지 않거나 삭제됨
    , ARCHIVE_404_001(HttpStatus.NOT_FOUND, "대상 자료를 찾을 수 없습니다.")
    // 404: 대상 첨부파일 없음
    , ARCHIVE_404_002(HttpStatus.NOT_FOUND, "대상 첨부파일을 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    ArchiveErrorCode(HttpStatus httpStatus, String message) {
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
