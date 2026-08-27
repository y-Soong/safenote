package com.prafta.app.req.req09.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.app.mypage.mypage01.mapper.AppMypage01Mapper;
import com.prafta.app.mypage.mypage01.result.PresetStepResult;
import com.prafta.app.req.req09.mapper.AppReq09Mapper;
import com.prafta.app.req.req09.service.AttdApprovalLineService;
import com.prafta.app.req.req09.service.AttdApprovalNotiService;
import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 근태 요청 결재 분기/라인 INSERT 공용 서비스 구현 (PRAFTA-APP-009-3).
 *
 * <p>req07 등록 트랜잭션 안에서 호출된다({@code @Transactional} 미부여 — 호출부 트랜잭션 참여).
 * 결재라인 INSERT 는 요청 INSERT 와 원자적으로 커밋/롤백되고, PUSH 적재만 예외 격리한다.
 *
 * <p>연차 {@code AppLeaveFlowServiceImpl#submitLeave}(336~392) 의 결재라인 생성 패턴을 미러한다
 * (신규 발명 최소). 근태결재선통합 P1-2(2026-08-23)로 구 'Y'/'N' 조직도 위임 분기를 폐지하고
 * 항상 결재선(다단계) 경로로 통일했다 — {@link #resolveApprovers} 참조.
 *
 * <p>PRAFTA-001(2026-08-27, 결재선 필수화): "본인 + 자체근태승인 ON" 이어도 그 결재 단계를
 * 신청 즉시 자동확정하지 않는다. 본인 지정 자격 게이트(ATTD_400_056)는 그대로 유지하되, 단계
 * 생성은 항상 정상 결재 단계(첫 단계 APPLIED / 나머지 WAIT)로 통일한다 — 명시적 승인 액션 필수.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttdApprovalLineServiceImpl implements AttdApprovalLineService {

    private final AppReq09Mapper appReq09Mapper;
    private final ApprovalLineMapper approvalLineMapper;
    private final AppMypage01Mapper appMypage01Mapper;
    private final AttdApprovalNotiService attdApprovalNotiService;

    // 결재 단계 상태 [SYS044]
    private static final String STEP_WAIT = "00";
    private static final String STEP_APPLIED = "01";

    @Override
    public void applyApprovalFlow(String cmpnyCd, String siteCd, String userCd, String reqId,
                                  List<String> approverUserCds, String presetId, String insertNo) {

        // 근태결재선통합 P1-2(2026-08-23): 'Y'/'N' 분기 폐지, 항상 결재선 경로로 통일.
        //   구 'Y' 즉시확정(D4)은 결재자 미지정 시 resolveApprovers 3번째 폴백(기본 결재자)이
        //   결재선 1단계로 재현한다. 구 'Y' + 일반근로자 위임 경로(D3)는 폐기한다
        //   (실사용 0건, 요청서 §1 — 조직도 위임 모델은 결재선으로 완전 대체).
        //   PRAFTA-001/004(2026-08-27): 자기지정이어도 신청 즉시 자동확정되지 않는다 — 본인이 결재선
        //   1단계 approver 로 지정되면, 다른 결재자와 동일하게 명시적 승인 액션을 거쳐야 확정된다.
        applyApprovalLine(cmpnyCd, siteCd, userCd, reqId, approverUserCds, presetId, insertNo);
    }

    /**
     * 'N' 결재라인 다단계 생성(D6/D7/D8). 연차 submitLeave 336~392 미러.
     *
     * <p>PRAFTA-001/004(2026-08-27): 신청 즉시 자동확정 개념 제거 — 항상 정상 결재 단계로 생성한다
     * (결재선 생성만 수행, REQ_STATUS 즉시확정 없음).
     */
    private void applyApprovalLine(String cmpnyCd, String siteCd, String userCd, String reqId,
                                   List<String> approverUserCds, String presetId, String insertNo) {

        List<String> approvers = resolveApprovers(cmpnyCd, siteCd, userCd, approverUserCds, presetId);
        if (approvers == null || approvers.isEmpty()) {
            // 구 D5: 노드에 지정 결재자/프리셋도 없고 기본 결재자(MAIN/SUB)도 없는 조직 설정오류.
            //   클라이언트 입력 문제가 아니므로 COMMON_400_001(필수 파라미터 누락)이 아닌 전용 코드 사용.
            throw new ApiException(AttdErrorCode.ATTD_400_105);
        }

        // D8 결재자 스코프 가드: 본문 결재자가 동일 회사+사업장+재직 사용자인지 서버 검증.
        //   타 사업장/회사/존재없는 USER_CD 주입 → 그 결재함에 신청 노출되는 cross-tenant 침해 차단.
        List<String> distinctApprovers = new ArrayList<>(new LinkedHashSet<>(approvers));
        int validCount = appReq09Mapper.countValidApprovers(cmpnyCd, siteCd, distinctApprovers);
        if (validCount != distinctApprovers.size()) {
            log.info("[attdAprvLine] 결재선 거부: 유효하지 않은 결재자 포함 (reqId={}, 요청={}, 유효={})",
                    reqId, distinctApprovers.size(), validCount);
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // D7 자기승인 자격: 본인 지정은 노드 자체근태승인 + 노드관리자일 때만.
        //   PRAFTA-001(2026-08-27): 자격 게이트(본인 지정 가능 여부)만 유지 — "즉시확정" 효과는 제거.
        boolean selfAllowed = isYes(appReq09Mapper.selectUserNodeSelfApproveYn(cmpnyCd, userCd));

        for (int i = 0; i < approvers.size(); i++) {
            boolean isSelf = userCd.equals(approvers.get(i));
            if (isSelf && !selfAllowed) {
                throw new ApiException(AttdErrorCode.ATTD_400_056);
            }
        }

        // PRAFTA-001(2026-08-27): self 여부와 무관하게 배열상 첫 번째 approver 가 STEP_APPLIED,
        //   나머지는 STEP_WAIT — self 도 다른 결재자와 동일하게 차례를 받아 명시적 승인이 필요하다.
        for (int i = 0; i < approvers.size(); i++) {
            String approver = approvers.get(i);
            ApprovalStepVO s = new ApprovalStepVO();
            s.setReqId(reqId);
            s.setApprovalStep(i + 1);
            s.setCmpnyCd(cmpnyCd);
            s.setApproverUserCd(approver);
            s.setApprovalStatus(i == 0 ? STEP_APPLIED : STEP_WAIT);
            s.setInsertNo(insertNo);
            approvalLineMapper.insertApprovalStep(s);
        }

        // 차례가 도래한 첫 단계(항상 index 0, self 포함) 결재자에게 차례 도래 PUSH(예외 격리는 noti 내부).
        String turnApprover = approvers.get(0);
        int turnStep = 1; // approvalStep 은 1-based
        attdApprovalNotiService.notifyAttdApprovalTurn(cmpnyCd, siteCd, userCd, reqId, turnStep, turnApprover, insertNo);
        log.info("[attdAprvLine] 'N' 결재라인 생성 완료 (reqId={}, 단계수={}, 차례단계={})",
                reqId, approvers.size(), turnStep);
    }

    /**
     * 결재자 목록 결정: approverUserCds 1차, 비어있고 presetId 가 있으면 본인 소유 프리셋을 전개,
     * 근태결재선통합 P1-2(§0-5) — 그 둘 다 없으면 신청자 소속 노드의 기본 결재자(정 관리자 우선,
     * 없으면 부 관리자) 1인을 단일 결재선으로 반환한다. 프리셋 전개는 mypage01
     * {@code selectPresetStepsById}(소유자 스코프) 재사용(타인 프리셋 차단).
     */
    private List<String> resolveApprovers(String cmpnyCd, String siteCd, String userCd,
                                          List<String> approverUserCds, String presetId) {
        if (approverUserCds != null && !approverUserCds.isEmpty()) {
            return approverUserCds;
        }
        if (presetId != null && !presetId.isBlank()) {
            List<PresetStepResult> steps =
                    appMypage01Mapper.selectPresetStepsById(cmpnyCd, userCd, presetId);
            List<String> expanded = new ArrayList<>(steps.size());
            for (PresetStepResult s : steps) {
                if (s.approverUserCd() != null && !s.approverUserCd().isBlank()) {
                    expanded.add(s.approverUserCd());
                }
            }
            return expanded;
        }
        // §0-5 3번째 폴백(신규): approverUserCds 도 presetId 도 없을 때만 — 신청자 소속 노드의 기본
        //   결재자. P1-1 lazy 폴백(ApprovalLineMapper.selectDefaultApproverOfNode)과 동일 형식(MAIN→SUB).
        //   구 'Y' 노드 즉시확정(D3/D4)을 결재선 1단계로 재현한다.
        String defaultApprover = appReq09Mapper.selectDefaultApproverForUser(cmpnyCd, siteCd, userCd);
        if (defaultApprover != null && !defaultApprover.isBlank()) {
            return List.of(defaultApprover);
        }
        return List.of();
    }

    /** 'Y'(대소문자 무관) → true. null/그외 → false. */
    private boolean isYes(String yn) {
        return "Y".equalsIgnoreCase(yn);
    }
}
