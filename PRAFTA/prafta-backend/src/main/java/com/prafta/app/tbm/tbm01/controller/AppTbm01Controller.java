package com.prafta.app.tbm.tbm01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.prafta.app.tbm.tbm01.application.param.TbmSessionDetailParam;
import com.prafta.app.tbm.tbm01.application.param.TbmSessionListParam;
import com.prafta.app.tbm.tbm01.dto.request.TbmEnterRequest;
import com.prafta.app.tbm.tbm01.dto.request.TbmEntryContextRequest;
import com.prafta.app.tbm.tbm01.dto.request.TbmExitRequest;
import com.prafta.app.tbm.tbm01.dto.request.TbmSessionListRequest;
import com.prafta.app.tbm.tbm01.dto.response.TbmActionResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmAttendeeListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmCompletionResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmContentResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEnterResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmEntryContextResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmExitResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmMyAttendanceResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmRiskListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmSessionListResponse;
import com.prafta.app.tbm.tbm01.dto.response.TbmSessionStateResponse;
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

    // -------------------------------------------------------------------------
    // prafta-app-tbm: 사용자 앱 TBM 허브 조회/액션 (A1~A10)
    //  - 식별자(CMPNY/SITE/USER/USER_TYPE)는 JWT 클레임에서만 도출(IDOR 차단).
    //  - sessionCd 외 어떤 식별자도 path/query/body 로 받지 않는다.
    // -------------------------------------------------------------------------

    /** A1/A2/A3: 탭별 세션 리스트(tab=AVAILABLE|IN_PROGRESS|COMPLETED). */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(
            @ModelAttribute TbmSessionListRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmSessionListResponse response = appTbm01Service.selectSessions(
                TbmSessionListParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A4: 참석자 리스트(이름+입실시각, PII 최소). */
    @GetMapping("/sessions/{sessionCd}/attendees")
    public ResponseEntity<?> getAttendees(
            @PathVariable("sessionCd") String sessionCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmAttendeeListResponse response = appTbm01Service.selectAttendees(
                TbmSessionDetailParam.from(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A5: 세션 시작/종료 상태(STATUS_CD 판정). on-demand 분기용. */
    @GetMapping("/sessions/{sessionCd}/state")
    public ResponseEntity<?> getState(
            @PathVariable("sessionCd") String sessionCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmSessionStateResponse response = appTbm01Service.selectState(
                TbmSessionDetailParam.from(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A6: 교육내용 + 자료 묶음(≤3). */
    @GetMapping("/sessions/{sessionCd}/content")
    public ResponseEntity<?> getContent(
            @PathVariable("sessionCd") String sessionCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmContentResponse response = appTbm01Service.selectContent(
                TbmSessionDetailParam.from(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A7: 연계 위험성평가 리스트. */
    @GetMapping("/sessions/{sessionCd}/risks")
    public ResponseEntity<?> getRisks(
            @PathVariable("sessionCd") String sessionCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmRiskListResponse response = appTbm01Service.selectRisks(
                TbmSessionDetailParam.from(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A8: 시작전 퇴실(출결 취소, 물리 삭제). 멱등. */
    @PostMapping("/sessions/{sessionCd}/leave-before")
    public ResponseEntity<?> leaveBefore(
            @PathVariable("sessionCd") String sessionCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmActionResponse response = appTbm01Service.leaveBefore(
                TbmSessionDetailParam.from(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A9: 중도퇴실(미이수 종료). 멱등. */
    @PostMapping("/sessions/{sessionCd}/withdraw")
    public ResponseEntity<?> withdraw(
            @PathVariable("sessionCd") String sessionCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmActionResponse response = appTbm01Service.withdraw(
                TbmSessionDetailParam.from(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A10: 완료 상세(교육내용/자료명/위험성제목/서명파일코드). */
    @GetMapping("/sessions/{sessionCd}/my-completion")
    public ResponseEntity<?> getMyCompletion(
            @PathVariable("sessionCd") String sessionCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmCompletionResponse response = appTbm01Service.selectMyCompletion(
                TbmSessionDetailParam.from(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * [정합성 수정] 본인 출결 상태 조회(대기/진행 화면 이탈 감지용).
     * <p>GET /prafta/appApi/tbm/sessions/{sessionCd}/my-attendance.
     * 스코프는 JWT(userCd)만 신뢰(IDOR 안전). present/entered/exitAt/exitTypeCd/completionStatusCd 반환.
     */
    @GetMapping("/sessions/{sessionCd}/my-attendance")
    public ResponseEntity<?> getMyAttendance(
            @PathVariable("sessionCd") String sessionCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        TbmMyAttendanceResponse response = appTbm01Service.selectMyAttendanceStatus(
                TbmSessionDetailParam.from(sessionCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
