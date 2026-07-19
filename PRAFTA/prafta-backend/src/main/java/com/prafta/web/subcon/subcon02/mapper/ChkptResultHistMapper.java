package com.prafta.web.subcon.subcon02.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 순회점검 결과 덮어쓰기 감사 이력(append-only) 캡처 매퍼(PRAFTA-SUBCON-T6-AUDIT-02).
 *
 * <p>write 경로(자체저장 + 전파)가 응답/불량조치를 쓸 때마다, 방금 쓴 행을 그대로 읽어 HIST 로 1행 append 한다
 * (INSERT ... SELECT — 사용자 입력 미경유, 서버 데이터만 복제하므로 주입 면 없음).
 *
 * <p><b>append-only 규율</b>: 본 매퍼에는 HIST 테이블을 대상으로 하는 UPDATE/DELETE 를 <b>절대</b> 두지 않는다
 * (감사 로그 변조 불가 — 코드리뷰/security 체크 항목). 존재여부 조회는 CHG_TYPE(신규/덮어쓰기) 판정용 PK point-lookup 이다.
 */
@Mapper
public interface ChkptResultHistMapper {

    /** 응답 좌표 존재여부(PK point-lookup) — write 직전 호출하여 CHG_TYPE(신규 01 / 덮어쓰기 02)을 판정한다. */
    int selectExistsAnswer(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("chkptCd") String chkptCd, @Param("inspectItemCd") String inspectItemCd,
            @Param("workDate") String workDate);

    /** 응답 스냅샷 append — write 직후 그 좌표의 방금 쓴 행(relabel/사진 스냅샷 포함)을 HIST 로 복제한다. */
    void insertAnswerHist(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("chkptCd") String chkptCd, @Param("inspectItemCd") String inspectItemCd,
            @Param("workDate") String workDate, @Param("chgType") String chgType,
            @Param("insertNo") String insertNo);

    /** 불량조치 좌표 존재여부(PK point-lookup) — write 직전 호출. */
    int selectExistsDefectAction(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("chkptCd") String chkptCd, @Param("inspectItemCd") String inspectItemCd,
            @Param("workDate") String workDate);

    /** 불량조치 스냅샷 append — write 직후 그 좌표의 방금 쓴 행을 HIST 로 복제한다. */
    void insertDefectActionHist(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("chkptCd") String chkptCd, @Param("inspectItemCd") String inspectItemCd,
            @Param("workDate") String workDate, @Param("chgType") String chgType,
            @Param("insertNo") String insertNo);
}
