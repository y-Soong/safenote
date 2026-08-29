package com.prafta.app.admin.employeestatus.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * PRAFTA-003: 앱 관리자 직원 GPS 궤적 조회 응답.
 *
 * <p>필드명은 웹 {@code AttdGpsTrailResponse}(내부 {@code AttdGpsTrailResult})의 항목과 100% 동일하게
 * 맞췄다(plan §PRAFTA-003 상세 — 프론트가 웹 {@code AttdGpsCoordPanel.vue} 렌더 로직을 그대로 이식하기 위한
 * 계약 정합). 좌표는 서비스 계층에서 이미 복호화된 평문이다({@code GpsCoordCrypto.resolveToBigDecimal}).
 */
@Getter
@Builder
public class EmployeeGpsTrailResponse {

    private final List<TrailItem> trail;

    @Getter
    @Builder
    public static class TrailItem {
        private final String gpsId;
        private final BigDecimal lat;
        private final BigDecimal lon;
        private final BigDecimal accuracy;
        private final String apiCallDate;
        private final String apiCallTime;
        private final String isMocked;
        private final String gpsInfoType;
    }
}
