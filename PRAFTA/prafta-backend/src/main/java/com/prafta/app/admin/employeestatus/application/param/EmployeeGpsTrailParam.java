package com.prafta.app.admin.employeestatus.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-003: 앱 관리자 직원 GPS 궤적 조회 파라미터.
 *
 * <p>웹 {@code AttdGpsTrailParam}과 검증 로직을 그대로 이식했다(§0-2 근거 — 암호화/입력검증 재설계 금지).
 * 응답이 <b>복호화된 위·경도 평문</b>이라 인가가 필수다. 파라미터는 {@code attdId} 뿐이라 인가를 여기서
 * 판정할 수 없으므로, 서비스가 근태 행의 사업장/부서를 먼저 조회한 뒤 판정한다. 그 입력이 되는 토큰
 * 클레임을 함께 싣는다.
 */
public record EmployeeGpsTrailParam(
      String attdId
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(EmployeeGpsTrailParam.class);

    public static EmployeeGpsTrailParam of(String attdId, TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            log.warn("EmployeeGpsTrailParam.of - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (attdId == null || attdId.isBlank()) {
            log.warn("EmployeeGpsTrailParam.of - required field missing: attdId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // ATTD_ID 는 DB 상 varchar(20) 이지만 애플리케이션 포맷은 숫자 시퀀스(yyyyMMdd + seq)다.
        // 비숫자는 "필수 누락"이 아니라 "유효하지 않은 값"이므로 COMMON_400_002 로 응답한다(웹 동일 원칙).
        try {
            Long.parseLong(attdId);
        } catch (NumberFormatException e) {
            log.warn("EmployeeGpsTrailParam.of - attdId 형식 오류(비숫자). attdId={}", attdId);
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        // 인가 판정 입력(토큰 클레임) 결손이면 fail-closed.
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("EmployeeGpsTrailParam.of - token claim missing: gv_cmpnyCd / gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new EmployeeGpsTrailParam(
                attdId,
                tokenInfo.gv_cmpnyCd(),
                tokenInfo.gv_authCd(),
                tokenInfo.gv_userCd(),
                tokenInfo.gv_siteCd());
    }
}
