package com.prafta.web.risk.riskimpr01.controller;

import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.dto.BytesMultipartFile;
import com.prafta.common.error.risk.RiskImprErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import jakarta.validation.Valid;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementCompleteParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemDeleteParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemListParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemSaveParam;
import com.prafta.web.risk.riskimpr01.dto.request.ImprovementCompleteRequest;
import com.prafta.web.risk.riskimpr01.dto.request.ImprovementItemDeleteRequest;
import com.prafta.web.risk.riskimpr01.dto.request.ImprovementItemListRequest;
import com.prafta.web.risk.riskimpr01.dto.request.ImprovementItemSaveRequest;
import com.prafta.web.risk.riskimpr01.dto.response.ImprovementItemListResponse;
import com.prafta.web.risk.riskimpr01.service.RiskImpr01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위험성평가 개선항목(지속평가대상 관리, prafta-058) 컨트롤러.
 * 식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출하여 IDOR 을 차단한다.
 * axios 프리픽스: /webApi/riskimpr01/...
 */
@Slf4j
@RestController
@RequestMapping("/riskimpr01")
@RequiredArgsConstructor
public class RiskImpr01Controller {

    // 개선사진 디코딩 후 최대 허용 바이트(Low-3). 프로젝트 공용 상수가 없어 합리적 상한(10MB)을 정의한다.
    private static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

    private final RiskImpr01Service riskImpr01Service;
    private final JwtUtil jwtUtil;

    // 개선항목 목록 (평가키 스코프)
    @GetMapping("/improvement-items")
    public ResponseEntity<?> getImprovementItems(
            @ModelAttribute ImprovementItemListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ImprovementItemListResponse response = riskImpr01Service.selectImprovementItems(
            ImprovementItemListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 개선항목 upsert (사진 Base64 JSON 동봉, risk03 선례)
    @PostMapping(value = "/save-item", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveItem(
            @RequestBody @Valid ImprovementItemSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        MultipartFile file = null;
        if (StringUtils.hasText(request.getItemBase64())) {
            byte[] bytes = Base64.getDecoder().decode(request.getItemBase64().trim());
            // Low-3: 디코딩 후 바이트 크기 상한 가드 (상한 초과 시 400)
            if (bytes.length > MAX_UPLOAD_BYTES) {
                throw new ApiException(RiskImprErrorCode.RISKIMPR_400_001);
            }
            String fileName = StringUtils.hasText(request.getItemOriginalFilename())
                    ? request.getItemOriginalFilename()
                    : "upload.bin";
            file = new BytesMultipartFile("item", fileName, null, bytes);
        }

        riskImpr01Service.saveImprovementItem(
            ImprovementItemSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)), file);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 개선항목 삭제 (soft delete)
    @PostMapping(value = "/delete-item", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteItem(
            @RequestBody ImprovementItemDeleteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        riskImpr01Service.deleteImprovementItem(
            ImprovementItemDeleteParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 개선완료 (005→003 전이 + 개선 후 위험도 1-3 가드)
    @PostMapping(value = "/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> complete(
            @RequestBody @Valid ImprovementCompleteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        riskImpr01Service.completeImprovement(
            ImprovementCompleteParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
