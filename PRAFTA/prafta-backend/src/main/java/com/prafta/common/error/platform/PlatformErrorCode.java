package com.prafta.common.error.platform;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 플랫폼 운영자(/platformApi) 영역 전용 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>신규 고객사 프로비저닝·글로벌 약관 관리 등 초고권한 엔드포인트의 접근 차단 사유.
 * 메뉴 숨김은 UX 일 뿐이고, 실제 차단은 본 코드들을 던지는 게이트(서버측)가 강제한다.
 */
public enum PlatformErrorCode implements ApiErrorCode {

    // 플랫폼 운영자 권한 아님(토큰 회사코드 ≠ prafta_system_admin / 토큰 부재).
    PLATFORM_403_001(HttpStatus.FORBIDDEN, "플랫폼 운영자만 접근할 수 있습니다.")
    // 허용되지 않은 IP 에서의 접근(IP 허용목록 미일치).
    , PLATFORM_403_002(HttpStatus.FORBIDDEN, "허용되지 않은 접근 경로입니다.")

    // ===== 신규 고객사 프로비저닝(POST /platformApi/company) 입력/처리 오류 =====
    // 필수 입력값(회사명/사업자번호/관리자명/관리자ID/관리자 휴대폰) 누락.
    , PLATFORM_400_001(HttpStatus.BAD_REQUEST, "필수 입력값이 누락되었습니다.")
    // 사업자번호 형식 오류(숫자 10자리).
    , PLATFORM_400_002(HttpStatus.BAD_REQUEST, "사업자번호 형식이 올바르지 않습니다.")
    // 관리자 휴대폰번호 형식 오류(숫자 10~11자리).
    , PLATFORM_400_003(HttpStatus.BAD_REQUEST, "관리자 휴대폰번호 형식이 올바르지 않습니다.")
    // 계약 종료일 형식 오류(YYYYMMDD 8자리).
    , PLATFORM_400_004(HttpStatus.BAD_REQUEST, "계약 종료일 형식이 올바르지 않습니다.")
    // 회사코드(CMPNY_CD) 발급 실패(충돌 재시도 한도 초과 — 일시적 오류, 재시도 권장).
    , PLATFORM_400_005(HttpStatus.BAD_REQUEST, "회사코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.")

    // 회사코드 직접 입력 전환(2026-08-16). CMPNY_CD 는 22개 테이블 복합 PK 선두 컬럼이라
    // 한 번 저장되면 사실상 변경 불가다 — 입력 시점에 형식·중복을 확실히 막는다.
    , PLATFORM_400_020(HttpStatus.BAD_REQUEST, "회사코드는 영문·숫자 2~20자로 입력해 주세요.")
    , PLATFORM_400_021(HttpStatus.BAD_REQUEST, "이미 사용 중인 회사코드입니다. 다른 코드를 입력해 주세요.")
    // 관리자 ID 길이 오류(3~50자).
    , PLATFORM_400_006(HttpStatus.BAD_REQUEST, "관리자 ID는 3~50자여야 합니다.")
    // 관리자 ID 형식 오류(영문/숫자/특수문자(_,-,.) 조합).
    , PLATFORM_400_007(HttpStatus.BAD_REQUEST, "관리자 ID는 영문, 숫자, 언더스코어(_), 하이픈(-), 점(.)만 사용 가능합니다.")
    // 휴대폰번호 중복(이미 다른 고객사의 master 계정으로 사용 중).
    , PLATFORM_400_008(HttpStatus.BAD_REQUEST, "이 휴대폰번호는 이미 다른 고객사의 관리자 계정으로 등록되어 있습니다.")

    , PLATFORM_400_009(HttpStatus.BAD_REQUEST, "이미 사용 중인 관리자 ID입니다. 다른 ID를 입력해 주세요.")

