package com.prafta.app.dailycontract.dailycontract01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractSignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.MySignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.SignGateResponse;
import com.prafta.app.dailycontract.dailycontract01.service.AppDailyContract01Service;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.dailycontract.DailyContractErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 일용직 근로계약서 컨트롤러 (일용직 계약서+승인제 T3, UI-DC-01·04).
 *
 * <p>최종 URL(ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET  /prafta/appApi/dailycontract01/sign-gate      (서명 게이트 판정 — R2/D8)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/contract-image (활성 계약서 원본 스트림)</li>
 *   <li>POST /prafta/appApi/dailycontract01/sign           (multipart 서명 PNG — 서버 합성 저장, R5)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/my-sign        (본인 서명 메타 — 교부 의무 §6-1)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/my-sign-image  (본인 합성본 스트림)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/dailycontract01/...
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd 는 JWT 클레임에서만 도출하며
 * 파일 경로는 응답에 노출하지 않는다(스트림 응답만). 쓰기(sign)는 일용직(gv_employmentType='DAILY')만 허용.
 */
@Slf4j
@RestController
@RequestMapping("/dailycontract01")
@RequiredArgsConstructor
public class AppDailyContract01Controller {

    private final AppDailyContract01Service appDailyContract01Service;
    private final JwtUtil jwtUtil;

    /** 서명 게이트 판정 — 일용직 아님/활성 계약서 없음/이미 서명이면 signRequiredYn='N'. */
    @GetMapping("/sign-gate")
    public ResponseEntity<?> getSignGate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        SignGateResponse response = appDailyContract01Service.judgeSignGate(
                token.gv_cmpnyCd(), token.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 활성 계약서 원본 이미지 스트림 (본인 사업장 자동 스코프 — 서명 화면 열람용). */
    @GetMapping("/contract-image")
    public ResponseEntity<?> getContractImage(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        ImageBytesResult image = appDailyContract01Service.loadContractImage(
                token.gv_cmpnyCd(), token.gv_userCd());
        if (image == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        return imageStream(image);
    }

    /** 서명 저장 — multipart 서명 PNG. 일용직 전용(서버측 고용형태 가드). */
    @PostMapping(value = "/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sign(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        // 서명은 일용직 본인만 — 정규 사용자 호출은 서버에서 차단(표시 숨김의 서버측 보강).
        if (!AuthRoleUtils.isDailyWorker(token.gv_employmentType())) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_403_001);
        }

        ContractSignResponse response = appDailyContract01Service.sign(
                token.gv_cmpnyCd(), token.gv_userCd(), file);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 본인 최신 서명 메타 — 없으면 signYn='N'(빈 상태 화면). */
    @GetMapping("/my-sign")
    public ResponseEntity<?> getMySign(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        MySignResponse response = appDailyContract01Service.findMySign(
                token.gv_cmpnyCd(), token.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 본인 최신 서명 합성본 스트림 (교부 의무 §6-1 — 열람/저장). */
    @GetMapping("/my-sign-image")
    public ResponseEntity<?> getMySignImage(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        ImageBytesResult image = appDailyContract01Service.loadMySignImage(
                token.gv_cmpnyCd(), token.gv_userCd());
        if (image == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_001);
        }

        return imageStream(image);
    }

    /** JWT 클레임 → TokenInfo. userCd 부재면 인증 결함(COMMON_400_003) — Terms01 미러. */
    private TokenInfo resolveToken(String authorization) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return tokenInfo;
    }

    /** 이미지 바이트 → 스트림 응답(경로 미노출 — inline 표시). */
    private ResponseEntity<byte[]> imageStream(ImageBytesResult image) {
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .body(image.data());
    }
}
