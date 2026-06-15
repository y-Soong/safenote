package com.prafta.app.notiset.notiset01.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.app.notiset.notiset01.application.PushNotiTypeConst;
import com.prafta.app.notiset.notiset01.application.PushNotiTypeConst.ToggleDef;
import com.prafta.app.notiset.notiset01.application.command.PushSettingUpsertCommand;
import com.prafta.app.notiset.notiset01.application.param.PushSettingQueryParam;
import com.prafta.app.notiset.notiset01.application.param.PushSettingSaveParam;
import com.prafta.app.notiset.notiset01.dto.response.PushSettingResponse;
import com.prafta.app.notiset.notiset01.dto.response.PushSettingResponse.PushSettingToggleResponse;
import com.prafta.app.notiset.notiset01.mapper.Notiset01Mapper;
import com.prafta.app.notiset.notiset01.mapper.result.PushSettingRowResult;
import com.prafta.app.notiset.notiset01.service.Notiset01Service;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 푸시 알림 수신 설정 서비스 구현 (PRAFTA-APP-021-1, 앱).
 *
 * <p>opt-out 모델: 행이 없는 NOTI_TYPE 은 ON 간주. USE_YN='N' 행이 하나라도 있으면 해당 토글 OFF.
 * 마스터 행('__MASTER__')이 'N' 이면 masterOn=false.
 *
 * <p>isAdmin = 전사역할(master/hr/safe) OR 전사 노드 정/부 관리자(existsNodeAdminAnySite).
 * isAdmin=false 면 응답에 관리자 토글(M*)을 포함하지 않고, 저장 시에도 관리자 토글을 무시한다(화이트리스트).
 * 읽기전용 항목(R*)은 항상 노출(savable=false)하되 저장은 거부한다(plan §8-R 7).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Notiset01ServiceImpl implements Notiset01Service {

    private final Notiset01Mapper notiset01Mapper;

    @Override
    public PushSettingResponse getMySettings(PushSettingQueryParam param) {
        boolean isAdmin = resolveIsAdmin(param.cmpnyCd(), param.userCd(), param.authCd());

        List<PushSettingRowResult> rows =
                notiset01Mapper.selectMySettings(param.cmpnyCd(), param.userCd());

        log.info("푸시 설정 조회 - userCd={}, isAdmin={}, 저장행={}건",
                param.userCd(), isAdmin, rows.size());

        return buildResponse(rows, isAdmin);
    }

    @Override
    @Transactional
    public PushSettingResponse saveMySettings(PushSettingSaveParam param) {
        boolean isAdmin = resolveIsAdmin(param.cmpnyCd(), param.userCd(), param.authCd());
        String actor = param.userCd();

        int affected = 0;

        // 1) 마스터 스위치(미변경이면 null → 저장 생략).
        if (param.masterOn() != null) {
            affected += notiset01Mapper.upsertSetting(PushSettingUpsertCommand.of(
                    param.cmpnyCd(), param.userCd(),
                    PushNotiTypeConst.MASTER_NOTI_TYPE, param.masterOn(), actor));
        }

        // 2) 토글 변경 — 서버 화이트리스트(isSavableToggle)로 관리자/읽기전용 거부 후 NOTI_TYPE 행으로 전개.
        for (PushSettingSaveParam.ToggleChange change : param.toggleChanges()) {
            if (!PushNotiTypeConst.isSavableToggle(change.toggleKey(), isAdmin)) {
                // 관리자 전용 토글을 비관리자가 보내거나, 읽기전용 항목/미정의 키 → 무시(가드).
                log.info("푸시 설정 저장 무시(비허용 토글) - userCd={}, toggleKey={}, isAdmin={}",
                        param.userCd(), change.toggleKey(), isAdmin);
                continue;
            }
            for (String notiType : PushNotiTypeConst.notiTypesOf(change.toggleKey())) {
                affected += notiset01Mapper.upsertSetting(PushSettingUpsertCommand.of(
                        param.cmpnyCd(), param.userCd(), notiType, change.on(), actor));
            }
        }

        log.info("푸시 설정 저장 완료 - userCd={}, isAdmin={}, upsert행={}건",
                param.userCd(), isAdmin, affected);

        // 3) 저장 후 최신 상태로 재조회 응답(클라 동기화 단일 출처).
        List<PushSettingRowResult> rows =
                notiset01Mapper.selectMySettings(param.cmpnyCd(), param.userCd());
        return buildResponse(rows, isAdmin);
    }

    /**
     * isAdmin 판정 — 전사역할(master/hr/safe) OR 전사 노드 정/부 관리자.
     * 전사역할이면 노드 조회를 생략(불필요 쿼리 회피).
     */
    private boolean resolveIsAdmin(String cmpnyCd, String userCd, String authCd) {
        if (AuthRoleUtils.canManageAllNodes(authCd)) {
            return true;
        }
        return notiset01Mapper.existsNodeAdminAnySite(cmpnyCd, userCd) == 1;
    }

    /**
     * 저장 행 + isAdmin → 응답 조립.
     * <ul>
     *   <li>masterOn = '__MASTER__' 행이 'N' 이 아니면 true(행 없음=ON).</li>
     *   <li>토글 on = 매핑 NOTI_TYPE 중 OFF('N') 행이 하나도 없으면 true.</li>
     *   <li>ADMIN 토글은 isAdmin 일 때만 항목에 포함. READONLY 는 항상 포함(savable=false).</li>
     * </ul>
     */
    private PushSettingResponse buildResponse(List<PushSettingRowResult> rows, boolean isAdmin) {
        // OFF('N') NOTI_TYPE 집합 + 마스터 OFF 여부 산출.
        Set<String> offNotiTypes = new HashSet<>();
        boolean masterOff = false;
        for (PushSettingRowResult row : rows) {
            if (!"N".equals(row.useYn())) {
                continue;
            }
            if (PushNotiTypeConst.MASTER_NOTI_TYPE.equals(row.notiType())) {
                masterOff = true;
            } else {
                offNotiTypes.add(row.notiType());
            }
        }

        List<PushSettingToggleResponse> items = new java.util.ArrayList<>();
        for (ToggleDef def : PushNotiTypeConst.allToggles()) {
            // 관리자 토글은 비관리자에게 미노출.
            if (def.getGroup() == PushNotiTypeConst.ToggleGroup.ADMIN && !isAdmin) {
                continue;
            }
            // 매핑 NOTI_TYPE 중 OFF 가 하나라도 있으면 토글 OFF.
            boolean on = def.getNotiTypes().stream().noneMatch(offNotiTypes::contains);
            boolean savable = PushNotiTypeConst.isSavableToggle(def.getToggleKey(), isAdmin);

            items.add(PushSettingToggleResponse.builder()
                    .toggleKey(def.getToggleKey())
                    .group(def.getGroup().name())
                    .on(on)
                    .savable(savable)
                    .build());
        }

        return PushSettingResponse.builder()
                .masterOn(!masterOff)
                .isAdmin(isAdmin)
                .items(items)
                .build();
    }
}
