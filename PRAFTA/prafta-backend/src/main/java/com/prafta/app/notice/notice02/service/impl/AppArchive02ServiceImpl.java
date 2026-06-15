package com.prafta.app.notice.notice02.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.notice.notice02.AppArchiveConstants;
import com.prafta.app.notice.notice02.application.command.AppArchiveFileSaveCommand;
import com.prafta.app.notice.notice02.application.command.AppArchiveSaveCommand;
import com.prafta.app.notice.notice02.application.param.AppArchiveFileDlParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveFileUploadParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveInfoParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveListParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveSaveParam;
import com.prafta.app.notice.notice02.application.param.AppArchiveTypeParam;
import com.prafta.app.notice.notice02.application.query.AppArchiveFileQuery;
import com.prafta.app.notice.notice02.application.query.AppArchiveIdSeqQuery;
import com.prafta.app.notice.notice02.application.query.AppArchiveInfoQuery;
import com.prafta.app.notice.notice02.application.query.AppArchiveListQuery;
import com.prafta.app.notice.notice02.application.query.AppArchiveTypeQuery;
import com.prafta.app.notice.notice02.dto.response.AppArchiveDetailResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveFileDlResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveFileUploadResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveListResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveSaveResponse;
import com.prafta.app.notice.notice02.dto.response.AppArchiveTypeResponse;
import com.prafta.app.notice.notice02.mapper.AppArchive02Mapper;
import com.prafta.app.notice.notice02.result.AppArchiveFileResult;
import com.prafta.app.notice.notice02.result.AppArchiveResult;
import com.prafta.app.notice.notice02.result.AppArchiveTypeResult;
import com.prafta.app.notice.notice02.service.AppArchive02Service;
import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.error.archive.ArchiveErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.PasswordHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 자료실(Archive) 서비스 구현 (prafta-app-025 J1-8).
 *
 * <p>식별자(cmpnyCd/userCd/authCd/siteCd)는 JWT 클레임에서만 도출(IDOR 차단).
 * 자료실=회사 전체 공통이므로 사업장/노드 스코프·대상 매칭 없음. 전 쿼리에 NOTICE_TYPE='ARCHIVE'
 * 스코프(mapper XML 박음)로 공지 데이터와 구조적 분리. 앱 완전분리(app-023): 웹 컨트롤러/서비스/매퍼를
 * 런타임 호출하지 않는다(자기 매퍼만).
 *
 * <p>★등록/첨부업로드 권한 게이트(서버 강제, 클라 버튼 노출에 의존 금지):
 * master ∥ hr ∥ safe ∥ 전사 노드관리자(노드 main/sub admin). 자료실=회사 전체 공통이므로
 * "노드관리자"는 특정 사업장이 아닌 전사(회사 전체) 기준으로 판정한다(access-context canEnterAdmin 과 동형).
 * 웹 Archive02ServiceImpl 과 동일 권한식(비대칭 제거).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppArchive02ServiceImpl implements AppArchive02Service {

    private static final int FILE_DL_TOKEN_TTL_MIN = 5;
    /** 발행자 소속 사업장이 없을 때(master/hr 등) 저장 경로용 폴백 세그먼트(웹 동형). */
    private static final String SITE_FALLBACK = "COMMON";

    private final AppArchive02Mapper appArchive02Mapper;
    private final PasswordHasher passwordHasher;
    private final JwtUtil jwtUtil;
    private final FileService fileService;
    private final FileMapper fileMapper;

    // ── 조회 ──────────────────────────────────────────────

    @Override
    public AppArchiveTypeResponse selectArchiveTypes(AppArchiveTypeParam param) {
        log.info("앱 자료타입 드롭다운 조회 진입 - cmpnyCd={}", param.gvCmpnyCd());

        // 코드그룹 상수 미주입이면 조회 없이 빈 목록 반환(의도된 게이트).
        if (!StringUtils.hasText(AppArchiveConstants.ARCHIVE_BAIM_VAL_CD)) {
            log.info("자료타입 코드그룹(BAIM_VAL_CD) 미주입 - 빈 드롭다운 반환");
            return AppArchiveTypeResponse.builder().typeList(List.of()).build();
        }

        List<AppArchiveTypeResult> typeList = appArchive02Mapper.selectArchiveTypeList(
            AppArchiveTypeQuery.from(param));

        return AppArchiveTypeResponse.builder().typeList(typeList).build();
    }

    @Override
    public AppArchiveListResponse selectArchiveList(AppArchiveListParam param) {
        log.info("앱 자료실 목록 조회 진입 - cmpnyCd={}, userCd={}", param.gvCmpnyCd(), param.gvUserCd());

        List<AppArchiveResult> archiveList = appArchive02Mapper.selectArchiveList(
            AppArchiveListQuery.from(param));

        return AppArchiveListResponse.builder()
            .archiveList(archiveList)
            .build();
    }

    @Override
    public AppArchiveDetailResponse selectArchiveInfo(AppArchiveInfoParam param) {
        log.info("앱 자료실 상세 조회 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AppArchiveInfoQuery query = AppArchiveInfoQuery.from(param);
        AppArchiveResult archiveInfo = appArchive02Mapper.selectArchiveOne(query);
        if (archiveInfo == null) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_001);
        }

        return AppArchiveDetailResponse.builder()
            .archiveInfo(archiveInfo)
            .fileList(appArchive02Mapper.selectArchiveFileList(query))
            .build();
    }

    // ── 첨부 업로드(등록 선행) ─────────────────────────────

    @Override
    @Transactional
    public AppArchiveFileUploadResponse uploadFile(AppArchiveFileUploadParam param) {
        log.info("앱 자료실 첨부 업로드 진입 - cmpnyCd={}, userCd={}", param.gvCmpnyCd(), param.gvUserCd());
        if (log.isDebugEnabled()) {
            log.debug("앱 자료실 첨부 원본명={}",
                param.file() == null ? null : param.file().getOriginalFilename());
        }

        // ★등록 권한 게이트(master∥hr∥safe∥전사노드관리자). 첨부 업로드도 동일 게이트.
        assertCanRegister(param.gvAuthCd(), param.gvCmpnyCd(), param.gvUserCd());

        // FILE_MGMT_CD 채번(SYS010 005=공지첨부 재사용). 회사+파일타입 단위 시퀀스.
        String fileMgmtCd = fileMapper.selectFileMgmtCd(
            FileInfoQuery.from(param.gvCmpnyCd(), AppArchiveConstants.FILE_TYPE_ARCHIVE));

        // 저장 경로 세그먼트용 사업장 — 발행자 소속 사업장이 없으면(전사 관리자 등) 폴백.
        String siteCd = StringUtils.hasText(param.gvSiteCd()) ? param.gvSiteCd() : SITE_FALLBACK;

        // 공통 FileService 재사용 — 확장자 화이트리스트 + path traversal 방어 + 디스크 저장 + tb_file_info INSERT
        fileService.fileSave(FileInfoParam.from(
            param.gvCmpnyCd()
            , param.gvUserCd()
            , siteCd
            , AppArchiveConstants.FILE_TYPE_ARCHIVE
            , fileMgmtCd
            , param.file()));

        String fileNm = param.file().getOriginalFilename();
        log.info("앱 자료실 첨부 업로드 완료 - fileMgmtCd={}", fileMgmtCd);
        if (log.isDebugEnabled()) {
            log.debug("앱 자료실 첨부 업로드 파일명={}", fileNm);
        }

        return AppArchiveFileUploadResponse.builder()
            .fileMgmtCd(fileMgmtCd)
            .fileNm(fileNm)
            .build();
    }

    // ── 등록 ──────────────────────────────────────────────

    @Override
    @Transactional
    public AppArchiveSaveResponse saveArchive(AppArchiveSaveParam param) {
        log.info("앱 자료실 등록 진입 - cmpnyCd={}, userCd={}, authCd={}",
            param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd());

        // ★등록 권한 게이트(master∥hr∥safe∥전사노드관리자). 서버 강제.
        assertCanRegister(param.gvAuthCd(), param.gvCmpnyCd(), param.gvUserCd());

        // 필수값 검증(생성: title/archiveTypeCd/editPwd 필수).
        validateSaveRequired(param);
        // 자료타입 유효성: 신규 생성은 USE_YN='Y' 만 허용(사용중지 코드 거부).
        validateArchiveType(param.gvCmpnyCd(), param.archiveTypeCd());

        // 채번: 'A' + YYYYMMDD + 3자리 SEQ (NOTICE_TYPE='ARCHIVE' 스코프).
        String noticeId = appArchive02Mapper.selectNextArchiveId(AppArchiveIdSeqQuery.from(param));

        // EDIT_PWD BCrypt(PasswordHasher 경유, BCryptPasswordEncoder 직접 호출 금지).
        String editPwdHash = passwordHasher.hash(param.editPwd());

        appArchive02Mapper.insertArchive(AppArchiveSaveCommand.from(param, noticeId, editPwdHash));

        // 첨부 다건 적재.
        if (param.fileList() != null && !param.fileList().isEmpty()) {
            appArchive02Mapper.insertArchiveFiles(AppArchiveFileSaveCommand.of(
                noticeId, param.fileList(), param.gvCmpnyCd(), param.gvUserCd()));
        }

        log.info("앱 자료실 등록 완료 - noticeId={}", noticeId);
        return AppArchiveSaveResponse.builder().noticeId(noticeId).build();
    }

    // ── 첨부 다운로드 토큰 ─────────────────────────────────

    @Override
    public AppArchiveFileDlResponse issueFileDownloadToken(AppArchiveFileDlParam param) {
        log.info("앱 자료실 다운로드 토큰 발급 진입 - cmpnyCd={}, noticeId={}, fileMgmtCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.fileMgmtCd());

        if (!StringUtils.hasText(param.noticeId()) || !StringUtils.hasText(param.fileMgmtCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AppArchiveFileQuery fileQuery = AppArchiveFileQuery.from(param);

        // 첨부 존재/스코프 확인(없으면 404).
        AppArchiveFileResult file = appArchive02Mapper.selectArchiveFileOne(fileQuery);
        if (file == null) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_002);
        }

        // 노출 검증(자료실=회사 전체 공통): 존재 + CMPNY + ARCHIVE + DEL_YN='N' 만 확인.
        if (appArchive02Mapper.countArchiveVisible(fileQuery) <= 0) {
            log.warn("앱 자료실 첨부 다운로드 스코프 외 차단 - cmpnyCd={}, noticeId={}, userCd={}",
                param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
            throw new ApiException(ArchiveErrorCode.ARCHIVE_403_001);
        }

        Map<String, Object> extra = new HashMap<>();
        extra.put("gv_noticeId", param.noticeId());
        extra.put("gv_fileMgmtCd", param.fileMgmtCd());
        // 웹 file-download(@NoAuth) 가 검증하는 scope=ARCHIVE_FILE_DL + cmpny/notice/file 바인딩 동형.
        String token = jwtUtil.generateScopeToken(
            param.gvCmpnyCd(), param.gvUserCd(), JwtScope.ARCHIVE_FILE_DL, FILE_DL_TOKEN_TTL_MIN, extra);

        return AppArchiveFileDlResponse.builder()
            .token(token)
            .expiresInSec(FILE_DL_TOKEN_TTL_MIN * 60L)
            .fileNm(file.fileNm())
            .build();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────

    /**
     * 등록(쓰기) 권한 게이트. 권한식 = master ∥ hr ∥ safe ∥ 전사 노드관리자.
     *
     * <p>접근차단(999999)이면 선차단. 역할(master/hr/safe)이면 통과, 아니면 전사 노드관리자
     * (TB_SITE_NODE MAIN/SUB_ADMIN_CD, CMPNY_CD 스코프) count&gt;0 이면 통과, 둘 다 아니면 403.
     * 자료실=회사 전체 공통이므로 사업장 멤버십 단독 게이트를 두지 않는다(J1-6 교훈 — 역할/전사노드 게이트만).
     * 웹 Archive02ServiceImpl.assertCanRegister 와 동일 권한식.
     */
    private void assertCanRegister(String authCd, String cmpnyCd, String userCd) {
        if (AuthRoleUtils.isAccessDenied(authCd)) {
            log.warn("앱 자료실 등록 권한 거부(접근차단) - authCd={}", authCd);
            throw new ApiException(ArchiveErrorCode.ARCHIVE_403_002);
        }
        if (AuthRoleUtils.canManageAllNodes(authCd)) {
            return;
        }
        if (appArchive02Mapper.countNodeAdminAnySite(cmpnyCd, userCd) > 0) {
            return;
        }
        log.warn("앱 자료실 등록 권한 거부 - authCd={}, userCd={}", authCd, userCd);
        throw new ApiException(ArchiveErrorCode.ARCHIVE_403_002);
    }

    /** 생성 필수값 검증(title/archiveTypeCd/editPwd 모두 필수, master 포함). */
    private void validateSaveRequired(AppArchiveSaveParam param) {
        if (!StringUtils.hasText(param.title())) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n필수값 누락 - TITLE");
        }
        if (!StringUtils.hasText(param.archiveTypeCd())) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n필수값 누락 - ARCHIVE_TYPE_CD");
        }
        if (!StringUtils.hasText(param.editPwd())) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n필수값 누락 - 비밀번호");
        }
    }

    /**
     * 자료타입(ARCHIVE_TYPE_CD) 유효성 검증(신규 생성).
     *  - 코드그룹 상수 미주입이면 어떤 코드도 유효할 수 없음 → 거부(저장 차단 게이트).
     *  - 해당 회사 baim_val_d 에 존재 + USE_YN='Y' 카운트 1 이상.
     */
    private void validateArchiveType(String cmpnyCd, String archiveTypeCd) {
        if (!StringUtils.hasText(AppArchiveConstants.ARCHIVE_BAIM_VAL_CD)) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_400_002);
        }
        int valid = appArchive02Mapper.countArchiveTypeValid(
            cmpnyCd, AppArchiveConstants.ARCHIVE_BAIM_VAL_CD, archiveTypeCd, true);
        if (valid <= 0) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_400_002);
        }
    }
}
