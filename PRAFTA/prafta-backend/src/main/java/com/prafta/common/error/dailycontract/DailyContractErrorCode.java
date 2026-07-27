package com.prafta.common.error.dailycontract;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 일용직 근로계약서(dailycontract) 도메인 전용 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>출처: 일용직 계약서+승인제 plan §3-5. 계약서 관리(웹 User_07)·서명 게이트/저장(앱)·
 * 서명본 열람(앱/웹 User_08) API 에서 사용한다.
 */
public enum DailyContractErrorCode implements ApiErrorCode {

    // 서명 이미지 multipart 검증 실패(비이미지/크기 초과/디코딩 실패).
    DAILYCONTRACT_400_001(HttpStatus.BAD_REQUEST, "서명 이미지가 올바르지 않습니다.")
    // 현재 승인 사이클에 이미 서명 완료(멱등 가드 — append-only 중복 방지).
    , DAILYCONTRACT_400_002(HttpStatus.BAD_REQUEST, "이미 서명이 완료되었습니다.")
    // 해당 사업장에 활성 계약서 없음(서명/열람/중지 대상 부재).
    , DAILYCONTRACT_400_003(HttpStatus.BAD_REQUEST, "등록된 계약서가 없습니다.")
    // 관리자 계약서 업로드 검증 실패(PDF/이미지 형식·확장자 정합·손상 파일/10MB 초과).
    , DAILYCONTRACT_400_004(HttpStatus.BAD_REQUEST, "계약서 파일이 올바르지 않습니다. (PDF 또는 이미지 파일, 10MB 이하)")
    // 계약서 양식 페이지 수 상한 초과(멀티페이지 지원 P5 — 최대 20페이지).
    , DAILYCONTRACT_400_005(HttpStatus.BAD_REQUEST, "계약서 페이지 수가 너무 많습니다. (최대 20페이지)")
    // 암호가 설정된 PDF(P8 — 업로드 시점 차단. 열람 시점 실패는 해당 사업장 전원 서명 마비를 유발).
    , DAILYCONTRACT_400_006(HttpStatus.BAD_REQUEST,
            "암호가 설정된 PDF는 등록할 수 없습니다. 암호를 해제한 후 다시 등록해 주세요.")
    // 페이지 파라미터 범위 밖(1 ~ pageCount).
    , DAILYCONTRACT_400_007(HttpStatus.BAD_REQUEST, "요청한 페이지가 존재하지 않습니다.")
    // 페이지 치수/해상도가 과대해 렌더 시 힙을 고갈시킬 수 있는 파일(sec SEC-1 — 업로드 시점 차단).
    //   바이트 크기·페이지 수만 보면 "수십 KB / 20페이지 / 페이지당 14400x14400pt" 같은 입력이 통과하는데,
    //   렌더 시점 OOM 은 Error 라 전역 핸들러가 잡지 못하고 인스턴스 전체로 파급된다.
    , DAILYCONTRACT_400_008(HttpStatus.BAD_REQUEST,
            "계약서 페이지 크기 또는 해상도가 너무 큽니다. A3 이하 크기로 다시 등록해 주세요.")
    // in-place 정정 조건 위반(해당 버전 서명 행 1건 이상 — 승인시점 버전확정 K6).
    //   ★서버측 강제: 프론트에서 "정정" 버튼을 숨기는 것만으로는 불충분하다(EP 직접 호출 가능).
    //   서명자가 있는 버전의 내용을 바꾸면 기존 서명 증적이 훼손되므로 새 버전으로 등록해야 한다.
    , DAILYCONTRACT_400_009(HttpStatus.BAD_REQUEST,
            "이미 서명한 근로자가 있어 정정할 수 없습니다. 새 버전으로 등록해 주세요.")
    // 서명본 열람 인가 실패(본인 아님 + 해당 사업장 관리자 아님 — IDOR 차단).
    , DAILYCONTRACT_403_001(HttpStatus.FORBIDDEN, "열람 권한이 없습니다.")
    // 서명본 미존재(타 회사 서명 포함 — 존재 비노출).
    , DAILYCONTRACT_404_001(HttpStatus.NOT_FOUND, "서명본을 찾을 수 없습니다.")
    // 계약서 행은 존재하는데 디스크 원본 파일이 없음(DB 만 이관되고 업로드 파일은 미이관된 경우 등).
    //   400_003("등록된 계약서가 없습니다")로 뭉뚱그리면 운영자가 "미등록"으로 오판하므로 별도 코드로 구분한다.
    , DAILYCONTRACT_404_002(HttpStatus.NOT_FOUND, "계약서 원본 파일을 찾을 수 없습니다. 계약서를 다시 등록해 주세요.")
    // 계약서 동시 등록 경합(최초 등록 시 버전 채번 무잠금 — PK/기능성 유니크 충돌을 재시도 안내로 매핑, qa L-1).
    , DAILYCONTRACT_409_001(HttpStatus.CONFLICT, "동시 등록이 감지되었습니다. 잠시 후 다시 시도해 주세요.")
    // in-place 정정 대상이 더 이상 활성이 아님(다른 관리자가 그 사이 교체/중지) 또는 동시 정정 경합
    //   (잠금 조회 시 USE_YN != 'Y' / 조건부 UPDATE 0행 — 승인시점 버전확정 T3).
    , DAILYCONTRACT_409_002(HttpStatus.CONFLICT, "계약서가 이미 변경되었습니다. 새로고침 후 다시 시도해 주세요.")
    // 페이지 렌더/PDF 조립 실패 또는 저장 파일 형식이 계약서 도메인 허용 범위를 벗어남
    //   (HTTP 는 500 이되 code 로 원인 식별 — 멀티페이지 지원 §5-3).
    , DAILYCONTRACT_500_001(HttpStatus.INTERNAL_SERVER_ERROR,
            "계약서 페이지를 표시할 수 없습니다. 관리자에게 문의해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    DailyContractErrorCode(HttpStatus httpStatus, String message) {
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
