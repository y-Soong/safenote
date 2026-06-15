package com.prafta.app.notice.notice02.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.notice.notice02.application.param.AppArchiveFileDlParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveFileUploadParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveInfoParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveListParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveSaveParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveTypeParam;
import com.prafta.app.notice.notice02.dto.request.AppArchiveFileDlRequest;
import com.prafta.app.notice.notice02.dto.request.AppArchiveInfoRequest;
import com.prafta.app.notice.notice02.dto.request.AppArchiveListRequest;
import com.prafta.app.notice.notice02.dto.request.AppArchiveSaveRequest;
import com.prafta.app.notice.notice02.dto.response.AppArchiveDetailResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveFileDlResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveFileUploadResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveListResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveSaveResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveTypeResponse;
import com.prafta.app.notice.notice02.service.AppArchive02Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 관리자 자료실(Archive) 컨트롤러 (prafta-app-025 J1-8). 조회 + 등록(작성+첨부) 전용.
 *
 * <p>실제 매핑 경로(자동 프리픽스 com.prafta.app.* → /prafta/appApi):
 *   GET  /prafta/appApi/notice02/archive-types
 *   GET  /prafta/appApi/notice02/archive-lists
 *   GET  /prafta/appApi/notice02/archive-info
 *   POST /prafta/appApi/notice02/upload-file (multipart)
 *   POST /prafta/appApi/notice02/save-archive (json)
 *   GET  /prafta/appApi/notice02/file-download-token
 * <p>식별자(cmpnyCd/userCd/authCd/siteCd)는 JWT 클레임에서만 도출(IDOR 차단). 바디/쿼리로 받지 않는다.
 * <p>앱 완전분리(app-023): 웹 컨트롤러(/webApi/notice02/*) 를 호출하지 않는다.
 *    첨부 실제 스트림은 웹 @NoAuth /webApi/notice02/file-download?token= 재사용(앱 스트림 EP 미신설).
 * <p>★등록(save/upload)은 서버에서 역할 게이트(master/hr/safe) 직접 강제(A-1, ServiceImpl).
 */
@Slf4j
@RestController
@RequestMapping("/notice02")
@RequiredArgsConstructor
public class AppArchive02Controller {

    private final AppArchive02Service appArchive02Service;
    private final JwtUtil jwtUtil;

    // 자료타입 드롭다운(COM008, USE_YN='Y')
    @GetMapping("/archive-types")
    public ResponseEntity<?> getArchiveTypes(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppArchiveTypeResponse response = appArchive02Service.selectArchiveTypes(
            AppArchiveTypeParam.from(tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 목록(자료타입/등록월/키워드 검색, 최신순)
    @GetMapping("/archive-lists")
    public ResponseEntity<?> getArchiveLists(
            @ModelAttribute AppArchiveListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppArchiveListResponse response = appArchive02Service.selectArchiveList(
            AppArchiveListParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 단건 상세(마스터 + 첨부 list)
    @GetMapping("/archive-info")
    public ResponseEntity<?> getArchiveInfo(
            @ModelAttribute AppArchiveInfoRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppArchiveDetailResponse response = appArchive02Service.selectArchiveInfo(
            AppArchiveInfoParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 첨부 단건 선업로드(★역할 게이트). multipart → tb_file_info 선저장 후 FILE_MGMT_CD 반환.
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppArchiveFileUploadResponse response = appArchive02Service.uploadFile(
            AppArchiveFileUploadParam.from(tokenInfo, file));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 등록(★역할 게이트 + 채번 'A'+YYYYMMDD+SEQ + BCrypt + 강제값 고정)
    @PostMapping(value = "/save-archive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveArchive(
            @RequestBody AppArchiveSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppArchiveSaveResponse response = appArchive02Service.saveArchive(
            AppArchiveSaveParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 첨부 다운로드 단기 토큰 발급(JWT scope=ARCHIVE_FILE_DL, 5분). 스트림은 웹 @NoAuth URL 재사용.
    @GetMapping("/file-download-token")
    public ResponseEntity<?> getFileDownloadToken(
            @ModelAttribute AppArchiveFileDlRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AppArchiveFileDlResponse response = appArchive02Service.issueFileDownloadToken(
            AppArchiveFileDlParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
