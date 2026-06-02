package com.prafta.app.tbm.tbm01.result;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-004-C: tb_tbm_session 조회 결과(입실 검증/컨텍스트 공용).
 * <p>좌표(managerGpsLat/Lon)는 거리 계산용으로만 서버 내부에서 사용하고 응답·로그에 노출하지 않는다(D5).
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
    private BigDecimal managerGpsLat;
    private BigDecimal managerGpsLon;
    private String gpsVerifyTypeCd;     // SYS048 AUTO/MANUAL/DISABLED
    private Integer gpsVerifyRadiusM;
}
