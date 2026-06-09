package com.prafta.app.tbm.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.tbm.admin.application.param.AdminAttendeeListParam;
import com.prafta.app.tbm.admin.application.param.AdminCancelEntryParam;
import com.prafta.app.tbm.admin.application.param.AdminCompletionParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialListParam;
import com.prafta.app.tbm.admin.application.param.AdminEduMaterialSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminForceExitParam;
import com.prafta.app.tbm.admin.application.param.AdminEligibleRegularParam;
import com.prafta.app.tbm.admin.application.param.AdminHistoryListParam;
import com.prafta.app.tbm.admin.application.param.AdminLiveTransitionParam;
import com.prafta.app.tbm.admin.application.param.AdminManagerDirectParam;
import com.prafta.app.tbm.admin.application.param.AdminQrScanParam;
import com.prafta.app.tbm.admin.application.param.AdminOptionParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionCancelParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionDetailParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionListParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionPrepareParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionPwdParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionSaveParam;
import com.prafta.app.tbm.admin.application.param.AdminSessionUpdateParam;
import com.prafta.app.tbm.admin.dto.request.AdminCompletionRequest;
import com.prafta.app.tbm.admin.dto.request.AdminManagerDirectRequest;
import com.prafta.app.tbm.admin.dto.request.AdminQrScanRequest;
import com.prafta.app.tbm.admin.dto.request.AdminSessionPrepareRequest;
import com.prafta.app.tbm.admin.dto.request.AdminEduMaterialSaveRequest;
import com.prafta.app.tbm.admin.dto.request.AdminForceExitRequest;
import com.prafta.app.tbm.admin.dto.request.AdminSessionCancelRequest;
import com.prafta.app.tbm.admin.dto.request.AdminSessionListRequest;
import com.prafta.app.tbm.admin.dto.request.AdminSessionSaveRequest;
import com.prafta.app.tbm.admin.dto.request.AdminSessionUpdateRequest;
import com.prafta.app.tbm.admin.dto.response.AdminAttendeeListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminCompletionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminContentOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialDetailResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEduMaterialSaveResponse;
import com.prafta.app.tbm.admin.dto.response.AdminEligibleRegularResponse;
import com.prafta.app.tbm.admin.dto.response.AdminForceExitResponse;
import com.prafta.app.tbm.admin.dto.response.AdminManagerDirectResponse;
import com.prafta.app.tbm.admin.dto.response.AdminQrScanResponse;
import com.prafta.app.tbm.admin.dto.response.AdminHistoryListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminLiveTransitionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminMaterialTypeOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminRiskOptionResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionContentsResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionDetailResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionListResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionPwdResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSessionSaveResponse;
import com.prafta.app.tbm.admin.dto.response.AdminSiteOptionResponse;
import com.prafta.app.tbm.admin.service.AppAdminTbmService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 001-P5: 모바일 앱 관리자 모드 TBM 관리 컨트롤러 (R1+R2 — 교육관리/상세/개설/수정/취소/비번재발급/옵션).
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 *   <ul>
 *     <li>GET  /prafta/appApi/admin/tbm/sessions                         (T-A1 교육관리 리스트)</li>
 *     <li>GET  /prafta/appApi/admin/tbm/sessions/{sessionCd}             (T-A2 세션 상세)</li>
 *     <li>POST /prafta/appApi/admin/tbm/sessions                         (T-A3 개설/임시저장)</li>
 *     <li>PUT  /prafta/appApi/admin/tbm/sessions/{sessionCd}             (T-A4 수정)</li>
 *     <li>POST /prafta/appApi/admin/tbm/sessions/{sessionCd}/cancel      (T-A4 취소)</li>
 *     <li>POST /prafta/appApi/admin/tbm/sessions/{sessionCd}/regenerate-password (T-A4 비번 재발급)</li>
 *     <li>GET  /prafta/appApi/admin/tbm/content-options                  (T-K)</li>
 *     <li>GET  /prafta/appApi/admin/tbm/risk-options                     (T-K)</li>
 *     <li>GET  /prafta/appApi/admin/tbm/site-options                     (T-K)</li>
 *   </ul>
 *
 * <p>인증/IDOR(D1): AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd/authCd/siteCd 는 JWT 클레임에서만
 *   도출하며 바디/패스로 식별자를 받지 않는다. path 의 sessionCd 는 식별자가 아닌 리소스 키이며,
 *   서버가 토큰 회사/스코프 내에 속하는지 재검증한다(스코프 누수/IDOR 차단).
 *
 * <p>경계: 본 컨트롤러는 OPENED 까지만 다룬다(R1+R2). 교육 시작(IN_PROGRESS)/진행/종료/강제퇴실/
 *   개별 미이수처리는 후속 라운드(R3/R4)에서 추가한다.
 */
