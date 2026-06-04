package com.prafta.app.req.req09.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.req.req09.dto.response.ApprovalContextResponse;
import com.prafta.app.req.req09.service.AttdApprovalContextService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-009-8: 근태 요청 폼 결재선 분기 컨텍스트 컨트롤러(읽기 전용).
 *
 * <p>실제 매핑 경로(자동 프리픽스 com.prafta.app.* → /prafta/appApi):
 * <ul>
 *   <li>{@code GET /prafta/appApi/req09/approval-context}</li>
 * </ul>
 *
 * <p>스케줄수정/근태보정/초과근무 신청 폼이 진입 시 호출하여 결재선 섹션 노출/숨김을 분기한다.
 * 식별값(cmpnyCd/siteCd/userCd)은 JWT(TokenInfo)에서만 도출하며 바디/쿼리로 받지 않는다(IDOR 가드).
 * {@code workYmd} 는 향후 일자별 노드 변동 대응을 위한 선택 파라미터지만, 현재 분기는 신청자 소속 노드
 * 기준이라 사용하지 않는다(폼 계약 유지 목적으로 수신만 허용).
 */
@Slf4j
@RestController
@RequestMapping("/req09")
@RequiredArgsConstructor
public class AppReq09Controller {

    private final AttdApprovalContextService attdApprovalContextService;
    private final JwtUtil jwtUtil;

    /** 결재선 분기 컨텍스트 조회({ selfApprvYn, isNodeAdmin }). */
    @GetMapping("/approval-context")
    public ResponseEntity<ApprovalContextResponse> getApprovalContext(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null
                || !StringUtils.hasText(tokenInfo.gv_cmpnyCd())
                || !StringUtils.hasText(tokenInfo.gv_siteCd())
                || !StringUtils.hasText(tokenInfo.gv_userCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        ApprovalContextResponse response = attdApprovalContextService.getApprovalContext(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_siteCd(), tokenInfo.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
