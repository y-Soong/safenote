package com.prafta.app.entryadmin.entryadmin01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.app.entryadmin.entryadmin01.application.param.EntryApproveParam;
import com.prafta.app.entryadmin.entryadmin01.application.param.EntryPendingListParam;
import com.prafta.app.entryadmin.entryadmin01.application.param.EntryRejectParam;
import com.prafta.app.entryadmin.entryadmin01.dto.response.EntryPendingItem;
import com.prafta.app.entryadmin.entryadmin01.dto.response.EntryPendingListResponse;
import com.prafta.app.entryadmin.entryadmin01.dto.response.EntryProcessResponse;
import com.prafta.app.entryadmin.entryadmin01.service.AppEntryAdmin01Service;
import com.prafta.common.cmm.dailyentry.application.query.EntryRequestListQuery;
import com.prafta.common.cmm.dailyentry.result.EntryRequestRow;
import com.prafta.common.cmm.dailyentry.service.DailyEntryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 관리자 일용직 입장 승인 서비스 구현 — core(DailyEntryService) 위임.
 *
 * <p>사업장 인가 가드/상태 검증/트랜잭션은 core 가 수행한다(웹 User_08 과 단일 출처).
 * 목록의 사업장 스코프는 JWT gv_siteCd 로만 강제한다(클라 파라미터 미수신).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppEntryAdmin01ServiceImpl implements AppEntryAdmin01Service {

    private final DailyEntryService dailyEntryService;

    /** [SYS082] 대기 상태 — 앱 대기 목록은 '01' 만 노출. */
    private static final String REQ_STATUS_PENDING = "01";

    /** 조회 상한(전수조회 방지). */
    private static final int LIST_LIMIT = 500;

    @Override
    public EntryPendingListResponse selectPendingList(EntryPendingListParam param) {
        log.info("앱 입장 승인 대기 목록 조회 진입 — cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.gvSiteCd());

        List<EntryRequestRow> rows = dailyEntryService.selectEntryRequests(
                new EntryRequestListQuery(
                        param.gvCmpnyCd()
                        , param.gvSiteCd()
                        , REQ_STATUS_PENDING
                        , null
                        , null
                        , LIST_LIMIT)
                , param.gvUserCd()
                , param.gvAuthCd());

        List<EntryPendingItem> items = rows.stream()
                .map(r -> new EntryPendingItem(
                        r.reqId()
                        , r.userNm()
                        , maskMblNo(r.mblNoLast4())
                        , r.reqType()
                        , r.reqDtime()
                        // 확정 계약서 표시(T4/K9) — 웹 User_08 과 동일 기준. pin 원값 그대로 전달한다.
                        , r.pinnedContractVer()
                        , r.pinnedFormatType()
                        , r.activeContractVer()
                        , r.activeFormatType()
                        , r.lastSignedContractVer()))
                .toList();

        log.info("앱 입장 승인 대기 목록 조회 종료 — siteCd={}, rows={}", param.gvSiteCd(), items.size());

        return EntryPendingListResponse.builder()
                .pendingList(items)
                .totalCount(items.size())
                .build();
    }

    @Override
    public EntryProcessResponse approve(EntryApproveParam param) {
        log.info("앱 입장 승인 처리 진입 — cmpnyCd={}, procUserCd={}, 요청건수={}",
                param.gvCmpnyCd(), param.gvUserCd(), param.reqIds() == null ? 0 : param.reqIds().size());

        int processed = dailyEntryService.approveRequests(
                param.gvCmpnyCd(), param.reqIds(), param.gvUserCd(), param.gvAuthCd());

        return EntryProcessResponse.builder()
                .processedCount(processed)
                .build();
    }

    @Override
    public EntryProcessResponse reject(EntryRejectParam param) {
        log.info("앱 입장 거부 처리 진입 — cmpnyCd={}, procUserCd={}, reqId={}",
                param.gvCmpnyCd(), param.gvUserCd(), param.reqId());

        dailyEntryService.rejectRequest(
                param.gvCmpnyCd(), param.reqId(), param.reason(), param.gvUserCd(), param.gvAuthCd());

        return EntryProcessResponse.builder()
                .processedCount(1)
                .build();
    }

    /** 휴대폰 last4 마스킹("***-****-XXXX"). last4 없으면 "-". 평문/ENC 는 조회 자체를 하지 않는다. */
    private String maskMblNo(String last4) {
        if (last4 == null || last4.isBlank()) {
            return "-";
        }
        return "***-****-" + last4;
    }
}
