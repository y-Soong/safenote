package com.prafta.web.notice.notice01.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.audit.AuditActionType;
import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.audit.command.AuditLogCommand;
import com.prafta.common.cmm.audit.service.AuditLogService;
import com.prafta.common.error.notice.NoticeErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.ClientIpExtractor;
import com.prafta.web.notice.notice01.application.param.NoticeAckParam;
import com.prafta.web.notice.notice01.application.param.NoticeDeleteParam;
import com.prafta.web.notice.notice01.application.param.NoticeFileDlParam;
import com.prafta.web.notice.notice01.application.param.NoticeFileUploadParam;
import com.prafta.web.notice.notice01.application.param.NoticeInfoParam;
import com.prafta.web.notice.notice01.application.param.NoticeListParam;
import com.prafta.web.notice.notice01.application.param.NoticePopupParam;
import com.prafta.web.notice.notice01.application.param.NoticePwdParam;
import com.prafta.web.notice.notice01.application.param.NoticeSaveParam;
import com.prafta.web.notice.notice01.application.param.NoticeScopeParam;
import com.prafta.web.notice.notice01.dto.request.NoticeAckRequest;
import com.prafta.web.notice.notice01.dto.request.NoticeDeleteRequest;
import com.prafta.web.notice.notice01.dto.request.NoticeFileDlRequest;
import com.prafta.web.notice.notice01.dto.request.NoticeInfoRequest;
import com.prafta.web.notice.notice01.dto.request.NoticeListRequest;
import com.prafta.web.notice.notice01.dto.request.NoticePwdRequest;
import com.prafta.web.notice.notice01.dto.request.NoticeSaveRequest;
import com.prafta.web.notice.notice01.dto.response.NoticeDetailResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeFileDlResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeFileUploadResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeListResponse;
import com.prafta.web.notice.notice01.dto.response.NoticePopupResponse;
import com.prafta.web.notice.notice01.dto.response.NoticePwdResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeSaveResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeScopeResponse;
import com.prafta.web.notice.notice01.result.NoticeFileResult;
import com.prafta.web.notice.notice01.service.Notice01Service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공지사항(Notice) 컨트롤러 (PRAFTA-047).
 * 식별자(cmpnyCd/userCd)는 JWT 클레임에서만 도출하여 IDOR 을 차단한다.
 * axios 프리픽스: /webApi/notice01/...
 */
@Slf4j
@RestController
@RequestMapping("/notice01")
@RequiredArgsConstructor
public class Notice01Controller {

    private final Notice01Service notice01Service;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    /** 업로드 파일 루트(FILE_UPLOAD_BASE_DIR). FileServiceImpl / ApiPrefixConfig 와 동일 값. */
    @Value("${file.upload.base-dir}")
    private String uploadBaseDir;

    // ── 047-3 관리 ────────────────────────────────────────────

