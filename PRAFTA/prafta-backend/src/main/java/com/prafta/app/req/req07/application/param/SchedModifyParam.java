package com.prafta.app.req.req07.application.param;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.prafta.app.req.req07.dto.request.SchedModifyRequest;
import com.prafta.app.req.req07.dto.request.SlotRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-007: 스케줄 수정 요청 등록 Param (JWT 기반).
 *
 * <p>식별값(cmpnyCd/siteCd/userCd) 은 JWT 토큰에서만 도출. 본 클래스 생성 시점에 IDOR 가드.
 * <p>구조적 검증 (slots null / 빈 배열 / size 범위) 만 본 클래스에서 수행한다.
 * 비즈니스 검증 (workSeq 중복, 시각 형식, 마감 여부 등) 은 Service 가 수행.
 */
public record SchedModifyParam(
        String cmpnyCd
        , String siteCd
        , String userCd
        , String workYmd
        , String nodeCd
        , List<SlotRequest> slots
        , String reqReason
) {

    public static SchedModifyParam from(SchedModifyRequest request, TokenInfo tokenInfo) {

        if (tokenInfo == null || request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String siteCd = tokenInfo.gv_siteCd();
        String userCd = tokenInfo.gv_userCd();

        if (!StringUtils.hasText(cmpnyCd)
                || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String workYmd = trim(request.getWorkYmd());
        // nodeCd 는 사용자 소속 부서이므로 바디가 아닌 JWT(gv_nodeCd)에서 도출한다(IDOR-safe).
        //   앱 근태 응답 DTO에 nodeCd가 없어 바디가 항상 비어 있었고, 그 결과 아래 검증이
        //   COMMON_400_003(토큰 없음)으로 떨어져 프론트가 강제 로그아웃되던 버그를 차단한다.
        String nodeCd = tokenInfo.gv_nodeCd();
        String reqReason = trim(request.getReqReason());

        // 필수 바디 파라미터 누락은 토큰 오류(COMMON_400_003)가 아니라 파라미터 오류(COMMON_400_001).
        if (!StringUtils.hasText(workYmd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // slots: 1~2 (Service 가 추가 검증)
        List<SlotRequest> slots = request.getSlots() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getSlots());

        return new SchedModifyParam(cmpnyCd, siteCd, userCd, workYmd, nodeCd, slots, reqReason);
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
