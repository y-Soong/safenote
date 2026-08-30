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
    // PRAFTA_COM_001 T7-17: 교육 인정시간 미입력/범위밖(1~60분) - 사용자 안내(웹/앱 공용)
    , TBM_400_015(HttpStatus.BAD_REQUEST, "교육 시간은 1분 이상 60분 이하로 입력해 주세요.")
    // 세션-위험성평가 연계 부적합(세션 사업장과 다른 사업장 평가 또는 미존재 평가 키) - 사용자 안내(2026-07-16 보안 보강, 웹/앱 공용)
    , TBM_400_016(HttpStatus.BAD_REQUEST, "위험성평가 연계 정보가 올바르지 않습니다.")
    // 수정 불가 상태(DRAFT/OPENED 외) - 비즈니스 룰
    , TBM_409_010(HttpStatus.CONFLICT, "현재 상태에서는 수정할 수 없습니다.")
    // 취소 불가 상태(DRAFT/OPENED 외) - 비즈니스 룰
    , TBM_409_011(HttpStatus.CONFLICT, "현재 상태에서는 취소할 수 없습니다.")
    // 비밀번호 재발급 불가 상태(OPENED 외) - 비즈니스 룰
    , TBM_409_012(HttpStatus.CONFLICT, "개설 상태에서만 비밀번호를 재발급할 수 있습니다.")

    // ===== PRAFTA-051 TBM 세션 상태머신 재설계 (웹 관리자 tbm02) =====
    // 교육준비(OPENED) 전이 불가 상태(DRAFT 외) - 비즈니스 룰. 동시 전이 경합 시에도 사용.
    , TBM_409_013(HttpStatus.CONFLICT, "개설 상태에서만 교육준비를 시작할 수 있습니다.")
    // 교육시작(IN_PROGRESS) 전이 불가 상태(OPENED 외) - 비즈니스 룰. 동시 전이 경합 시에도 사용.
    , TBM_409_014(HttpStatus.CONFLICT, "교육준비 상태에서만 교육을 시작할 수 있습니다.")
    // 교육준비 연장 불가(OPENED 아님 또는 15분 경과) - 비즈니스 룰
    , TBM_409_015(HttpStatus.CONFLICT, "교육준비 시간이 지나 연장할 수 없습니다.")
    // 교육종료(COMPLETED) 전이 불가 상태(IN_PROGRESS 외) - 비즈니스 룰. 동시 전이 경합 시에도 사용.
    , TBM_409_016(HttpStatus.CONFLICT, "교육시작 상태에서만 교육을 종료할 수 있습니다.")
    // 종료비밀번호 재발급 불가 상태(COMPLETED 외) - 비즈니스 룰
    , TBM_409_017(HttpStatus.CONFLICT, "교육종료 상태에서만 종료 비밀번호를 재발급할 수 있습니다.")

    // ===== 001-P5 R3 라이브 제어(앱 관리자 admin: 교육 시작/종료/강제퇴실/개별 미이수) =====
    // [머지 com-007] prafta-051 과 409_013~017 번호 충돌하여 본 기능셋을 05x 블록으로 재배정.
    // 개설자만 시작/종료 가능(T1) - 비즈니스 룰, 사용자 안내
    , TBM_403_012(HttpStatus.FORBIDDEN, "교육을 개설한 관리자만 시작 또는 종료할 수 있습니다.")
    // GPS 비검증(DISABLED) 세션에서 개별 이수처리 시도(T4) - 비즈니스 룰, 사용자 안내
    , TBM_403_013(HttpStatus.FORBIDDEN, "GPS 검증을 사용한 세션에서만 개별 이수처리를 할 수 있습니다.")
    // 교육 시작 불가 상태(OPENED 외) - 비즈니스 룰(상태 전이 충돌). (구 TBM_409_013)
    , TBM_409_050(HttpStatus.CONFLICT, "개설 상태에서만 교육을 시작할 수 있습니다.")
    // 교육 종료 불가 상태(IN_PROGRESS 외) - 비즈니스 룰(상태 전이 충돌). (구 TBM_409_014)
    , TBM_409_051(HttpStatus.CONFLICT, "진행 중 상태에서만 교육을 종료할 수 있습니다.")
    // 강제 퇴실 불가 세션 상태(IN_PROGRESS 외) - 비즈니스 룰. (구 TBM_409_015)
    , TBM_409_052(HttpStatus.CONFLICT, "진행 중 세션에서만 강제 퇴실할 수 있습니다.")
    // 강제 퇴실 대상 없음/이미 퇴실(멱등 가드 0건) - 비즈니스 룰. (구 TBM_409_016)
    , TBM_409_053(HttpStatus.CONFLICT, "이미 퇴실 처리되었거나 대상 출결이 없습니다.")
    // 개별 이수처리 불가 세션 상태(COMPLETED 외) - 비즈니스 룰. (구 TBM_409_017)
    , TBM_409_054(HttpStatus.CONFLICT, "종료된 세션에서만 개별 이수처리를 할 수 있습니다.")

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

    // ===== PRAFTA-APP-004-C 앱 TBM 입실/종료 (정규직 MVP) =====
    // 입실/종료 대상 세션 없음 - 사용자 안내
    , TBM_404_030(HttpStatus.NOT_FOUND, "입실할 TBM 세션을 찾을 수 없습니다.")
    // 입실 불가 상태(개설 상태에서만 입실 가능, D3) - 비즈니스 룰
    , TBM_409_030(HttpStatus.CONFLICT, "현재 입실할 수 없는 세션 상태입니다.")
    // 입실/종료 비밀번호 불일치 - 사용자 안내(현장 공유 passcode 특성상 잠금 없이 재시도 가능)
    , TBM_400_030(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.")
    // GPS 검증 반경 밖(AUTO 모드, D5) - 사용자 안내(좌표 비노출, 거리만)
    , TBM_403_030(HttpStatus.FORBIDDEN, "근무지 반경 밖에서는 입실할 수 없습니다.")
    // 종료 시 본인 입실 기록 없음 - 비즈니스 룰
    , TBM_409_031(HttpStatus.CONFLICT, "입실 기록이 없어 종료할 수 없습니다.")
    // 이미 종료 처리됨 - 비즈니스 룰
    , TBM_409_032(HttpStatus.CONFLICT, "이미 종료 처리된 출결입니다.")
    // 종료 서명 누락(D1: 종료 시 서명 필수) - 사용자 안내
    , TBM_400_031(HttpStatus.BAD_REQUEST, "종료 서명을 등록해 주세요.")

    // ===== PRAFTA-051-08 앱 종료 상태 가드 =====
    // 종료 불가 상태(교육시작/교육종료에서만 종료 가능, C6) - 비즈니스 룰
    , TBM_409_033(HttpStatus.CONFLICT, "현재 종료할 수 없는 세션 상태입니다.")

    // ===== PRAFTA-051-11 관리자 대리/검색 입실 =====
    // 대상유형 코드 부적합(REGULAR/DAILY 외) - 사용자 안내
    , TBM_400_040(HttpStatus.BAD_REQUEST, "입실 대상 유형이 올바르지 않습니다.")
    // 대리입실 대상 사용자가 세션 사업장 소속이 아님/일용직 만료·탈퇴 - 사용자 안내
    , TBM_403_040(HttpStatus.FORBIDDEN, "해당 세션에 입실할 수 없는 대상입니다.")
    // 대리입실 불가 상태(교육준비 상태에서만 입실 처리 가능) - 비즈니스 룰
    , TBM_409_040(HttpStatus.CONFLICT, "교육준비 상태에서만 입실 처리할 수 있습니다.")
    // 이미 입실 처리된 대상(UNIQUE 충돌) - 비즈니스 룰(멱등 안내)
    , TBM_409_041(HttpStatus.CONFLICT, "이미 입실 처리된 대상입니다.")
    // prafta-051 R-D E11: QR 페이로드 파싱 실패/형식오류 또는 일용직 식별키(userCd) 누락 - 사용자 안내
    , TBM_400_044(HttpStatus.BAD_REQUEST, "QR 코드 정보를 확인할 수 없습니다.")

    // ===== PRAFTA-051-12 입실자 내보내기(eject) =====
    // 내보내기 사유 누락 - 사용자 안내
    , TBM_400_041(HttpStatus.BAD_REQUEST, "내보내기 사유를 입력해 주세요.")
    // 내보내기 불가 상태(교육준비 상태에서만 가능, C8) - 비즈니스 룰
    , TBM_409_042(HttpStatus.CONFLICT, "교육준비 상태에서만 입실자를 내보낼 수 있습니다.")
    // 내보내기 대상 출결 없음/세션 불일치/이미 제거됨 - 비즈니스 룰
    , TBM_409_043(HttpStatus.CONFLICT, "내보낼 수 있는 입실 기록을 찾을 수 없습니다.")
    // prafta-app-025 J1-7 작업 D: 당일 출근 기록 없는 일용직의 QR 입실 차단 - 사용자 안내(409 — 앱 인터셉터 토큰오류 오인 회피).
    , TBM_409_044(HttpStatus.CONFLICT, "출근 기록이 없어 입실할 수 없습니다. 현장 처리에서 출근을 먼저 등록해 주세요.")

    // ===== 001-P5 R5 교육자료 관리(앱 관리자 admin) =====
    // [머지 com-007] prafta-051 과 400_040/400_041/403_040 번호 충돌하여 05x 블록으로 재배정.
    // 자료 조회 결과 없음(스코프 밖 mtrlCd 포함). (충돌 없음, 번호 유지)
    , TBM_404_040(HttpStatus.NOT_FOUND, "해당 교육자료를 찾을 수 없습니다.")
    // 자료 제목 누락/길이 초과 - 사용자 안내. (구 TBM_400_040)
    , TBM_400_050(HttpStatus.BAD_REQUEST, "자료 제목을 200자 이내로 입력해 주세요.")
    // 자료 타입(COM003) 부적합 - 사용자 안내. (구 TBM_400_041)
    , TBM_400_051(HttpStatus.BAD_REQUEST, "자료 타입 값이 올바르지 않습니다.")
    // 항목 누락(최소 1개) 또는 항목 구성 부적합(파일/URL 누락) - 사용자 안내. (충돌 없음, 번호 유지)
    , TBM_400_042(HttpStatus.BAD_REQUEST, "자료 항목을 올바르게 구성해 주세요.")
    // 업로드 파일 부적합(MIME/확장자/크기) - 사용자 안내. (충돌 없음, 번호 유지)
    , TBM_400_043(HttpStatus.BAD_REQUEST, "업로드할 수 없는 파일입니다. 형식과 크기를 확인해 주세요.")
    // 회사공통 자료 등록/수정/삭제 권한 부족(master/safe 전용) - 사용자 안내. (구 TBM_403_040)
    , TBM_403_050(HttpStatus.FORBIDDEN, "회사 공통 자료는 안전관리자만 관리할 수 있습니다.")

    // ===== T5-2 사용 중 교육자료 수정/삭제 차단 =====
    // 이미 TBM 세션에서 사용(취소 외)된 교육자료는 수정/삭제 불가 - 비즈니스 룰
    , TBM_409_055(HttpStatus.CONFLICT, "이미 사용된 교육자료는 수정할 수 없습니다.")

    // ===== AI 교육안 생성 사전 차단 =====
    // 세션에 묶인 AI 분석 지정 항목 중 미확정(NONE/ANALYZING/DRAFT/FAILED/NULL 등)이 하나라도 있음 - 비즈니스 룰
    // (실제 응답은 어떤 항목이 미확정인지 상세 메시지로 대체해 내려간다)
    , TBM_409_060(HttpStatus.CONFLICT, "AI 분석이 확정되지 않은 자료가 있습니다.")
    // 교육안 생성 목표 글자수(targetChars) 범위 위반(800~5000) - 사용자 안내(2026-07-16 R3)
    , TBM_400_061(HttpStatus.BAD_REQUEST, "목표 글자수는 800자 이상 5000자 이하로 입력해 주세요.")

    // ===== PRAFTA-SUBCON-T5 TBM 연동 회사 지정(+재지정 체인) =====
    // 지정 파라미터 누락/형식(자기 회사·개설사 지정 시도 포함) - 사용자 안내
    , TBM_400_060(HttpStatus.BAD_REQUEST, "요청 정보가 올바르지 않습니다.")
    // 입실 범위 밖(참석자 소속 회사가 {개설사} ∪ SHARE 체인 에 없음). 입실 경로 P1~P4 공통.
    , TBM_403_060(HttpStatus.FORBIDDEN, "이 교육에 참여할 수 없는 회사 소속입니다.")
    // 연동 회사 지정 권한 없음(개설사도 체인 회사도 아님)
    , TBM_403_061(HttpStatus.FORBIDDEN, "이 교육의 연동 회사를 지정할 권한이 없습니다.")
    // 처리할 지정 행 없음(미존재/타사 지정분/이미 해제 통합 — 존재 비노출)
    , TBM_404_060(HttpStatus.NOT_FOUND, "처리할 연동 회사 지정을 찾을 수 없습니다.")
    // 중복 지정(UK 백스톱 DuplicateKeyException 포함)
    , TBM_409_061(HttpStatus.CONFLICT, "이미 지정된 회사입니다.")
    // 지정 대상과 연동 관계(tb_cmpny_relation STATUS='ACCEPTED')가 없음
    , TBM_409_062(HttpStatus.CONFLICT, "연동 관계가 수립된 회사가 아닙니다.")
    // 지정/해제 불가 상태(DRAFT/OPENED 외)
    , TBM_409_063(HttpStatus.CONFLICT, "지금은 연동 회사를 변경할 수 없는 상태입니다.")

    // ==== 주관자 서명 (tbm04-manager-sign, 2026-08-30) ====
    // 종료/사후서명 시 서명 파일 누락 또는 검증(크기/타입/매직바이트) 실패 - 사용자 안내
    , TBM_400_070(HttpStatus.BAD_REQUEST, "주관자 서명을 등록해 주세요.")
    // 사후서명 중복(이미 서명됨 — 재서명 불가 확정) - 비즈니스 룰
    , TBM_409_070(HttpStatus.CONFLICT, "이미 주관자 서명이 등록된 세션입니다.")
    // 사후서명 불가 상태(COMPLETED 외) - 비즈니스 룰
    , TBM_409_071(HttpStatus.CONFLICT, "종료된 세션에서만 주관자 서명을 등록할 수 있습니다.")

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
