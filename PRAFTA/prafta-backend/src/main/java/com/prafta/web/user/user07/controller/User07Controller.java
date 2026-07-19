package com.prafta.web.user.user07.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.dailycontract.DailyContractErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user07.dto.request.ContractStopRequest;
import com.prafta.web.user.user07.dto.response.ContractListResponse;
import com.prafta.web.user.user07.dto.response.ContractRegResponse;
import com.prafta.web.user.user07.dto.response.ContractStopResponse;
import com.prafta.web.user.user07.service.User07Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 웹 User_07(일용직 계약서 관리) 컨트롤러 (일용직 계약서+승인제 T3, UI-DC-05).
 *
 * <p>엔드포인트 (프론트 호출 = /webApi/user07/...):
 * <ul>
 *   <li>GET  /user07/contract-lists?siteCd= — 활성 요약 + 버전 이력</li>
 *   <li>POST /user07/contract — 계약서 등록/교체 (multipart, 업로드=새 버전 D8-②)</li>
 *   <li>POST /user07/contract-stop — 사용중지</li>
 *   <li>GET  /user07/contract-image?siteCd=&amp;contractVer= — 버전별 이미지 스트림(미리보기)</li>
 * </ul>
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. 회사/처리자 식별은 JWT 클레임에서만 도출하며,
 * siteCd 는 리소스 키로 받되 core(DailyContractService)가 사업장 인가를 재검증한다.
 * 파일 경로는 응답에 노출하지 않는다(스트림 응답만).
 */
@Slf4j
@RestController
@RequestMapping("/user07")
@RequiredArgsConstructor
public class User07Controller {

    private final User07Service user07Service;
    private final JwtUtil jwtUtil;

    /** 계약서 관리 조회 — 활성 요약 카드 + 버전 이력 테이블(siteCd 필수). */
    @GetMapping("/contract-lists")
    public ResponseEntity<?> getContractList(
            @RequestParam("siteCd") String siteCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ContractListResponse response = user07Service.selectContractList(siteCd, token);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 계약서 등록/교체 — multipart(siteCd, contractNm, file). 업로드=새 버전(재서명 트리거 D8-②). */
    @PostMapping(value = "/contract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerContract(
            @RequestParam("siteCd") String siteCd,
            @RequestParam("contractNm") String contractNm,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ContractRegResponse response = user07Service.registerContract(siteCd, contractNm, file, token);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 계약서 사용중지 — body={siteCd}. 활성 계약서 없으면 400_003. */
    @PostMapping(value = "/contract-stop", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stopContract(
            @RequestBody ContractStopRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ContractStopResponse response = user07Service.stopContract(request.getSiteCd(), token);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 버전별 계약서 이미지 스트림 (관리자 미리보기 — 사업장 인가 가드는 core 수행). */
    @GetMapping("/contract-image")
    public ResponseEntity<?> getContractImage(
            @RequestParam("siteCd") String siteCd,
            @RequestParam("contractVer") int contractVer,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ImageBytesResult image = user07Service.loadContractImage(siteCd, contractVer, token);
        if (image == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .body(image.data());
    }
}
