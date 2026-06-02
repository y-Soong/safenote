package com.prafta.web.user.user04.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user04.application.param.ApprovalCandidateParam;
import com.prafta.web.user.user04.application.param.PresetActionParam;
import com.prafta.web.user.user04.application.param.PresetSaveParam;
import com.prafta.web.user.user04.dto.request.ApprovalCandidateRequest;
import com.prafta.web.user.user04.dto.request.PresetActionRequest;
import com.prafta.web.user.user04.dto.request.PresetSaveRequest;
import com.prafta.web.user.user04.dto.response.ApprovalCandidateListResponse;
import com.prafta.web.user.user04.dto.response.PresetListResponse;
import com.prafta.web.user.user04.service.User04Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결재라인 구성 보조 컨트롤러 (prafta-019-D, User_04).
 */
@Slf4j
@RestController
@RequestMapping("/user04")
@RequiredArgsConstructor
public class User04Controller {

    private final User04Service user04Service;
    private final JwtUtil jwtUtil;

    /** 결재자 후보 목록 + 본인 직급 순서 조회. */
    @GetMapping("/approval-candidates")
    public ResponseEntity<?> getApprovalCandidates(
            @ModelAttribute @Valid ApprovalCandidateRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        ApprovalCandidateListResponse response = user04Service.getApprovalCandidates(
                ApprovalCandidateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 본인 결재라인 프리셋 목록 조회 (prafta-020). */
    @GetMapping("/presets")
    public ResponseEntity<?> getPresets(
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        PresetListResponse response = user04Service.getPresets(token.gv_cmpnyCd(), token.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 프리셋 저장(신규/수정) (prafta-020). */
    @PostMapping("/presets/save")
    public ResponseEntity<?> savePreset(
            @RequestBody @Valid PresetSaveRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        String presetId = user04Service.savePreset(
                PresetSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(java.util.Map.of("presetId", presetId));
    }

    /** 프리셋 삭제 (prafta-020). */
    @PostMapping("/presets/delete")
    public ResponseEntity<?> deletePreset(
            @RequestBody @Valid PresetActionRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        user04Service.deletePreset(
                PresetActionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 프리셋 기본 지정 (prafta-020). */
    @PostMapping("/presets/set-default")
    public ResponseEntity<?> setDefaultPreset(
            @RequestBody @Valid PresetActionRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        user04Service.setDefaultPreset(
                PresetActionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
