package com.prafta.web.acct.acct01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.acct.acct01.application.param.AcctCreateParam;
import com.prafta.web.acct.acct01.application.param.AcctDeleteParam;
import com.prafta.web.acct.acct01.application.param.AcctInfoParam;
import com.prafta.web.acct.acct01.application.param.AcctListParam;
import com.prafta.web.acct.acct01.application.param.AcctUpdateParam;
import com.prafta.web.acct.acct01.application.param.AcctVictimAddParam;
import com.prafta.web.acct.acct01.application.param.AcctVictimListParam;
import com.prafta.web.acct.acct01.application.param.AcctVictimRemoveParam;
import com.prafta.web.acct.acct01.application.param.AcctVictimUpdateParam;
import com.prafta.web.acct.acct01.application.param.AttdTbmPrintParam;
import com.prafta.web.acct.acct01.application.param.ChkptOptionParam;
import com.prafta.web.acct.acct01.application.param.LegalStepListParam;
import com.prafta.web.acct.acct01.application.param.LegalStepSaveParam;
import com.prafta.web.acct.acct01.application.param.LinkConfirmParam;
import com.prafta.web.acct.acct01.application.param.LinkQueryParam;
import com.prafta.web.acct.acct01.application.param.LinkSnapshotParam;
import com.prafta.web.acct.acct01.application.param.RiskAssessmentPrintParam;
import com.prafta.web.acct.acct01.application.param.RiskCategoryOptionParam;
import com.prafta.web.acct.acct01.application.param.VictimSearchParam;
import com.prafta.web.acct.acct01.dto.request.AcctCreateRequest;
import com.prafta.web.acct.acct01.dto.request.AcctDeleteRequest;
import com.prafta.web.acct.acct01.dto.request.AcctInfoRequest;
import com.prafta.web.acct.acct01.dto.request.AcctListRequest;
import com.prafta.web.acct.acct01.dto.request.AcctUpdateRequest;
import com.prafta.web.acct.acct01.dto.request.AcctVictimAddRequest;
import com.prafta.web.acct.acct01.dto.request.AcctVictimListRequest;
import com.prafta.web.acct.acct01.dto.request.AcctVictimRemoveRequest;
import com.prafta.web.acct.acct01.dto.request.AcctVictimUpdateRequest;
import com.prafta.web.acct.acct01.dto.request.AttdTbmPrintRequest;
import com.prafta.web.acct.acct01.dto.request.ChkptOptionRequest;
import com.prafta.web.acct.acct01.dto.request.LegalStepListRequest;
import com.prafta.web.acct.acct01.dto.request.LegalStepSaveRequest;
import com.prafta.web.acct.acct01.dto.request.LinkConfirmRequest;
import com.prafta.web.acct.acct01.dto.request.LinkQueryRequest;
import com.prafta.web.acct.acct01.dto.request.LinkSnapshotRequest;
import com.prafta.web.acct.acct01.dto.request.RiskAssessmentPrintRequest;
import com.prafta.web.acct.acct01.dto.request.RiskCategoryOptionRequest;
import com.prafta.web.acct.acct01.dto.request.VictimSearchRequest;
import com.prafta.web.acct.acct01.service.Acct01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사고관리(웹 관리자) 컨트롤러.
 * 식별자(cmpnyCd/userCd/siteCd 권한)는 JWT 클레임에서만 도출하여 cross-site IDOR 을 차단한다.
 * axios 프리픽스: /webApi/acct01/... (전역 /prafta/webApi prefix 자동 부여)
 */
@Slf4j
@RestController
@RequestMapping("/acct01")
@RequiredArgsConstructor
public class Acct01Controller {

    private final Acct01Service acct01Service;
    private final JwtUtil jwtUtil;

    // ── 048-03 CRUD ──────────────────────────────────────────────