@Slf4j
@RestController
@RequestMapping("/admin/tbm")
@RequiredArgsConstructor
public class AppAdminTbmController {

    private final AppAdminTbmService appAdminTbmService;
    private final JwtUtil jwtUtil;

    /** T-A1 교육관리 리스트(DRAFT/OPENED/IN_PROGRESS, 상태/제목/출결·이수·미이수). */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(@ModelAttribute AdminSessionListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminSessionListResponse response = appAdminTbmService.selectSessionList(
                AdminSessionListParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** T-A2 세션 상세(메타/내용/GPS/콘텐츠/위험성 + OPENED↑ 비번). */
    @GetMapping("/sessions/{sessionCd}")
    public ResponseEntity<?> getSessionDetail(@PathVariable("sessionCd") String sessionCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminSessionDetailResponse response = appAdminTbmService.selectSessionDetail(
                AdminSessionDetailParam.of(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** T-A3 개설(OPENED) / 임시저장(DRAFT). OPENED 시 입실/종료 비번 발급. */
    @PostMapping(value = "/sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveSession(@RequestBody AdminSessionSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminSessionSaveResponse response = appAdminTbmService.saveSession(
                AdminSessionSaveParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** T-A4 수정(DRAFT/OPENED만). */
    @PutMapping(value = "/sessions/{sessionCd}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateSession(@PathVariable("sessionCd") String sessionCd,
            @RequestBody AdminSessionUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appAdminTbmService.updateSession(
                AdminSessionUpdateParam.from(sessionCd, request, tokenInfo));

        return ResponseEntity.ok().build();
    }

    /** T-A4 취소(DRAFT/OPENED만, 사유 필수). */
    @PostMapping(value = "/sessions/{sessionCd}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancelSession(@PathVariable("sessionCd") String sessionCd,
            @RequestBody AdminSessionCancelRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appAdminTbmService.cancelSession(
                AdminSessionCancelParam.from(sessionCd, request, tokenInfo));

        return ResponseEntity.ok().build();
    }

    /** prafta-051 E6 입실비밀번호 전용 재발급(OPENED만). */
    @PostMapping(value = "/sessions/{sessionCd}/regenerate-entry-password")
    public ResponseEntity<?> regenerateEntryPassword(@PathVariable("sessionCd") String sessionCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminSessionPwdResponse response = appAdminTbmService.regenerateEntryPassword(
                AdminSessionPwdParam.of(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** prafta-051 E7 종료비밀번호 전용 재발급(COMPLETED만). */
    @PostMapping(value = "/sessions/{sessionCd}/regenerate-exit-password")
    public ResponseEntity<?> regenerateExitPassword(@PathVariable("sessionCd") String sessionCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminSessionPwdResponse response = appAdminTbmService.regenerateExitPassword(
                AdminSessionPwdParam.of(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** T-K 콘텐츠 선택 옵션. */
    @GetMapping("/content-options")
    public ResponseEntity<?> getContentOptions(
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestParam(value = "searchKeyword", required = false) String searchKeyword,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminContentOptionResponse response = appAdminTbmService.selectContentOptions(
                AdminOptionParam.of(siteCd, searchKeyword, null, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** T-K 위험성평가 선택 옵션. */
    @GetMapping("/risk-options")
    public ResponseEntity<?> getRiskOptions(
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestParam(value = "processCd", required = false) String processCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminRiskOptionResponse response = appAdminTbmService.selectRiskOptions(
                AdminOptionParam.of(siteCd, null, processCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** T-K 사업장 선택 옵션(접근가능 사업장 — access-context.accessibleSites 동일 소스). */
    @GetMapping("/site-options")
    public ResponseEntity<?> getSiteOptions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminSiteOptionResponse response = appAdminTbmService.selectSiteOptions(tokenInfo);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ============================ prafta-051 R-A 교육준비/연장 ============================

    /** prafta-051 E2 교육준비 전이(DRAFT→OPENED). 입실비번/관리자좌표/PREP_START_AT 확정. 개설자만. */
    @PostMapping(value = "/sessions/{sessionCd}/prepare", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> prepareSession(@PathVariable("sessionCd") String sessionCd,
            @RequestBody(required = false) AdminSessionPrepareRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminLiveTransitionResponse response = appAdminTbmService.prepareSession(
                AdminSessionPrepareParam.from(sessionCd, request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** prafta-051 E3 교육준비 연장(PREP_START_AT 리셋). 개설자만. */
    @PostMapping("/sessions/{sessionCd}/extend-prep")
    public ResponseEntity<?> extendPrep(@PathVariable("sessionCd") String sessionCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appAdminTbmService.extendPrep(AdminLiveTransitionParam.of(sessionCd, tokenInfo));

        return ResponseEntity.ok().build();
    }

    // ============================ R3 라이브 제어 ============================

    /** R3 T1 교육 시작(OPENED→IN_PROGRESS). 개설자만 허용. */
    @PostMapping("/sessions/{sessionCd}/start")
    public ResponseEntity<?> startSession(@PathVariable("sessionCd") String sessionCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminLiveTransitionResponse response = appAdminTbmService.startSession(
                AdminLiveTransitionParam.of(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** R3 T1 교육 종료(IN_PROGRESS→COMPLETED) + T2 미종료 출결 자동이수. 개설자만 허용. */
    @PostMapping("/sessions/{sessionCd}/end")
    public ResponseEntity<?> endSession(@PathVariable("sessionCd") String sessionCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminLiveTransitionResponse response = appAdminTbmService.endSession(
                AdminLiveTransitionParam.of(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** R3 출결 리스트(phase=LIVE: 입실자만 / COMPLETED: 출결 전체). */
    @GetMapping("/sessions/{sessionCd}/attendees")
    public ResponseEntity<?> getAttendees(@PathVariable("sessionCd") String sessionCd,
            @RequestParam(value = "phase", required = false) String phase,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminAttendeeListResponse response = appAdminTbmService.selectAttendees(
                AdminAttendeeListParam.of(sessionCd, phase, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** R3 T3 강제 퇴실(진행 중) + 자동 미이수. */
    @PostMapping(value = "/sessions/{sessionCd}/attendees/{attendanceCd}/force-exit",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forceExitAttendee(@PathVariable("sessionCd") String sessionCd,
            @PathVariable("attendanceCd") String attendanceCd,
            @RequestBody(required = false) AdminForceExitRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminForceExitResponse response = appAdminTbmService.forceExitAttendee(
                AdminForceExitParam.from(sessionCd, attendanceCd, request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** R3 T4 개별 이수처리(이수↔미이수, GPS 검증 세션 한정). */
    @PostMapping(value = "/sessions/{sessionCd}/attendees/{attendanceCd}/completion",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateAttendeeCompletion(@PathVariable("sessionCd") String sessionCd,
            @PathVariable("attendanceCd") String attendanceCd,
            @RequestBody AdminCompletionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminCompletionResponse response = appAdminTbmService.updateAttendeeCompletion(
                AdminCompletionParam.from(sessionCd, attendanceCd, request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ============================ prafta-051 R-B 입실경로(정규직 대리입실) ============================

    /**
     * prafta-051 E9 정규직 대리입실 후보 검색(세션 사업장/노드 스코프 내 정규직, 이름/사번).
     * 이미 입실한 사용자는 alreadyEntered=true 로 표시(목록 유지, 프론트가 비활성 처리).
     */
    @GetMapping("/sessions/{sessionCd}/eligible-regulars")
    public ResponseEntity<?> getEligibleRegulars(@PathVariable("sessionCd") String sessionCd,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminEligibleRegularResponse response = appAdminTbmService.selectEligibleRegulars(
                AdminEligibleRegularParam.of(sessionCd, keyword, page, pageSize, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * prafta-051 E10 정규직 관리자 대리입실(MANAGER_DIRECT). 세션 OPENED + 대상 스코프 재검증 + UK 멱등.
     * 비번 입력 없음, GPS 반경검증 안 함(D-4). ENTRY_GPS 는 세션 관리자 좌표를 감사용으로 복사.
     */
    @PostMapping(value = "/sessions/{sessionCd}/attendees/manager-direct",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> managerDirectEnter(@PathVariable("sessionCd") String sessionCd,
            @RequestBody AdminManagerDirectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminManagerDirectResponse response = appAdminTbmService.managerDirectEnter(
                AdminManagerDirectParam.from(sessionCd, request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ============================ prafta-051 R-D 입실경로(일용직 QR 입실) ============================

    /**
     * prafta-051 E11 일용직 QR 입실(MANAGER_QR_SCAN). 세션 OPENED + QR 페이로드 파싱(userCd) + 일용직 유효성
     * 재검증 + UK 멱등. 비번 입력 없음, GPS 반경검증 안 함(D-4). QR 의 회사/사업장은 신뢰하지 않고 토큰 CMPNY +
     * 세션 SITE 로 서버 재검증한다(#DF-1). 파싱 실패/userCd 누락은 400.
     */
    @PostMapping(value = "/sessions/{sessionCd}/attendees/qr-scan",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> qrScanEnter(@PathVariable("sessionCd") String sessionCd,
            @RequestBody AdminQrScanRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminQrScanResponse response = appAdminTbmService.qrScanEnter(
                AdminQrScanParam.from(sessionCd, request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ============================ prafta-051 R-C 이탈자 내보내기(입실취소) ============================

    /**
     * prafta-051 E13 입실취소(GPS 이탈자 내보내기). 세션 OPENED(교육 미시작) + 스코프 재검증 후 물리삭제(#D-RE2).
     * 강제퇴실(force-exit, IN_PROGRESS)/미이수와 구분되는 교육 미시작 단계의 단순 입실취소다. 재입실 자유.
     */
    @PostMapping("/sessions/{sessionCd}/attendees/{attendanceCd}/cancel-entry")
    public ResponseEntity<?> cancelEntry(@PathVariable("sessionCd") String sessionCd,
            @PathVariable("attendanceCd") String attendanceCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appAdminTbmService.cancelEntry(
                AdminCancelEntryParam.of(sessionCd, attendanceCd, tokenInfo));

        return ResponseEntity.ok().build();
    }

    /** R3 진행화면 슬라이드용 자료 항목 조회. */
    @GetMapping("/sessions/{sessionCd}/contents")
    public ResponseEntity<?> getSessionContents(@PathVariable("sessionCd") String sessionCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminSessionContentsResponse response = appAdminTbmService.selectSessionContents(
                AdminSessionDetailParam.of(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ============================ R5 교육자료 관리 ============================

    /** R5 자료 리스트(탭3). 스코프=회사공통 OR 접근가능 사업장. */
    @GetMapping("/edu-materials")
    public ResponseEntity<?> getEduMaterials(
            @RequestParam(value = "mtrlType", required = false) String mtrlType,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "useYn", required = false) String useYn,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminEduMaterialListResponse response = appAdminTbmService.selectEduMaterials(
                AdminEduMaterialListParam.of(mtrlType, title, useYn, page, pageSize, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** R5 자료 상세(묶음 + 항목). mtrlCd 토큰 스코프 소유 재검증(IDOR). */
    @GetMapping("/edu-materials/{mtrlCd}")
    public ResponseEntity<?> getEduMaterialDetail(@PathVariable("mtrlCd") String mtrlCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminEduMaterialDetailResponse response = appAdminTbmService.selectEduMaterialDetail(
                AdminEduMaterialDetailParam.of(mtrlCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** R5 자료 등록(멀티파트 A안). data(JSON) + files(신규 업로드). 묶음+항목 INSERT + 파일 저장. */
    @PostMapping(value = "/edu-materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEduMaterial(
            @RequestPart("data") AdminEduMaterialSaveRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminEduMaterialSaveResponse response = appAdminTbmService.saveEduMaterial(
                AdminEduMaterialSaveParam.from(null, request, files, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** R5 자료 수정(멀티파트 A안). 묶음 UPDATE + 항목 재구성 + 신규 파일 저장. */
    @PutMapping(value = "/edu-materials/{mtrlCd}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEduMaterial(@PathVariable("mtrlCd") String mtrlCd,
            @RequestPart("data") AdminEduMaterialSaveRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminEduMaterialSaveResponse response = appAdminTbmService.updateEduMaterial(
                AdminEduMaterialSaveParam.from(mtrlCd, request, files, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** R5 자료 삭제(소프트삭제 USE_YN='N'). 세션-콘텐츠 매핑/이력 참조 보존. */
    @DeleteMapping("/edu-materials/{mtrlCd}")
    public ResponseEntity<?> deleteEduMaterial(@PathVariable("mtrlCd") String mtrlCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appAdminTbmService.deleteEduMaterial(AdminEduMaterialDetailParam.of(mtrlCd, tokenInfo));

        return ResponseEntity.ok().build();
    }

    /** R5 자료 타입(COM003) 옵션. 프론트 타입 필터/셀렉트용. */
    @GetMapping("/material-type-options")
    public ResponseEntity<?> getMaterialTypeOptions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminMaterialTypeOptionResponse response = appAdminTbmService.selectMaterialTypeOptions(tokenInfo);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ============================ R6 이력 ============================

    /** R6 이력 리스트(탭4) + 상단 통계. STATUS_CD IN (COMPLETED, CANCELLED), R3 동일 스코프. */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(value = "statusCd", required = false) String statusCd,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminHistoryListResponse response = appAdminTbmService.selectHistory(
                AdminHistoryListParam.of(statusCd, startDate, endDate, keyword, page, pageSize, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
