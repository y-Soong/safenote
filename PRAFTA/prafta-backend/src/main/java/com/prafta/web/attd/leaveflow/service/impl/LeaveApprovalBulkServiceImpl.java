package com.prafta.web.attd.leaveflow.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.leaveflow.application.param.LeaveApprovalActionParam;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApprovalBulkRequest;
import com.prafta.web.attd.leaveflow.dto.response.LeaveApprovalBulkResponse;
import com.prafta.web.attd.leaveflow.service.LeaveApprovalBulkService;
import com.prafta.web.attd.leaveflow.service.LeaveFlowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-leavemulti: 연차 결재 일괄 승인/반려 구현.
 *
 * <p>★ 트랜잭션 설계 (여기가 핵심이다)
 * <ul>
 *   <li>이 클래스는 {@code LeaveFlowServiceImpl} 과 <b>별도 빈</b>이다 → {@code approveStep} 호출이 프록시를 탄다.</li>
 *   <li>본 메서드에는 <b>{@code @Transactional} 을 걸지 않는다</b> → 각 {@code approveStep}(REQUIRED)이
 *       자기 트랜잭션을 새로 열고 커밋한다. 1건이 실패해도 이미 커밋된 건은 유지된다(부분 성공).</li>
 * </ul>
 * 둘 중 하나라도 어기면 1건 실패에 전건이 롤백되어 "13일 승인 + 1일 실패"가 불가능해진다.
 *
 * <p>권한·마감·상태·자기결재 가드는 전부 기존 단건 경로가 수행한다 — 신규 검증 로직 없음.
 * 목록 조회 시점과 처리 시점 사이에 다른 결재자가 먼저 처리했다면 단건 가드가 409 로 거부하고,
 * 그 건만 실패로 기록된다(동시성 자연 처리).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApprovalBulkServiceImpl implements LeaveApprovalBulkService {

    private final LeaveFlowService leaveFlowService;

    @Override
    public LeaveApprovalBulkResponse approveBulk(TokenInfo tokenInfo, LeaveApprovalBulkRequest request) {
        return process(tokenInfo, request, leaveFlowService::approveStep, "승인");
    }

    @Override
    public LeaveApprovalBulkResponse rejectBulk(TokenInfo tokenInfo, LeaveApprovalBulkRequest request) {
        return process(tokenInfo, request, leaveFlowService::rejectStep, "반려");
    }

    // ============================================================

    private LeaveApprovalBulkResponse process(TokenInfo tokenInfo, LeaveApprovalBulkRequest request,
                                              Consumer<LeaveApprovalActionParam> action, String actionNm) {

        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 같은 (reqId, step) 이 중복으로 실려도 1회만 처리한다(중복 클릭·화면 중복 선택 방어).
        Set<String> seen = new LinkedHashSet<>();
        List<LeaveApprovalBulkResponse.Failed> failed = new ArrayList<>();
        int success = 0;

        for (LeaveApprovalBulkRequest.Item item : request.getItems()) {
            if (item == null || item.getReqId() == null || item.getReqId().isBlank()
                    || item.getApprovalStep() == null) {
                // 형식 불량 1건 때문에 전체를 막지 않는다 — 그 건만 실패로 기록.
                failed.add(LeaveApprovalBulkResponse.Failed.builder()
                        .reqId(item == null ? null : item.getReqId())
                        .approvalStep(item == null ? null : item.getApprovalStep())
                        .reasonCode(CommonErrorCode.COMMON_400_001.code())
                        .reason(CommonErrorCode.COMMON_400_001.message())
                        .build());
                continue;
            }
            if (!seen.add(item.getReqId() + "#" + item.getApprovalStep())) {
                continue;   // 이미 처리한 (reqId, step)
            }

            LeaveApprovalActionParam param = new LeaveApprovalActionParam(
                    item.getReqId(), item.getApprovalStep(), request.getComment(),
                    tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
            try {
                // ★프록시 경유 호출 — 각 건이 자기 트랜잭션에서 커밋된다(부분 성공의 근거).
                action.accept(param);
                success++;
            } catch (ApiException e) {
                // 마감/이미 처리됨/권한 없음 등 — 그 건만 실패로 기록하고 계속 진행한다.
                failed.add(LeaveApprovalBulkResponse.Failed.builder()
                        .reqId(item.getReqId())
                        .approvalStep(item.getApprovalStep())
                        .reasonCode(e.getErrorCode() == null ? null : e.getErrorCode().code())
                        .reason(e.getResolvedMessage())
                        .build());
                log.info("[leavemulti] 연차 일괄 {} 스킵 — reqId={}, step={}, 사유={}",
                        actionNm, item.getReqId(), item.getApprovalStep(), e.getResolvedMessage());
            } catch (Exception e) {
                failed.add(LeaveApprovalBulkResponse.Failed.builder()
                        .reqId(item.getReqId())
                        .approvalStep(item.getApprovalStep())
                        .reasonCode(null)
                        .reason("처리 중 오류가 발생했습니다.")
                        .build());
                log.error("[leavemulti] 연차 일괄 {} 실패 — reqId={}, step={}",
                        actionNm, item.getReqId(), item.getApprovalStep(), e);
            }
        }

        log.info("[leavemulti] 연차 일괄 {} 완료 — 처리자={}, 성공={}건, 실패={}건",
                actionNm, tokenInfo.gv_userCd(), success, failed.size());

        return LeaveApprovalBulkResponse.builder()
                .successCount(success)
                .failedCount(failed.size())
                .failedList(failed)
                .build();
    }
}
