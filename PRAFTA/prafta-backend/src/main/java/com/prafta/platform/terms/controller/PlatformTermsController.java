package com.prafta.platform.terms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.baim.baim03.application.param.TermsDetailInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoParam;
import com.prafta.web.baim.baim03.application.param.TermsListParam;
import com.prafta.web.baim.baim03.dto.request.TermsDetailInfoListRequest;
import com.prafta.web.baim.baim03.dto.request.TermsInfoListRequest;
import com.prafta.web.baim.baim03.dto.request.TermsInfoRequest;
import com.prafta.web.baim.baim03.dto.response.TermsDetailInfoListResponse;
import com.prafta.web.baim.baim03.dto.response.TermsInfoListResponse;
import com.prafta.web.baim.baim03.service.Baim03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이용약관 관리 컨트롤러(플랫폼 운영자 전용).
 *
 * <p>최종 경로: {@code /prafta/platformApi/terms/*}
 * (패키지 {@code com.prafta.platform.*} → ApiPrefixConfig 가 {@code /prafta/platformApi} 프리픽스 부여).
 *
 * <p>약관(TB_TERMS)은 회사 스코프가 없는 글로벌 데이터이므로, 기존 고객 웹(/webApi/baim03/*)에서
 * 임의 고객사 master 가 편집할 수 있던 경로를 봉쇄하고 본 운영자 전용 경로로 이전한다.
 * 서비스/매퍼/DTO/Param 은 기존 {@code com.prafta.web.baim.baim03.*} 자산을 그대로 재사용한다.
 *
 * <p>운영자 인가/IP 게이트는 {@code PlatformOperatorGateInterceptor} 가, JWT 유효성은 {@code AuthAspect}
 * 가 강제하므로 본 컨트롤러는 별도 권한 분기를 두지 않는다.
 */
@Slf4j
@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class PlatformTermsController {

    private final Baim03Service baim03Service;
    private final JwtUtil jwtUtil;

    @GetMapping("/terms-info-lists")
    public ResponseEntity<?> getTermsList(@ModelAttribute TermsInfoListRequest request) {

        TermsInfoListResponse response = baim03Service.selectTermsList(TermsInfoListParam.from(request));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/terms-detail-info-list")
    public ResponseEntity<?> getTermsDList(@ModelAttribute TermsDetailInfoListRequest request) {

        TermsDetailInfoListResponse response = baim03Service.selectTermsDList(TermsDetailInfoListParam.from(request));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/update-terms-info")
    public ResponseEntity<?> updateTermsInfo(@RequestBody TermsInfoRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        baim03Service.updateTermsInfo(TermsInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteTermsInfo(@RequestBody List<TermsInfoRequest> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        baim03Service.deleteCmmCodeDetailInfo(TermsListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
