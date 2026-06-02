package com.prafta.app.chkLst.chkLst01.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.chkLst.chkLst01.application.param.ChecklistInfoParam;
import com.prafta.app.chkLst.chkLst01.application.param.InspectResultSaveParam;
import com.prafta.app.chkLst.chkLst01.dto.request.ChecklistInfoRequest;
import com.prafta.app.chkLst.chkLst01.dto.request.SaveInspectResultRequest;
import com.prafta.app.chkLst.chkLst01.dto.response.ChecklistInfoResponse;
import com.prafta.app.chkLst.chkLst01.dto.response.SaveInspectResultResponse;
import com.prafta.app.chkLst.chkLst01.service.AppChkLst01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-036-B1: 앱 chkLst01 컨트롤러.
 * <p>URL/메서드는 기존 그대로 유지(앱 FE 호환):
 *   <ul>
 *     <li>GET  /chkLst01/checklist-infos</li>
 *     <li>POST /chkLst01/save-inspect-result (multipart/form-data)</li>
 *   </ul>
 * <p>prafta-036-C(H-1): 클래스 레벨 @NoAuth 제거 -- AuthAspect 의 JWT 검증이 정상 적용된다.
 * <p>prafta-app-011 변경사항:
 *   <ul>
 *     <li>checklist-infos: 체크포인트 미존재/siteCd 불일치 처리를 service 에 위임 (controller 의 null 체크 제거).</li>
 *     <li>save-inspect-result: void -> SaveInspectResultResponse 반환 (화면 C 요약 표시용).</li>
 *   </ul>
 */
@Slf4j
@RestController
@RequestMapping("/chkLst01")
@RequiredArgsConstructor
public class AppChkLst01Controller {

    private final AppChkLst01Service appChkLst01Service;
    private final JwtUtil jwtUtil;

    /**
     * 체크리스트 정보 조회.
     * <p>prafta-app-011: 응답에 checkpoint 컨텍스트 객체 추가.
     *   siteCd 불일치 -> 403, 체크포인트 미존재 -> 404 (service 에서 throw).
     */
    @GetMapping("/checklist-infos")
    public ResponseEntity<?> getChkLstInfo(
            @ModelAttribute ChecklistInfoRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        ChecklistInfoResponse response = appChkLst01Service.selectChkLstInfo(
                ChecklistInfoParam.from(request, tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 점검결과 저장(multipart/form-data).
     * <p>multipart 처리 보존: @ModelAttribute, files (단수형) Map, 정규식 패턴 모두 유지.
     * <p>prafta-app-011: 저장 요약 응답(SaveInspectResultResponse) 반환.
     */
    @PostMapping(value = "/save-inspect-result", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveInspectResult(
            @ModelAttribute SaveInspectResultRequest request
            , @RequestParam(required = false) Map<String, MultipartFile> file
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        SaveInspectResultResponse response = appChkLst01Service.saveInspectResult(
                InspectResultSaveParam.from(request, file, tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
