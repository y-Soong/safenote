package com.prafta.common.cmm.leave.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.mapper.LeaveRefusalMapper;
import com.prafta.common.cmm.leave.service.LeaveRefusalDetectService;
import com.prafta.common.cmm.leave.vo.RefusalTargetVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 노무수령거부 원천 차단 가드 구현 (PRAFTA-COM-008-B, com-001 detect→block 전환).
 *
 * <p>진입부에서 레코드 생성 이전에 호출된다. 흐름:
 * <pre>
 *   target = selectLaborRefusalTarget(...)   // tb_user_leave_use 촉진단계(3게이트)
 *   if (target == null) return;              // 대상 아님 → 호출부 정상 진행
 *   recorder.recordBlockAndAlert(...)        // ★REQUIRES_NEW: BLOCKED 이력 + 관리자 PUSH 선커밋
 *   throw ApiException(ATTD_400_150)         // 출근/근태 트랜잭션 롤백(증빙은 이미 커밋됨)
 * </pre>
 *
 * <p>★ 트랜잭션 경계(함정): 차단 증빙은 차단 throw 로 호출부 트랜잭션이 롤백돼도 보존되어야 하므로
 * {@link LeaveRefusalBlockRecorder#recordBlockAndAlert}({@code REQUIRES_NEW})로 선커밋한다. 본
 * 구현이 같은 빈 내부에서 그 메서드를 호출하면 프록시를 안 타 새 트랜잭션이 안 열리므로, recorder 를
 * 별도 빈으로 주입받아 호출한다. recorder 내부 실패가 발생해도 차단 throw 는 항상 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRefusalDetectServiceImpl implements LeaveRefusalDetectService {

    private final LeaveRefusalMapper leaveRefusalMapper;
    private final LeaveRefusalBlockRecorder leaveRefusalBlockRecorder;

    @Override
    public void guardAndRecord(String cmpnyCd, String siteCd, String userCd, String nodeCd,
                               String workYmd, String attemptType, String operatorUserCd) {
        // 1) 차단 대상 판정: 촉진(1·2차) 확정 법정 연차일 + 비휴일 (tb_user_leave_use 기준).
        RefusalTargetVO target =
                leaveRefusalMapper.selectLaborRefusalTarget(cmpnyCd, siteCd, userCd, workYmd);
        if (target == null) {
            // 대상 아님(자발/비법정/휴일/연차 없음) → 조용히 통과(호출부 정상 진행).
            return;
        }

        log.info("[leaveRefusal] 노무수령거부 차단 발동 (userCd={}, targetYmd={}, attemptType={}, leaveId={})",
                userCd, target.getTargetYmd(), attemptType, target.getLeaveId());

        // 2) ★REQUIRES_NEW 선커밋: BLOCKED 이력 + 관리자 PUSH outbox(별도 빈 — 프록시 경유).
        //    내부 실패가 발생해도(증빙 누락 위험은 log.error) 차단 throw 는 항상 수행한다.
        try {
            leaveRefusalBlockRecorder.recordBlockAndAlert(
                    cmpnyCd, siteCd, userCd, target.getTargetYmd(),
                    target.getLeaveId(), attemptType, operatorUserCd);
        } catch (Exception e) {
            // PUSH/이력 적재 실패가 차단을 무효화하면 안 된다 — 로깅 후 그대로 차단 throw 로 진행.
            log.error("[leaveRefusal] 차단 증빙(이력/PUSH) 적재 실패 — 차단은 계속 수행 (userCd={}, targetYmd={}, attemptType={})",
                    userCd, target.getTargetYmd(), attemptType, e);
        }

        // 3) 차단 예외 throw — 호출부의 출근/근태 트랜잭션을 롤백시켜 레코드 생성을 막는다.
        throw new ApiException(AttdErrorCode.ATTD_400_150);
    }

    @Override
    public boolean isRefusalTarget(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        // 부작용 없는 순수 판정: guardAndRecord 와 동일 술어(촉진 1·2차 확정 법정 연차일 · 비휴일) 재사용.
        //   화면 사전 안내(F2 Route B)용 조회 전용 — 이력/PUSH 적재나 예외 throw 없음.
        return leaveRefusalMapper.selectLaborRefusalTarget(cmpnyCd, siteCd, userCd, workYmd) != null;
    }
}