    // 사고 목록
    @GetMapping("/list")
    public ResponseEntity<?> getList(
            @ModelAttribute AcctListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectAcctList(
                AcctListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 사고 단건 상세
    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(
            @ModelAttribute AcctInfoRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectAcctInfo(
                AcctInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 사고 등록
    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(
            @RequestBody AcctCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.createAcct(
                AcctCreateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 사고 수정
    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(
            @RequestBody AcctUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        acct01Service.updateAcct(
            AcctUpdateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 사고 삭제 (soft delete, master/system)
    @PostMapping(value = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(
            @RequestBody AcctDeleteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        acct01Service.deleteAcct(
            AcctDeleteParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 재해자 검색 (정규 + 일용)
    @GetMapping("/victim-search")
    public ResponseEntity<?> victimSearch(
            @ModelAttribute VictimSearchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.searchVictim(
                VictimSearchParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ── 065 재해자 (사고 1:N) ────────────────────────────────────

    // 사고 재해자 목록 (순번순, 대표 여부 포함)
    @GetMapping("/victims")
    public ResponseEntity<?> victims(
            @ModelAttribute AcctVictimListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectAcctVictims(
                AcctVictimListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 사고 재해자 추가 (쓰기 역할, 사고 단위 잠금, MAX+1 채번)
    @PostMapping(value = "/victims/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> victimAdd(
            @RequestBody AcctVictimAddRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.addVictim(
                AcctVictimAddParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 사고 재해자 속성 수정 (인물 불변)
    @PostMapping(value = "/victims/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> victimUpdate(
            @RequestBody AcctVictimUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        acct01Service.updateVictimAttr(
            AcctVictimUpdateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 사고 재해자 제외 (하한 1명, 대표 제외 시 승계)
    @PostMapping(value = "/victims/remove", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> victimRemove(
            @RequestBody AcctVictimRemoveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        acct01Service.removeVictim(
            AcctVictimRemoveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // ── 048-04 연계 조회 ─────────────────────────────────────────

    // 근태 (당일)
    @GetMapping("/link/attendance")
    public ResponseEntity<?> linkAttendance(
            @ModelAttribute LinkQueryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectLinkAttendance(
                LinkQueryParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 순회점검 (1주일)
    @GetMapping("/link/patrol")
    public ResponseEntity<?> linkPatrol(
            @ModelAttribute LinkQueryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectLinkPatrol(
                LinkQueryParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 위험성평가 (3개월)
    @GetMapping("/link/risk")
    public ResponseEntity<?> linkRisk(
            @ModelAttribute LinkQueryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectLinkRisk(
                LinkQueryParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // TBM (당일 고정)
    @GetMapping("/link/tbm")
    public ResponseEntity<?> linkTbm(
            @ModelAttribute LinkQueryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectLinkTbm(
                LinkQueryParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ── T8 안전관리 현황 일괄 출력 ───────────────────────────────

    // ③ 근태(스케줄+실근태) + TBM 합본 집계 (siteCd/acctId 만 받고 victim/occurYmd 는 서버 도출)
    @GetMapping("/print/attd-tbm")
    public ResponseEntity<?> printAttdTbm(
            @ModelAttribute AttdTbmPrintRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectAttdTbmPrint(
                AttdTbmPrintParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ② 위험성평가 출력 상세 보강 (assessmentCd 연계 검증 후 라이브 재조회)
    @GetMapping("/print/risk-assessment")
    public ResponseEntity<?> printRiskAssessment(
            @ModelAttribute RiskAssessmentPrintRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectRiskAssessmentForPrint(
                RiskAssessmentPrintParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 점검대상 검색 옵션 (ChkptSearchPop)
    @GetMapping("/patrol/chkpt-options")
    public ResponseEntity<?> chkptOptions(
            @ModelAttribute ChkptOptionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectChkptOptions(
                ChkptOptionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 위험성평가 3계층 옵션
    @GetMapping("/risk/category-options")
    public ResponseEntity<?> riskCategoryOptions(
            @ModelAttribute RiskCategoryOptionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectRiskCategoryOptions(
                RiskCategoryOptionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ── 048-05 스냅샷/법정절차 ───────────────────────────────────

    // 연계 확정 스냅샷 저장
    @PostMapping(value = "/link/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> linkConfirm(
            @RequestBody LinkConfirmRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        acct01Service.confirmLink(
            LinkConfirmParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 확정 스냅샷 조회 (①탭)
    @GetMapping("/link/snapshot")
    public ResponseEntity<?> linkSnapshot(
            @ModelAttribute LinkSnapshotRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectLinkSnapshot(
                LinkSnapshotParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 법정절차 목록 (②탭)
    @GetMapping("/legal-step/list")
    public ResponseEntity<?> legalStepList(
            @ModelAttribute LegalStepListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectLegalStepList(
                LegalStepListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // 법정절차 조치완료 체크/비고 저장 (②탭)
    @PostMapping(value = "/legal-step/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> legalStepSave(
            @RequestBody LegalStepSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        acct01Service.saveLegalStep(
            LegalStepSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 처리 이력 (③탭, 파생 롤업)
    @GetMapping("/legal-step/history")
    public ResponseEntity<?> legalStepHistory(
            @ModelAttribute LegalStepListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
            acct01Service.selectLegalStepHistory(
                LegalStepListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }
}
