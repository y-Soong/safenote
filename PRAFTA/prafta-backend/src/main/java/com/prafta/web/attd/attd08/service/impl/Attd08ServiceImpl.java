package com.prafta.web.attd.attd08.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.security.crypto.GpsCoordCrypto;
import com.prafta.web.attd.attd08.application.param.AttdGpsTrailParam;
import com.prafta.web.attd.attd08.application.param.AttdListsParam;
import com.prafta.web.attd.attd08.application.query.AttdGpsTrailQuery;
import com.prafta.web.attd.attd08.application.query.AttdListsQuery;
import com.prafta.web.attd.attd08.dto.response.AttdGpsTrailResponse;
import com.prafta.web.attd.attd08.dto.response.AttdListsResponse;
import com.prafta.web.attd.attd08.mapper.Attd08Mapper;
import com.prafta.web.attd.attd08.result.AttdGpsTrailResult;
import com.prafta.web.attd.attd08.result.AttdGpsTrailRow;
import com.prafta.web.attd.attd08.result.AttdListsResult;
import com.prafta.web.attd.attd08.service.Attd08Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd08ServiceImpl implements Attd08Service {

    private final Attd08Mapper attd08Mapper;

    /** GPS좌표-암호화-전환-03: 좌표 fallback 복호화(ENC 우선, NULL 이면 구 평문). */
    private final GpsCoordCrypto gpsCoordCrypto;

    @Override
    public AttdListsResponse getAttdLists(AttdListsParam param) {

        List<AttdListsResult> attdListsResultList = attd08Mapper.selectAttdLists(AttdListsQuery.from(param));

        return AttdListsResponse.builder()
                .attdListsResultList(attdListsResultList)
                .build();
    }

    @Override
    public AttdGpsTrailResponse getAttdGpsTrail(AttdGpsTrailParam param) {

        List<AttdGpsTrailRow> rows = attd08Mapper.selectAttdGpsTrail(AttdGpsTrailQuery.from(param));

        // GPS좌표-암호화-전환-03: 행 단위 fallback 복호화(ENC 우선, NULL 이면 구 평문) 후
        // 기존 AttdGpsTrailResult 로 재조립 — 응답 구조/필드명/타입 불변(웹 3개 소비처 무수정).
        // 좌표 평문/복호화값은 로그에 출력하지 않는다.
        List<AttdGpsTrailResult> attdGpsTrailResultList = new ArrayList<>();
        for (AttdGpsTrailRow row : rows) {
            attdGpsTrailResultList.add(new AttdGpsTrailResult(
                    row.gpsId()
                    , gpsCoordCrypto.resolveToBigDecimal(row.latEnc(), row.lat())
                    , gpsCoordCrypto.resolveToBigDecimal(row.lonEnc(), row.lon())
                    , row.accuracy()
                    , row.apiCallDate()
                    , row.apiCallTime()
                    , row.isMocked()
                    , row.gpsInfoType()
            ));
        }

        return AttdGpsTrailResponse.builder()
                .attdGpsTrailResultList(attdGpsTrailResultList)
                .build();
    }
}
