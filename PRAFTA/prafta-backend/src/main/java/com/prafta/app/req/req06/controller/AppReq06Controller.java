package com.prafta.app.req.req06.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.req.req06.application.param.ApprovalLineDetailParam;
import com.prafta.app.req.req06.application.param.MyReqListParam;
import com.prafta.app.req.req06.dto.request.MyReqListRequest;
import com.prafta.app.req.req06.dto.response.ApprovalLineDetailResponse;
import com.prafta.app.req.req06.dto.response.MyReqListResponse;
import com.prafta.app.req.req06.service.AppReq06Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-006: 본인 요청 목록 컨트롤러.
 *
 * <p>실제 매핑 경로: {@code GET /prafta/appApi/req06/my} (자동 프리픽스 com.prafta.app.* → /prafta/appApi).
 *
 * <p>인증/식별: AuthAspect 가 JWT 를 검증하고, 본 컨트롤러는 {@link JwtUtil#getAllClaimsAsMap}
 * 으로 TokenInfo 를 도출한다. 식별값(cmpnyCd/siteCd/userCd)을 쿼리/바디로 받지 않는다 (IDOR 가드).
 */
@Slf4j
@RestController
@RequestMapping("/req06")
@RequiredArgsConstructor
public class AppReq06Controller {

    private final AppReq06Service appReq06Service;
    private final JwtUtil jwtUtil;

    /**
     * 본인이 등록한 근태 요청 목록 페이지 조회.
     *
     * @param request       쿼리 파라미터 (reqTypes/reqStatuses/targetYmdFrom/To/sort/offset)
     * @param authorization Bearer JWT — 토큰의 gv_cmpnyCd/gv_siteCd/gv_userCd 만 사용
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyReqList(
            @ModelAttribute MyReqListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        MyReqListResponse response = appReq06Service.selectMyReqList(
                MyReqListParam.from(request, tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * PRAFTA-내승인요청결재라인-1: 본인 요청 결재라인 상세 조회.
     *
     * @param reqId         조회 대상 요청 ID (경로변수, 소유권은 서비스 단계에서 검증)
     * @param authorization Bearer JWT — 토큰의 gv_cmpnyCd/gv_siteCd/gv_userCd 만 사용
     */
    @GetMapping("/my/{reqId}/approval-line")
    public ResponseEntity<?> getMyApprovalLineDetail(
            @PathVariable String reqId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        ApprovalLineDetailResponse response = appReq06Service.selectApprovalLineDetail(
                ApprovalLineDetailParam.from(reqId, tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
