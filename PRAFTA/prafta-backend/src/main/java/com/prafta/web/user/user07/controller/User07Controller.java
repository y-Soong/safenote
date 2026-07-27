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

import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.dailycontract.DailyContractErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user07.dto.request.ContractStopRequest;
import com.prafta.web.user.user07.dto.response.ContractAmendPrecheckResponse;
import com.prafta.web.user.user07.dto.response.ContractAmendResponse;
import com.prafta.web.user.user07.dto.response.ContractListResponse;
import com.prafta.web.user.user07.dto.response.ContractMetaResponse;
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
 *   <li>GET  /user07/contract-image?siteCd=&amp;contractVer= — 버전별 원본 스트림(미리보기, PDF/이미지)</li>
 *   <li>GET  /user07/contract-meta?siteCd=&amp;contractVer= — 형식/페이지 수(활성 카드 표시, T4)</li>
 *   <li>POST /user07/contract-amend — 미서명 계약서 in-place 정정 (multipart, 버전 미증가·재서명 미발생)</li>
 *   <li>GET  /user07/contract-amend-precheck?siteCd=&amp;contractVer= — 정정 가능 여부 + 경고 카운트</li>
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

    /**
     * 버전별 계약서 원본 스트림 (관리자 미리보기 — 사업장 인가 가드는 core 수행).
     *
     * <p>Content-Type 이 동적이라 PDF 는 브라우저 내장 뷰어로, 이미지는 그대로 열린다(무변경 동작).
     */
    @GetMapping("/contract-image")
    public ResponseEntity<?> getContractImage(
            @RequestParam("siteCd") String siteCd,
            @RequestParam("contractVer") int contractVer,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        FileBytesResult image = user07Service.loadContractImage(siteCd, contractVer, token);
        if (image == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }

    /**
     * 버전별 계약서 형식/페이지 수 (활성 카드 "PDF · N페이지" 표시 — T4).
     *
     * <p>사업장 인가 가드는 core 가 {@code contract-image} 와 동일 기준으로 재검증한다(IDOR).
     * 해당 버전이 없으면 400_003(존재 비노출).
     */
    @GetMapping("/contract-meta")
    public ResponseEntity<?> getContractMeta(
            @RequestParam("siteCd") String siteCd,
            @RequestParam("contractVer") int contractVer,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ContractMetaResponse response = user07Service.loadContractMeta(siteCd, contractVer, token);
        if (response == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 미서명 계약서 in-place 정정 — multipart(siteCd, contractVer, file). 승인시점 버전확정 T3.
     *
     * <p>"교체(새 버전)"는 기존 {@code POST /user07/contract} 를 그대로 쓴다 — 경로가 달라
     * 오작동 위험이 낮다. 본 EP 는 <b>버전을 올리지 않으므로 재서명 트리거가 발생하지 않는다</b>.
     *
     * <p>★{@code contractNm} 은 받지 않는다: 계약서명은 등록 시 고정값이라 정정에서만 변경 가능하면
     * 일관성이 깨진다. 파라미터를 받아두고 무시하면 EP 직접 호출로 정책을 우회할 여지가 남으므로
     * <b>파라미터 자체를 두지 않는다</b>.
     *
     * <p>{@code contractVer} 는 정정 대상 지정용이며 서버가 활성 여부를 재검증한다(클라이언트가 pin 이나
     * 서명 대상 버전에 영향을 줄 수 없다 — K10). 서명 0건 조건도 서버에서 강제한다(400_009).
     *
     * <p>{@code PUT} multipart 는 프로젝트에 선례가 없어 등록 EP 와 동일하게 {@code POST} 로 통일했다.
     */
    @PostMapping(value = "/contract-amend", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> amendContract(
            @RequestParam("siteCd") String siteCd,
            @RequestParam("contractVer") int contractVer,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ContractAmendResponse response = user07Service.amendContract(siteCd, contractVer, file, token);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 정정 사전 점검 — 정정 가능 여부 + 경고 카운트(J10). 사업장 인가 가드는 core 가 재검증한다.
     *
     * <p>응답은 UI 표시 목적이며 <b>최종 방어는 정정 API 의 서버측 재검증</b>이다
     * (여기서 {@code amendable=true} 를 받았다고 정정이 보장되지는 않는다 — 그 사이 서명이 들어올 수 있다).
     * 해당 버전이 없으면 400_003(존재 비노출).
     */
    @GetMapping("/contract-amend-precheck")
    public ResponseEntity<?> getContractAmendPrecheck(
            @RequestParam("siteCd") String siteCd,
            @RequestParam("contractVer") int contractVer,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ContractAmendPrecheckResponse response = user07Service.loadAmendPrecheck(siteCd, contractVer, token);
        if (response == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
