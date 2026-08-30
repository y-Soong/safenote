package com.prafta.app.tbm.admin.result;

/**
 * T-A2 세션 단건 상세(헤더) 조회 결과.
 *
 * <p>비밀번호(entryPwd/exitPwd)는 매퍼에서 그대로 조회하되, 서비스가 상태/권한 게이트에 따라
 * 응답 노출 여부를 결정한다(OPENED/IN_PROGRESS + 관리자만).
 *
 * <p>GPS좌표-암호화-전환-06: 좌표는 암호문(managerGpsLatEnc/LonEnc)+구 평문 병렬 조회 —
 * 서비스가 fallback 복호화(ENC 우선)로 응답 String 좌표를 채운다.
 * ⚠️ record 매핑은 SELECT 컬럼 순서 의존 — selectSessionDetail 의 SELECT 순서와 완전 일치 유지.
 */
public record AdminSessionResult(
    String sessionCd
    , String cmpnyCd
    , String siteCd
    , String siteNm
    , String eduTypeCd
    , String title
    , String contentBody
    , String contentFormatCd
    , String statusCd
    , String statusNm
    , String entryPwd
    , String exitPwd
    , String managerUserCd
    , String managerUserNm
    , String managerGpsLat      // 구 평문 위도(전환기 fallback — 소거 후 NULL)
    , String managerGpsLon      // 구 평문 경도
    , String managerGpsLatEnc   // 위도 암호문(AES-GCM v1.)
    , String managerGpsLonEnc   // 경도 암호문
    , String gpsVerifyTypeCd
    , Integer gpsVerifyRadiusM
    , Integer eduMinutes        // 교육 인정시간(분, 1~60). 미설정 시 null
    , String gpsManualConfirmYn
    , String openedAt
    , String prepStartAt
    , String startedAt
    , String endedAt
    , String cancelledAt
    , String cancelReason
    , String insertNm
    , String insertDate
    , Long prepStartAtEpoch // 준비 타이머 절대시각(epoch 초, UNIX_TIMESTAMP - 저장 TZ 가정 불필요)
    // tbm04-manager-sign: 주관자 서명 여부/시각(이력 상세 사후서명 노출 판단·표시용). SELECT 끝 2컬럼과 순서 일치
    , String managerSignYn      // 'Y'=서명 존재
    , String managerSignedAt    // 'yyyy-MM-dd HH:mm' (파일 내 표준 = DATE_FORMAT 직표시)
){
}
