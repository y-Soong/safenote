package com.prafta.app.chkLst.chkLst01.service;

import com.prafta.app.chkLst.chkLst01.application.param.ChecklistInfoParam;
import com.prafta.app.chkLst.chkLst01.application.param.InspectResultSaveParam;
import com.prafta.app.chkLst.chkLst01.dto.response.ChecklistInfoResponse;
import com.prafta.app.chkLst.chkLst01.dto.response.SaveInspectResultResponse;

/**
 * prafta-036-B1: chkLst01 서비스 인터페이스.
 */
public interface AppChkLst01Service {

    /**
     * 체크리스트 정보 조회.
     */
    ChecklistInfoResponse selectChkLstInfo(ChecklistInfoParam param);

    /**
     * 점검결과 저장(multipart).
     * <p>prafta-app-011: 저장 요약(chkptName/okCount/badCount/savedCount/workDate) 반환.
     */
    SaveInspectResultResponse saveInspectResult(InspectResultSaveParam param);
}
