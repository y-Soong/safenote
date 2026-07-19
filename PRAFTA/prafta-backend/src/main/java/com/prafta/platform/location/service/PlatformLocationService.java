package com.prafta.platform.location.service;

import com.prafta.platform.location.application.param.GpsListParam;
import com.prafta.platform.location.application.param.LocationSiteListParam;
import com.prafta.platform.location.application.param.PlatformOperatorParam;
import com.prafta.platform.location.application.param.SmsVerifyParam;
import com.prafta.platform.location.dto.response.GpsListResponse;
import com.prafta.platform.location.dto.response.LocationSiteListResponse;
import com.prafta.platform.location.dto.response.SmsStatusResponse;

/**
 * 플랫폼 위치정보 열람 서비스(Platform_04 — SMS 게이트 + 위치 조회 + 열람 로그).
 */
public interface PlatformLocationService {

    /** SMS 인증코드 발송(운영자 본인 등록 휴대폰 — 클라 휴대폰 입력 없음, 코드 유효 1분). */
    void sendSmsAuth(PlatformOperatorParam param);

    /** SMS 인증번호 검증(통과 시 인증 상태 10분 유효). */
    void verifySmsAuth(SmsVerifyParam param);

    /** 현재 유효 SMS 인증 상태 조회(재진입 시 불필요한 재인증 방지 — 게이트 자체는 gps-lists 가 강제). */
    SmsStatusResponse selectSmsStatus(PlatformOperatorParam param);

    /** 대상 회사 사업장 목록 조회(시설 좌표 — 개인위치정보 아님, SMS 게이트 미적용). */
    LocationSiteListResponse selectSiteList(LocationSiteListParam param);

    /** 위치정보 조회 — 매 호출: SMS 인증 판정 → 조회 → 열람 로그 INSERT(동일 트랜잭션). */
    GpsListResponse selectGpsList(GpsListParam param);
}
