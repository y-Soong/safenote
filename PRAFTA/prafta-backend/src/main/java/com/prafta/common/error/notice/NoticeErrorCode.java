package com.prafta.common.error.notice;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 공지사항(Notice) 도메인 에러코드 (PRAFTA-047).
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum NoticeErrorCode implements ApiErrorCode {

    // 400: 비밀번호 불일치(수정/삭제 검증 실패)
    NOTICE_400_001(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.")
    // 400: 팝업 ON 인데 팝업 기간(FROM/TO) 누락
    , NOTICE_400_002(HttpStatus.BAD_REQUEST, "팝업 기간을 입력해주세요.")
    // 400: 고정 순번이 잘못됨(1 미만)
    , NOTICE_400_003(HttpStatus.BAD_REQUEST, "고정 순번이 올바르지 않습니다.")
    // 400: 한시숨김(SNOOZED) 거부 — 일용직 또는 비고정 공지 대상
    , NOTICE_400_004(HttpStatus.BAD_REQUEST, "한시 숨김을 적용할 수 없는 공지입니다.")
    // 403: 대상 권한 범위 초과(발행자 스코프 밖 사업장/노드 지정). appendf 로 상세 부착
    , NOTICE_403_001(HttpStatus.FORBIDDEN, "대상 권한 범위를 초과했습니다.")
    // 403: 다운로드 토큰 scope 불일치 / 만료
    , NOTICE_403_002(HttpStatus.FORBIDDEN, "유효하지 않은 다운로드 요청입니다.")
    // 403: 다운로드 발급 시 노출 대상 외(요청자가 해당 공지의 대상이 아님, 047-001)
    , NOTICE_403_003(HttpStatus.FORBIDDEN, "해당 공지의 첨부에 접근할 수 없습니다.")
    // 404: 대상 공지가 존재하지 않거나 삭제됨
    , NOTICE_404_001(HttpStatus.NOT_FOUND, "대상 공지를 찾을 수 없습니다.")
    // 404: 대상 첨부파일 없음
    , NOTICE_404_002(HttpStatus.NOT_FOUND, "대상 첨부파일을 찾을 수 없습니다.")
    // 422: 팝업 기간 중 내용 수정 차단(요청서 §8-1 ⓐ)
    , NOTICE_422_001(HttpStatus.UNPROCESSABLE_ENTITY, "팝업 노출 기간 중에는 공지 내용을 수정할 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    NoticeErrorCode(HttpStatus httpStatus, String message) {
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
