package com.prafta.web.user.user04.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user04.application.param.ApprovalCandidateParam;
import com.prafta.web.user.user04.application.param.PresetActionParam;
import com.prafta.web.user.user04.application.param.PresetSaveParam;
import com.prafta.web.user.user04.dto.response.ApprovalCandidateListResponse;
import com.prafta.web.user.user04.dto.response.PresetItemResult;
import com.prafta.web.user.user04.dto.response.PresetListResponse;
import com.prafta.web.user.user04.dto.response.PresetStepItem;
import com.prafta.web.user.user04.mapper.User04Mapper;
import com.prafta.web.user.user04.result.ApprovalCandidateResult;
import com.prafta.web.user.user04.result.AprvPresetStepVO;
import com.prafta.web.user.user04.result.AprvPresetVO;
import com.prafta.web.user.user04.service.User04Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link User04Service} 구현 (prafta-019-D, prafta-020).
 *
 * <p>프리셋은 로그인 사용자 본인 소유(CMPNY_CD + USER_CD). 모든 CRUD는 토큰 회사/사용자
 * 스코프로 격리하고, 단건 액션은 소유권을 재확인한다(IDOR 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class User04ServiceImpl implements User04Service {

    private final User04Mapper user04Mapper;

    @Override
    public ApprovalCandidateListResponse getApprovalCandidates(ApprovalCandidateParam param) {
        Integer myRankSortIdx = user04Mapper.selectUserRankSortIdx(param.gvCmpnyCd(), param.gvUserCd());

        List<ApprovalCandidateResult> candidates = user04Mapper.selectApprovalCandidates(
                param.gvCmpnyCd(), param.gvSiteCd(), param.gvUserCd(), param.nodeCd(), param.userNm());

        return ApprovalCandidateListResponse.builder()
                .myRankSortIdx(myRankSortIdx)
                .candidates(candidates)
                .build();
    }

    // ============================================================
    // 프리셋 CRUD (prafta-020)
    // ============================================================

    @Override
    public PresetListResponse getPresets(String cmpnyCd, String userCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || userCd == null || userCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        List<AprvPresetVO> masters = user04Mapper.selectPresetMasters(cmpnyCd, userCd);
        List<AprvPresetStepVO> steps = user04Mapper.selectPresetStepsByUser(cmpnyCd, userCd);

        // presetId → 스텝 목록 (마스터 순서 보존은 masters 순회로 처리)
        Map<String, List<PresetStepItem>> stepsByPreset = new LinkedHashMap<>();
        for (AprvPresetStepVO s : steps) {
            stepsByPreset.computeIfAbsent(s.presetId(), k -> new ArrayList<>())
                    .add(PresetStepItem.builder()
                            .stepNo(s.stepNo())
                            .approverUserCd(s.approverUserCd())
                            .userNm(s.userNm())
                            .userId(s.userId())
                            .build());
        }

        List<PresetItemResult> presets = new ArrayList<>(masters.size());
        for (AprvPresetVO m : masters) {
            presets.add(PresetItemResult.builder()
                    .presetId(m.presetId())
                    .presetNm(m.presetNm())
                    .defaultYn(m.defaultYn())
                    .steps(stepsByPreset.getOrDefault(m.presetId(), new ArrayList<>()))
                    .build());
        }
        return PresetListResponse.builder().presets(presets).build();
    }

    @Override
    @Transactional
    public String savePreset(PresetSaveParam param) {
        String cmpnyCd = param.gvCmpnyCd();
        String userCd = param.gvUserCd();
        String siteCd = param.gvSiteCd();

        // 1. 입력 검증
        if (param.presetNm() == null || param.presetNm().isBlank()) {
            throw new ApiException(UserErrorCode.USER_400_010);
        }
        List<String> approvers = sanitizeApprovers(param.approverUserCds());
        if (approvers.isEmpty()) {
            throw new ApiException(UserErrorCode.USER_400_011);
        }
        // 2. 결재자 유효성 — 본인 사업장 활성 후보 + 본인/중복 불가
        for (String a : approvers) {
            if (a.equals(userCd)) {
                throw new ApiException(UserErrorCode.USER_400_013);
            }
            if (user04Mapper.countActiveCandidate(cmpnyCd, siteCd, a) < 1) {
                log.warn("프리셋 저장 - 후보 밖 결재자. cmpnyCd={}, approver={}", cmpnyCd, a);
                throw new ApiException(UserErrorCode.USER_400_013);
            }
        }

        boolean makeDefault = "Y".equals(param.defaultYn());
        String presetId = param.presetId();

        if (presetId == null) {
            // 3-A. 신규
            presetId = user04Mapper.selectNextPresetId(cmpnyCd);
            if (makeDefault) {
                user04Mapper.clearDefaultForUser(cmpnyCd, userCd);
            }
            user04Mapper.insertPresetMaster(cmpnyCd, presetId, userCd, param.presetNm(),
                    makeDefault ? "Y" : "N", userCd);
        } else {
            // 3-B. 수정 — 소유권 재확인
            requireOwnership(cmpnyCd, presetId, userCd);
            if (makeDefault) {
                user04Mapper.clearDefaultForUser(cmpnyCd, userCd);
            }
            user04Mapper.updatePresetMaster(cmpnyCd, presetId, param.presetNm(),
                    makeDefault ? "Y" : "N", userCd);
            user04Mapper.deletePresetSteps(cmpnyCd, presetId);
        }

        // 4. 스텝 재구성
        int stepNo = 1;
        for (String a : approvers) {
            user04Mapper.insertPresetStep(cmpnyCd, presetId, stepNo++, a, userCd);
        }

        log.info("결재라인 프리셋 저장. cmpnyCd={}, userCd={}, presetId={}, 단계={}, 기본={}",
                cmpnyCd, userCd, presetId, approvers.size(), makeDefault);
        return presetId;
    }

    @Override
    @Transactional
    public void deletePreset(PresetActionParam param) {
        requireOwnership(param.gvCmpnyCd(), param.presetId(), param.gvUserCd());
        user04Mapper.deletePresetSteps(param.gvCmpnyCd(), param.presetId());
        user04Mapper.deletePresetMaster(param.gvCmpnyCd(), param.presetId());
        log.info("결재라인 프리셋 삭제. cmpnyCd={}, userCd={}, presetId={}",
                param.gvCmpnyCd(), param.gvUserCd(), param.presetId());
    }

    @Override
    @Transactional
    public void setDefaultPreset(PresetActionParam param) {
        requireOwnership(param.gvCmpnyCd(), param.presetId(), param.gvUserCd());
        user04Mapper.clearDefaultForUser(param.gvCmpnyCd(), param.gvUserCd());
        user04Mapper.setDefault(param.gvCmpnyCd(), param.presetId(), param.gvUserCd());
        log.info("결재라인 프리셋 기본지정. cmpnyCd={}, userCd={}, presetId={}",
                param.gvCmpnyCd(), param.gvUserCd(), param.presetId());
    }

    // ----- helpers

    /** 프리셋이 본인 소유인지 검증. 미존재 404, 타인 소유 403. */
    private void requireOwnership(String cmpnyCd, String presetId, String userCd) {
        if (presetId == null || presetId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String owner = user04Mapper.selectPresetOwner(cmpnyCd, presetId);
        if (owner == null) {
            throw new ApiException(UserErrorCode.USER_404_001);
        }
        if (!owner.equals(userCd)) {
            log.warn("프리셋 소유권 위반. cmpnyCd={}, presetId={}, owner={}, requester={}",
                    cmpnyCd, presetId, owner, userCd);
            throw new ApiException(UserErrorCode.USER_403_001);
        }
    }

    /** trim + 빈값 제거 + 중복(USER_400_012) 검사. 입력 순서 보존. */
    private List<String> sanitizeApprovers(List<String> raw) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (raw == null) {
            return result;
        }
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (!seen.add(t)) {
                throw new ApiException(UserErrorCode.USER_400_012);
            }
            result.add(t);
        }
        return result;
    }
}
