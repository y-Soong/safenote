package com.prafta.web.notice.notice02.controller;

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
import com.prafta.common.error.archive.ArchiveErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.ClientIpExtractor;
import com.prafta.web.notice.notice02.application.param.ArchiveDeleteParam;
import com.prafta.web.notice.notice02.application.param.ArchiveFileDlParam;
import com.prafta.web.notice.notice02.application.param.ArchiveFileUploadParam;
import com.prafta.web.notice.notice02.application.param.ArchiveInfoParam;
import com.prafta.web.notice.notice02.application.param.ArchiveListParam;
import com.prafta.web.notice.notice02.application.param.ArchivePwdParam;
import com.prafta.web.notice.notice02.application.param.ArchiveSaveParam;
import com.prafta.web.notice.notice02.application.param.ArchiveTypeParam;
import com.prafta.web.notice.notice02.dto.request.ArchiveDeleteRequest;
import com.prafta.web.notice.notice02.dto.request.ArchiveFileDlRequest;
import com.prafta.web.notice.notice02.dto.request.ArchiveInfoRequest;
import com.prafta.web.notice.notice02.dto.request.ArchiveListRequest;
import com.prafta.web.notice.notice02.dto.request.ArchivePwdRequest;
import com.prafta.web.notice.notice02.dto.request.ArchiveSaveRequest;
import com.prafta.web.notice.notice02.dto.response.ArchiveDetailResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveFileDlResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveFileUploadResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveListResponse;
import com.prafta.web.notice.notice02.dto.response.ArchivePwdResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveSaveResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveTypeResponse;
import com.prafta.web.notice.notice02.result.ArchiveFileResult;
import com.prafta.web.notice.notice02.service.Archive02Service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 자료실(Archive) 컨트롤러 (PRAFTA-053).
 * 식별자(cmpnyCd/userCd)는 JWT 클레임에서만 도출하여 IDOR 을 차단한다.
 * axios 프리픽스: /webApi/notice02/...  (공지 /notice01 과 완전 분리)
 */
@Slf4j
@RestController
@RequestMapping("/notice02")
@RequiredArgsConstructor
public class Archive02Controller {

    private final Archive02Service archive02Service;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    /** 업로드 파일 루트(FILE_UPLOAD_BASE_DIR). notice01 / FileServiceImpl 과 동일 값. */
    @Value("${file.upload.base-dir}")
    private String uploadBaseDir;

    // 자료타입 드롭다운 (053-3)
    @GetMapping("/archive-types")
    public ResponseEntity<?> getArchiveTypes(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ArchiveTypeResponse response = archive02Service.selectArchiveTypes(
            ArchiveTypeParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 목록 (자료타입/등록월/키워드 검색)
    @GetMapping("/archive-lists")
    public ResponseEntity<?> getArchiveLists(
            @ModelAttribute ArchiveListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ArchiveListResponse response = archive02Service.selectArchiveList(
            ArchiveListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 단건 상세 (마스터 + 첨부 list)
    @GetMapping("/archive-info")
    public ResponseEntity<?> getArchiveInfo(
            @ModelAttribute ArchiveInfoRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ArchiveDetailResponse response = archive02Service.selectArchiveInfo(
            ArchiveInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 생성 (채번 'A'+YYYYMMDD+SEQ + BCrypt + 강제값 고정)
    @PostMapping(value = "/save-archive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveArchive(
            @RequestBody ArchiveSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ArchiveSaveResponse response = archive02Service.saveArchive(
            ArchiveSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 첨부 단건 업로드 (save-archive 선행). multipart → tb_file_info 선저장 후 FILE_MGMT_CD 반환.
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ArchiveFileUploadResponse response = archive02Service.uploadFile(
            ArchiveFileUploadParam.from(jwtUtil.getAllClaimsAsMap(authorization), file));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 수정 (editPwd 검증·master 면제 / 자료타입·제목·내용·첨부 재설정)
    @PostMapping(value = "/update-archive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateArchive(
            @RequestBody ArchiveSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        archive02Service.updateArchive(
            ArchiveSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 비밀번호 검증 (BCrypt match, master 통과)
    @PostMapping(value = "/verify-pwd", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyPwd(
            @RequestBody ArchivePwdRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ArchivePwdResponse response = archive02Service.verifyPwd(
            ArchivePwdParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 자료실 삭제 (논리삭제 DEL_YN='Y', editPwd 검증·master 면제)
    @PostMapping(value = "/delete-archive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteArchive(
            @RequestBody ArchiveDeleteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        archive02Service.deleteArchive(
            ArchiveDeleteParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 첨부 다운로드 단기 토큰 발급 (JWT scope=ARCHIVE_FILE_DL, 5분)
    @GetMapping("/file-download-token")
    public ResponseEntity<?> getFileDownloadToken(
            @ModelAttribute ArchiveFileDlRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ArchiveFileDlResponse response = archive02Service.issueFileDownloadToken(
            ArchiveFileDlParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 첨부 다운로드 (단기 토큰 검증 → 파일 스트림 + 다운로드 감사 로그).
     *
     * <p>토큰을 쿼리로 받으므로 일반 Authorization JWT 흐름과 분리({@code @NoAuth}).
     * 토큰 scope(ARCHIVE_FILE_DL)/만료/claim 을 직접 검증하고, 토큰에 바인딩된
     * cmpnyCd/noticeId/fileMgmtCd 로만 조회한다(웹 표준 다운로드).
     */
    @NoAuth
    @GetMapping("/file-download")
    public ResponseEntity<?> fileDownload(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest) throws IOException {

        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_403_001);
        }
        Claims claims = jwtUtil.parseToken(token);
        String scope = claims.get("gv_scope", String.class);
        if (!JwtScope.ARCHIVE_FILE_DL.equals(scope)) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_403_001);
        }
        String cmpnyCd = claims.get("gv_cmpnyCd", String.class);
        String userCd = claims.get("gv_userCd", String.class);
        String noticeId = claims.get("gv_noticeId", String.class);
        String fileMgmtCd = claims.get("gv_fileMgmtCd", String.class);
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(noticeId) || !StringUtils.hasText(fileMgmtCd)) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_403_001);
        }

        ArchiveFileResult file = archive02Service.resolveDownloadFile(cmpnyCd, noticeId, fileMgmtCd);

        // 물리 파일 경로 = uploadBaseDir + (FILE_PATH - '/uploads' 프리픽스) + '/{fileMgmtCd}{ext}'
        Path absoluteFile = resolvePhysicalPath(file);
        if (!Files.exists(absoluteFile) || !Files.isRegularFile(absoluteFile)) {
            log.warn("자료실 첨부 실파일 없음 - noticeId={}, fileMgmtCd={}, path={}",
                noticeId, fileMgmtCd, absoluteFile);
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_002);
        }

        // 다운로드 감사 로그(본업 영향 0, REQUIRES_NEW). ACTION_TYPE='01' 다운로드 재사용. resourceType=ARCHIVE_FILE.
        auditLogService.record(
            AuditLogCommand.builder()
                .cmpnyCd(cmpnyCd)
                .userCd(userCd)
                .actionType(AuditActionType.DOWNLOAD)
                .resourceType("ARCHIVE_FILE")
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
    private Path resolvePhysicalPath(ArchiveFileResult file) {
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
            throw new ApiException(ArchiveErrorCode.ARCHIVE_403_001);
        }
        return resolved;
    }
}