    // ===== 위치정보 열람 콘솔(Platform_04 — /platformApi/location/*) =====
    // SMS 인증 미통과/만료 상태에서 위치정보 조회 API 호출(서버측 게이트 — 프론트 게이트는 보조).
    , PLATFORM_403_003(HttpStatus.FORBIDDEN, "SMS 인증이 필요합니다. 인증 후 다시 시도해 주세요.")
    // SMS 인증번호 불일치 또는 만료(코드 유효 1분).
    , PLATFORM_400_010(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않거나 만료되었습니다.")
    // 위치정보 조회 필수 파라미터(회사/사업장/일자) 누락 또는 형식 오류.
    , PLATFORM_400_011(HttpStatus.BAD_REQUEST, "회사, 사업장, 조회 일자를 모두 지정해야 합니다.")
    // 운영자 계정에 휴대폰번호가 등록되어 있지 않아 SMS 인증 발송 불가.
    , PLATFORM_400_012(HttpStatus.BAD_REQUEST, "운영자 계정에 등록된 휴대폰번호가 없습니다.")
    // SMS 인증코드 발송 레이트리밋(동일 목적 최근 1분 내 재발송 거부 — 브루트포스 방어 V-2).
    , PLATFORM_400_013(HttpStatus.BAD_REQUEST, "인증번호가 방금 발송되었습니다. 잠시 후 다시 시도해 주세요.")

    // ===== AI 토큰 한도 수정(POST /platformApi/customer/token-quota) =====
    // 한도 모드/값 검증 실패(모드 미상 또는 LIMIT 모드 만 단위 정수 1~1,000,000 위반).
    , PLATFORM_400_014(HttpStatus.BAD_REQUEST, "AI 토큰 한도 입력값이 올바르지 않습니다.")
    // 대상 회사 미존재(TB_CMPNY 부재) 또는 운영자 자기 자신(prafta_system_admin) 지정.
    , PLATFORM_400_015(HttpStatus.BAD_REQUEST, "대상 회사를 찾을 수 없습니다.")

    // ===== SMS 발송 관리(Platform_05 — POST /platformApi/sms/*) =====
    // 발송 임계값 입력 검증 실패(범위 위반 / 시간당 > 일별 교차조건 위반 / 정책행 부재).
    // ★phoneWindowSec 는 1~59 만 허용한다 — 프론트 재발송 타이머(60초)보다 크면
    //   타이머 만료 직후 클릭이 대부분 차단되는 결함이 화면을 통해 다시 들어온다.
    , PLATFORM_400_016(HttpStatus.BAD_REQUEST, "발송 임계값 입력이 올바르지 않습니다.")
    // 킬스위치가 이미 해제 상태(동시 클릭 포함).
    , PLATFORM_400_017(HttpStatus.BAD_REQUEST, "이미 해제된 상태입니다.")
    // [3차 / sec N-7] 킬스위치 발동 중 전역 상한 <b>상향</b> 시도 거부.
    //   ★"발동 → 상한 상향 → 해제" 로 킬스위치를 사실상 무력화하는 우회를 막는다.
    //     해제 버튼은 그대로 있으므로 정상 운영에 지장이 없다 — 원인 확인 후 해제하고, 그 다음에 상한을 조정한다.
    , PLATFORM_400_018(HttpStatus.BAD_REQUEST, "킬스위치 발동 중에는 전역 상한을 올릴 수 없습니다.\n원인을 확인하고 킬스위치를 해제한 뒤 변경해 주세요.")

    // ===== 신규 고객사 기본 근무타입 시간(Platform_01, 2026-08-17 확장) =====
    // 기본 근무시간/휴게시간 검증 실패(HHMM 형식, 종료>시작, 휴게는 근무구간 안에서 시작·종료 쌍 입력).
    , PLATFORM_400_019(HttpStatus.BAD_REQUEST, "기본 근무시간 입력이 올바르지 않습니다.\n종료는 시작 이후여야 하고, 휴게시간은 근무시간 안에서 시작·종료를 함께 입력해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    PlatformErrorCode(HttpStatus httpStatus, String message) {
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
