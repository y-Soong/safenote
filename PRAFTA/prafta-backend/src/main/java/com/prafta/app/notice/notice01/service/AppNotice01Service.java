package com.prafta.app.notice.notice01.service;

import com.prafta.app.notice.notice01.application.param.AppNoticeAckParam;
import com.prafta.app.notice.notice01.application.param.AppNoticeFileDlParam;
import com.prafta.app.notice.notice01.application.param.AppNoticeInfoParam;
import com.prafta.app.notice.notice01.application.param.AppNoticePopupParam;
import com.prafta.app.notice.notice01.dto.response.AppMyNoticeListResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeDetailResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeFileDlResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticePopupResponse;
import com.prafta.app.notice.notice01.dto.response.AppNoticeUnreadCountResponse;

/**
 * 앱 공지 표시/ACK/다운로드 서비스 (prafta-app-023-1).
 * 웹 Notice01Service 의 표시계열 로직을 앱 전용으로 미러링(런타임 호출 금지).
 */
public interface AppNotice01Service {

    // 로그인 팝업 노출 판정 (세션 기준, §6)
    AppNoticePopupResponse selectPopupNotices(AppNoticePopupParam param);

    // 내 공지 목록 (카드/전체목록) + 미열람 카운트 동봉
    AppMyNoticeListResponse selectMyNotices(AppNoticePopupParam param);

    // 미열람 공지 카운트 (독립 호출)
    AppNoticeUnreadCountResponse selectUnreadCount(AppNoticePopupParam param);

    // 공지 상세 (대상 재검증 fail-closed + LAST_READ_DATE 갱신)
    AppNoticeDetailResponse selectNoticeInfo(AppNoticeInfoParam param);

    // 확인(CONFIRMED)
    void ackConfirm(AppNoticeAckParam param);

    // 한시숨김(SNOOZED, 오늘+7) — 정규직·고정공지 한정
    void ackSnooze(AppNoticeAckParam param);

    // 열람(LAST_READ_DATE 갱신)
    void read(AppNoticeAckParam param);

    // 첨부 다운로드 단기 토큰 발급
    AppNoticeFileDlResponse issueFileDownloadToken(AppNoticeFileDlParam param);
}
