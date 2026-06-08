package com.prafta.app.notice.notice01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 로그인 팝업 노출 공지 응답(최대 10건, 정렬 §5).
 * 각 항목에 첨부 list(fileList)를 포함해 팝업에서 직접 다운로드한다.
 */
@Getter
@Builder
public class AppNoticePopupResponse {
    private List<AppNoticePopupItem> popupList;
}
