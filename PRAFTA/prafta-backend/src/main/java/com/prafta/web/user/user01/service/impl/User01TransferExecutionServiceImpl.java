package com.prafta.web.user.user01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.sch.service.DefaultSchGenService;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.leaveflow.service.LeaveFlowService;
import com.prafta.web.user.user01.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user01.mapper.User01Mapper;
import com.prafta.web.user.user01.mapper.UserTransferMapper;
import com.prafta.web.user.user01.result.TransferBlockReason;
import com.prafta.web.user.user01.result.TransferReservationExecRow;
import com.prafta.web.user.user01.result.UserTransferBasicResult;
import com.prafta.web.user.user01.service.User01TransferExecutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 소속이동 발효(실행) 서비스 구현 — PRAFTA-WEB_001-2(Terminal B).
 *
 * <p>정규직 정의 = EMPLOYMENT_TYPE != 'DAILY'(NULL 포함). 담당 정 자동등록·기본근무 발효는 정규직만.
 * 진행중 요청 반려/취소·TBM 종료/미이수는 기존 인터랙티브 반려 서비스가 호출자(승인자) 권위·단건 REQ_ID
 * 컨텍스트에 강결합되어 배치 재사용이 불가하므로, 각 캐노니컬 상태 전이를 동일하게 미러한
 * 회사+사용자 스코프 일괄 UPDATE(UserTransferMapper)로 수행한다.
 *
 * <p>[수정1] 등록~발효 사이 상태변화 차단: 발효 첫 단계에서 이동 사업장/부서 실재 + 5종 불가케이스를
 * Terminal A 자산({@link UserTransferValidator}, selectSiteExists/selectNodeExists, selectNodeHasAdmin)으로 재검증한다.
 * <p>[수정2] 전입은 발효일(신소속 첫날) 포함 근무계획 생성(D3 "당일 미변경" 의 의도적 carve-out).
 * <p>[수정6] 진입 시 예약 행을 FOR UPDATE 로 선점 잠그고 RESERVED 재확인(다중 인스턴스 이중작업 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class User01TransferExecutionServiceImpl implements User01TransferExecutionService {

    private final UserTransferMapper userTransferMapper;
    private final User01Mapper user01Mapper; // 사업장권한 UPSERT/회수(D7)·노드 관리자 존재(USER_400_056) 재사용(중복 구현 금지).
    private final DefaultSchGenService defaultSchGenService; // 기본근무 발효 재사용(교대 비소속/평일/멱등).
    private final UserTransferValidator userTransferValidator; // [수정1] 5종 불가케이스 재검증 재사용(Terminal A).
    private final LeaveFlowService leaveFlowService; // QT-11-7: 종료된 연차 요청의 원장 원복(정상 반려와 단일 출처).

    /** 진행중 요청 반려/취소 사유(요청 4-1). TBM 미이수 사유와 공용. */
    private static final String REASON_TRANSFER = "소속이동";

    /** FAIL_REASON DDL varchar(500) 정합 — 초과분 절단. */
    private static final int FAIL_REASON_MAX_LEN = 500;

    @Override
    @Transactional
    public boolean executeReservation(TransferReservationExecRow row) {
        String cmpnyCd = row.cmpnyCd();
        String userCd = row.userCd();
        String actor = row.insertNo(); // 발효 변경 감사 actor = 등록자(master/hr).

        // [수정6] 예약 행 선점 잠금 + RESERVED 재확인. 패자/이미 처리분은 무동작으로 빠진다(이중작업 방지).
        String lockedStatus = userTransferMapper.selectReservationStatusForUpdate(cmpnyCd, row.reservationId());
        if (!"RESERVED".equals(lockedStatus)) {
            log.info("소속이동 발효 선점 스킵(RESERVED 아님) - cmpnyCd={}, reservationId={}, status={}",
                    cmpnyCd, row.reservationId(), lockedStatus);
            return false;
        }

        // [수정1] 발효일 재검증 — 등록~발효 사이 상태변화 차단(실패 시 throw → FAILED 격리).
        //   대상자 현재 상태를 재조회(이미 탈퇴/이동 시 null → 차단). 고용형태/소속은 현재 값으로 재판정.
        UserTransferBasicResult target = userTransferMapper.selectUserBasic(cmpnyCd, userCd);
        if (target == null) {
            throw new IllegalStateException("재검증 실패: 대상 사용자 없음(탈퇴/삭제) userCd=" + userCd);
        }
        boolean daily = AuthRoleUtils.isDailyWorker(target.employmentType());
        // 정규직만 기본근무타입 적용. 일용직은 DEFAULT_SCH 미적용(예약 단계에서 null 고정).
        boolean applyDefaultSch = !daily && row.toDefaultSchCd() != null && !row.toDefaultSchCd().isBlank();

        revalidate(row, target, daily);

        log.info("소속이동 발효 시작 - cmpnyCd={}, reservationId={}, userCd={}, toSite={}, toNode={}, daily={}",
                cmpnyCd, row.reservationId(), userCd, row.toSiteCd(), row.toNodeCd(), daily);

        // (a) 소속 발효: tb_user SITE_CD/NODE_CD (+ 정규직 DEFAULT_SCH).
        userTransferMapper.updateUserSiteNodeForTransfer(
                cmpnyCd, userCd, row.toSiteCd(), row.toNodeCd(), row.toDefaultSchCd(), applyDefaultSch, actor);

        // (b) 신규 사업장 권한 활성(UPSERT USE_YN='Y') — 기존 단건 UPSERT 재사용.
        user01Mapper.insertUserSiteAuth(
                new UserSiteAuthCommand(cmpnyCd, userCd, row.toSiteCd(), "Y", actor));
        // 비전사역할(master/hr/safe 아님)은 D7 회수 패턴으로 신소속 사업장(TO_SITE) 1건만 잔존시킨다.
        // 전사역할은 전 사업장 자동부여 상태이므로 회수하지 않는다(무리한 삭제 금지).
        String authCd = user01Mapper.selectUserAuthCd(cmpnyCd, userCd);
        if (!isCompanyWideAuthRole(authCd)) {
            user01Mapper.deleteSiteAuthExceptHome(cmpnyCd, userCd, row.toSiteCd(), actor);
        }

        // (c) 담당 정 자동등록(정규직): 이동 노드 MAIN_ADMIN_CD 비어있으면 대상자로 세팅.
        if (!daily) {
            int adminSet = userTransferMapper.updateNodeMainAdminIfEmpty(
                    cmpnyCd, row.toSiteCd(), row.toNodeCd(), userCd, actor);
            if (adminSet > 0) {
                log.info("소속이동 발효 - 담당 정 자동등록 cmpnyCd={}, siteCd={}, nodeCd={}, userCd={}",
                        cmpnyCd, row.toSiteCd(), row.toNodeCd(), userCd);
            }
        }

        // (d) 기본근무 발효(정규직): [수정2] 전입은 발효일(moveDate) 포함 ~ 당해 12/31 평일 근무계획 생성.
        //     applyDefaultSchChange 는 tomorrow 부터라 발효일(신소속 첫날)이 누락되므로, 전입 경로는
        //     generateForUser 를 (발효일, 당해연말) 범위로 직접 호출한다(D3 "당일 미변경" 의 의도적 carve-out:
        //     본인 default 변경이 아닌 전입이므로 발효일부터 근무계획이 있어야 함).
        //     generateForUser 는 교대 비소속·평일·미마감월·빈 날만 멱등 생성 → 기존 생성과 중복 INSERT 없음.
        //     동일 트랜잭션 — 실패 시 발효 전체 롤백(부분 발효 금지).
        if (applyDefaultSch) {
            String yearEnd = row.moveDate().substring(0, 4) + "1231";
            defaultSchGenService.generateForUser(
                    cmpnyCd, row.toSiteCd(), userCd, row.toDefaultSchCd(), row.moveDate(), yearEnd);
        }

        // (e) 진행중 요청 일괄 반려/취소.
        //   QT-11-7: 상태 UPDATE 는 연차 원장(use 행·GRANT USED_DAYS)을 건드리지 않는다. 종료되는 연차 요청의
        //   REQ_ID 를 UPDATE 전에 스냅샷해 두었다가, UPDATE 직후 정상 반려와 동일한 원복 시퀀스를 태운다.
        //   (스냅샷을 안 하면 REQ_STATUS='01' 조건이 깨져 대상을 다시 찾을 수 없다.)
        List<String> applicantLeaveReqIds = userTransferMapper.selectActiveLeaveReqIdsByApplicant(cmpnyCd, userCd);

        //   신청자(대상자)건 취소.
        int reqCancelled = userTransferMapper.cancelActiveAttdReqByApplicant(cmpnyCd, userCd, REASON_TRANSFER, actor);
        //   승인대기 결재자(대상자)건 반려 — [수정5] 대상 REQ_ID 스냅샷 후 본 요청/결재단계 동일 집합 키로 반려
        //   (순차 UPDATE 상호 무력화 방지).
        List<String> approverReqIds = userTransferMapper.selectActiveApproverReqIds(cmpnyCd, userCd);
        int reqRejected = 0;
        int stepRejected = 0;
        List<String> approverLeaveReqIds = List.of();
        if (approverReqIds != null && !approverReqIds.isEmpty()) {
            // 원복 대상(연차 사용 '05')만 추린다 — 상태 UPDATE 전에 확정해야 REQ_TYPE 재조회가 안전하다.
            approverLeaveReqIds = userTransferMapper.selectLeaveReqIdsIn(cmpnyCd, approverReqIds);
            reqRejected = userTransferMapper.rejectAttdReqByReqIds(cmpnyCd, approverReqIds, REASON_TRANSFER, actor);
            stepRejected = userTransferMapper.rejectApprovalStepsForApproverByReqIds(
                    cmpnyCd, userCd, approverReqIds, REASON_TRANSFER, actor);
        }

        //   QT-11-7: 종료된 연차 요청의 차감 원복(use 행 취소 + GRANT 재집계 + 가불 회수 + 시간차 재정산).
        //   ★ 결재자였을 뿐인 제3자(신청자)의 연차도 여기서 함께 복구된다 — 반려됐는데 차감만 남는 상태 방지.
        //   동일 트랜잭션(REQUIRED) — 원복 실패 시 발효 전체 롤백(부분 처리 금지).
        int leaveRestored = 0;
        for (String reqId : concatDistinct(applicantLeaveReqIds, approverLeaveReqIds)) {
            leaveFlowService.restoreLeaveLedgerOnTerminate(cmpnyCd, reqId, REASON_TRANSFER, actor);
            leaveRestored++;
        }
        if (leaveRestored > 0) {
            log.info("소속이동 발효 - 종료된 연차 요청 원장 원복 {}건. userCd={}", leaveRestored, userCd);
        }
        int otCancelled = userTransferMapper.cancelActiveOvertimeByUser(cmpnyCd, userCd, actor);
        int leaveChgRejected = userTransferMapper.rejectActiveLeaveChangeByUser(cmpnyCd, userCd, REASON_TRANSFER, actor);

        // (f) TBM. [수정3] 미이수는 "진행중 세션" 한정. 자기 관리세션을 본인이 참석한 경우를 위해
        //   미이수 처리(f-나)를 세션 종료(f-가)보다 먼저 수행한다(세션 종료 후엔 ENDED_AT 로 진행중 EXISTS 가 제외됨).
        int tbmNotCompleted = userTransferMapper.markTbmAttendanceNotCompleted(cmpnyCd, userCd, REASON_TRANSFER, actor);
        int tbmEnded = userTransferMapper.endActiveTbmSessionsAsManager(cmpnyCd, userCd, actor);

        log.info("소속이동 발효 진행중요청/TBM 처리 - userCd={}, attdReq취소={}, attdReq반려={}, 결재단계반려={}, OT취소={}, 연차변경반려={}, TBM미이수={}, TBM세션종료={}",
                userCd, reqCancelled, reqRejected, stepRejected, otCancelled, leaveChgRejected, tbmNotCompleted, tbmEnded);

        // (g) 예약 발효 마킹(RESERVED 가드). 0행이면 동시 발효 경합 → 롤백(멱등 백스톱).
        int applied = userTransferMapper.markReservationApplied(cmpnyCd, row.reservationId(), actor);
        if (applied == 0) {
            throw new IllegalStateException(
                    "소속이동 예약 발효 마킹 0행(이미 처리/경합) reservationId=" + row.reservationId());
        }

        log.info("소속이동 발효 완료 - cmpnyCd={}, reservationId={}, userCd={}", cmpnyCd, row.reservationId(), userCd);
        return true;
    }

    /**
     * [수정1] 발효일 재검증 — 등록~발효 사이 상태변화 차단. 실패 시 RuntimeException(전체 롤백 → FAILED 격리).
     *
     * <ul>
     *   <li>이동 사업장/부서 실재(Terminal A selectSiteExists/selectNodeExists 재사용) — daily/regular 공통.</li>
     *   <li>일용직: 5종 불가케이스 미적용. 이동 부서 관리자 존재 필수(selectNodeHasAdmin, USER_400_056 동치).</li>
     *   <li>정규직: 5종 불가케이스 전체 재실행(UserTransferValidator.evaluate). 예약행 이동 정보로 입력 재구성.</li>
     * </ul>
     */
    private void revalidate(TransferReservationExecRow row, UserTransferBasicResult target, boolean daily) {
        String cmpnyCd = row.cmpnyCd();

        // 이동 사업장/부서 실재(공통). 등록 후 사업장/부서 삭제 대비.
        if (userTransferMapper.selectSiteExists(cmpnyCd, row.toSiteCd()) <= 0) {
            throw new IllegalStateException("재검증 실패: 이동 사업장 미존재 toSiteCd=" + row.toSiteCd());
        }
        if (userTransferMapper.selectNodeExists(cmpnyCd, row.toSiteCd(), row.toNodeCd()) <= 0) {
            throw new IllegalStateException("재검증 실패: 이동 부서 미존재 toNodeCd=" + row.toNodeCd());
        }

        if (daily) {
            // 일용직: 이동 부서 관리자(정/부) 존재 필수(Terminal A 등록 경로 동일 가드 재사용).
            if (user01Mapper.selectNodeHasAdmin(cmpnyCd, row.toSiteCd(), row.toNodeCd()) <= 0) {
                throw new IllegalStateException("재검증 실패: 이동 부서 관리자 부재(USER_400_056) toNodeCd=" + row.toNodeCd());
            }
            return;
        }

        // 정규직: 5종 불가케이스 재실행(예약행 이동 정보로 입력 재구성, 대상자 현재 상태 기준).
        List<TransferBlockReason> reasons = userTransferValidator.evaluate(
                cmpnyCd, target, row.toDefaultSchCd(), row.toSiteCd(), row.moveDate());
        if (!reasons.isEmpty()) {
            TransferBlockReason first = reasons.get(0);
            throw new IllegalStateException(
                    "재검증 실패: 불가케이스(" + first.code() + ") " + first.message() + " [총 " + reasons.size() + "건]");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String cmpnyCd, String reservationId, String failReason, String actor) {
        String reason = failReason == null ? "발효 실패" : failReason;
        if (reason.length() > FAIL_REASON_MAX_LEN) {
            reason = reason.substring(0, FAIL_REASON_MAX_LEN);
        }
        int updated = userTransferMapper.markReservationFailed(cmpnyCd, reservationId, reason, actor);
        log.warn("소속이동 발효 실패 격리 - cmpnyCd={}, reservationId={}, 영향={}, reason={}",
                cmpnyCd, reservationId, updated, reason);
    }

    /**
     * 전 사업장 접근(사업장권한 전사 자동부여 대상) 역할 판정 — master/hr/safe.
     * User01ServiceImpl.isCompanyWideAuthRole(D7) 동형. 전사역할은 신소속 발효 시 권한 회수 대상이 아니다.
     */
    private static boolean isCompanyWideAuthRole(String authCd) {
        if (authCd == null || authCd.isEmpty()) {
            return false;
        }
        return AuthRoleUtils.AUTH_MASTER.equals(authCd)
                || AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd)
                || AuthRoleUtils.AUTH_SAFETY_MANAGER.equals(authCd);
    }

    /**
     * QT-11-7 — 두 REQ_ID 목록을 중복 없이 합친다(입력 순서 유지, null/빈값 방어).
     * 대상자가 신청자이면서 동시에 결재자인 요청은 이론상 없지만(본인 결재는 자동승인),
     * 원복이 2회 실행되지 않도록 방어한다(원복 자체는 멱등이나 불필요한 재정산 호출을 피한다).
     */
    private static List<String> concatDistinct(List<String> a, List<String> b) {
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        if (a != null) {
            for (String s : a) {
                if (s != null && !s.isBlank()) merged.add(s);
            }
        }
        if (b != null) {
            for (String s : b) {
                if (s != null && !s.isBlank()) merged.add(s);
            }
        }
        return new java.util.ArrayList<>(merged);
    }
}
