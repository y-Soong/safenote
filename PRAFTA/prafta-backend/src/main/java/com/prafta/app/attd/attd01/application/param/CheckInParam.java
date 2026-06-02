package com.prafta.app.attd.attd01.application.param;

import java.math.BigDecimal;

import org.springframework.util.StringUtils;

import com.prafta.app.attd.attd01.dto.request.CheckInRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-003 A1: 셀프 출근(check-in) Param.
 *
 * <p>IDOR 가드: CMPNY_CD/USER_CD/SITE_CD/NODE_CD 는 모두 JWT(tokenInfo) 출처. 본문으로 받지 않는다.
 *
 * <p>GPS 정책(prafta-app-003 확정 모델). from() 에서 1차 검증:
 *   <ul>
 *     <li>lat/lon 누락: 거부하지 않는다. 위치권한 하드게이트로 좌표는 통상 존재하나,
 *         측위 실패 등 결측 시 온사이트(정상)로 폴백(A안). 좌표가 있으면 서비스에서 지오펜스 판정.</li>
 *     <li>isMocked='Y': 폴백과 무관하게 거부(ATTD_400_005). Mock 위치는 위치 신뢰 불가(부정 방지).</li>
 *   </ul>
 *   상세 사유는 서비스/로그에 남기고 사용자에게는 일반화 메시지만 노출(정보 누출 방지).
 *
 * <p>workYmd: 출근 대상 근무일(YYYYMMDD). 미전달/형식오류면 null → 서비스가 서버 today 로 폴백.
 * <p>ipAddr 은 Controller 에서 HttpServletRequest 로 추출하여 주입한다(GPS INSERT 기록용).
 */
public record CheckInParam(
    String cmpnyCd
    , String userCd
    , String siteCd
    , String nodeCd
    , BigDecimal lat
    , BigDecimal lon
    , BigDecimal accuracy
    , String isMocked
    , String workYmd
    , String offsiteReason          // prafta-app-008: 외근 사유(외근일 때 필수). 온사이트면 무시.
    , boolean confirmSkipPrevSlot   // prafta-app-008: §5.5 Case C 확인 플래그(2구간 선행 1구간 스킵 동의).
    , String ipAddr
    , TokenInfo tokenInfo
) {
    public static CheckInParam from(CheckInRequest request, String ipAddr, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();
        String siteCd = tokenInfo.gv_siteCd();

        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(siteCd)) {
            // 사업장 미선택/토큰 손상.
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // Mock 위치 거부(부정 방지). 좌표 결측은 폴백 처리(거부하지 않음).
        if ("Y".equalsIgnoreCase(request.getIsMocked())) {
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // IS_MOCKED 정규화: 'Y' 가 아니면 'N'(스키마 char(1) NOT NULL DEFAULT 'N').
        String isMocked = "Y".equalsIgnoreCase(request.getIsMocked()) ? "Y" : "N";

        // workYmd 정규화: 8자리 숫자만 채택, 그 외(미전달/형식오류)는 null → 서비스 today 폴백.
        String workYmd = request.getWorkYmd();
        if (workYmd != null) {
            workYmd = workYmd.trim();
            if (!workYmd.matches("\\d{8}")) {
                workYmd = null;
            }
        }

        // NODE_CD 는 INSERT 컬럼(소속부서). JWT 출처(빈값 가능 → 서비스가 사용자 NODE_CD 로 폴백).
        String nodeCd = tokenInfo.gv_nodeCd();

        // 외근 사유 정규화: 트림 후 빈값이면 null(외근 필수 검증은 지오펜스 판정 후 서비스에서 수행).
        //   varchar(500) 초과분은 컷(저장 안전). 사용자 입력 그대로 보존(마스킹 대상 아님).
        String offsiteReason = request.getOffsiteReason();
        if (offsiteReason != null) {
            offsiteReason = offsiteReason.trim();
            if (offsiteReason.isEmpty()) {
                offsiteReason = null;
            } else if (offsiteReason.length() > 500) {
                offsiteReason = offsiteReason.substring(0, 500);
            }
        }

        // Case C 확인 플래그: null 은 false(미동의)로 본다. true 일 때만 선행 1구간 스킵 출근 허용.
        boolean confirmSkipPrevSlot = Boolean.TRUE.equals(request.getConfirmSkipPrevSlot());

        return new CheckInParam(
            cmpnyCd
            , userCd
            , siteCd
            , nodeCd
            , request.getLat()
            , request.getLon()
            , request.getAccuracy()
            , isMocked
            , workYmd
            , offsiteReason
            , confirmSkipPrevSlot
            , ipAddr
            , tokenInfo
        );
    }
}