    // 공지 관리 목록 (본인 뱃지/대상요약 포함, 정렬 §5)
    @GetMapping("/notice-lists")
    public ResponseEntity<?> getNoticeLists(
            @ModelAttribute NoticeListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NoticeListResponse response = notice01Service.selectNoticeList(
            NoticeListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 공지 단건 상세 (마스터 + 첨부 list + 대상 list)
    @GetMapping("/notice-info")
    public ResponseEntity<?> getNoticeInfo(
            @ModelAttribute NoticeInfoRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NoticeDetailResponse response = notice01Service.selectNoticeInfo(
            NoticeInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 공지 생성 (채번 + BCrypt + PIN 정규화 + 발행자 스코프 재검증)
    @PostMapping(value = "/save-notice", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveNotice(
            @RequestBody NoticeSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NoticeSaveResponse response = notice01Service.saveNotice(
            NoticeSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 공지 첨부 단건 업로드 (save-notice 선행). multipart 파일 → tb_file_info 선저장 후 FILE_MGMT_CD 반환.
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NoticeFileUploadResponse response = notice01Service.uploadFile(
            NoticeFileUploadParam.from(jwtUtil.getAllClaimsAsMap(authorization), file));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 공지 수정 (editPwd 검증·master 면제 / §8 기간중 수정 차단 / 대상 재설정+재검증)
    @PostMapping(value = "/update-notice", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateNotice(
            @RequestBody NoticeSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        notice01Service.updateNotice(
            NoticeSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 비밀번호 검증 (BCrypt match, master 통과)
    @PostMapping(value = "/verify-pwd", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyPwd(
            @RequestBody NoticePwdRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NoticePwdResponse response = notice01Service.verifyPwd(
            NoticePwdParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 공지 삭제 (논리삭제 + PIN 재압축)
    @PostMapping(value = "/delete-notice", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteNotice(
            @RequestBody NoticeDeleteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        notice01Service.deleteNotice(
            NoticeDeleteParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 발행자 대상선택 트리 (발행자 스코프 제한)
    @GetMapping("/scope-tree")
    public ResponseEntity<?> getScopeTree(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NoticeScopeResponse response = notice01Service.selectScopeTree(
            NoticeScopeParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ── 047-4 노출/ACK/다운로드 ───────────────────────────────

    // 로그인 직후 팝업 노출 판정 (web/mobile 공통)
    @PostMapping("/popup")
    public ResponseEntity<?> getPopupNotices(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NoticePopupResponse response = notice01Service.selectPopupNotices(
            NoticePopupParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 확인 처리(CONFIRMED)
    @PostMapping(value = "/ack-confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ackConfirm(
            @RequestBody NoticeAckRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        notice01Service.ackConfirm(
            NoticeAckParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 한시숨김(SNOOZED, 오늘+7) — 정규직·고정공지 한정
    @PostMapping(value = "/ack-snooze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ackSnooze(
            @RequestBody NoticeAckRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        notice01Service.ackSnooze(
            NoticeAckParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 열람(LAST_READ_DATE 갱신, 뱃지 소멸)
    @PostMapping(value = "/read", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> read(
            @RequestBody NoticeAckRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        notice01Service.read(
            NoticeAckParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 첨부 다운로드 단기 토큰 발급 (JWT scope=NOTICE_FILE_DL, 5분)
    @GetMapping("/file-download-token")
    public ResponseEntity<?> getFileDownloadToken(
            @ModelAttribute NoticeFileDlRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        NoticeFileDlResponse response = notice01Service.issueFileDownloadToken(
            NoticeFileDlParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 첨부 다운로드 (단기 토큰 검증 → 파일 스트림 + 다운로드 감사 로그).
     *
     * <p>토큰을 쿼리로 받으므로 일반 Authorization JWT 흐름과 분리({@code @NoAuth}).
     * 토큰 scope/만료/claim 을 직접 검증하고, 토큰에 바인딩된 cmpnyCd/noticeId/fileMgmtCd 로만 조회한다.
     * 앱은 이 URL/토큰만 받아 네이티브가 처리(별도 앱 티켓 — 본 EP 는 웹 직접 다운로드).
     */
    @NoAuth
    @GetMapping("/file-download")
    public ResponseEntity<?> fileDownload(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest) throws IOException {

        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            throw new ApiException(NoticeErrorCode.NOTICE_403_002);
        }
        Claims claims = jwtUtil.parseToken(token);
        String scope = claims.get("gv_scope", String.class);
        if (!JwtScope.NOTICE_FILE_DL.equals(scope)) {
            throw new ApiException(NoticeErrorCode.NOTICE_403_002);
        }
        String cmpnyCd = claims.get("gv_cmpnyCd", String.class);
        String userCd = claims.get("gv_userCd", String.class);
        String noticeId = claims.get("gv_noticeId", String.class);
        String fileMgmtCd = claims.get("gv_fileMgmtCd", String.class);
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(noticeId) || !StringUtils.hasText(fileMgmtCd)) {
            throw new ApiException(NoticeErrorCode.NOTICE_403_002);
        }

        NoticeFileResult file = notice01Service.resolveDownloadFile(cmpnyCd, noticeId, fileMgmtCd);

        // 물리 파일 경로 = uploadBaseDir + (FILE_PATH - '/uploads' 프리픽스) + '/{fileMgmtCd}{ext}'
        Path absoluteFile = resolvePhysicalPath(file);
        if (!Files.exists(absoluteFile) || !Files.isRegularFile(absoluteFile)) {
            log.warn("공지 첨부 실파일 없음 - noticeId={}, fileMgmtCd={}, path={}",
                noticeId, fileMgmtCd, absoluteFile);
            throw new ApiException(NoticeErrorCode.NOTICE_404_002);
        }

        // 다운로드 감사 로그(본업 영향 0, REQUIRES_NEW). ACTION_TYPE='01' 다운로드 재사용.
        auditLogService.record(
            AuditLogCommand.builder()
                .cmpnyCd(cmpnyCd)
                .userCd(userCd)
                .actionType(AuditActionType.DOWNLOAD)
                .resourceType("NOTICE_FILE")
                .resourceKey(noticeId + "/" + fileMgmtCd)
                .build(),
            new AuditContext(
                ClientIpExtractor.extract(httpRequest),
                httpRequest.getHeader("User-Agent")));

        String downloadName = StringUtils.hasText(file.fileNm())
            ? file.fileNm()
            : (fileMgmtCd + (StringUtils.hasText(file.fileExt()) ? file.fileExt() : ""));
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");

        InputStream in = Files.newInputStream(absoluteFile);
        long contentLength = Files.size(absoluteFile);

        return ResponseEntity.status(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new InputStreamResource(in));
    }

    /** FILE_PATH(공개 상대경로 '/uploads/...') + fileMgmtCd + ext → 물리 절대경로. path traversal 방어. */
    private Path resolvePhysicalPath(NoticeFileResult file) {
        String relPath = file.filePath() == null ? "" : file.filePath();
        // 공개 마운트 프리픽스 '/uploads' 제거 → 디스크 루트(uploadBaseDir) 기준 상대경로
        String stripped = relPath.replace("\\", "/");
        if (stripped.startsWith("/uploads")) {
            stripped = stripped.substring("/uploads".length());
        }
        String ext = StringUtils.hasText(file.fileExt()) ? file.fileExt() : "";
        String saveFileName = file.fileMgmtCd() + ext;

        Path base = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path resolved = base.resolve("." + stripped).resolve(saveFileName).normalize();
        // path traversal 가드: resolved 가 base 하위가 아니면 거부
        if (!resolved.startsWith(base)) {
            throw new ApiException(NoticeErrorCode.NOTICE_403_002);
        }
        return resolved;
    }
}
