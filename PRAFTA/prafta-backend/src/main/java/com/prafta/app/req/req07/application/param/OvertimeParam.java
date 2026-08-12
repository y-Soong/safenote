package com.prafta.app.req.req07.application.param;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.prafta.app.req.req07.dto.request.OvertimeRequest;
import com.prafta.app.req.req07.dto.request.SlotRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-007: 초과근무 신청 등록 Param (JWT 기반). REQ_TYPE='03' 고정.
 */
public record OvertimeParam(
        String cmpnyCd
        , String siteCd
        , String userCd
        , String workYmd
        , String nodeCd
        , List<SlotRequest> slots
        , String reqReason
        , List<String> approverUserCds
        , String presetId
        /**
         * 소정-07 - 근로자 명시 청구 확인 값('Y' 만 확인으로 인정). 단축 기간(육아기·가족돌봄)에만 소비된다.
         * 미전송(null)이면 확인 없음으로 보고 단축 기간 한정 거부(ATTD_400_201) — fail-safe.
         */
        , String reducedWorkOtClaimYn
) {

    /** 소정-07 - 근로자 명시 청구가 확인되었는지 여부('Y' 만 인정). */
    public boolean workerClaimConfirmed() {
        return "Y".equalsIgnoreCase(reducedWorkOtClaimYn);
    }

    public static OvertimeParam from(OvertimeRequest request, TokenInfo tokenInfo) {

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

        List<SlotRequest> slots = request.getSlots() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getSlots());

        // prafta-app-009: 결재선(approverUserCds/presetId)은 바디 신뢰 입력(스코프 가드는 결재 서비스가 수행).
        List<String> approverUserCds = request.getApproverUserCds() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getApproverUserCds());
        String presetId = trim(request.getPresetId());

        // 소정-07: 근로자 명시 청구 확인 값(단축 기간에만 소비. 미전송이면 null → 거부).
        return new OvertimeParam(cmpnyCd, siteCd, userCd, workYmd, nodeCd, slots, reqReason,
                approverUserCds, presetId, trim(request.getReducedWorkOtClaimYn()));
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
