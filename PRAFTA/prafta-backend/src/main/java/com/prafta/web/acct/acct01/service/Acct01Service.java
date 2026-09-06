package com.prafta.web.acct.acct01.service;

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
import com.prafta.web.acct.acct01.dto.response.AcctCreateResponse;
import com.prafta.web.acct.acct01.dto.response.AcctInfoResponse;
import com.prafta.web.acct.acct01.dto.response.AcctListResponse;
import com.prafta.web.acct.acct01.dto.response.AcctVictimAddResponse;
import com.prafta.web.acct.acct01.dto.response.AcctVictimListResponse;
import com.prafta.web.acct.acct01.dto.response.AttdTbmPrintResponse;
import com.prafta.web.acct.acct01.dto.response.AttendanceLinkResponse;
import com.prafta.web.acct.acct01.dto.response.ChkptOptionResponse;
import com.prafta.web.acct.acct01.dto.response.LegalStepHistoryResponse;
import com.prafta.web.acct.acct01.dto.response.LegalStepListResponse;
import com.prafta.web.acct.acct01.dto.response.LinkSnapshotResponse;
import com.prafta.web.acct.acct01.dto.response.PatrolLinkResponse;
import com.prafta.web.acct.acct01.result.RiskAssessmentDetailResult;
import com.prafta.web.acct.acct01.dto.response.RiskCategoryOptionResponse;
import com.prafta.web.acct.acct01.dto.response.RiskLinkResponse;
import com.prafta.web.acct.acct01.dto.response.TbmLinkResponse;
import com.prafta.web.acct.acct01.dto.response.VictimSearchResponse;

public interface Acct01Service {

    // ── 048-03 CRUD ──
    AcctListResponse selectAcctList(AcctListParam param);

    AcctInfoResponse selectAcctInfo(AcctInfoParam param);

    AcctCreateResponse createAcct(AcctCreateParam param);

    void updateAcct(AcctUpdateParam param);

    void deleteAcct(AcctDeleteParam param);

    VictimSearchResponse searchVictim(VictimSearchParam param);

    // ── 065 재해자 (사고 1:N) ──
    // 재해자 목록 (대표 여부 포함)
    AcctVictimListResponse selectAcctVictims(AcctVictimListParam param);

    // 재해자 추가 (MAX+1 채번, 동일 인물 차단, 상한 50)
    AcctVictimAddResponse addVictim(AcctVictimAddParam param);

    // 재해자 속성 수정 (인물 불변)
    void updateVictimAttr(AcctVictimUpdateParam param);

    // 재해자 제외 (하한 1명, 대표 제외 시 승계)
    void removeVictim(AcctVictimRemoveParam param);

    // ── 048-04 연계 조회 ──
    AttendanceLinkResponse selectLinkAttendance(LinkQueryParam param);

    PatrolLinkResponse selectLinkPatrol(LinkQueryParam param);

    RiskLinkResponse selectLinkRisk(LinkQueryParam param);

    TbmLinkResponse selectLinkTbm(LinkQueryParam param);

    // ── T8 안전관리 현황 일괄 출력 ──
    // ③ 근태(스케줄+실근태) + TBM 합본 집계 (사고 피해자 본인 한정, 라이브 재조회)
    AttdTbmPrintResponse selectAttdTbmPrint(AttdTbmPrintParam param);

    // ② 위험성평가 출력 상세 보강 (개선실행계획서/개선완료보고서, assessmentCd 연계 검증 후 라이브 재조회)
    RiskAssessmentDetailResult selectRiskAssessmentForPrint(RiskAssessmentPrintParam param);

    ChkptOptionResponse selectChkptOptions(ChkptOptionParam param);

    RiskCategoryOptionResponse selectRiskCategoryOptions(RiskCategoryOptionParam param);

    // ── 048-05 스냅샷/법정절차 ──
    void confirmLink(LinkConfirmParam param);

    LinkSnapshotResponse selectLinkSnapshot(LinkSnapshotParam param);

    LegalStepListResponse selectLegalStepList(LegalStepListParam param);

    void saveLegalStep(LegalStepSaveParam param);

    LegalStepHistoryResponse selectLegalStepHistory(LegalStepListParam param);
}
