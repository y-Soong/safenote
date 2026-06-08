package com.prafta.app.notice.notice01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.notice.notice01.application.param.AppNoticeAckParam;
import com.prafta.app.notice.notice01.application.param.AppNoticeFileDlParam;
import com.prafta.app.notice.notice01.application.param.AppNoticeInfoParam;
import com.prafta.app.notice.notice01.application.param.AppNoticePopupParam;
import com.prafta.app.notice.notice01.dto.request.AppNoticeAckRequest;
import com.prafta.app.notice.notice01.dto.request.AppNoticeFileDlRequest;
import com.prafta.app.notice.notice01.dto.request.AppNoticeInfoRequest;
import com.prafta.app.notice.notice01.dto.response.AppMyNoticeListResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeDetailResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeFileDlResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticePopupResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeUnreadCountResponse;
import com.prafta.app.notice.notice01.service.AppNotice01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 공지 표시/ACK/다운로드 컨트롤러 (prafta-app-023-1).
 *
 * <p>실제 매핑 경로(자동 프리픽스 com.prafta.app.* → /prafta/appApi):
 *   POST /prafta/appApi/notice01/popup, GET /my-notices, GET /unread-count,
 *   GET /notice-info, POST /ack-confirm, POST /ack-snooze, POST /read, GET /file-download-token
 * <p>식별자(cmpnyCd/userCd/siteCd/nodeCd/authCd)는 JWT 클레임에서만 도출(IDOR 차단).
 *    userCd/siteCd/nodeCd 등 식별값을 바디/쿼리로 받지 않는다.
 * <p>앱 완전분리(app-010/012): 웹 컨트롤러(/webApi/notice01/*) 를 호출하지 않는다.
 *    첨부 실제 스트림은 웹 @NoAuth /webApi/notice01/file-download?token= 재사용(앱 스트림 EP 미신설).
 */
@Slf4j
@RestController
@RequestMapping("/notice01")
@RequiredArgsConstructor
public class AppNotice01Controller {

    private final AppNotice01Service appNotice01Service;
    private final JwtUtil jwtUtil;

    // 로그인 직후 팝업 노출 판정 (세션 기준)
    @PostMapping("/popup")
    public ResponseEntity<?> getPopupNotices(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppNoticePopupResponse response = appNotice01Service.selectPopupNotices(
            AppNoticePopupParam.from(tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 내 공지 목록 (카드/전체목록) + 미열람 카운트 동봉
    @GetMapping("/my-notices")
    public ResponseEntity<?> getMyNotices(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppMyNoticeListResponse response = appNotice01Service.selectMyNotices(
            AppNoticePopupParam.from(tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 미열람 공지 카운트 (독립 호출)
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppNoticeUnreadCountResponse response = appNotice01Service.selectUnreadCount(
            AppNoticePopupParam.from(tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 공지 단건 상세 (대상 재검증 fail-closed + LAST_READ_DATE 갱신)
    @GetMapping("/notice-info")
    public ResponseEntity<?> getNoticeInfo(
            @ModelAttribute AppNoticeInfoRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppNoticeDetailResponse response = appNotice01Service.selectNoticeInfo(
            AppNoticeInfoParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 확인 처리(CONFIRMED)
    @PostMapping(value = "/ack-confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ackConfirm(
            @RequestBody AppNoticeAckRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appNotice01Service.ackConfirm(AppNoticeAckParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 한시숨김(SNOOZED, 오늘+7) — 정규직·고정공지 한정
    @PostMapping(value = "/ack-snooze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ackSnooze(
            @RequestBody AppNoticeAckRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appNotice01Service.ackSnooze(AppNoticeAckParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 열람(LAST_READ_DATE 갱신, 뱃지 소멸)
    @PostMapping(value = "/read", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> read(
            @RequestBody AppNoticeAckRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appNotice01Service.read(AppNoticeAckParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 첨부 다운로드 단기 토큰 발급 (JWT scope=NOTICE_FILE_DL, 5분). 스트림은 웹 @NoAuth URL 재사용.
    @GetMapping("/file-download-token")
    public ResponseEntity<?> getFileDownloadToken(
            @ModelAttribute AppNoticeFileDlRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppNoticeFileDlResponse response = appNotice01Service.issueFileDownloadToken(
            AppNoticeFileDlParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
