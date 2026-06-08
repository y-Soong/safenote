package com.prafta.app.notice.notice01.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.notice.notice01.application.command.AppNoticeAckCommand;
import com.prafta.app.notice.notice01.application.param.AppNoticeAckParam;
import com.prafta.app.notice.notice01.application.param.AppNoticeFileDlParam;
import com.prafta.app.notice.notice01.application.param.AppNoticeInfoParam;
import com.prafta.app.notice.notice01.application.param.AppNoticePopupParam;
import com.prafta.app.notice.notice01.application.query.AppNoticeFileQuery;
import com.prafta.app.notice.notice01.application.query.AppNoticeInfoQuery;
import com.prafta.app.notice.notice01.application.query.AppNoticePopupQuery;
import com.prafta.app.notice.notice01.dto.response.AppMyNoticeListResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeDetailResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeFileDlResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticePopupItem;
import com.prafta.app.notice.notice01.dto.response.AppNoticePopupResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeUnreadCountResponse;
import com.prafta.app.notice.notice01.mapper.AppNotice01Mapper;
import com.prafta.app.notice.notice01.result.AppMyNoticeResult;
import com.prafta.app.notice.notice01.result.AppNoticeFileResult;
import com.prafta.app.notice.notice01.result.AppNoticePopupResult;
import com.prafta.app.notice.notice01.result.AppNoticeResult;
import com.prafta.app.notice.notice01.service.AppNotice01Service;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.notice.NoticeErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtScope;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 공지 표시/ACK/다운로드 서비스 구현 (prafta-app-023-1).
 *
 * <p>식별자(cmpnyCd/userCd/siteCd/nodeCd)는 JWT 클레임에서만 도출(IDOR 차단).
 *    웹 Notice01ServiceImpl 의 표시계열 로직을 앱 전용으로 미러링하되,
 *    상세(notice-info)에는 노출 대상 fail-closed 재검증을 추가한다(§0.3).
 * <p>앱 완전분리(app-010/012 선례): 웹 컨트롤러/서비스를 런타임 호출하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppNotice01ServiceImpl implements AppNotice01Service {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int SNOOZE_DAYS = 7;
    private static final int FILE_DL_TOKEN_TTL_MIN = 5;

    private final AppNotice01Mapper appNotice01Mapper;
    private final JwtUtil jwtUtil;

    @Override
    public AppNoticePopupResponse selectPopupNotices(AppNoticePopupParam param) {
        log.info("앱 공지 팝업 조회 진입 - cmpnyCd={}, userCd={}, siteCd={}",
            param.gvCmpnyCd(), param.gvUserCd(), param.curSiteCd());

        boolean isDaily = isDailyUser(param.gvCmpnyCd(), param.gvUserCd());

        List<AppNoticePopupResult> popupList =
            appNotice01Mapper.selectPopupNoticeList(AppNoticePopupQuery.from(param, isDaily));

        // 팝업 항목별 첨부 list 합성(최대 10건이라 건별 조회 허용).
        List<AppNoticePopupItem> items = new ArrayList<>();
        for (AppNoticePopupResult r : popupList) {
            List<AppNoticeFileResult> files = appNotice01Mapper.selectNoticeFileList(
                AppNoticeInfoQuery.of(r.noticeId(), param.gvCmpnyCd(), param.gvUserCd()));
            items.add(AppNoticePopupItem.builder()
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

        return AppNoticePopupResponse.builder()
            .popupList(items)
            .build();
    }

    @Override
    public AppMyNoticeListResponse selectMyNotices(AppNoticePopupParam param) {
        log.info("앱 내 공지 목록 조회 진입 - cmpnyCd={}, userCd={}, siteCd={}",
            param.gvCmpnyCd(), param.gvUserCd(), param.curSiteCd());

        boolean isDaily = isDailyUser(param.gvCmpnyCd(), param.gvUserCd());
        AppNoticePopupQuery query = AppNoticePopupQuery.from(param, isDaily);

        List<AppMyNoticeResult> noticeList = appNotice01Mapper.selectMyNoticeList(query);
        int unreadCount = appNotice01Mapper.countMyUnreadNotice(query);

        return AppMyNoticeListResponse.builder()
            .noticeList(noticeList)
            .unreadCount(unreadCount)
            .build();
    }

    @Override
    public AppNoticeUnreadCountResponse selectUnreadCount(AppNoticePopupParam param) {
        log.info("앱 미열람 공지 카운트 조회 진입 - cmpnyCd={}, userCd={}",
            param.gvCmpnyCd(), param.gvUserCd());

        boolean isDaily = isDailyUser(param.gvCmpnyCd(), param.gvUserCd());
        int unreadCount =
            appNotice01Mapper.countMyUnreadNotice(AppNoticePopupQuery.from(param, isDaily));

        return AppNoticeUnreadCountResponse.builder()
            .unreadCount(unreadCount)
            .build();
    }

    @Override
    @Transactional
    public AppNoticeDetailResponse selectNoticeInfo(AppNoticeInfoParam param) {
        log.info("앱 공지 상세 조회 진입 - cmpnyCd={}, noticeId={}, userCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());

        if (!StringUtils.hasText(param.noticeId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // §0.3 상세 진입 전 노출 대상 fail-closed 재검증(웹과 달리 앱은 일반 근로자 직접 호출 → IDOR 차단).
        assertNoticeVisible(param);

        AppNoticeInfoQuery query = AppNoticeInfoQuery.of(
            param.noticeId(), param.gvCmpnyCd(), param.gvUserCd());
        AppNoticeResult noticeInfo = appNotice01Mapper.selectNoticeOne(query);
        if (noticeInfo == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_001);
        }

        // 상세 열람 시 LAST_READ_DATE 갱신(뱃지 소멸). 조회 응답값은 갱신 전 스냅샷 사용(웹 동일).
        appNotice01Mapper.upsertNoticeRead(AppNoticeAckCommand.read(
            new AppNoticeAckParam(param.noticeId(), param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd())));

        return AppNoticeDetailResponse.builder()
            .noticeInfo(noticeInfo)
            .fileList(appNotice01Mapper.selectNoticeFileList(query))
            .build();
    }

    @Override
    @Transactional
    public void ackConfirm(AppNoticeAckParam param) {
        log.info("앱 공지 확인 처리 진입 - cmpnyCd={}, noticeId={}, userCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
        requireNoticeId(param.noticeId());
        appNotice01Mapper.upsertNoticeAck(AppNoticeAckCommand.confirm(param));
    }

    @Override
    @Transactional
    public void ackSnooze(AppNoticeAckParam param) {
        log.info("앱 공지 한시숨김 처리 진입 - cmpnyCd={}, noticeId={}, userCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
        requireNoticeId(param.noticeId());

        // 한시숨김 거부 가드(웹 동일): 일용직(SNOOZED 미제공, §10) / 비고정 공지(고정 공지만 SNOOZED, §6-6)
        boolean isDaily = isDailyUser(param.gvCmpnyCd(), param.gvUserCd());
        if (isDaily) {
            throw new ApiException(NoticeErrorCode.NOTICE_400_004);
        }
        AppNoticeResult notice = appNotice01Mapper.selectNoticeForSnooze(
            AppNoticeInfoQuery.of(param.noticeId(), param.gvCmpnyCd(), param.gvUserCd()));
        if (notice == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_001);
        }
        if (!"Y".equals(notice.pinYn())) {
            throw new ApiException(NoticeErrorCode.NOTICE_400_004);
        }

        String snoozeUntil = LocalDate.now().plusDays(SNOOZE_DAYS).format(YMD);
        appNotice01Mapper.upsertNoticeAck(AppNoticeAckCommand.snooze(param, snoozeUntil));
    }

    @Override
    @Transactional
    public void read(AppNoticeAckParam param) {
        requireNoticeId(param.noticeId());
        appNotice01Mapper.upsertNoticeRead(AppNoticeAckCommand.read(param));
    }

    @Override
    public AppNoticeFileDlResponse issueFileDownloadToken(AppNoticeFileDlParam param) {
        log.info("앱 공지 다운로드 토큰 발급 진입 - cmpnyCd={}, noticeId={}, fileMgmtCd={}",
            param.gvCmpnyCd(), param.noticeId(), param.fileMgmtCd());

        if (!StringUtils.hasText(param.noticeId()) || !StringUtils.hasText(param.fileMgmtCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 첨부 존재/회사 스코프 확인(없으면 404).
        AppNoticeFileResult file = appNotice01Mapper.selectNoticeFileOne(AppNoticeFileQuery.from(param));
        if (file == null) {
            throw new ApiException(NoticeErrorCode.NOTICE_404_002);
        }

        // 노출 대상 재검증(웹 assertNoticeDownloadable 미러). 첨부 존재+동일 회사만으론 부족.
        assertNoticeDownloadable(param);

        Map<String, Object> extra = new HashMap<>();
        extra.put("gv_noticeId", param.noticeId());
        extra.put("gv_fileMgmtCd", param.fileMgmtCd());
        String token = jwtUtil.generateScopeToken(
            param.gvCmpnyCd(), param.gvUserCd(), JwtScope.NOTICE_FILE_DL, FILE_DL_TOKEN_TTL_MIN, extra);

        return AppNoticeFileDlResponse.builder()
            .token(token)
            .expiresInSec(FILE_DL_TOKEN_TTL_MIN * 60L)
            .fileNm(file.fileNm())
            .build();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

    private boolean isDailyUser(String cmpnyCd, String userCd) {
        return appNotice01Mapper.countDailyUser(cmpnyCd, userCd) > 0;
    }

    private void requireNoticeId(String noticeId) {
        if (!StringUtils.hasText(noticeId)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    private boolean isMaster(String authCd) {
        return AuthRoleUtils.AUTH_MASTER.equals(authCd);
    }

    /**
     * §0.3 앱 상세 진입 전 노출 대상 fail-closed 재검증.
     *  - master 는 통과(전사 관리자).
     *  - 발행자 본인(INSERT_NO == userCd)도 통과.
     *  - 그 외는 현재 소속(사업장/노드, 일용직 여부) 기준 노출 대상 도달 가능성(§6 매칭)을 확인.
     * 0 이면 NOTICE_403_003 으로 거부.
     */
    private void assertNoticeVisible(AppNoticeInfoParam param) {
        if (isMaster(param.gvAuthCd())) {
            return;
        }
        String insertNo = appNotice01Mapper.selectNoticeInsertNo(param.gvCmpnyCd(), param.noticeId());
        if (insertNo != null && insertNo.equals(param.gvUserCd())) {
            return; // 발행자 본인 통과
        }

        boolean isDaily = isDailyUser(param.gvCmpnyCd(), param.gvUserCd());
        int visible = appNotice01Mapper.countNoticeVisibleToUser(
            param.gvCmpnyCd(), param.noticeId(), param.curSiteCd(), param.curNodeCd(), isDaily);
        if (visible <= 0) {
            log.warn("앱 공지 상세 대상 외 차단 - cmpnyCd={}, noticeId={}, userCd={}",
                param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
            throw new ApiException(NoticeErrorCode.NOTICE_403_003);
        }
    }

    /**
     * 첨부 다운로드 가능 여부 재검증(웹 assertNoticeDownloadable 미러).
     *  - master 통과 / 발행자 본인 통과 / 그 외는 노출 대상 매칭(§6) + DEL_YN='N' 확인.
     * 0 이면 NOTICE_403_003.
     */
    private void assertNoticeDownloadable(AppNoticeFileDlParam param) {
        if (isMaster(param.gvAuthCd())) {
            return;
        }
        String insertNo = appNotice01Mapper.selectNoticeInsertNo(param.gvCmpnyCd(), param.noticeId());
        if (insertNo != null && insertNo.equals(param.gvUserCd())) {
            return; // 발행자 본인 통과
        }

        boolean isDaily = isDailyUser(param.gvCmpnyCd(), param.gvUserCd());
        int visible = appNotice01Mapper.countNoticeVisibleToUser(
            param.gvCmpnyCd(), param.noticeId(), param.curSiteCd(), param.curNodeCd(), isDaily);
        if (visible <= 0) {
            log.warn("앱 공지 첨부 다운로드 대상 외 차단 - cmpnyCd={}, noticeId={}, userCd={}",
                param.gvCmpnyCd(), param.noticeId(), param.gvUserCd());
            throw new ApiException(NoticeErrorCode.NOTICE_403_003);
        }
    }
}
