package com.prafta.common.error.tbm;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * tbm(TBM 교육관리) 도메인 에러 카탈로그.
 *
 * 명명 규칙: {MODULE}_{HTTP}_{SEQ}.
 *
 * 사용자 노출 메시지 정책:
 * <ul>
 *   <li>입력 오류 / 비즈니스 룰 위반 등 사용자가 인지하고 조치할 수 있는 항목 →
 *       구체적인 한글 메시지를 노출한다.</li>
 *   <li>보안 민감 항목(권한 부족, scope 위반 등) →
 *       내부 사정을 노출하면 정보 누출이 되므로 일반화된 "권한이 없습니다."로만 노출하고
 *       상세 분기 사유는 서버 로그에만 기록한다.</li>
 * </ul>
 */
public enum TbmErrorCode implements ApiErrorCode {

    // ===== PRAFTA-033-A 콘텐츠 라이브러리 스코프/권한 =====
    // 보안 민감 - 권한 부족: 일반 메시지만 노출 (서버 로그에 상세 기록)
      TBM_403_001(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    // 회사공통 콘텐츠 등록 권한 부족(master/safe 전용) - 사용자 안내 메시지
    , TBM_403_002(HttpStatus.FORBIDDEN, "회사 공통 콘텐츠는 안전관리자만 등록할 수 있습니다.")
    // 타 사업장 콘텐츠 접근(스코프 격리 위반) - 일반 메시지
    , TBM_403_003(HttpStatus.FORBIDDEN, "권한이 없습니다.")

    // ===== PRAFTA-033-B TBM 세션 관리 =====
    // 세션 개설 권한 부족(safe + 회사별 커스텀) - 보안 민감, 일반 메시지
    , TBM_403_010(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    // 타 사업장 세션 접근(스코프 격리 위반) - 일반 메시지
    , TBM_403_011(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    // 세션 조회 결과 없음
    , TBM_404_010(HttpStatus.NOT_FOUND, "해당 TBM 세션을 찾을 수 없습니다.")
    // 교육 내용(리치 HTML) 텍스트가 최소 길이 미만 - 사용자 안내
    , TBM_400_010(HttpStatus.BAD_REQUEST, "교육 내용을 10자 이상 입력해 주세요.")
    // 세션 제목 누락/길이 초과 - 사용자 안내
    , TBM_400_011(HttpStatus.BAD_REQUEST, "세션 제목을 200자 이내로 입력해 주세요.")
    // GPS 검증 설정 부적합(AUTO인데 좌표 없음 / MANUAL인데 미확인) - 사용자 안내
    , TBM_400_012(HttpStatus.BAD_REQUEST, "GPS 검증 설정을 확인해 주세요.")
    // GPS 검증 반경 범위(50~1000m) 벗어남 - 사용자 안내
    , TBM_400_013(HttpStatus.BAD_REQUEST, "GPS 검증 반경은 50~1000m 사이로 입력해 주세요.")
    // 취소 사유 누락 - 사용자 안내
    , TBM_400_014(HttpStatus.BAD_REQUEST, "취소 사유를 입력해 주세요.")
    // 수정 불가 상태(DRAFT/OPENED 외) - 비즈니스 룰
    , TBM_409_010(HttpStatus.CONFLICT, "현재 상태에서는 수정할 수 없습니다.")
    // 취소 불가 상태(DRAFT/OPENED 외) - 비즈니스 룰
    , TBM_409_011(HttpStatus.CONFLICT, "현재 상태에서는 취소할 수 없습니다.")
    // 비밀번호 재발급 불가 상태(OPENED 외) - 비즈니스 룰
    , TBM_409_012(HttpStatus.CONFLICT, "개설 상태에서만 비밀번호를 재발급할 수 있습니다.")

    // ===== PRAFTA-033-D TBM 이력 관리 =====
    // 미이수 처리 권한 부족(개설자/safe/master) - 보안 민감, 일반 메시지
    , TBM_403_020(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    // 타 사업장 출결/이력 접근(스코프 격리 위반) - 일반 메시지
    , TBM_403_021(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    // 출결 조회 결과 없음
    , TBM_404_020(HttpStatus.NOT_FOUND, "해당 출결 정보를 찾을 수 없습니다.")
    // 이수상태 코드 부적합(COMPLETED/NOT_COMPLETED 외) - 사용자 안내
    , TBM_400_020(HttpStatus.BAD_REQUEST, "이수 상태 값이 올바르지 않습니다.")
    // 미이수 처리 사유 길이 미달(10자 미만) - 사용자 안내
    , TBM_400_021(HttpStatus.BAD_REQUEST, "변경 사유를 10자 이상 입력해 주세요.")

    ;

    private final HttpStatus httpStatus;
    private final String message;

    TbmErrorCode(HttpStatus httpStatus, String message) {
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
