package com.prafta.platform.location.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.location.dto.request.GpsListRequest;

/**
 * 위치정보 조회 파라미터(gps-lists).
 *
 * <p>회사/사업장/일자 3개 파라미터 전부 필수(누락 시 PLATFORM_400_011).
 * clientIp(해석값 — 신뢰 프록시 경유 시 XFF 선두)와 remoteAddr(직접 연결 IP 원시값)는
 * 열람 로그 기록용 — 게이트 인터셉터와 동일 규칙으로 컨트롤러에서 해석해 전달한다(V-1 병기).
 */
public record GpsListParam(
    String cmpnyCd
    , String siteCd
    , String date
    , String gvCmpnyCd
    , String gvUserCd
    , String clientIp
    , String remoteAddr
) {
    public static GpsListParam from(GpsListRequest request, TokenInfo tokenInfo, String clientIp, String remoteAddr) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (request == null
                || isBlank(request.getCmpnyCd())
                || isBlank(request.getSiteCd())
                || isBlank(request.getDate())) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_011);
        }

        // 일자 형식 검증(YYYYMMDD 8자리 숫자) — 하이픈 입력 허용 후 정규화.
        String date = request.getDate().replace("-", "").trim();
        if (date.length() != 8 || !date.chars().allMatch(Character::isDigit)) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_011);
        }

        return new GpsListParam(
            request.getCmpnyCd().trim()
            , request.getSiteCd().trim()
            , date
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , clientIp
            , remoteAddr
        );
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
