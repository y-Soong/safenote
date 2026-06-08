package com.prafta.web.notice.notice02.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
import com.prafta.web.notice.notice02.ArchiveConstants;
import com.prafta.web.notice.notice02.application.command.ArchiveDeleteCommand;
import com.prafta.web.notice.notice02.application.command.ArchiveFileSaveCommand;
import com.prafta.web.notice.notice02.application.command.ArchiveSaveCommand;
import com.prafta.web.notice.notice02.application.command.ArchiveUpdateCommand;
import com.prafta.web.notice.notice02.application.param.ArchiveDeleteParam;
import com.prafta.web.notice.notice02.application.param.ArchiveFileDlParam;
import com.prafta.web.notice.notice02.application.param.ArchiveFileUploadParam;
import com.prafta.web.notice.notice02.application.param.ArchiveInfoParam;
import com.prafta.web.notice.notice02.application.param.ArchiveListParam;
import com.prafta.web.notice.notice02.application.param.ArchivePwdParam;
import com.prafta.web.notice.notice02.application.param.ArchiveSaveParam;
import com.prafta.web.notice.notice02.application.param.ArchiveTypeParam;
import com.prafta.web.notice.notice02.application.query.ArchiveFileQuery;
import com.prafta.web.notice.notice02.application.query.ArchiveIdSeqQuery;
import com.prafta.web.notice.notice02.application.query.ArchiveInfoQuery;
import com.prafta.web.notice.notice02.application.query.ArchiveListQuery;
import com.prafta.web.notice.notice02.application.query.ArchiveTypeQuery;
import com.prafta.web.notice.notice02.dto.response.ArchiveDetailResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveFileDlResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveFileUploadResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveListResponse;
import com.prafta.web.notice.notice02.dto.response.ArchivePwdResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveSaveResponse;
import com.prafta.web.notice.notice02.dto.response.ArchiveTypeResponse;
import com.prafta.web.notice.notice02.mapper.Archive02Mapper;
import com.prafta.web.notice.notice02.result.ArchiveFileResult;
import com.prafta.web.notice.notice02.result.ArchiveResult;
import com.prafta.web.notice.notice02.result.ArchiveTypeResult;
import com.prafta.web.notice.notice02.service.Archive02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 자료실(Archive) 서비스 구현 (PRAFTA-053).
 *
 * <p>식별자(cmpnyCd/userCd)는 JWT 클레임에서만 도출(IDOR 차단). master 판정은 서버 Role
 * (AuthRoleUtils, 클라 플래그 불신). 자료실=회사 전체 공통이므로 사업장/노드 스코프·대상 매칭 없음.
 * 전 쿼리에 NOTICE_TYPE='ARCHIVE' 스코프(mapper XML 박음)로 공지 데이터와 구조적 분리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Archive02ServiceImpl implements Archive02Service {

    private static final int FILE_DL_TOKEN_TTL_MIN = 5;
    /** 발행자 소속 사업장이 없을 때(master/hr 등) 저장 경로용 폴백 세그먼트. */
    private static final String SITE_FALLBACK = "COMMON";

    private final Archive02Mapper archive02Mapper;
    private final PasswordHasher passwordHasher;
    private final JwtUtil jwtUtil;
    private final FileService fileService;
    private final FileMapper fileMapper;

    // ── 053-3 자료타입 드롭다운 ───────────────────────────────

    @Override
    public ArchiveTypeResponse selectArchiveTypes(ArchiveTypeParam param) {
        log.info("자료타입 드롭다운 조회 진입 - cmpnyCd={}", param.gvCmpnyCd());

        // 코드그룹 상수 미주입이면 조회 없이 빈 목록 반환(의도된 게이트, plan §0/R1)
        if (!StringUtils.hasText(ArchiveConstants.ARCHIVE_BAIM_VAL_CD)) {
            log.info("자료타입 코드그룹(BAIM_VAL_CD) 미주입 - 빈 드롭다운 반환");
            return ArchiveTypeResponse.builder().typeList(List.of()).build();
        }

        List<ArchiveTypeResult> typeList = archive02Mapper.selectArchiveTypeList(
            ArchiveTypeQuery.from(param, ArchiveConstants.ARCHIVE_BAIM_VAL_CD));

        return ArchiveTypeResponse.builder().typeList(typeList).build();
    }

    // ── 053-4 CRUD ────────────────────────────────────────────

    @Override
    public ArchiveListResponse selectArchiveList(ArchiveListParam param) {
        log.info("자료실 목록 조회 진입 - cmpnyCd={}, userCd={}", param.gvCmpnyCd(), param.gvUserCd());

        List<ArchiveResult> archiveList = archive02Mapper.selectArchiveList(ArchiveListQuery.from(param));

        return ArchiveListResponse.builder()
            .archiveList(archiveList)
            .build();
    }

    @Override
    public ArchiveDetailResponse selectArchiveInfo(ArchiveInfoParam param) {
        log.info("자료실 상세 조회 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        ArchiveInfoQuery query = ArchiveInfoQuery.from(param);
        ArchiveResult archiveInfo = archive02Mapper.selectArchiveOne(query);
        if (archiveInfo == null) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_001);
        }

        return ArchiveDetailResponse.builder()
            .archiveInfo(archiveInfo)
            .fileList(archive02Mapper.selectArchiveFileList(query))
            .build();
    }

    @Override
    @Transactional
    public ArchiveSaveResponse saveArchive(ArchiveSaveParam param) {
        log.info("자료실 생성 진입 - cmpnyCd={}, userCd={}", param.gvCmpnyCd(), param.gvUserCd());

        validateSaveRequired(param, true);
        // 자료타입 유효성: 신규 생성은 USE_YN='Y' 만 허용(사용중지 코드 거부)
        validateArchiveType(param.gvCmpnyCd(), param.archiveTypeCd(), true);

        // 채번: 'A' + YYYYMMDD + 3자리 SEQ (NOTICE_TYPE='ARCHIVE' 스코프)
        String noticeId = archive02Mapper.selectNextArchiveId(ArchiveIdSeqQuery.from(param));

        // EDIT_PWD BCrypt(PasswordHasher 경유, BCryptPasswordEncoder 직접 호출 금지)
        String editPwdHash = passwordHasher.hash(param.editPwd());

        archive02Mapper.insertArchive(ArchiveSaveCommand.from(param, noticeId, editPwdHash));

        // 첨부 다건 적재
        insertFiles(param, noticeId);

        log.info("자료실 생성 완료 - noticeId={}", noticeId);
        return ArchiveSaveResponse.builder().noticeId(noticeId).build();
    }

    @Override
    @Transactional
    public void updateArchive(ArchiveSaveParam param) {
        log.info("자료실 수정 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        validateSaveRequired(param, false);

        ArchiveInfoQuery infoQuery = ArchiveInfoQuery.of(param.noticeId(), param.gvCmpnyCd());
        ArchiveResult current = archive02Mapper.selectArchiveForUpdate(infoQuery);
        if (current == null) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_001);
        }

        // 비밀번호 검증(master 면제, 서버 Role 판별)
        if (!isMaster(param.gvAuthCd())) {
            String hash = archive02Mapper.selectArchivePwdHash(infoQuery);
            if (hash == null || !passwordHasher.matches(param.editPwd(), hash)) {
                throw new ApiException(ArchiveErrorCode.ARCHIVE_400_001);
            }
        }

        // 자료타입 유효성: 수정은 사용중지(USE_YN='N') 코드도 허용(기존 저장값 정합성). 단 코드 존재는 검증.
        validateArchiveType(param.gvCmpnyCd(), param.archiveTypeCd(), false);

        int updated = archive02Mapper.updateArchive(ArchiveUpdateCommand.from(param));
        if (updated == 0) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_001);
        }

        // 첨부 전체 재설정(삭제 후 재삽입). tb_file_info 는 유지(물리삭제 안 함).
        archive02Mapper.deleteArchiveFileByNoticeId(infoQuery);
        insertFiles(param, param.noticeId());

        log.info("자료실 수정 완료 - noticeId={}", param.noticeId());
    }

    @Override
    public ArchivePwdResponse verifyPwd(ArchivePwdParam param) {
        log.info("자료실 비번 검증 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // master 면제(서버 Role) — 비번 무관 통과
        if (isMaster(param.gvAuthCd())) {
            return ArchivePwdResponse.builder().verified(true).build();
        }

        ArchiveInfoQuery query = ArchiveInfoQuery.of(param.noticeId(), param.gvCmpnyCd());
        String hash = archive02Mapper.selectArchivePwdHash(query);
        if (hash == null) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_001);
        }
        boolean verified = passwordHasher.matches(param.editPwd(), hash);
        if (!verified) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_400_001);
        }
        return ArchivePwdResponse.builder().verified(true).build();
    }

    @Override
    @Transactional
    public void deleteArchive(ArchiveDeleteParam param) {
        log.info("자료실 삭제 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        ArchiveInfoQuery query = ArchiveInfoQuery.of(param.noticeId(), param.gvCmpnyCd());

        // 비밀번호 검증(master 면제, 서버 Role)
        if (!isMaster(param.gvAuthCd())) {
            String hash = archive02Mapper.selectArchivePwdHash(query);
            if (hash == null) {
                throw new ApiException(ArchiveErrorCode.ARCHIVE_404_001);
            }
            if (!passwordHasher.matches(param.editPwd(), hash)) {
                throw new ApiException(ArchiveErrorCode.ARCHIVE_400_001);
            }
        }

        int deleted = archive02Mapper.updateArchiveDelYn(ArchiveDeleteCommand.from(param));
        if (deleted == 0) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_001);
        }

        log.info("자료실 삭제 완료 - noticeId={}", param.noticeId());
    }

    @Override
    @Transactional
    public ArchiveFileUploadResponse uploadFile(ArchiveFileUploadParam param) {
        log.info("자료실 첨부 업로드 진입 - cmpnyCd={}, userCd={}, 원본명={}",
            param.gvCmpnyCd(), param.gvUserCd(),
            param.file() == null ? null : param.file().getOriginalFilename());

        // FILE_MGMT_CD 채번(SYS010 005=공지첨부 재사용). 회사+파일타입 단위 시퀀스.
        String fileMgmtCd = fileMapper.selectFileMgmtCd(
            FileInfoQuery.from(param.gvCmpnyCd(), ArchiveConstants.FILE_TYPE_ARCHIVE));

        // 저장 경로 세그먼트용 사업장 — 발행자 소속 사업장이 없으면(전사 관리자 등) 폴백.
        String siteCd = StringUtils.hasText(param.gvSiteCd()) ? param.gvSiteCd() : SITE_FALLBACK;

        // 공통 FileService 재사용 — 확장자 화이트리스트 + path traversal 방어 + 디스크 저장 + tb_file_info INSERT
        fileService.fileSave(FileInfoParam.from(
            param.gvCmpnyCd()
            , param.gvUserCd()
            , siteCd
            , ArchiveConstants.FILE_TYPE_ARCHIVE
            , fileMgmtCd
            , param.file()));

        String fileNm = param.file().getOriginalFilename();
        log.info("자료실 첨부 업로드 완료 - fileMgmtCd={}, 파일명={}", fileMgmtCd, fileNm);

        return ArchiveFileUploadResponse.builder()
            .fileMgmtCd(fileMgmtCd)
            .fileNm(fileNm)
            .build();
    }

    @Override
    public ArchiveFileDlResponse issueFileDownloadToken(ArchiveFileDlParam param) {
        log.info("자료실 다운로드 토큰 발급 진입 - cmpnyCd={}, noticeId={}, fileMgmtCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.fileMgmtCd());

        if (!StringUtils.hasText(param.noticeId()) || !StringUtils.hasText(param.fileMgmtCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        ArchiveFileQuery fileQuery = ArchiveFileQuery.from(param);

        // 첨부 존재/스코프 확인(없으면 404). 토큰에 cmpnyCd/noticeId/fileMgmtCd 바인딩.
        ArchiveFileResult file = archive02Mapper.selectArchiveFileOne(fileQuery);
        if (file == null) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_002);
        }

        // 노출 검증(자료실=회사 전체 공통): 존재 + CMPNY + ARCHIVE + DEL_YN='N' 만 확인(대상 재귀 불필요).
        if (archive02Mapper.countArchiveVisible(fileQuery) <= 0) {
            log.warn("자료실 첨부 다운로드 스코프 외 차단 - cmpnyCd={}, noticeId={}, userCd={}",
                param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
            throw new ApiException(ArchiveErrorCode.ARCHIVE_403_001);
        }

        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("gv_noticeId", param.noticeId());
        extra.put("gv_fileMgmtCd", param.fileMgmtCd());
        String token = jwtUtil.generateScopeToken(
            param.gvCmpnyCd(), param.gvUserCd(), JwtScope.ARCHIVE_FILE_DL, FILE_DL_TOKEN_TTL_MIN, extra);

        return ArchiveFileDlResponse.builder()
            .token(token)
            .expiresInSec(FILE_DL_TOKEN_TTL_MIN * 60L)
            .fileNm(file.fileNm())
            .build();
    }

    @Override
    public ArchiveFileResult resolveDownloadFile(String cmpnyCd, String noticeId, String fileMgmtCd) {
        ArchiveFileResult file =
            archive02Mapper.selectArchiveFileOne(ArchiveFileQuery.of(cmpnyCd, noticeId, fileMgmtCd));
        if (file == null) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_404_002);
        }
        return file;
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

    private boolean isMaster(String authCd) {
        return AuthRoleUtils.AUTH_MASTER.equals(authCd);
    }

    /** 생성/수정 공통 필수값 검증. isCreate=true 면 editPwd 도 필수. */
    private void validateSaveRequired(ArchiveSaveParam param, boolean isCreate) {
        if (!StringUtils.hasText(param.title())) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n필수값 누락 - TITLE");
        }
        if (!StringUtils.hasText(param.archiveTypeCd())) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n필수값 누락 - ARCHIVE_TYPE_CD");
        }
        if (isCreate && !StringUtils.hasText(param.editPwd())) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n필수값 누락 - 비밀번호");
        }
    }

    /**
     * 자료타입(ARCHIVE_TYPE_CD) 유효성 검증.
     *  - 코드그룹 상수 미주입이면 어떤 코드도 유효할 수 없음 → 거부(신규 저장 차단 게이트, R1).
     *  - forCreate=true(신규): 해당 회사 baim_val_d 에 존재 + USE_YN='Y' 카운트 1 이상.
     *  - forCreate=false(수정): 존재만 확인(USE_YN='N' 허용, 기존 저장값 정합성).
     */
    private void validateArchiveType(String cmpnyCd, String archiveTypeCd, boolean forCreate) {
        if (!StringUtils.hasText(ArchiveConstants.ARCHIVE_BAIM_VAL_CD)) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_400_002);
        }
        int valid = archive02Mapper.countArchiveTypeValid(
            cmpnyCd, ArchiveConstants.ARCHIVE_BAIM_VAL_CD, archiveTypeCd, forCreate);
        if (valid <= 0) {
            throw new ApiException(ArchiveErrorCode.ARCHIVE_400_002);
        }
    }

    private void insertFiles(ArchiveSaveParam param, String noticeId) {
        if (param.fileList() != null && !param.fileList().isEmpty()) {
            archive02Mapper.insertArchiveFiles(ArchiveFileSaveCommand.of(
                noticeId, param.fileList(), param.gvCmpnyCd(), param.gvUserCd()));
        }
    }
}
