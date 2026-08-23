package com.prafta.app.req.req06.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-내승인요청결재라인-1: 본인 요청 결재라인 상세 조회 Param.
 *
 * <p>식별값(cmpnyCd/siteCd/userCd)은 JWT 토큰에서만 도출한다(클라 입력 무시).
 * {@code reqId} 는 경로변수로 받되, 소유권 검증은 서비스 단계(existsMyReqId)에서 수행한다
 * (본 record 자체는 형식 검증만, IDOR 가드는 별도 — MyReqListParam 패턴 동일).
 *
 * <p>작업지시서_소속이동-이력가시성-보정: {@code siteCd} 는 더 이상 소유권 검증 필터에 쓰이지 않는다
 * (소속이동 전 요청도 본인은 항상 상세 조회 가능해야 함). IDOR 가드 원칙 유지 차원에서 JWT 파생값
 * 자체는 계속 보존한다.
 */
public record ApprovalLineDetailParam(
        String cmpnyCd
        , String siteCd
        , String userCd
        , String reqId
) {

    public static ApprovalLineDetailParam from(String reqId, TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String siteCd = tokenInfo.gv_siteCd();
        String userCd = tokenInfo.gv_userCd();

        if (!StringUtils.hasText(cmpnyCd)
                || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(userCd)
                || !StringUtils.hasText(reqId)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new ApprovalLineDetailParam(cmpnyCd, siteCd, userCd, reqId.trim());
    }
}
