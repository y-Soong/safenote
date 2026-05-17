package com.prafta.web.attd.attd08.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd08.application.query.AttdGpsTrailQuery;
import com.prafta.web.attd.attd08.application.query.AttdListsQuery;
import com.prafta.web.attd.attd08.result.AttdGpsTrailResult;
import com.prafta.web.attd.attd08.result.AttdListsResult;

@Mapper
public interface Attd08Mapper {

    List<AttdListsResult> selectAttdLists(AttdListsQuery query);

    List<AttdGpsTrailResult> selectAttdGpsTrail(AttdGpsTrailQuery query);
}
