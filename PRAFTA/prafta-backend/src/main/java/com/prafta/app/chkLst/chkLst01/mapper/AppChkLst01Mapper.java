package com.prafta.app.chkLst.chkLst01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.chkLst.chkLst01.application.command.InspectResultSaveCommand;
import com.prafta.app.chkLst.chkLst01.application.query.ChecklistInfoQuery;
import com.prafta.app.chkLst.chkLst01.result.ChecklistInfoResult;
import com.prafta.common.dto.TokenInfo;

@Mapper
public interface AppChkLst01Mapper {

    /**
     * prafta-036-B1: 체크리스트 정보 조회.
     * prafta-app-011: TB_SITE 조인으로 SITE_NM / CHKPT_NM 포함.
     */
    List<ChecklistInfoResult> selectChkLstInfo(
            @Param(value = "param") ChecklistInfoQuery query
            , @Param(value = "token") TokenInfo tokenInfo
    );

    /**
     * prafta-app-011: 토큰 사업장명 조회 — 403 차단 시 메시지 문구 구성용.
     * @return SITE_NM (없으면 null)
     */
    String selectSiteNm(
            @Param(value = "cmpnyCd") String cmpnyCd
            , @Param(value = "siteCd") String siteCd
    );

    /**
     * prafta-036-B1: 점검답변 UPSERT.
     */
    void mergeChkptInspectAnswer(
            @Param(value = "param") InspectResultSaveCommand command
            , @Param(value = "token") TokenInfo tokenInfo
    );
}
