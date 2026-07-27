package com.prafta.app.dailycontract.dailycontract01.controller;

import org.springframework.http.HttpHeaders;
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

import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractMetaResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractSignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.MySignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.SignGateResponse;
import com.prafta.app.dailycontract.dailycontract01.service.AppDailyContract01Service;
import com.prafta.common.cmm.file.application.model.FileBytesResult;
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
 *   <li>GET  /prafta/appApi/dailycontract01/contract-meta  (활성 계약서 형식/페이지 수 — pager 초기화, T4)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/contract-page  (활성 계약서 단일 페이지 PNG — T4)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/contract-image (활성 계약서 원본 스트림 — <b>구버전 앱 폴백</b>, T5)</li>
 *   <li>POST /prafta/appApi/dailycontract01/sign           (multipart 서명 PNG — 서버 합성 저장, R5)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/my-sign        (본인 서명 메타 — 교부 의무 §6-1)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/my-sign-page   (본인 서명본 단일 페이지 PNG — T4)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/my-sign-file   (본인 서명본 원본 다운로드 — T5)</li>
 *   <li>GET  /prafta/appApi/dailycontract01/my-sign-image  (본인 서명본 스트림 — <b>구버전 앱 폴백</b>, T5)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/dailycontract01/...
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd 는 JWT 클레임에서만 도출하며
 * (리소스 키를 파라미터로 받지 않는다 — 타 회사/타 사업장 열거 불가) 파일 경로는 응답에 노출하지 않는다
 * (스트림 응답만). 쓰기(sign)는 일용직(gv_employmentType='DAILY')만 허용.
 *
 * <p>신설 스트림 EP 는 {@code Cache-Control: no-store}(plan §4 D-P10) — 계약서는 성명+자필서명 PII 문서로
 * 웹뷰/프록시 캐시에 잔류해서는 안 된다. 기존 EP({@code contract-image}/{@code my-sign-image})는
 * 구버전 앱 호환을 위해 응답 헤더까지 무변경으로 유지한다.
 */
@Slf4j
@RestController
@RequestMapping("/dailycontract01")
@RequiredArgsConstructor
public class AppDailyContract01Controller {

    private final AppDailyContract01Service appDailyContract01Service;
    private final JwtUtil jwtUtil;

    /** 계약서 스트림 캐시 금지(D-P10) — 성명+자필서명 PII 문서의 웹뷰/프록시 캐시 잔류 방지. */
    private static final String CACHE_NO_STORE = "no-store";

    /** 서명본 다운로드 파일명 base — PII 미포함 고정값(확장자만 가변). */
    private static final String DOWNLOAD_BASE_NM = "contract-signed";

    /** 서명 게이트 판정 — 일용직 아님/활성 계약서 없음/이미 서명이면 signRequiredYn='N'. */
    @GetMapping("/sign-gate")
    public ResponseEntity<?> getSignGate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        SignGateResponse response = appDailyContract01Service.judgeSignGate(
                token.gv_cmpnyCd(), token.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 활성 계약서 메타 — 형식(PDF/IMG)·페이지 수(pager 초기화 T4). 미등록/일용직 아님이면 400_003. */
    @GetMapping("/contract-meta")
    public ResponseEntity<?> getContractMeta(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        ContractMetaResponse response = appDailyContract01Service.findContractMeta(
                token.gv_cmpnyCd(), token.gv_userCd());
        if (response == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 활성 계약서 단일 페이지 스트림 (pager — T4, 1-base).
     *
     * <p>PDF 는 150DPI PNG 온디맨드 렌더, 이미지 양식은 {@code page=1} 만 유효(원본 바이트).
     * 범위 밖/비정수 page 는 400_007 로 통일한다(500 노출 금지).
     *
     * <p>{@code required = false} 필수 — 기본값(true)이면 파라미터 누락 시 Spring 이 메서드 본문
     *    진입 전에 MissingServletRequestParameterException 을 던져 전역 catch-all 로 500 이 된다.
     *    null 은 {@code parsePage} 가 400_007 로 매핑하므로 수신만 허용하면 된다(qa M-1).
     */
    @GetMapping("/contract-page")
    public ResponseEntity<?> getContractPage(
            @RequestParam(value = "page", required = false) String page,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        FileBytesResult file = appDailyContract01Service.loadContractPage(
                token.gv_cmpnyCd(), token.gv_userCd(), parsePage(page));
        if (file == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        return fileStream(file);
    }

    /**
     * 활성 계약서 원본 스트림 (본인 사업장 자동 스코프 — <b>구버전 앱 서명 화면</b> 열람용).
     *
     * <p>T5/P7: PDF 양식이면 core 가 전 페이지 세로 병합 PNG 로 변환해 내려 준다(URL·인가·응답 형태 무변경 —
     * 구버전 앱 코드 수정 불필요). 신규 앱은 {@code contract-meta}/{@code contract-page} 만 사용한다.
     */
    @GetMapping("/contract-image")
    public ResponseEntity<?> getContractImage(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        FileBytesResult image = appDailyContract01Service.loadContractImage(
                token.gv_cmpnyCd(), token.gv_userCd());
        if (image == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_003);
        }

        return legacyImageStream(image);
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

    /**
     * 본인 서명본 단일 페이지 스트림 (내 계약서 pager — T4, 1-base. 레거시 PNG 는 page=1 만 유효).
     *
     * <p>{@code required = false} 이유는 {@link #getContractPage} 주석 참조(qa M-1).
     */
    @GetMapping("/my-sign-page")
    public ResponseEntity<?> getMySignPage(
            @RequestParam(value = "page", required = false) String page,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        FileBytesResult file = appDailyContract01Service.loadMySignPage(
                token.gv_cmpnyCd(), token.gv_userCd(), parsePage(page));
        if (file == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_001);
        }

        return fileStream(file);
    }

    /**
     * 본인 서명본 원본 다운로드 (신규 앱 저장용 — T5, PDF 또는 레거시 PNG 원본 바이트).
     *
     * <p>파일명은 확장자만 가변인 고정 문자열이다 — 성명/휴대폰 등 PII 를 파일명에 넣지 않는다
     * (정책서 {@code common/11-security-privacy.md} §11.1).
     */
    @GetMapping("/my-sign-file")
    public ResponseEntity<?> getMySignFile(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        FileBytesResult file = appDailyContract01Service.loadMySignFile(
                token.gv_cmpnyCd(), token.gv_userCd());
        if (file == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_001);
        }

        String downloadNm = DOWNLOAD_BASE_NM + "." + resolveDownloadExt(file.fileExt());
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, CACHE_NO_STORE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadNm + "\"")
                .body(file.data());
    }

    /**
     * 본인 서명본 스트림 (교부 의무 §6-1 — <b>구버전 앱</b> 열람/저장).
     *
     * <p>T5/P7 + plan §2 A-5: PDF 서명본이면 core 가 세로 병합 PNG 로 변환해 내려 준다
     * (구버전 앱의 {@code <img src>} 열람 유지). 레거시 PNG 합성본은 바이트 그대로.
     */
    @GetMapping("/my-sign-image")
    public ResponseEntity<?> getMySignImage(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = resolveToken(authorization);
        FileBytesResult image = appDailyContract01Service.loadMySignImage(
                token.gv_cmpnyCd(), token.gv_userCd());
        if (image == null) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_001);
        }

