package com.prafta.web.attd.attd08.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd08.application.query.AttdGpsTrailQuery;
import com.prafta.web.attd.attd08.application.query.AttdListsQuery;
import com.prafta.web.attd.attd08.result.AttdGpsTrailRow;
import com.prafta.web.attd.attd08.result.AttdListsResult;
import com.prafta.web.attd.attd08.result.AttdOwnerScopeResult;
import com.prafta.web.attd.attd08.result.HalfLeaveWindowResult;

@Mapper
public interface Attd08Mapper {

    List<AttdListsResult> selectAttdLists(AttdListsQuery query);

    /**
     * HB-05(D1)/D-3: 조회기간 안의 확정 반차 면제 구간 목록(한 날 다건 가능 — 집계하지 않는다).
     * 지각·조퇴 판정용 유효 소정 시각은 서비스가 {@code PartialLeaveWindowUtils} 로 산출한다.
     */
    List<HalfLeaveWindowResult> selectHalfLeaveWindows(AttdListsQuery query);

    /**
     * A안(2026-08-17): 조회기간 안의 확정 "시각 보유" 연차(반차 01 + 시간차 02/03/04) 구간 목록.
     * FE 실근로/인정시간 표시의 겹침 차감용(연차 시간은 근로시간 미산입). record 는 동일 4컬럼이라 재사용.
     */
    List<HalfLeaveWindowResult> selectTimeLeaveWindows(AttdListsQuery query);

    /** security H-1: GPS 궤적 인가 판정용 근태 행 스코프(사업장/부서/소유자). 없으면 null. */
    AttdOwnerScopeResult selectAttdOwnerScope(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("attdId") String attdId);

    /** GPS좌표-암호화-전환-03: 암호문+평문 병렬 행 조회(fallback 복호화는 서비스 계층). */
    List<AttdGpsTrailRow> selectAttdGpsTrail(AttdGpsTrailQuery query);
}
