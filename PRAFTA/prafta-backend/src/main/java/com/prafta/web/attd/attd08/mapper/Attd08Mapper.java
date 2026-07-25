package com.prafta.web.attd.attd08.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd08.application.query.AttdGpsTrailQuery;
import com.prafta.web.attd.attd08.application.query.AttdListsQuery;
import com.prafta.web.attd.attd08.result.AttdGpsTrailRow;
import com.prafta.web.attd.attd08.result.AttdListsResult;

@Mapper
public interface Attd08Mapper {

    List<AttdListsResult> selectAttdLists(AttdListsQuery query);

    /** GPS좌표-암호화-전환-03: 암호문+평문 병렬 행 조회(fallback 복호화는 서비스 계층). */
    List<AttdGpsTrailRow> selectAttdGpsTrail(AttdGpsTrailQuery query);
}
