package com.prafta.web.user.user08.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.dailycontract.application.query.ContractSignListQuery;
import com.prafta.common.cmm.dailycontract.result.ContractSignRow;
import com.prafta.common.cmm.dailycontract.service.DailyContractService;
import com.prafta.common.cmm.dailyentry.application.query.EntryRequestListQuery;
import com.prafta.common.cmm.dailyentry.result.EntryRequestRow;
import com.prafta.common.cmm.dailyentry.service.DailyEntryService;
import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.web.user.user08.application.param.ContractSignListParam;
import com.prafta.web.user.user08.application.param.EntryApproveParam;
import com.prafta.web.user.user08.application.param.EntryRejectParam;
import com.prafta.web.user.user08.application.param.EntryRequestListParam;
import com.prafta.web.user.user08.dto.response.ContractSignItem;
import com.prafta.web.user.user08.dto.response.ContractSignListResponse;
import com.prafta.web.user.user08.dto.response.EntryProcessResponse;
import com.prafta.web.user.user08.dto.response.EntryRequestItem;
import com.prafta.web.user.user08.dto.response.EntryRequestListResponse;
import com.prafta.web.user.user08.service.User08Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 웹 User_08(입장 승인 + 서명 이력) 서비스 구현 — core(DailyEntryService / DailyContractService) 위임.
 *
 * <p>사업장 인가 가드(처리자 SITE 권한 검증)/상태 검증/트랜잭션은 core 가 수행한다
 * (앱 entryadmin01 과 단일 출처). 회사 스코프는 JWT 클레임으로만 강제한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class User08ServiceImpl implements User08Service {

    private final DailyEntryService dailyEntryService;
    private final DailyContractService dailyContractService;
    // 휴대폰 평문 표시를 위한 AES-GCM 복호화(MBL_NO_ENC → 평문). 관리자 운영 화면 요구(User_05 전례 미러).
    private final AesGcmCrypto aesGcmCrypto;

    /** 조회 상한(전수조회 방지). */
    private static final int LIST_LIMIT = 500;

    @Override
    public EntryRequestListResponse selectEntryRequestList(EntryRequestListParam param) {
        log.info("웹 입장 승인요청 목록 조회 진입 — cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        List<EntryRequestRow> rows = dailyEntryService.selectEntryRequests(
                new EntryRequestListQuery(
                        param.gvCmpnyCd()
                        , param.siteCd()
                        , param.reqStatus()
                        , param.reqType()
                        , param.reqDate()
                        , LIST_LIMIT)
                , param.gvUserCd()
                , param.gvAuthCd());

        List<EntryRequestItem> items = rows.stream()
                .map(r -> new EntryRequestItem(
                        r.reqId()
                        , r.siteCd()
                        , r.userCd()
                        , r.userNm()
                        , formatMblNo(decryptMblNo(r.mblNoEnc()))
                        , r.reqType()
                        , r.reqStatus()
                        , r.reqDtime()
                        , r.procUserNm()
                        , r.procDtime()
                        , r.rejectReason()
                        // 확정 계약서 표시(T4/K9) — pin 원값 그대로 전달한다.
                        //   ★null(레거시)과 0(승인 시점 미등록)을 서로 바꾸거나 뭉개지 않는다(화면 표기가 다름).
                        , r.pinnedContractVer()
                        , r.pinnedFormatType()
                        , r.activeContractVer()
                        , r.activeFormatType()
                        , r.lastSignedContractVer()))
                .toList();

        log.info("웹 입장 승인요청 목록 조회 종료 — siteCd={}, rows={}", param.siteCd(), items.size());

        return EntryRequestListResponse.builder()
                .entryRequestList(items)
                .totalCount(items.size())
                .build();
    }

    @Override
    public EntryProcessResponse approve(EntryApproveParam param) {
        log.info("웹 입장 승인 처리 진입 — cmpnyCd={}, procUserCd={}, 요청건수={}",
                param.gvCmpnyCd(), param.gvUserCd(), param.reqIds() == null ? 0 : param.reqIds().size());

        int processed = dailyEntryService.approveRequests(
                param.gvCmpnyCd(), param.reqIds(), param.gvUserCd(), param.gvAuthCd());

        return EntryProcessResponse.builder()
                .processedCount(processed)
                .build();
    }

    @Override
    public EntryProcessResponse reject(EntryRejectParam param) {
        log.info("웹 입장 거부 처리 진입 — cmpnyCd={}, procUserCd={}, reqId={}",
                param.gvCmpnyCd(), param.gvUserCd(), param.reqId());

        dailyEntryService.rejectRequest(
                param.gvCmpnyCd(), param.reqId(), param.reason(), param.gvUserCd(), param.gvAuthCd());

        return EntryProcessResponse.builder()
                .processedCount(1)
                .build();
    }

    @Override
    public ContractSignListResponse selectContractSignList(ContractSignListParam param) {
        log.info("웹 계약서 서명 이력 조회 진입 — cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        List<ContractSignRow> rows = dailyContractService.findSignListForAdmin(
                new ContractSignListQuery(
                        param.gvCmpnyCd()
                        , param.siteCd()
                        , param.fromDate()
                        , param.toDate()
                        , param.userNm()
                        , LIST_LIMIT)
                , param.gvUserCd()
                , param.gvAuthCd());

        List<ContractSignItem> items = rows.stream()
                .map(r -> new ContractSignItem(
                        r.signId()
                        , r.userCd()
                        , r.userNmSnapshot()
                        , formatMblNo(decryptMblNo(r.mblNoEnc()))
                        , r.contractVer()
                        , r.firstWorkDate()
                        , r.signDtime()
                        , r.mergedSha256()))
                .toList();

        log.info("웹 계약서 서명 이력 조회 종료 — siteCd={}, rows={}", param.siteCd(), items.size());

        return ContractSignListResponse.builder()
                .contractSignList(items)
                .totalCount(items.size())
                .build();
    }

    @Override
    public FileBytesResult loadContractSignImage(String signId, String gvCmpnyCd, String gvUserCd, String gvAuthCd) {
        log.info("웹 계약서 서명본 열람 진입 — cmpnyCd={}, signId={}, 열람자={}", gvCmpnyCd, signId, gvUserCd);

        // signId 의 SITE_CD 기준 사업장 인가 가드(IDOR)는 core 가 수행한다.
        return dailyContractService.loadSignImageForAdmin(gvCmpnyCd, signId, gvUserCd, gvAuthCd);
    }

    /**
     * MBL_NO_ENC(AES-GCM 암호문)를 평문 휴대폰 숫자열로 복호화한다 (User_05 전례 미러).
     *
     * <p>암호문이 없으면 null. 복호화 실패(키 불일치/손상)는 조회 전체를 막지 않도록 격리하고
     * 평문(휴대폰)은 로그에 남기지 않는다(PII 평문 로그 금지).
     */
    private String decryptMblNo(String mblNoEnc) {
        if (mblNoEnc == null || mblNoEnc.isBlank()) {
            return null;
        }
        try {
            return aesGcmCrypto.decrypt(mblNoEnc);
        } catch (Exception e) {
            log.warn("일용직 휴대폰 복호화 실패(해당 행만 '-' 표시) - {}", e.getMessage());
            return null;
        }
    }

    /**
     * 숫자열 휴대폰을 하이픈 포맷으로 변환한다(11자리 010-XXXX-XXXX, 10자리 0XX-XXX-XXXX).
     * 없으면 '-', 그 외 자리수는 원문 그대로 반환한다.
     */
    private String formatMblNo(String digits) {
        if (digits == null || digits.isBlank()) {
            return "-";
        }
        String d = digits.replaceAll("\\D", "");
        if (d.length() == 11) {
            return d.substring(0, 3) + "-" + d.substring(3, 7) + "-" + d.substring(7);
        }
        if (d.length() == 10) {
            return d.substring(0, 3) + "-" + d.substring(3, 6) + "-" + d.substring(6);
        }
        return digits;
    }
}
