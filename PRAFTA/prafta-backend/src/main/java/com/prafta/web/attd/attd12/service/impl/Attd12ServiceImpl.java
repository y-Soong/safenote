package com.prafta.web.attd.attd12.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd12.application.param.FraudAttdSuspectParam;
import com.prafta.web.attd.attd12.application.query.FraudAttdSuspectQuery;
import com.prafta.web.attd.attd12.dto.response.FraudAttdSuspectResponse;
import com.prafta.web.attd.attd12.dto.response.FraudSuspectMember;
import com.prafta.web.attd.attd12.dto.response.FraudSuspectRow;
import com.prafta.web.attd.attd12.mapper.Attd12Mapper;
import com.prafta.web.attd.attd12.result.SharedDeviceLoginResult;
import com.prafta.web.attd.attd12.service.Attd12Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-016-F 9-1 - 부정 출퇴근(기기 공유) 의심 탐지 서비스 구현.
 *
 * <p>판정 데이터 출처를 출퇴근 시 도장되는 CHECK_IN_DEVICE_UUID(대부분 NULL)에서
 *   로그인마다 DEVICE_UUID(NOT NULL)가 적재되는 tb_user_device_login_hist 로 전환했다.
 *
 * <p>판정 룰(단일): 한 사용자가 APP 로그인한 기기를 그 로그인 직전 7일 내에 다른 사용자가
 *   APP 로그인한 적이 있으면 → 부정사용(기기 공유/대리) 의심. 의심유형은 SHARED_DEVICE 단일값.
 *   (기존 check-in 기반 RULE1/2/3 은 device UUID 가 거의 비어 미발화하여 폐기. 죽은 쿼리/메서드/baseline
 *   로직은 매퍼·result 와 함께 제거했다.)
 *
 * <p>표시 모델: 기기 중심(1 기기 → N 사용자). 매퍼가 의심 기기에 관여한 로그인 이력을 평탄 행으로
 *   내려주면 DEVICE_UUID 로 그룹핑해 FraudSuspectRow(기기 + members[사용자·로그인시각])로 조립한다.
 *
 * <p>deviceId 는 클라 제공값(위조 가능, 신뢰경계 밖) — 탐지 보조 신호일 뿐 차단/식별에 쓰지 않는다.
 *   권한 게이트(canManageNode)와 cross-site IDOR 필터는 본 화면이 PII(사번/이름)를 노출하므로 서버에서 강제한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Attd12ServiceImpl implements Attd12Service {

    private static final String SUSPECT_TYPE_SHARED_DEVICE = "SHARED_DEVICE";

    private final Attd12Mapper attd12Mapper;
    private final AttdCloseService attdCloseService;
    /** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
    private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;

    @Override
    public FraudAttdSuspectResponse getFraudAttdSuspects(FraudAttdSuspectParam param) {

        log.info("Attd_12 부정 출퇴근(기기 공유) 의심 조회 진입 - workYm={}, siteCd={}, nodeCd={}, incSub={}, type={}",
                param.workYm(), param.siteCd(), param.nodeCd(), param.incSubNodeYn(), param.suspectType());

        // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체 — User_03 원장 기반).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 권한 게이트 — master/hr/safe 전사 또는 노드 관리자만. 그 외/타부서는 차단(PII 노출 화면).
        //   프론트 가드는 우회 가능하므로 서버에서 강제(Attd_11 동일 패턴).
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("Attd_12 조회 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        FraudAttdSuspectQuery query = FraudAttdSuspectQuery.from(param);

        // 매퍼가 "공유 의심 기기"에 관여한 APP 로그인 이력을 DEVICE_UUID, LOGIN_DTIME 순으로 평탄 반환.
        List<SharedDeviceLoginResult> loginRows = attd12Mapper.selectSharedDeviceLoginSuspects(query);

        // 기기(DEVICE_UUID) 단위로 그룹핑 → 1 기기 → N 사용자(members) 표시 모델.
        //   등장 순서 유지(매퍼 정렬). 대표 부서/사업장은 그룹 첫 로그인 기준.
        Map<String, FraudSuspectRow.FraudSuspectRowBuilder> rowBuilders = new LinkedHashMap<>();
        Map<String, List<FraudSuspectMember>> membersByDevice = new LinkedHashMap<>();

        for (SharedDeviceLoginResult r : loginRows) {
            if (r.deviceUuid() == null || r.deviceUuid().isEmpty()) {
                continue; // login_hist DEVICE_UUID 는 NOT NULL 이나 방어적으로 가드.
            }
            rowBuilders.computeIfAbsent(r.deviceUuid(), k -> FraudSuspectRow.builder()
                    .suspectKey("SD-" + k)
                    .deviceUuid(k)
                    .suspectType(SUSPECT_TYPE_SHARED_DEVICE)
                    .nodeNm(r.nodeNm())
                    .siteNm(r.siteNm()));
            membersByDevice.computeIfAbsent(r.deviceUuid(), k -> new ArrayList<>())
                    .add(FraudSuspectMember.builder()
                            .userCd(r.userCd())
                            .userId(r.userId())
                            .userNm(r.userNm())
                            .loginDtime(r.loginDtime())
                            .clientType(r.clientType())
                            .build());
        }

        List<FraudSuspectRow> result = new ArrayList<>();
        for (Map.Entry<String, FraudSuspectRow.FraudSuspectRowBuilder> e : rowBuilders.entrySet()) {
            result.add(e.getValue()
                    .members(membersByDevice.getOrDefault(e.getKey(), new ArrayList<>()))
                    .build());
        }

        // 의심유형 필터(요청 시). 현재 단일값(SHARED_DEVICE)이지만 FE 셀렉트(전체/SHARED_DEVICE) 계약 유지.
        final String filter = param.suspectType();
        List<FraudSuspectRow> filtered;
        if (filter == null || filter.isEmpty()) {
            filtered = result;
        } else {
            filtered = new ArrayList<>();
            for (FraudSuspectRow row : result) {
                if (filter.equals(row.getSuspectType())) {
                    filtered.add(row);
                }
            }
        }

        log.info("Attd_12 부정 출퇴근(기기 공유) 의심 조회 종료 - 의심 기기 {}건(필터 적용 후)", filtered.size());

        return FraudAttdSuspectResponse.builder()
                .fraudSuspectRowList(filtered)
                .build();
    }
}
