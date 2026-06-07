package com.prafta.web.acct.acct01.service;

import com.prafta.web.acct.acct01.application.param.AcctCreateParam;
import com.prafta.web.acct.acct01.application.param.AcctDeleteParam;
import com.prafta.web.acct.acct01.application.param.AcctInfoParam;
import com.prafta.web.acct.acct01.application.param.AcctListParam;
import com.prafta.web.acct.acct01.application.param.AcctUpdateParam;
import com.prafta.web.acct.acct01.application.param.ChkptOptionParam;
import com.prafta.web.acct.acct01.application.param.LegalStepListParam;
import com.prafta.web.acct.acct01.application.param.LegalStepSaveParam;
import com.prafta.web.acct.acct01.application.param.LinkConfirmParam;
import com.prafta.web.acct.acct01.application.param.LinkQueryParam;
import com.prafta.web.acct.acct01.application.param.LinkSnapshotParam;
import com.prafta.web.acct.acct01.application.param.RiskCategoryOptionParam;
import com.prafta.web.acct.acct01.application.param.VictimSearchParam;
import com.prafta.web.acct.acct01.dto.response.AcctCreateResponse;
import com.prafta.web.acct.acct01.dto.response.AcctInfoResponse;
import com.prafta.web.acct.acct01.dto.response.AcctListResponse;
import com.prafta.web.acct.acct01.dto.response.AttendanceLinkResponse;
import com.prafta.web.acct.acct01.dto.response.ChkptOptionResponse;
import com.prafta.web.acct.acct01.dto.response.LegalStepHistoryResponse;
import com.prafta.web.acct.acct01.dto.response.LegalStepListResponse;
import com.prafta.web.acct.acct01.dto.response.LinkSnapshotResponse;
import com.prafta.web.acct.acct01.dto.response.NearMissLinkResponse;
import com.prafta.web.acct.acct01.dto.response.PatrolLinkResponse;
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

    // ── 048-04 연계 조회 ──
    AttendanceLinkResponse selectLinkAttendance(LinkQueryParam param);

    PatrolLinkResponse selectLinkPatrol(LinkQueryParam param);

    RiskLinkResponse selectLinkRisk(LinkQueryParam param);

    TbmLinkResponse selectLinkTbm(LinkQueryParam param);

    NearMissLinkResponse selectLinkNearMiss(LinkQueryParam param);

    ChkptOptionResponse selectChkptOptions(ChkptOptionParam param);

    RiskCategoryOptionResponse selectRiskCategoryOptions(RiskCategoryOptionParam param);

    // ── 048-05 스냅샷/법정절차 ──
    void confirmLink(LinkConfirmParam param);

    LinkSnapshotResponse selectLinkSnapshot(LinkSnapshotParam param);

    LegalStepListResponse selectLegalStepList(LegalStepListParam param);

    void saveLegalStep(LegalStepSaveParam param);

    LegalStepHistoryResponse selectLegalStepHistory(LegalStepListParam param);
}
