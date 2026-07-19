package com.prafta.app.entryadmin.entryadmin01.service;

import com.prafta.app.entryadmin.entryadmin01.application.param.EntryApproveParam;
import com.prafta.app.entryadmin.entryadmin01.application.param.EntryPendingListParam;
import com.prafta.app.entryadmin.entryadmin01.application.param.EntryRejectParam;
import com.prafta.app.entryadmin.entryadmin01.dto.response.EntryPendingListResponse;
import com.prafta.app.entryadmin.entryadmin01.dto.response.EntryProcessResponse;

/**
 * 앱 관리자 일용직 입장 승인 서비스 (얇은 위임 계층 — core: DailyEntryService).
 *
 * <p>출처: 일용직 계약서+승인제 plan §T2 endpoint / UI-DC-03.
 */
public interface AppEntryAdmin01Service {

    /** 현재 사업장(JWT gv_siteCd)의 승인 대기('01') 목록 조회. */
    EntryPendingListResponse selectPendingList(EntryPendingListParam param);

    /** 일괄/개별 승인 처리 (D9 — all-or-nothing). */
    EntryProcessResponse approve(EntryApproveParam param);

    /** 거부 처리 (D10 — 사유 필수). */
    EntryProcessResponse reject(EntryRejectParam param);
}
