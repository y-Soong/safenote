package com.prafta.web.subcon.subcon03.service;

import com.prafta.web.subcon.subcon03.application.param.ShareReqApproveInfoParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqApproveParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqCandidatesParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqCreateParam;
import com.prafta.web.subcon.subcon03.application.param.ShareReqProcessParam;
import com.prafta.web.subcon.subcon03.application.param.ShareScopeParam;
import com.prafta.web.subcon.subcon03.application.param.SnapshotDetailParam;
import com.prafta.web.subcon.subcon03.application.param.SnapshotFileParam;
import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqApproveInfoResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqApproveResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqCandidatesResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqCreateResponse;
import com.prafta.web.subcon.subcon03.dto.response.ShareReqListResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotDetailResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotListResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotNearmissDetailResponse;
import com.prafta.web.subcon.subcon03.dto.response.SnapshotRiskDetailResponse;

public interface Subcon03Service {

    /** 공유 요청 목록(자사 당사자 전 상태 — 프론트 보낸/받은 2분류, 목록=이력). */
    ShareReqListResponse selectShareReqList(ShareScopeParam param);

    /** 요청 생성 후보(관계 ACCEPTED 상대 회사 + 선택 회사와 체인이 있는 내 사업장 — §5-2). */
    ShareReqCandidatesResponse selectShareReqCandidates(ShareReqCandidatesParam param);

    /** 요청 생성(§5-3 가드 6종: 필수값/자기회사/관계/사업장체인/기간/중복). */
    ShareReqCreateResponse createShareReq(ShareReqCreateParam param);

    /** 취소(REQUESTED→CANCELLED, 요청측 소속만). */
    void cancelShareReq(ShareReqProcessParam param);

    /** 거부(REQUESTED→REJECTED, 제공측 소속만, 사유 필수 ≤500자). */
    void rejectShareReq(ShareReqProcessParam param);

    /** 승인 사전정보(마감 상태 + 미마감 월 + 릴레이 후보 — §5-4·§5-7). */
    ShareReqApproveInfoResponse selectApproveInfo(ShareReqApproveInfoParam param);

    /** 승인 = 스냅샷 생성 트랜잭션(선점 → 관계/마감 재검사 → 헤더 → 상세행 → 릴레이 — §5-6). */
    ShareReqApproveResponse approveShareReq(ShareReqApproveParam param);

    /** 수신 보유 스냅샷 목록(OWNER_CMPNY_CD = 자사 — §5-8). */
    SnapshotListResponse selectSnapshotList(ShareScopeParam param);

    /** 수신 스냅샷 상세(읽기전용 페이징 — 소유 검증은 SQL 내부 강제). */
    SnapshotDetailResponse selectSnapshotDetail(SnapshotDetailParam param);

    /** [T7] 위험성평가 수신 상세(평가행 + 개선항목 자식 — 읽기전용, 소유 검증 SQL 내부). */
    SnapshotRiskDetailResponse selectSnapshotRiskDetail(SnapshotDetailParam param);

    /** [T7] 아차사고 수신 상세(사고 카드 — 읽기전용, 소유 검증 SQL 내부). */
    SnapshotNearmissDetailResponse selectSnapshotNearmissDetail(SnapshotDetailParam param);

    /** [T7] 수신 스냅샷 첨부 바이트 서빙(소유+참조 검증 — IDOR 봉인). 없으면 SUBCON_404_007. */
    FileBytesResult selectSnapshotFile(SnapshotFileParam param);
}
