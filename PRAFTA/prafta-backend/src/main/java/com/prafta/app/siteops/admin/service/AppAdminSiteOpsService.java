package com.prafta.app.siteops.admin.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.siteops.admin.application.param.SiteOpsQrParam;
import com.prafta.app.siteops.admin.application.param.SiteOpsTargetParam;
import com.prafta.app.siteops.admin.dto.response.SiteOpsAttendanceResponse;
import com.prafta.app.siteops.admin.dto.response.SiteOpsContractMetaResponse;
import com.prafta.app.siteops.admin.dto.response.SiteOpsSignResponse;
import com.prafta.common.cmm.file.application.model.FileBytesResult;

/**
 * J1-7(prafta-app-025) 관리자 현장 일용직 QR 출퇴근 등록 서비스.
 *
 * <p>C1: 진입 게이트(SITE_OPS)·사업장 스코프·대상 유효성·멱등은 모두 서버가 최종 판정한다.
 *
 * <p>현장 계약서 서명(2026-08-30): 출근 시 서명 게이트를 판정해 미서명이면 SIGN_REQUIRED 를
 * 반환하고(출근 미등록), 관리자 폰에서 근로자 <b>본인이 직접</b> 계약서를 확인·서명한 뒤
 * 재출근 요청으로 완료한다. 게이트·대상 계약서·합성·멱등은 core(DailyContractService)의
 * 근로자 앱 서명 경로와 단일 출처다.
 */
public interface AppAdminSiteOpsService {

    /** S1 일용직 QR 출근 등록(멱등). 미서명(게이트 'Y')이면 SIGN_REQUIRED 반환(출근 미등록). */
    SiteOpsAttendanceResponse checkIn(SiteOpsQrParam param);

    /** S2 일용직 QR 퇴근 등록(멱등). 서명 게이트 미적용(최초 행위=출근에만 적용). */
    SiteOpsAttendanceResponse checkOut(SiteOpsQrParam param);

    /** 현장 서명 대상 계약서 메타(형식/페이지 수). 대상 없음/미등록이면 DAILYCONTRACT_400_003. */
    SiteOpsContractMetaResponse findContractMeta(SiteOpsTargetParam param);

    /** 현장 서명 대상 계약서 단일 페이지(1-base) 스트림. */
    FileBytesResult loadContractPage(SiteOpsTargetParam param, int page);

    /** 현장 계약서 서명 저장(근로자 본인 서명 PNG — 서버 합성, 근로자 앱 sign 과 동일 core 경로). */
    SiteOpsSignResponse sign(SiteOpsTargetParam param, MultipartFile signFile);
}
