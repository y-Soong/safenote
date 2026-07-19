package com.prafta.web.tbm.tbmai02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.tbm.tbmai02.application.model.TbmSessionGenSource;
import com.prafta.web.tbm.tbmai02.application.model.TbmSessionRiskRow;
import com.prafta.web.tbm.tbmai02.application.model.TbmUnconfirmedAiItem;

@Mapper
public interface TbmAi02Mapper {

    // 교육안 생성 입력 원천(회사 소유 세션 + 제목/사업장/상태). 없으면 null → TBM_404_010(존재 미노출)
    TbmSessionGenSource selectSessionGenSource(
        @Param("sessionCd") String sessionCd
        , @Param("cmpnyCd") String cmpnyCd
    );

    // 세션에 묶인 교육자료 항목 중 CONFIRMED 且 AI_CONFIRM_DESC 비공백 확정 서술 통합
    // (세션→세션콘텐츠→자료→항목 4조인, DISPLAY_ORDER,SORT_IDX 순)
    List<String> selectSessionConfirmedItemDescs(
        @Param("sessionCd") String sessionCd
        , @Param("cmpnyCd") String cmpnyCd
    );

    // 세션에 묶인 교육자료 항목 중 AI 분석 지정(AI_ANALYZE_YN='Y')됐지만 미확정인 항목 목록
    // (세션→세션콘텐츠→자료→항목 4조인, DISPLAY_ORDER,SORT_IDX 순). 교육안 생성 사전 차단 안내용
    List<TbmUnconfirmedAiItem> selectSessionUnconfirmedAiItems(
        @Param("sessionCd") String sessionCd
        , @Param("cmpnyCd") String cmpnyCd
    );

    // 세션 매핑 위험성평가 + 개선항목 flat 조회(서비스에서 평가 키 단위 그룹핑). 교육안 생성 재료용
    List<TbmSessionRiskRow> selectSessionRiskRows(
        @Param("sessionCd") String sessionCd
        , @Param("cmpnyCd") String cmpnyCd
    );

    // 사업장 격리: 사용자가 대상 사업장(SITE_CD) 권한(tb_user_site_auth USE_YN='Y')을 보유하는지 카운트. >0 이면 접근 허용
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );
}
