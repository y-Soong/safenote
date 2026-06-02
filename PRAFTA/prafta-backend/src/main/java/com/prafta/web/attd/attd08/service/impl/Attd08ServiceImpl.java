package com.prafta.web.attd.attd08.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.attd.attd08.application.param.AttdGpsTrailParam;
import com.prafta.web.attd.attd08.application.param.AttdListsParam;
import com.prafta.web.attd.attd08.application.query.AttdGpsTrailQuery;
import com.prafta.web.attd.attd08.application.query.AttdListsQuery;
import com.prafta.web.attd.attd08.dto.response.AttdGpsTrailResponse;
import com.prafta.web.attd.attd08.dto.response.AttdListsResponse;
import com.prafta.web.attd.attd08.mapper.Attd08Mapper;
import com.prafta.web.attd.attd08.result.AttdGpsTrailResult;
import com.prafta.web.attd.attd08.result.AttdListsResult;
import com.prafta.web.attd.attd08.service.Attd08Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd08ServiceImpl implements Attd08Service {

    private final Attd08Mapper attd08Mapper;

    @Override
    public AttdListsResponse getAttdLists(AttdListsParam param) {

        List<AttdListsResult> attdListsResultList = attd08Mapper.selectAttdLists(AttdListsQuery.from(param));

        return AttdListsResponse.builder()
                .attdListsResultList(attdListsResultList)
                .build();
    }

    @Override
    public AttdGpsTrailResponse getAttdGpsTrail(AttdGpsTrailParam param) {

        List<AttdGpsTrailResult> attdGpsTrailResultList = attd08Mapper.selectAttdGpsTrail(AttdGpsTrailQuery.from(param));

        return AttdGpsTrailResponse.builder()
                .attdGpsTrailResultList(attdGpsTrailResultList)
                .build();
    }
}
