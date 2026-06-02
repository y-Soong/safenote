package com.prafta.app.risk.risk01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.risk.risk01.application.param.RiskAssessmentSaveParam;
import com.prafta.app.risk.risk01.application.param.RiskTypeInfoParam;
import com.prafta.app.risk.risk01.dto.request.RiskAssessmentRequest;
import com.prafta.app.risk.risk01.dto.request.RiskTypeInfoRequest;
import com.prafta.app.risk.risk01.dto.response.RiskTypeInfoResponse;
import com.prafta.app.risk.risk01.service.AppRisk01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-036-B2: 앱 위험성평가(risk01) 컨트롤러.
 * <p>URL/메서드는 기존 그대로 유지(앱 FE 호환):
 *   <ul>
 *     <li>GET  /risk01/risk-type-infos</li>
 *     <li>POST /risk01/save-risk-assessments (multipart/form-data, 단일 파일)</li>
 *   </ul>
 * <p>prafta-036-C(H-1): 클래스 레벨 @NoAuth 제거 — AuthAspect 의 JWT 검증이 정상 적용된다.
 */
@Slf4j
@RestController
@RequestMapping("/risk01")
@RequiredArgsConstructor
public class AppRisk01Controller {

    private final AppRisk01Service appRisk01Service;
    private final JwtUtil jwtUtil;

    /**
     * 위험성평가 구분/분류/발생상황 조회.
     */
    @GetMapping("/risk-type-infos")
    public ResponseEntity<?> getRiskTypeInfo(
            @ModelAttribute RiskTypeInfoRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        RiskTypeInfoResponse response = appRisk01Service.selectRiskTypeInfo(
                RiskTypeInfoParam.from(request, tokenInfo)
        );

        if (response == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 위험성평가 저장 (multipart/form-data, 단일 파일).
     * <p>multipart 처리 보존: @ModelAttribute, @RequestPart(value="item") MultipartFile (단일 파일).
     */
    @PostMapping(value = "/save-risk-assessments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveRiskAssessments(
            @ModelAttribute RiskAssessmentRequest request
            , @RequestPart(value = "item", required = false) MultipartFile file
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        appRisk01Service.saveRiskAssessments(
                RiskAssessmentSaveParam.from(request, file, tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
