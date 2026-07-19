package com.prafta.web.subcon.subcon03.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.subcon.subcon03.application.param.ShareReqApproveInfoParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqApproveParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqCandidatesParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqCreateParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqProcessParam;
import com.prafta.web.subcon.subcon03.application.param.ShareScopeParam;
import com.prafta.web.subcon.subcon03.application.param.SnapshotDetailParam;
import com.prafta.web.subcon.subcon03.application.param.SnapshotFileParam;
import com.prafta.web.subcon.subcon03.dto.request.ShareReqApproveRequest;
import com.prafta.web.subcon.subcon03.dto.request.ShareReqCreateRequest;
import com.prafta.web.subcon.subcon03.dto.request.ShareReqProcessRequest;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqApproveInfoResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqApproveResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqCandidatesResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqCreateResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqListResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotDetailResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotListResponse;
import com.prafta.web.subcon.subcon03.service.Subcon03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 공유(Subcon_03 요청함 / Subcon_04 수신 자료) 컨트롤러 — PRAFTA-SUBCON-T3.
 *
 * <p>회사 스코프는 JWT 클레임(gv_cmpnyCd)로만 강제한다(클라 바디의 회사코드 불신).
 * 인증은 AuthAspect 가 처리하며, 화면/버튼 인가는 서비스의 TB_SYST_AUTH_MENU 게이트가 강제한다.
 */
@Slf4j
@RestController
@RequestMapping("/subcon03")
@RequiredArgsConstructor
public class Subcon03Controller {

    private final Subcon03Service subcon03Service;
    private final JwtUtil jwtUtil;

    /** 공유 요청 목록(자사 당사자 전 상태 — 프론트가 보낸/받은 2분류, 목록=이력). */
    @GetMapping("/share-req-lists")
    public ResponseEntity<?> getShareReqList(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShareReqListResponse response = subcon03Service.selectShareReqList(
                ShareScopeParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 요청 생성 후보(관계 ACCEPTED 상대 회사 + 그 회사와 사업장 체인이 있는 내 사업장). */
    @GetMapping("/share-req-candidates")
    public ResponseEntity<?> getShareReqCandidates(
            @RequestParam(value = "prvCmpnyCd", required = false) String prvCmpnyCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShareReqCandidatesResponse response = subcon03Service.selectShareReqCandidates(
                ShareReqCandidatesParam.from(prvCmpnyCd, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 요청 생성(가드 6종: 필수값·유형/자기회사/기간/관계/사업장 소유·체인/중복 — 전부 서버 강제). */
    @PostMapping("/share-req-create")
    public ResponseEntity<?> createShareReq(
            @RequestBody ShareReqCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShareReqCreateResponse response = subcon03Service.createShareReq(
                ShareReqCreateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 취소(REQUESTED→CANCELLED, 요청측 소속만). */
    @PostMapping("/share-req-cancel")
    public ResponseEntity<?> cancelShareReq(
            @RequestBody ShareReqProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon03Service.cancelShareReq(
                ShareReqProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 거부(REQUESTED→REJECTED, 제공측 소속만, 사유 필수 ≤500자). */
    @PostMapping("/share-req-reject")
    public ResponseEntity<?> rejectShareReq(
            @RequestBody ShareReqProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon03Service.rejectShareReq(
                ShareReqProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 승인 사전정보(마감 상태 + 미마감 월 + 릴레이 후보 — 제공측 승인 팝업 전용). */
    @GetMapping("/share-req-approve-info")
    public ResponseEntity<?> getShareReqApproveInfo(
            @RequestParam("shareReqId") Long shareReqId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShareReqApproveInfoResponse response = subcon03Service.selectApproveInfo(
                ShareReqApproveInfoParam.from(shareReqId, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 승인 = 스냅샷 생성(선점 → 관계·마감 재검사 → 헤더 → 상세행 → 릴레이 relabel 복사, 단일 트랜잭션). */
    @PostMapping("/share-req-approve")
    public ResponseEntity<?> approveShareReq(
            @RequestBody ShareReqApproveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShareReqApproveResponse response = subcon03Service.approveShareReq(
                ShareReqApproveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 수신 보유 스냅샷 목록(OWNER_CMPNY_CD = 자사 — Subcon_04). */
    @GetMapping("/snapshot-lists")
    public ResponseEntity<?> getSnapshotList(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        SnapshotListResponse response = subcon03Service.selectSnapshotList(
                ShareScopeParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 수신 스냅샷 상세(읽기전용 페이징 — 소유 검증은 조회 SQL 내부 강제). */
    @GetMapping("/snapshot-detail")
    public ResponseEntity<?> getSnapshotDetail(
            @RequestParam("snapshotId") Long snapshotId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        SnapshotDetailResponse response = subcon03Service.selectSnapshotDetail(
                SnapshotDetailParam.from(snapshotId, page, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** [T7] 위험성평가 수신 상세(평가행 + 개선항목 자식 — 읽기전용, 소유 검증 SQL 내부). */
    @GetMapping("/snapshot-risk-detail")
    public ResponseEntity<?> getSnapshotRiskDetail(
            @RequestParam("snapshotId") Long snapshotId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(subcon03Service.selectSnapshotRiskDetail(
                SnapshotDetailParam.from(snapshotId, null, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    /** [T7] 아차사고 수신 상세(사고 카드 — 읽기전용, 소유 검증 SQL 내부). */
    @GetMapping("/snapshot-nearmiss-detail")
    public ResponseEntity<?> getSnapshotNearmissDetail(
            @RequestParam("snapshotId") Long snapshotId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(subcon03Service.selectSnapshotNearmissDetail(
                SnapshotDetailParam.from(snapshotId, null, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    /**
     * [T7] 수신 스냅샷 첨부 서빙(§5-9 — IDOR 봉인).
     * 회사 스코프는 토큰 gv 만 사용(클라 파라미터 회사코드 금지). 소유+참조 검증은 서비스 SQL 내부.
     */
    @GetMapping("/snapshot-file")
    public ResponseEntity<byte[]> getSnapshotFile(
            @RequestParam("snapshotId") Long snapshotId,
            @RequestParam("fileMgmtCd") String fileMgmtCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        FileBytesResult file = subcon03Service.selectSnapshotFile(
                SnapshotFileParam.from(snapshotId, fileMgmtCd, jwtUtil.getAllClaimsAsMap(authorization)));

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(mediaType)
                .body(file.data());
    }
}
