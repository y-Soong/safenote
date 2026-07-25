package com.prafta.app.tbm.tbm01.result;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-004-C: tb_tbm_session 조회 결과(입실 검증/컨텍스트 공용).
 * <p>좌표(managerGpsLat/Lon)는 거리 계산용으로만 서버 내부에서 사용하고 응답·로그에 노출하지 않는다(D5).
 * <p>GPS좌표-암호화-전환-06: 암호문(managerGpsLatEnc/LonEnc)+구 평문 병렬 조회 —
 * 입실 지오펜스 판정(resolveDistanceAndVerify)에서 fallback 복호화(ENC 우선)로 좌표를 확정한다.
 */
@Getter
@Setter
public class TbmSessionResult {
    private String sessionCd;
    private String cmpnyCd;
    private String siteCd;
    private String title;
    private String statusCd;            // SYS046
    private String entryPwd;
    private String exitPwd;
    private BigDecimal managerGpsLat;   // 구 평문 위도(전환기 fallback — 소거 후 NULL)
    private BigDecimal managerGpsLon;   // 구 평문 경도
    private String managerGpsLatEnc;    // 위도 암호문(AES-GCM v1.)
    private String managerGpsLonEnc;    // 경도 암호문
    private String gpsVerifyTypeCd;     // SYS048 AUTO/MANUAL/DISABLED
    private Integer gpsVerifyRadiusM;
}
