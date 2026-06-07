package com.prafta.web.notice.notice01.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.notice.NoticeErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.PasswordHasher;
import com.prafta.web.notice.notice01.application.command.NoticeAckCommand;
import com.prafta.web.notice.notice01.application.command.NoticeDeleteCommand;
import com.prafta.web.notice.notice01.application.command.NoticeFileSaveCommand;
import com.prafta.web.notice.notice01.application.command.NoticeSaveCommand;
import com.prafta.web.notice.notice01.application.command.NoticeTargetSaveCommand;
import com.prafta.web.notice.notice01.application.command.NoticeUpdateCommand;
import com.prafta.web.notice.notice01.application.model.NoticeTargetModel;
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
import com.prafta.web.notice.notice01.application.query.NoticeFileQuery;
import com.prafta.web.notice.notice01.application.query.NoticeIdSeqQuery;
import com.prafta.web.notice.notice01.application.query.NoticeInfoQuery;
import com.prafta.web.notice.notice01.application.query.NoticeListQuery;
import com.prafta.web.notice.notice01.application.query.NoticePopupQuery;
import com.prafta.web.notice.notice01.application.query.NoticeScopeQuery;
import com.prafta.web.notice.notice01.dto.response.NoticeDetailResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeFileDlResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeFileUploadResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeListResponse;
import com.prafta.web.notice.notice01.dto.response.NoticePopupResponse;
import com.prafta.web.notice.notice01.dto.response.NoticePwdResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeSaveResponse;
import com.prafta.web.notice.notice01.dto.response.NoticeScopeResponse;
import com.prafta.web.notice.notice01.mapper.Notice01Mapper;
import com.prafta.web.notice.notice01.service.Notice01Service;
import com.prafta.web.notice.notice01.result.NoticeFileResult;
import com.prafta.web.notice.notice01.result.NoticePopupItem;
import com.prafta.web.notice.notice01.result.NoticePopupResult;
import com.prafta.web.notice.notice01.result.NoticeResult;
import com.prafta.web.notice.notice01.result.NoticeScopeResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공지사항(Notice) 서비스 구현 (PRAFTA-047-3/4).
 *
 * <p>식별자(cmpnyCd/userCd)는 JWT 클레임에서만 도출(IDOR 차단).
 * 발행자 대상 스코프는 기존 권한 컴포넌트(AuthRoleUtils.isCompanyWide / tb_user_site_auth /
 * 노드 자손 재귀 CTE)를 재사용해 서버 재검증한다(요청서 §2-2, 신규 권한 규칙 만들지 않음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Notice01ServiceImpl implements Notice01Service {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int SNOOZE_DAYS = 7;
    private static final int FILE_DL_TOKEN_TTL_MIN = 5;
    private static final String SCOPE_ALL  = "ALL";
    private static final String SCOPE_SITE = "SITE";
    private static final String SCOPE_NODE = "NODE";
    /** 공지 첨부 파일타입(SYS010 005=공지첨부). FILE_MGMT_CD 채번/저장 경로 분기 키. */
    private static final String FILE_TYPE_NOTICE = "005";
    /** 발행자 소속 사업장이 없을 때(master/hr 등) 저장 경로용 폴백 세그먼트. */
    private static final String SITE_FALLBACK = "COMMON";

    private final Notice01Mapper notice01Mapper;
    private final PasswordHasher passwordHasher;
    private final JwtUtil jwtUtil;
    private final FileService fileService;
    private final FileMapper fileMapper;

    // ── 047-3 관리 ────────────────────────────────────────────

    @Override
    public NoticeListResponse selectNoticeList(NoticeListParam param) {
        log.info("공지 목록 조회 진입 - cmpnyCd={}, userCd={}", param.gvCmpnyCd(), param.gvUserCd());

        List<NoticeResult> noticeList = notice01Mapper.selectNoticeList(NoticeListQuery.from(param));

        return NoticeListResponse.builder()
            .noticeList(noticeList)
            .build();
    }

    @Override
    @Transactional
    public NoticeDetailResponse selectNoticeInfo(NoticeInfoParam param) {
        log.info("공지 상세 조회 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        NoticeInfoQuery query = NoticeInfoQuery.from(param);
        NoticeResult noticeInfo = notice01Mapper.selectNoticeOne(query);
        if (noticeInfo == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_001);
        }

        // 상세 열람 시 LAST_READ_DATE 갱신(뱃지 소멸, §7). 조회 응답값은 갱신 전 스냅샷 사용.
        notice01Mapper.upsertNoticeRead(NoticeAckCommand.read(
            new NoticeAckParam(param.noticeId(), param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd())));

        return NoticeDetailResponse.builder()
            .noticeInfo(noticeInfo)
            .fileList(notice01Mapper.selectNoticeFileList(query))
            .targetList(notice01Mapper.selectNoticeTargetList(query))
            .build();
    }

    @Override
    @Transactional
    public NoticeSaveResponse saveNotice(NoticeSaveParam param) {
        log.info("공지 생성 진입 - cmpnyCd={}, userCd={}, scope={}",
            param.gvCmpnyCd(), param.gvUserCd(), param.targetScope());

        validateSaveRequired(param, true);
        // 발행자 대상 스코프 서버 재검증(§2-2)
        validateTargetScope(param);

        // 채번: N + YYYYMMDD + 3자리 SEQ
        String noticeId = notice01Mapper.selectNextNoticeId(NoticeIdSeqQuery.from(param));

        // EDIT_PWD BCrypt(PasswordHasher 경유, BCryptPasswordEncoder 직접 호출 금지)
        String editPwdHash = passwordHasher.hash(param.editPwd());

        // BUG-3: 회사 단위 PIN 직렬화 잠금(고정 0건일 때 첫 고정 동시삽입 중복 방지).
        notice01Mapper.lockPinSerialization(param.gvCmpnyCd());

        // PIN 정규화(§5): 단일 트랜잭션 + CMPNY_CD FOR UPDATE
        Integer normalizedPinOrder = normalizePinOrderForInsert(param);

        notice01Mapper.insertNotice(
            NoticeSaveCommand.from(param, noticeId, editPwdHash, normalizedPinOrder));

        // 대상/첨부 다건 적재
        insertTargetsAndFiles(param, noticeId);

        log.info("공지 생성 완료 - noticeId={}", noticeId);
        return NoticeSaveResponse.builder().noticeId(noticeId).build();
    }

    @Override
    @Transactional
    public void updateNotice(NoticeSaveParam param) {
        log.info("공지 수정 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        validateSaveRequired(param, false);

        NoticeInfoQuery infoQuery = new NoticeInfoQuery(param.noticeId(), param.gvCmpnyCd(), param.gvUserCd());
        NoticeResult current = notice01Mapper.selectNoticeForUpdate(infoQuery);
        if (current == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_001);
        }

        // 비밀번호 검증(master 면제, 서버 판별)
        if (!isMaster(param.gvAuthCd())) {
            String hash = notice01Mapper.selectNoticePwdHash(infoQuery);
            if (hash == null || !passwordHasher.matches(param.editPwd(), hash)) {
                throw new ApiException(NoticeErrorCode.NOTICE_400_001);
            }
        }

        // [PRAFTA-049 D1] §8-1 "팝업 게시기간 중 수정 차단" 정책 완전 제거.
        // 게시기간 중에도 제목/내용/대상/첨부 전부 수정 가능(사용자 확정).
        // (isInActivePopupWindow 헬퍼·NOTICE_422_001 은 미사용이 되나 호출부만 제거.)

        // 대상 재설정(§2-2 재검증 동일 적용)
        validateTargetScope(param);

        // BUG-3: 회사 단위 PIN 직렬화 잠금(시프트/압축 경합 방지).
        notice01Mapper.lockPinSerialization(param.gvCmpnyCd());

        // PIN 정규화(§5, 수정 — 본인 제외 후 클램프/시프트)
        Integer normalizedPinOrder = normalizePinOrderForUpdate(param);

        int updated = notice01Mapper.updateNotice(NoticeUpdateCommand.from(param, normalizedPinOrder));
        if (updated == 0) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_001);
        }

        // 대상/첨부 전체 재설정(삭제 후 재삽입)
        notice01Mapper.deleteNoticeTargetByNoticeId(param.gvCmpnyCd(), param.noticeId());
        notice01Mapper.deleteNoticeFileByNoticeId(param.gvCmpnyCd(), param.noticeId());
        insertTargetsAndFiles(param, param.noticeId());

        // BUG-2: PIN 변경(유지/해제 무관) 후 항상 1..N 연속 재압축.
        // 유지 케이스도 더 낮은 순번으로 수정 시 시프트로 인해 {2,3,4} 같은 갭이 생길 수 있어 재압축 필수.
        notice01Mapper.compactPinOrder(param.gvCmpnyCd(), param.gvUserCd());

        log.info("공지 수정 완료 - noticeId={}", param.noticeId());
    }

    @Override
    public NoticePwdResponse verifyPwd(NoticePwdParam param) {
        log.info("공지 비번 검증 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // master 면제(서버 판별) — 비번 무관 통과
        if (isMaster(param.gvAuthCd())) {
            return NoticePwdResponse.builder().verified(true).build();
        }

        NoticeInfoQuery query = new NoticeInfoQuery(param.noticeId(), param.gvCmpnyCd(), param.gvUserCd());
        String hash = notice01Mapper.selectNoticePwdHash(query);
        if (hash == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_001);
        }
        boolean verified = passwordHasher.matches(param.editPwd(), hash);
        if (!verified) {
            throw new ApiException(NoticeErrorCode.NOTICE_400_001);
        }
        return NoticePwdResponse.builder().verified(true).build();
    }

    @Override
    @Transactional
    public void deleteNotice(NoticeDeleteParam param) {
        log.info("공지 삭제 진입 - cmpnyCd={}, noticeId={}", param.gvCmpnyCd(), param.noticeId());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        NoticeInfoQuery query = new NoticeInfoQuery(param.noticeId(), param.gvCmpnyCd(), param.gvUserCd());

        // 비밀번호 검증(master 면제)
        if (!isMaster(param.gvAuthCd())) {
            String hash = notice01Mapper.selectNoticePwdHash(query);
            if (hash == null) {
                throw new ApiException(NoticeErrorCode.NOTICE_404_001);
            }
            if (!passwordHasher.matches(param.editPwd(), hash)) {
                throw new ApiException(NoticeErrorCode.NOTICE_400_001);
            }
        }

        // BUG-3: 회사 단위 PIN 직렬화 잠금(삭제 후 압축이 동시 고정 연산과 경합하지 않도록).
        notice01Mapper.lockPinSerialization(param.gvCmpnyCd());

        int deleted = notice01Mapper.updateNoticeDelYn(NoticeDeleteCommand.from(param));
        if (deleted == 0) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_001);
        }

        // 삭제 시 고정 해제됨 → 잔여 고정 공지 순번 재압축(§5-3)
        notice01Mapper.compactPinOrder(param.gvCmpnyCd(), param.gvUserCd());

        log.info("공지 삭제 완료 - noticeId={}", param.noticeId());
    }

    @Override
    public NoticeScopeResponse selectScopeTree(NoticeScopeParam param) {
        log.info("공지 대상트리 조회 진입 - cmpnyCd={}, userCd={}", param.gvCmpnyCd(), param.gvUserCd());

        boolean companyWide = AuthRoleUtils.isCompanyWide(param.gvAuthCd());
        List<NoticeScopeResult> scopeList =
            notice01Mapper.selectScopeSiteNodeTree(NoticeScopeQuery.from(param, companyWide));

        return NoticeScopeResponse.builder()
            .canSelectAll(companyWide)
            .scopeList(scopeList)
            .build();
    }

    @Override
    @Transactional
    public NoticeFileUploadResponse uploadFile(NoticeFileUploadParam param) {
        log.info("공지 첨부 업로드 진입 - cmpnyCd={}, userCd={}, 원본명={}",
            param.gvCmpnyCd(), param.gvUserCd(),
            param.file() == null ? null : param.file().getOriginalFilename());

        // FILE_MGMT_CD 채번(SYS010 005=공지첨부). 회사+파일타입 단위 시퀀스.
        String fileMgmtCd = fileMapper.selectFileMgmtCd(
            FileInfoQuery.from(param.gvCmpnyCd(), FILE_TYPE_NOTICE));

        // 저장 경로 세그먼트용 사업장 — 발행자 소속 사업장이 없으면(전사 관리자 등) 폴백.
        // FILE_PATH 는 그대로 DB 저장되고 다운로드 시 동일 경로를 사용하므로 값만 일관되면 된다.
        String siteCd = StringUtils.hasText(param.gvSiteCd()) ? param.gvSiteCd() : SITE_FALLBACK;

        // 공통 FileService 재사용 — 확장자 화이트리스트 검증 + path traversal 방어 + 디스크 저장 + tb_file_info INSERT
        fileService.fileSave(FileInfoParam.from(
            param.gvCmpnyCd()
            , param.gvUserCd()
            , siteCd
            , FILE_TYPE_NOTICE
            , fileMgmtCd
            , param.file()));

        String fileNm = param.file().getOriginalFilename();
        log.info("공지 첨부 업로드 완료 - fileMgmtCd={}, 파일명={}", fileMgmtCd, fileNm);

        return NoticeFileUploadResponse.builder()
            .fileMgmtCd(fileMgmtCd)
            .fileNm(fileNm)
            .build();
    }

    // ── 047-4 노출/ACK/다운로드 ───────────────────────────────

    @Override
    public NoticePopupResponse selectPopupNotices(NoticePopupParam param) {
        log.info("공지 팝업 조회 진입 - cmpnyCd={}, userCd={}, siteCd={}",
            param.gvCmpnyCd(), param.gvUserCd(), param.curSiteCd());

        boolean isDaily = notice01Mapper.countDailyUser(param.gvCmpnyCd(), param.gvUserCd()) > 0;

        List<NoticePopupResult> popupList =
            notice01Mapper.selectPopupNoticeList(NoticePopupQuery.from(param, isDaily));

        // 팝업 항목별 첨부 list 합성(최대 10건이라 건별 조회 허용). 다운로드는 단기 토큰 흐름 재사용.
        List<NoticePopupItem> items = new java.util.ArrayList<>();
        for (NoticePopupResult r : popupList) {
            List<NoticeFileResult> files = notice01Mapper.selectNoticeFileList(
                new NoticeInfoQuery(r.noticeId(), param.gvCmpnyCd(), param.gvUserCd()));
            items.add(NoticePopupItem.builder()
                .noticeId(r.noticeId())
                .title(r.title())
                .content(r.content())
                .pinYn(r.pinYn())
                .popupFromYmd(r.popupFromYmd())
                .popupToYmd(r.popupToYmd())
                .insertDate(r.insertDate())
                .fileCnt(files.size())
                .isDaily(r.isDaily())
                .fileList(files)
                .build());
        }

        return NoticePopupResponse.builder()
            .popupList(items)
            .build();
    }

    @Override
    @Transactional
    public void ackConfirm(NoticeAckParam param) {
        log.info("공지 확인 처리 진입 - cmpnyCd={}, noticeId={}, userCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
        requireNoticeId(param.noticeId());
        notice01Mapper.upsertNoticeAck(NoticeAckCommand.confirm(param));
    }

    @Override
    @Transactional
    public void ackSnooze(NoticeAckParam param) {
        log.info("공지 한시숨김 처리 진입 - cmpnyCd={}, noticeId={}, userCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
        requireNoticeId(param.noticeId());

        // 한시숨김 거부 가드: 일용직(SNOOZED 미제공, §10) / 비고정 공지(고정 공지만 SNOOZED, §6-6)
        boolean isDaily = notice01Mapper.countDailyUser(param.gvCmpnyCd(), param.gvUserCd()) > 0;
        if (isDaily) {
            throw new ApiException(NoticeErrorCode.NOTICE_400_004);
        }
        NoticeResult notice = notice01Mapper.selectNoticeForUpdate(
            new NoticeInfoQuery(param.noticeId(), param.gvCmpnyCd(), param.gvUserCd()));
        if (notice == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_001);
        }
        if (!"Y".equals(notice.pinYn())) {
            throw new ApiException(NoticeErrorCode.NOTICE_400_004);
        }

        String snoozeUntil = LocalDate.now().plusDays(SNOOZE_DAYS).format(YMD);
        notice01Mapper.upsertNoticeAck(NoticeAckCommand.snooze(param, snoozeUntil));
    }

    @Override
    @Transactional
    public void read(NoticeAckParam param) {
        requireNoticeId(param.noticeId());
        notice01Mapper.upsertNoticeRead(NoticeAckCommand.read(param));
    }

    @Override
    public NoticeFileDlResponse issueFileDownloadToken(NoticeFileDlParam param) {
        log.info("공지 다운로드 토큰 발급 진입 - cmpnyCd={}, noticeId={}, fileMgmtCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.fileMgmtCd());

        if (!StringUtils.hasText(param.noticeId()) || !StringUtils.hasText(param.fileMgmtCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 첨부 존재/스코프 확인(없으면 404). 토큰에 cmpnyCd/noticeId/fileMgmtCd 바인딩.
        NoticeFileResult file = notice01Mapper.selectNoticeFileOne(NoticeFileQuery.from(param));
        if (file == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_002);
        }

        // 047-001: 노출 대상 재검증. 첨부 존재+동일 회사만으론 부족 — 요청자가 이 공지를
        // 실제로 받을 수 있는 대상(§6 popup 매칭)인지 서버에서 재확인한다(대상 외 무단 다운로드 차단).
        assertNoticeDownloadable(param);

        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("gv_noticeId", param.noticeId());
        extra.put("gv_fileMgmtCd", param.fileMgmtCd());
        String token = jwtUtil.generateScopeToken(
            param.gvCmpnyCd(), param.gvUserCd(), JwtScope.NOTICE_FILE_DL, FILE_DL_TOKEN_TTL_MIN, extra);

        return NoticeFileDlResponse.builder()
            .token(token)
            .expiresInSec(FILE_DL_TOKEN_TTL_MIN * 60L)
            .fileNm(file.fileNm())
            .build();
    }

    @Override
    public NoticeFileResult resolveDownloadFile(String cmpnyCd, String noticeId, String fileMgmtCd) {
        NoticeFileResult file =
            notice01Mapper.selectNoticeFileOne(NoticeFileQuery.of(cmpnyCd, noticeId, fileMgmtCd));
        if (file == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_002);
        }
        return file;
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

    private void requireNoticeId(String noticeId) {
        if (!StringUtils.hasText(noticeId)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    private boolean isMaster(String authCd) {
        return AuthRoleUtils.AUTH_MASTER.equals(authCd);
    }

    /**
     * 047-001: 첨부 다운로드 가능 여부 재검증.
     *  - master 는 무조건 통과(전사 관리자).
     *  - 발행자 본인(INSERT_NO == userCd)도 통과(관리 화면 접근자).
     *  - 그 외는 현재 소속(사업장/노드, 일용직 여부) 기준 노출 대상 도달 가능성(§6 매칭) + DEL_YN='N' 을 확인.
     * 0 이면 NOTICE_403_003 으로 거부. 관리 목록에서도 다운로드하므로 팝업 기간/이력은 보지 않는다.
     */
    private void assertNoticeDownloadable(NoticeFileDlParam param) {
        if (isMaster(param.gvAuthCd())) {
            return;
        }
        String insertNo = notice01Mapper.selectNoticeInsertNo(param.gvCmpnyCd(), param.noticeId());
        if (insertNo != null && insertNo.equals(param.gvUserCd())) {
            return; // 발행자 본인 통과
        }

        boolean isDaily = notice01Mapper.countDailyUser(param.gvCmpnyCd(), param.gvUserCd()) > 0;
        int visible = notice01Mapper.countNoticeVisibleToUser(
            param.gvCmpnyCd(), param.noticeId(), param.curSiteCd(), param.curNodeCd(), isDaily);
        if (visible <= 0) {
            log.warn("공지 첨부 다운로드 대상 외 차단 - cmpnyCd={}, noticeId={}, userCd={}",
                param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
            throw new ApiException(NoticeErrorCode.NOTICE_403_003);
        }
    }

    /** 생성/수정 공통 필수값 검증. isCreate=true 면 editPwd 도 필수. */
    private void validateSaveRequired(NoticeSaveParam param, boolean isCreate) {
        if (!StringUtils.hasText(param.title())
                || !StringUtils.hasText(param.content())
                || !StringUtils.hasText(param.targetScope())) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n필수값 누락 - 제목/내용/대상스코프");
        }
        if (isCreate && !StringUtils.hasText(param.editPwd())) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n필수값 누락 - 비밀번호");
        }
        // 팝업 ON 이면 기간 필수(§1-1)
        if ("Y".equals(param.popupYn())
                && (!StringUtils.hasText(param.popupFromYmd()) || !StringUtils.hasText(param.popupToYmd()))) {
            throw new ApiException(NoticeErrorCode.NOTICE_400_002);
        }
        // 고정 ON 이면 순번 1 이상
        if ("Y".equals(param.pinYn()) && param.pinOrder() != null && param.pinOrder() < 1) {
            throw new ApiException(NoticeErrorCode.NOTICE_400_003);
        }
    }

    /**
     * 발행자 대상 스코프 서버 재검증(§2-2). 신규 권한 규칙을 만들지 않고 기존 컴포넌트 재사용:
     *  - master: 전 대상 허용 + ALL(전사) 가능(요청서 §0/§2-2 — ALL 은 master 전용).
     *  - 그 외(safe 포함): ALL 금지. 각 target 이 (소속 사업장 자기노드+자손) 또는
     *    (권한 보유 사업장 전 노드) 내인지 본인 스코프로 정상 검증한다(우회 금지).
     */
    private void validateTargetScope(NoticeSaveParam param) {
        String scope = param.targetScope();

        if (SCOPE_ALL.equals(scope)) {
            // BUG-1: 전사(ALL) 발행은 master 전용(요청서 §0/§2-2). safe 는 SITE/NODE 만 가능.
            if (!isMaster(param.gvAuthCd())) {
                throw ApiException.appendf(NoticeErrorCode.NOTICE_403_001, "%n전사 공지 발행 권한 없음");
            }
            return;
        }

        if (!SCOPE_SITE.equals(scope) && !SCOPE_NODE.equals(scope)) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        List<NoticeTargetModel> targets = param.targetList();
        if (targets == null || targets.isEmpty()) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n대상 미지정");
        }

        for (NoticeTargetModel t : targets) {
            if (!StringUtils.hasText(t.siteCd())) {
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "%n대상 사업장 누락");
            }
            // BUG-1: per-target 검증 스킵은 master 일 때만(전사). safe 등은 본인 스코프 검증을 받아야 함.
            if (isMaster(param.gvAuthCd())) {
                continue; // master 는 모든 사업장/노드 허용
            }

            // 사업장 접근 권한: 권한 보유 사업장 또는 본인 소속 사업장
            boolean isOwnSite = StringUtils.hasText(param.gvSiteCd()) && t.siteCd().equals(param.gvSiteCd());
            boolean siteAllowed =
                notice01Mapper.countUserSiteAuth(param.gvCmpnyCd(), param.gvUserCd(), t.siteCd()) > 0
                || isOwnSite;

            if (!siteAllowed) {
                throw ApiException.appendf(NoticeErrorCode.NOTICE_403_001,
                    "%n대상 권한 범위 초과 - %s/%s", t.siteCd(), t.nodeCd());
            }

            // 노드 지정(NODE 스코프)이면 노드 범위 재검증
            if (StringUtils.hasText(t.nodeCd())) {
                boolean nodeAllowed;
                if (isOwnSite && StringUtils.hasText(param.gvNodeCd())) {
                    // 소속 사업장: 자기노드 + 자손만 허용
                    nodeAllowed = notice01Mapper.countNodeInSelfSubtree(
                        param.gvCmpnyCd(), t.siteCd(), param.gvNodeCd(), t.nodeCd()) > 0;
                } else {
                    // 권한 보유(비소속) 사업장: 전 노드 허용(실재 노드인지만 확인)
                    nodeAllowed = notice01Mapper.countNodeInSite(
                        param.gvCmpnyCd(), t.siteCd(), t.nodeCd()) > 0;
                }
                if (!nodeAllowed) {
                    throw ApiException.appendf(NoticeErrorCode.NOTICE_403_001,
                        "%n대상 권한 범위 초과 - %s/%s", t.siteCd(), t.nodeCd());
                }
            }
        }
    }

    private void insertTargetsAndFiles(NoticeSaveParam param, String noticeId) {
        // ALL 스코프는 대상 매핑 없음(요청서 §1-1)
        if (!SCOPE_ALL.equals(param.targetScope())
                && param.targetList() != null && !param.targetList().isEmpty()) {
            notice01Mapper.insertNoticeTargets(NoticeTargetSaveCommand.of(
                noticeId, param.targetList(), param.gvCmpnyCd(), param.gvUserCd()));
        }
        if (param.fileList() != null && !param.fileList().isEmpty()) {
            notice01Mapper.insertNoticeFiles(NoticeFileSaveCommand.of(
                noticeId, param.fileList(), param.gvCmpnyCd(), param.gvUserCd()));
        }
    }

    /**
     * PIN 정규화(생성, §5): 고정 아니면 null. 고정이면 min(요청, 현재고정수+1) 클램프 후 시프트.
     * 단일 트랜잭션 + CMPNY_CD FOR UPDATE 로 동시성 보호.
     */
    private Integer normalizePinOrderForInsert(NoticeSaveParam param) {
        if (!"Y".equals(param.pinYn())) {
            return null;
        }
        int pinnedCount = notice01Mapper.selectPinnedCountForUpdate(param.gvCmpnyCd(), null);
        int requested = (param.pinOrder() == null || param.pinOrder() < 1) ? pinnedCount + 1 : param.pinOrder();
        int target = Math.min(requested, pinnedCount + 1);
        // 신규 삽입 위치 이상(>=) 기존 고정 공지 +1 시프트
        notice01Mapper.shiftPinOrderUp(param.gvCmpnyCd(), target, null, param.gvUserCd());
        return target;
    }

    /**
     * PIN 정규화(수정, §5): 본인 제외 후 동일 규칙. 고정 해제면 null(호출 측에서 compact).
     */
    private Integer normalizePinOrderForUpdate(NoticeSaveParam param) {
        if (!"Y".equals(param.pinYn())) {
            return null;
        }
        int pinnedCount = notice01Mapper.selectPinnedCountForUpdate(param.gvCmpnyCd(), param.noticeId());
        int requested = (param.pinOrder() == null || param.pinOrder() < 1) ? pinnedCount + 1 : param.pinOrder();
        int target = Math.min(requested, pinnedCount + 1);
        notice01Mapper.shiftPinOrderUp(param.gvCmpnyCd(), target, param.noticeId(), param.gvUserCd());
        return target;
    }

    /** §8: POPUP_YN='Y' 이고 오늘이 팝업 기간(FROM~TO) 내인지. */
    private boolean isInActivePopupWindow(NoticeResult notice) {
        if (!"Y".equals(notice.popupYn())) {
            return false;
        }
        if (!StringUtils.hasText(notice.popupFromYmd()) || !StringUtils.hasText(notice.popupToYmd())) {
            return false;
        }
        String today = LocalDate.now().format(YMD);
        return today.compareTo(notice.popupFromYmd()) >= 0
            && today.compareTo(notice.popupToYmd()) <= 0;
    }
}