        return legacyImageStream(image);
    }

    /** JWT 클레임 → TokenInfo. userCd 부재면 인증 결함(COMMON_400_003) — Terms01 미러. */
    private TokenInfo resolveToken(String authorization) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return tokenInfo;
    }

    /**
     * page 쿼리 파라미터를 1-base 정수로 파싱한다.
     *
     * <p>{@code int} 바인딩으로 두면 비정수/대형값이 Spring 타입 변환 실패 → catch-all 500 으로 떨어지므로
     * 문자열로 받아 도메인 코드(400_007)로 통일한다. 상한 검증은 실제 페이지 수를 아는 core 가 수행한다.
     */
    private int parsePage(String page) {
        try {
            int parsed = Integer.parseInt(page == null ? "" : page.trim());
            if (parsed < 1) {
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_007);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_007);
        }
    }

    /** 신설 스트림 응답(경로 미노출 — inline 표시 + no-store, D-P10). */
    private ResponseEntity<byte[]> fileStream(FileBytesResult file) {
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, CACHE_NO_STORE)
                .body(file.data());
    }

    /**
     * 기존 EP 스트림 응답 — 구버전 앱 호환 유지(URL·인가·바디·Content-Type 무변경).
     *
     * <p>{@code Cache-Control: no-store} 는 예외적으로 추가한다(sec SEC-5 / plan §11 S11). 계약서·서명본은
     * 성명·자필서명이 담긴 3년 보존 법정 문서인데, 오리진이 캐시 금지 신호를 전혀 보내지 않으면
     * 방어가 CloudFront 의 {@code CachingDisabled} 설정에만 의존하게 된다. 캐시 정책 변경·nginx 캐시
     * 도입 중 하나만 발생해도 문서가 캐시되고, 쿼리스트링을 캐시 키에서 제외하는 정책에서는
     * <b>사용자 A 의 계약서가 사용자 B 에게 서빙</b>될 수 있다. 헤더 추가는 상태코드·Content-Type·바디를
     * 바꾸지 않으므로 blob 으로 소비하는 구버전 앱 동작에 영향이 없다.
     */
    private ResponseEntity<byte[]> legacyImageStream(FileBytesResult file) {
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, CACHE_NO_STORE)
                .body(file.data());
    }

    /** 다운로드 파일명 확장자 — 계약서 도메인 허용 확장자만 사용하고 그 외는 pdf 로 폴백(파일명 인젝션 차단). */
    private String resolveDownloadExt(String fileExt) {
        if ("png".equals(fileExt) || "jpg".equals(fileExt) || "jpeg".equals(fileExt)) {
            return fileExt;
        }
        return "pdf";
    }
}
