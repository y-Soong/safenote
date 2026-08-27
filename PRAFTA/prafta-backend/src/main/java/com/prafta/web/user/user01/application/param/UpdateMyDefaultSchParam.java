package com.prafta.web.user.user01.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UpdateMyDefaultSchRequest;

/**
 * F-8-2: 본인 기본 근무타입 자기변경 Param(웹).
 *
 * <p>대상 회사/사용자는 세션 토큰(gv_cmpnyCd/gv_userCd)에서만 도출한다(IDOR 방지).
 * 사업장(SITE_CD)은 파라미터로 받지 않는다 — 본인 사업장 변경은 소속이동 전용.
 *
 * <p>PRAFTA-001(기본근무타입-승인제, 2026-08-27): 요청등록 전환에 따라 reqReason(변경 사유,
 * 필수) 과 nodeCd(REQ INSERT 의 NODE_CD 컬럼용, 세션 토큰에서만 도출)를 추가한다.
 *
 * <p>PRAFTA-002(결재자 선택 UI, 2026-08-27): 결재선(approverUserCds/presetId)을 운반한다.
 * 바디 신뢰 입력(스코프 가드는 결재 서비스가 수행) — app(UpdateDefaultSchParam) 과 동일 계약.
 */
public record UpdateMyDefaultSchParam(
      String cmpnyCd
    , String userCd
    , String nodeCd
    , String defaultSchCd
    , String reqReason
    , List<String> approverUserCds
    , String presetId
) {
    public static UpdateMyDefaultSchParam from(UpdateMyDefaultSchRequest request, TokenInfo tokenInfo) {
        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // PRAFTA-002: 결재선(approverUserCds/presetId)은 바디 신뢰 입력(스코프 가드는 결재 서비스가 수행).
        List<String> approverUserCds = request.getApproverUserCds() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getApproverUserCds());
        String presetId = trim(request.getPresetId());

        // 본인 기본 근무타입 변경: 대상 회사/사용자 식별자는 토큰 값으로 강제한다(IDOR 방지).
        return new UpdateMyDefaultSchParam(
              tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_nodeCd()
            , request.getDefaultSchCd()
            , request.getReqReason()
            , approverUserCds
            , presetId
        );
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
