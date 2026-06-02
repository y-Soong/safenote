package com.prafta.app.tbm.tbm01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.tbm.tbm01.application.param.TbmEnterParam;
import com.prafta.app.tbm.tbm01.application.param.TbmEntryContextParam;
import com.prafta.app.tbm.tbm01.application.param.TbmExitParam;
import com.prafta.app.tbm.tbm01.dto.request.TbmEnterRequest;
import com.prafta.app.tbm.tbm01.dto.request.TbmEntryContextRequest;
import com.prafta.app.tbm.tbm01.dto.request.TbmExitRequest;
import com.prafta.app.tbm.tbm01.dto.response.TbmEnterResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEntryContextResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmExitResponse;
import com.prafta.app.tbm.tbm01.service.AppTbm01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-004-C: 모바일 앱 TBM 입실/종료 컨트롤러 (tbm01, 정규직 REGULAR MVP).
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 *   <ul>
 *     <li>GET  /prafta/appApi/tbm/entry-context?sessionCd=...</li>
 *     <li>POST /prafta/appApi/tbm/enter         (application/json)</li>
 *     <li>POST /prafta/appApi/tbm/exit          (multipart/form-data, item=종료서명)</li>
 *   </ul>
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. CMPNY_CD/USER_CD/SITE_CD 는 JWT 에서만 얻으며
 *   바디로 식별자를 받지 않는다. USER_TYPE_CD='REGULAR' 고정.
 */
@Slf4j
@RestController
@RequestMapping("/tbm")
@RequiredArgsConstructor
public class AppTbm01Controller {

    private final AppTbm01Service appTbm01Service;
    private final JwtUtil jwtUtil;

    /** C3: 입실 컨텍스트 조회. */
    @GetMapping("/entry-context")
    public ResponseEntity<?> getEntryContext(
            @ModelAttribute TbmEntryContextRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmEntryContextResponse response = appTbm01Service.selectEntryContext(
                TbmEntryContextParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** C1: 입실(JSON). */
    @PostMapping("/enter")
    public ResponseEntity<?> enter(
            @RequestBody TbmEnterRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmEnterResponse response = appTbm01Service.enter(
                TbmEnterParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** C2: 종료(multipart/form-data, 종료 서명 단일 파일 item). */
    @PostMapping(value = "/exit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> exit(
            @ModelAttribute TbmExitRequest request
            , @RequestPart(value = "item", required = false) MultipartFile file
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmExitResponse response = appTbm01Service.exit(
                TbmExitParam.from(request, file, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
