package com.prafta.web.user.user08.service;

import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.web.user.user08.application.param.ContractSignListParam;
import com.prafta.web.user.user08.application.param.EntryApproveParam;
import com.prafta.web.user.user08.application.param.EntryRejectParam;
import com.prafta.web.user.user08.application.param.EntryRequestListParam;
import com.prafta.web.user.user08.dto.response.ContractSignListResponse;
import com.prafta.web.user.user08.dto.response.EntryProcessResponse;
import com.prafta.web.user.user08.dto.response.EntryRequestListResponse;

/**
 * 웹 User_08(입장 승인 + 서명 이력) 서비스 (얇은 위임 계층 — core: DailyEntryService / DailyContractService).
 *
 * <p>출처: 일용직 계약서+승인제 plan §T2 endpoint / UI-DC-06 탭1.
 * <p>서명 이력(탭2 — contract-sign-lists / contract-sign-image)은 T3 확장분(core: DailyContractService).
 */
public interface User08Service {

    /** 입장 승인요청 목록 조회 (사업장/상태/유형/요청일 필터). */
    EntryRequestListResponse selectEntryRequestList(EntryRequestListParam param);

    /** 일괄/개별 승인 처리 (D9 — all-or-nothing). */
    EntryProcessResponse approve(EntryApproveParam param);

    /** 거부 처리 (D10 — 사유 필수). */
    EntryProcessResponse reject(EntryRejectParam param);

    /** 서명 이력 목록 조회 (탭2 — 만료/탈퇴 계정 포함, §6-2). */
    ContractSignListResponse selectContractSignList(ContractSignListParam param);

    /** 서명본 합성 이미지 로드 (열람/다운로드 — signId 사업장 인가 가드는 core 수행). */
    ImageBytesResult loadContractSignImage(String signId, String gvCmpnyCd, String gvUserCd, String gvAuthCd);
}
