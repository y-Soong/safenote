package com.prafta.web.attd.attd12.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.prafta.web.attd.attd12.result.FraudAttdRowResult;
import com.prafta.web.attd.attd12.result.UserDeviceBaselineResult;
import com.prafta.web.attd.attd12.service.Attd12Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-003 C6 - 부정 출퇴근 의심 탐지 서비스 구현(on-view 대조).
 *
 * <p>규칙(작업지시서 §1 D6):
 *   <ol>
 *     <li>RULE1(결정타) — 같은 CMPNY_CD·WORK_YMD·CHECK_IN_DEVICE_UUID(NOT NULL) 그룹에서
 *         서로 다른 USER_CD 가 2명 이상이면 의심. 한 기기로 여러 계정이 출퇴근(D5: 공용기기 없음).</li>
 *     <li>RULE2(보조) — 사용자의 출근 기기가 본인 평소(baseline=로그인 이력) 기기집합과 다름.</li>
 *     <li>RULE3(보조) — 로그인 이력에 한 번도 없던 신규 기기에서 출근(전사 baseline 미관측).</li>
 *   </ol>
 *   RULE1 그룹에 속한 (날짜·기기)는 RULE2/3 에서 제외하여 중복 노출을 막는다. RULE3 가 RULE2 보다 우선.
 *
 * <p>deviceId 는 클라 제공값(위조 가능, 신뢰경계 밖) — 탐지 보조 신호일 뿐 차단/식별에 쓰지 않는다(D4 표시 전용).
 *   권한 게이트(canManageNode)와 cross-site IDOR 필터는 본 화면이 PII(사번/이름)를 노출하므로 서버에서 강제한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Attd12ServiceImpl implements Attd12Service {

    private final Attd12Mapper attd12Mapper;
    private final AttdCloseService attdCloseService;

    @Override
    public FraudAttdSuspectResponse getFraudAttdSuspects(FraudAttdSuspectParam param) {

        log.info("Attd_12 부정 출퇴근 의심 조회 진입 - workYm={}, siteCd={}, nodeCd={}, incSub={}, type={}",
                param.workYm(), param.siteCd(), param.nodeCd(), param.incSubNodeYn(), param.suspectType());

        // 권한 게이트 — master/hr/safe 전사 또는 노드 관리자만. 그 외/타부서는 차단(PII 노출 화면).
        //   프론트 가드는 우회 가능하므로 서버에서 강제(Attd_11 동일 패턴).
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("Attd_12 조회 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        FraudAttdSuspectQuery query = FraudAttdSuspectQuery.from(param);

        List<FraudAttdRowResult> rows = attd12Mapper.selectScopedAttdRows(query);
        List<UserDeviceBaselineResult> baseline = attd12Mapper.selectUserDeviceBaseline(query);

        // baseline 인덱스: 사용자별 기기집합 + 전사 관측 기기집합.
        Map<String, Set<String>> baselineByUser = new java.util.HashMap<>();
        Set<String> allKnownDevices = new HashSet<>();
        for (UserDeviceBaselineResult b : baseline) {
            if (b.deviceUuid() == null) {
                continue;
            }
            baselineByUser.computeIfAbsent(b.userCd(), k -> new HashSet<>()).add(b.deviceUuid());
            allKnownDevices.add(b.deviceUuid());
        }

        List<FraudSuspectRow> result = new ArrayList<>();

        // ── RULE1: (WORK_YMD, CHECK_IN_DEVICE_UUID) 그룹에서 distinct USER_CD >= 2 ──
        //   그룹 등장 순서 유지(매퍼가 WORK_YMD, deviceUuid 순 정렬). 그룹키 = ymd + '' + device.
        Map<String, List<FraudAttdRowResult>> deviceDayGroups = new LinkedHashMap<>();
        for (FraudAttdRowResult r : rows) {
            if (r.checkInDeviceUuid() == null || r.checkInDeviceUuid().isEmpty()) {
                continue; // 출근 기기 미기록(웹 등록/구버전 앱)은 규칙1 대상 아님.
            }
            String key = r.workYmd() + "" + r.checkInDeviceUuid();
            deviceDayGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        // RULE1 로 묶인 (날짜·기기) 집합 — RULE2/3 중복 노출 방지용.
        Set<String> rule1Keys = new HashSet<>();
        int seq = 0;
        for (Map.Entry<String, List<FraudAttdRowResult>> e : deviceDayGroups.entrySet()) {
            List<FraudAttdRowResult> grp = e.getValue();
            Set<String> distinctUsers = new HashSet<>();
            for (FraudAttdRowResult r : grp) {
                distinctUsers.add(r.userCd());
            }
            if (distinctUsers.size() < 2) {
                continue; // 같은 기기라도 단일 계정이면 정상.
            }
            rule1Keys.add(e.getKey());

            FraudAttdRowResult head = grp.get(0);
            // 사용자별로 1명씩 멤버 구성(같은 사용자가 여러 차수면 첫 행 채택 — 대표 출퇴근 시각).
            Map<String, FraudSuspectMember> memberByUser = new LinkedHashMap<>();
            for (FraudAttdRowResult r : grp) {
                memberByUser.computeIfAbsent(r.userCd(), k -> FraudSuspectMember.builder()
                        .userCd(r.userCd())
                        .userId(r.userId())
                        .userNm(r.userNm())
                        .checkInTime(r.checkInTime())
                        .checkOutTime(r.checkOutTime())
                        .build());
            }
            result.add(FraudSuspectRow.builder()
                    .suspectKey("R1-" + (seq++))
                    .workYmd(head.workYmd())
                    .deviceUuid(head.checkInDeviceUuid())
                    .suspectType("RULE1")
                    .nodeNm(head.nodeNm())
                    .siteNm(head.siteNm())
                    .members(new ArrayList<>(memberByUser.values()))
                    .build());
        }

        // ── RULE2/RULE3: 행별 baseline 대조(출근 기기 기준). RULE1 그룹(날짜·기기)은 건너뛴다. ──
        //   중복 방지: 같은 (userCd, ymd, device) 1회만.
        Set<String> seenSingle = new HashSet<>();
        for (FraudAttdRowResult r : rows) {
            String device = r.checkInDeviceUuid();
            if (device == null || device.isEmpty()) {
                continue;
            }
            String dayDeviceKey = r.workYmd() + "" + device;
            if (rule1Keys.contains(dayDeviceKey)) {
                continue; // 이미 RULE1 로 표기됨.
            }
            String singleKey = r.userCd() + "" + dayDeviceKey;
            if (!seenSingle.add(singleKey)) {
                continue;
            }

            String type;
            if (!allKnownDevices.contains(device)) {
                // 로그인 이력에 한 번도 없던 기기.
                type = "RULE3";
            } else {
                Set<String> userDevices = baselineByUser.get(r.userCd());
                if (userDevices != null && !userDevices.isEmpty() && !userDevices.contains(device)) {
                    // 본인 평소 기기집합에 없음(다른 사람이 쓰던 기기에서 출근).
                    type = "RULE2";
                } else {
                    continue; // 평소 기기와 일치하거나 baseline 부재 → 의심 아님.
                }
            }

            result.add(FraudSuspectRow.builder()
                    .suspectKey("RS-" + (seq++))
                    .workYmd(r.workYmd())
                    .deviceUuid(device)
                    .suspectType(type)
                    .nodeNm(r.nodeNm())
                    .siteNm(r.siteNm())
                    .members(List.of(FraudSuspectMember.builder()
                            .userCd(r.userCd())
                            .userId(r.userId())
                            .userNm(r.userNm())
                            .checkInTime(r.checkInTime())
                            .checkOutTime(r.checkOutTime())
                            .build()))
                    .build());
        }

        // 의심유형 필터(요청 시).
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

        log.info("Attd_12 부정 출퇴근 의심 조회 종료 - 의심 케이스 {}건(필터 적용 후)", filtered.size());

        return FraudAttdSuspectResponse.builder()
                .fraudSuspectRowList(filtered)
                .build();
    }
}
