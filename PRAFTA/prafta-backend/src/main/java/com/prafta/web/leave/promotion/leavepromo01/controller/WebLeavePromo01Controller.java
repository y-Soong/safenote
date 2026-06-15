package com.prafta.web.leave.promotion.leavepromo01.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionDesignateParam;
import com.prafta.web.leave.promotion.leavepromo01.application.param.PromotionTargetSearchParam;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.AutoBatchCommitRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.AutoBatchPreviewRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.PromotionDesignateRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.request.PromotionTargetSearchRequest;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.AutoBatchCommitResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionDesignateResultResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionExcelUploadResponse;
import com.prafta.web.leave.promotion.leavepromo01.dto.response.PromotionTargetListResponse;
import com.prafta.web.leave.promotion.leavepromo01.service.WebLeavePromo01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-008-A-4: 2차 회사직권 연차 사용촉진 웹 컨트롤러.
 *
 * <p>프론트 호출 경로(자동 프리픽스 com.prafta.web.* → /prafta/webApi):
 * <ul>
 *   <li>GET  /webApi/leavepromo01/targets   — 2차 대상자 + 미사용 연차수 조회(노드 권한)</li>
 *   <li>POST /webApi/leavepromo01/designate — 사용자 1명에 날짜 다건 직권지정 + PUSH</li>
 * </ul>
 * 인증/식별: jwtUtil.getAllClaimsAsMap(Authorization) → TokenInfo. siteCd 세션 고정 검증·노드 권한은
 *   Param/서비스에서 강제(IDOR). 근로자 지정일 이동은 attd13 동의흐름(C) 재사용 — 본 컨트롤러 범위 외.
 */
@Slf4j
@RestController
@RequestMapping("/leavepromo01")
@RequiredArgsConstructor
public class WebLeavePromo01Controller {

    private final WebLeavePromo01Service webLeavePromo01Service;
    private final JwtUtil jwtUtil;

    /** 2차 대상자 + 미사용 연차수 조회(조회조건·노드 권한). */
    @GetMapping("/targets")
    public ResponseEntity<?> getTargets(
            @ModelAttribute PromotionTargetSearchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        PromotionTargetListResponse response = webLeavePromo01Service.getDesignateTargets(
                PromotionTargetSearchParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 사용자 1명에 날짜 다건 직권지정(2차/회사직권) + 근로자 PUSH. */
    @PostMapping("/designate")
    public ResponseEntity<?> designate(
            @RequestBody PromotionDesignateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        PromotionDesignateResultResponse response = webLeavePromo01Service.designate(
                PromotionDesignateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ===== prafta-com-008-A-5: 자동배치 프리뷰/커밋 =====

    /** 자동배치 프리뷰(2전략, 등록 없음). 노드 권한 게이트 + 순수 계산. */
    @PostMapping("/autobatch/preview")
    public ResponseEntity<?> autoBatchPreview(
            @RequestBody AutoBatchPreviewRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        BatchProposal proposal = webLeavePromo01Service.previewAutoBatch(
                request, jwtUtil.getAllClaimsAsMap(authorization));
        return ResponseEntity.status(HttpStatus.OK).body(proposal);
    }

    /** 자동배치 커밋(관리자 확인본 직권지정 + PUSH). DIRECT_USE_KEY 멱등으로 TOCTOU 방어. */
    @PostMapping("/autobatch/commit")
    public ResponseEntity<?> autoBatchCommit(
            @RequestBody AutoBatchCommitRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        AutoBatchCommitResponse response = webLeavePromo01Service.commitAutoBatch(
                request, jwtUtil.getAllClaimsAsMap(authorization));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ===== prafta-com-008-A-6: 엑셀 양식/업로드/실패행 2시트 =====

    /** 조회조건 기준 일괄지정 엑셀 양식(.xlsx) 다운로드. 노드 권한 게이트. */
    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> excelTemplate(
            @ModelAttribute PromotionTargetSearchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        byte[] xlsx = webLeavePromo01Service.buildExcelTemplate(
                PromotionTargetSearchParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        String filename = "연차일괄지정양식.xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        headers.setContentLength(xlsx.length);
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(xlsx);
    }

    /** 엑셀 업로드(행=사용자-연차날짜) → 일괄 직권지정. 실패행은 failsToken 으로 2시트 다운로드. */
    @PostMapping(value = "/excel/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> excelUpload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        PromotionExcelUploadResponse response = webLeavePromo01Service.uploadExcel(
                file, jwtUtil.getAllClaimsAsMap(authorization));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 실패행 2시트(.xlsx) 다운로드. failsToken 소유자(cmpny+user) 재검증 후 복호화. 무효/만료면 404. */
    @GetMapping("/excel/fails")
    public ResponseEntity<byte[]> excelFails(
            @RequestParam("token") String token,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        byte[] xlsx = webLeavePromo01Service.downloadFails(token, jwtUtil.getAllClaimsAsMap(authorization));
        if (xlsx == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String filename = "연차일괄지정_실패행.xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        headers.setContentLength(xlsx.length);
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(xlsx);
    }
}
