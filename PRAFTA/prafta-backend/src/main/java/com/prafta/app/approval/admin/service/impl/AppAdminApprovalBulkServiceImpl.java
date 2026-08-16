package com.prafta.app.approval.admin.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.prafta.app.approval.admin.application.param.ApprovalProcessParam;
import com.prafta.app.leave.leaveflow.service.MultiDayLeavePlanner;
import com.prafta.app.approval.admin.dto.request.ApprovalBulkProcessRequest;
import com.prafta.app.approval.admin.dto.request.ApprovalProcessRequest;
import com.prafta.app.approval.admin.dto.response.ApprovalBulkProcessResponse;
import com.prafta.app.approval.admin.service.AppAdminApprovalBulkService;
import com.prafta.app.approval.admin.service.AppAdminApprovalService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-leavemulti: 앱 관리자 승인 일괄 처리 구현 — <b>기존 단건 경로를 N회 호출하는 얇은 어댑터</b>.
 *
 * <p>★ 트랜잭션 설계 (여기가 핵심이다)
 * <ul>
 *   <li>이 클래스는 {@code AppAdminApprovalServiceImpl} 과 <b>별도 빈</b>이다 → {@code process} 호출이 프록시를 탄다.</li>
 *   <li>이 클래스/메서드에는 <b>{@code @Transactional} 을 걸지 않는다</b> → 각 {@code process}(REQUIRED)가
 *       자기 트랜잭션을 새로 열고 커밋한다. 1건이 실패해도 이미 커밋된 건은 유지된다(부분 성공).</li>
 * </ul>
 * 둘 중 하나라도 어기면 1건 실패에 전건이 롤백되어 "13일 승인 + 1일 실패"가 불가능해진다
 * (웹 {@code LeaveApprovalBulkServiceImpl} 이 같은 함정을 이미 문서화해 뒀고, 그 설계를 그대로 미러한다).
 *
 * <p>★ 가드 재사용: 권한(IDOR 스코프)·마감·멱등(409)·결재단계·차감은 전부 기존 단건 {@code process} 와
 * 그 위임 대상(web attd07/leaveflow)이 수행한다. 여기에는 판정 로직이 한 줄도 없다.
 * 목록 조회 시점과 처리 시점 사이에 다른 결재자가 먼저 처리했다면 단건 가드가 409 로 거부하고 그 건만 실패로 기록된다.
 *
 * <p>★ 요청 단위 검증(decision 화이트리스트 / 반려사유 필수)은 {@link ApprovalProcessParam#of} 팩토리가 담당한다.
 * 그 변환을 <b>루프 전에 전건 수행</b>하므로, 반려 사유가 비면 한 건도 처리되지 않고 전건 400 이 된다
 * (일부만 통과해 절반이 반려되는 사고를 막는다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminApprovalBulkServiceImpl implements AppAdminApprovalBulkService {

    /** ★다른 빈의 프록시 경유 호출이어야 건별 트랜잭션이 성립한다(자기 호출이면 분리되지 않는다). */
    private final AppAdminApprovalService appAdminApprovalService;

    /** v1 일괄 대상 그룹. 근태보정/초과/스케줄은 기간 묶음 개념이 없어 묶을 근거가 없다. */
    private static final String G_LEAVE = "LEAVE";

    /**
     * items 상한. 임의 대량 전송에 의한 장시간 트랜잭션 연쇄를 막는 방어값이다.
     *
     * <p>★기간신청 상한({@link MultiDayLeavePlanner#MAX_RANGE_DAYS})보다 <b>작게 두면 안 된다</b>.
     * 작으면 신청 가능한 최대 묶음이 일괄 처리에서 통째로 거부되어(전건 400), 관리자가 그 묶음만
     * 날짜별로 하나씩 처리해야 한다 — 이 기능이 없애려던 상황이 최대 묶음에서만 되살아난다.
     * 종전 60 은 62일 묶음을 막고 있었다(security L-1). 여유를 둬 상한과 연동한다.
     */
    private static final int MAX_ITEMS = MultiDayLeavePlanner.MAX_RANGE_DAYS + 8;

    @Override
    public ApprovalBulkProcessResponse bulkProcess(ApprovalBulkProcessRequest request, TokenInfo token) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String group = request.getGroup() == null ? null : request.getGroup().trim().toUpperCase();
        if (!G_LEAVE.equals(group)) {
            log.warn("[leavemulti] 일괄 처리 미지원 그룹 - group={}", group);
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 일괄은 "요청대로 승인" 또는 "반려"만 가능하다(정책 §7.7 — 일괄에서는 시간 조정 불가).
        //   나머지 decision 검증(화이트리스트·반려사유 필수)은 아래 팩토리가 수행한다.
        String decision = request.getDecision() == null ? null : request.getDecision().trim().toUpperCase();
        if (!ApprovalProcessParam.DECISION_APPROVE_ASIS.equals(decision)
                && !ApprovalProcessParam.DECISION_REJECT.equals(decision)) {
            log.warn("[leavemulti] 일괄 처리 미지원 decision - decision={}", decision);
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getItems().size() > MAX_ITEMS) {
            log.warn("[leavemulti] 일괄 처리 items 상한 초과 - size={}, max={}", request.getItems().size(), MAX_ITEMS);
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 1단계: 전 items 를 단건 팩토리로 변환한다(decision 화이트리스트 / 반려사유 필수 / 토큰 존재 검증).
        //   여기서 던지면 한 건도 처리되지 않는다 — "사유 없이 절반만 반려" 같은 부분 처리를 원천 차단.
        //   같은 (reqId, step) 이 중복으로 실려도 1회만 처리한다(중복 클릭·화면 중복 선택 방어).
        Set<String> seen = new LinkedHashSet<>();
        List<ApprovalProcessParam> params = new ArrayList<>(request.getItems().size());
        for (ApprovalBulkProcessRequest.Item item : request.getItems()) {
            if (item == null || item.getReqId() == null || item.getReqId().isBlank()) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            if (!seen.add(item.getReqId().trim() + "#" + item.getApprovalStep())) {
                continue;
            }
            ApprovalProcessRequest one = new ApprovalProcessRequest();
            one.setReqId(item.getReqId());
            one.setGroup(G_LEAVE);
            one.setDecision(request.getDecision());
            one.setApprovalStep(item.getApprovalStep());
            one.setComment(request.getComment());
            params.add(ApprovalProcessParam.of(one, token));
        }

        // 2단계: 건별 처리. 실패는 사유와 함께 수집하고 계속 진행한다(부분 성공).
        List<ApprovalBulkProcessResponse.Failed> failed = new ArrayList<>();
        int success = 0;
        for (ApprovalProcessParam p : params) {
            try {
                // ★프록시 경유 호출 — 각 건이 자기 트랜잭션에서 커밋된다(부분 성공의 근거).
                appAdminApprovalService.process(p);
                success++;
            } catch (ApiException e) {
                // 마감(400_042)/이미 처리됨(409_001)/권한 없음(403_002) 등 — 그 건만 실패로 기록.
                failed.add(ApprovalBulkProcessResponse.Failed.builder()
                        .reqId(p.reqId())
                        .approvalStep(p.approvalStep())
                        .reasonCode(e.getErrorCode() == null ? null : e.getErrorCode().code())
                        .reason(e.getResolvedMessage())
                        .build());
                log.info("[leavemulti] 앱 일괄 {} 스킵 - reqId={}, step={}, 사유={}",
                        p.decision(), p.reqId(), p.approvalStep(), e.getResolvedMessage());
            } catch (Exception e) {
                failed.add(ApprovalBulkProcessResponse.Failed.builder()
                        .reqId(p.reqId())
                        .approvalStep(p.approvalStep())
                        .reasonCode(null)
                        .reason("처리 중 오류가 발생했습니다.")
                        .build());
                log.error("[leavemulti] 앱 일괄 {} 실패 - reqId={}, step={}",
                        p.decision(), p.reqId(), p.approvalStep(), e);
            }
        }

        log.info("[leavemulti] 앱 일괄 처리 완료 - group={}, decision={}, 처리자={}, 성공={}건, 실패={}건",
                group, request.getDecision(), token == null ? null : token.gv_userCd(), success, failed.size());

        return ApprovalBulkProcessResponse.builder()
                .successCount(success)
                .failedCount(failed.size())
                .failedList(failed)
                .build();
    }
}
