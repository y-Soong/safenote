package com.prafta.common.cmm.dailylogin.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.dailyjoin.application.command.DailyUserSlotUpdCommand;
import com.prafta.common.cmm.dailyjoin.application.command.InsertDailyUserCommand;
import com.prafta.common.cmm.dailyjoin.application.command.InsertSlotHisCommand;
import com.prafta.common.cmm.dailyjoin.application.query.EmptySlotQuery;
import com.prafta.common.cmm.dailyjoin.application.query.LinkPolicyQuery;
import com.prafta.common.cmm.dailyjoin.mapper.DailyJoinMapper;
import com.prafta.common.cmm.dailyentry.service.DailyEntryService;
import com.prafta.common.cmm.dailylogin.application.query.DailyLoginQuery;
import com.prafta.common.cmm.dailylogin.mapper.DailyLoginMapper;
import com.prafta.common.cmm.dailylogin.result.DailyUserResult;
import com.prafta.common.error.dailylogin.DailyLoginErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-032 B — 일용직 로그인 재활성(입장 승인제) 트랜잭션 처리기.
 *
 * <p>일용직 계약서+승인제(D5): 무마찰 재입장은 공식 폐기되었다. 본 처리기는 <b>승인('02')된
 * 입장 승인요청이 존재하는 경우에만</b> 호출되며, 재활성 + 슬롯 점유 + 이력 적재 + 승인요청 소진('02'→'05')을
 * 하나의 트랜잭션으로 묶는다. 승인 판정(006/007/요청 생성)은 self-invocation 함정을 피하기 위해
 * 호출자({@link DailyLoginServiceImpl}, 비-Transactional)가 수행한다.
 *
 * <p>비밀번호 검증/실패카운트 누적은 {@link DailyLoginServiceImpl#login} 측(비-Transactional)에서 끝낸 뒤,
 * <b>비밀번호가 일치하고 승인된 재활성/활성화 대상</b>인 경우에만 본 처리기를 호출한다.
 * 토글 OFF/정원 부족/종료 사업장/정합 깨짐/소진 경합 시 전체 롤백된다(부분 점유 방지).
 *
 * <p>신규가입 승인대기(ACCOUNT_STATUS='04', USE_YN='Y') 계정의 승인 후 첫 로그인 활성화도
 * 본 메서드를 재사용한다(reactivateDailyUser 는 UPDATE 라 '04'→'01' 전이에 그대로 성립).
 *
 * <p>self-invocation 으로는 {@code @Transactional} 이 적용되지 않으므로, 로그인 서비스와 분리된
 * 별도 빈으로 둔다.
 *
 * <p>결정(§1~§4): 빈 슬롯 NODE_CD null 이면 TB_USER.NODE_CD no-op(복귀자 부서 유지),
 * 값 있으면 갱신 / 재활성·슬롯 SQL 은 DailyJoinMapper 재사용(복제 금지) / 정원없음=DAILYLOGIN_400_004,
 * 토글 OFF=DAILYLOGIN_400_003 / 소진 경합=DAILYLOGIN_400_006.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReentryProcessor {

    // §3 — 재활성/슬롯 statement 는 DailyJoinMapper 를 재사용한다(복제 금지).
    private final DailyJoinMapper dailyJoinMapper;
    // 재활성 후 활성 단건 재조회(원자적 정합 가드)용.
    private final DailyLoginMapper dailyLoginMapper;
    // 입장 승인제(D6) — 승인요청 소진('02'→'05')을 같은 트랜잭션에서 수행.
    private final DailyEntryService dailyEntryService;
    // baim05-slot-default-sch — 점유 시 슬롯 기본 근무타입 → TB_USER.DEFAULT_SCH_CD 복사(로그인 게이트 setDefaultSch 미러).
    private final com.prafta.common.cmm.sch.service.DefaultSchOptionService defaultSchOptionService;
    private final com.prafta.common.cmm.sch.service.DefaultSchGenService defaultSchGenService;
    private final com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper defaultSchGenMapper;

    // 자동생성 트리거 운영 게이트(LoginServiceImpl/배치와 동일 프로퍼티, 코드 기본값 true 통일)
    @org.springframework.beans.factory.annotation.Value("${prafta.default-sch.gen.enabled:true}")
    private boolean defaultSchGenEnabled;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 승인된 일용직 행 {@code r} 을 재활성(또는 '04' 첫 활성화)하고 빈 슬롯을 점유한 뒤,
     * 승인요청 {@code approvedReqId} 를 소진('02'→'05')한다.
     *
     * @param approvedReqId 승인('02') 상태로 판정된 입장 승인요청 ID — 호출자가 findLoginDecision 으로 확정
     * @return 재활성 후 활성 로그인 대상 단건(USE_YN='Y' 가드 통과). 토큰 발급 등 후속은 호출자가 진행.
     * @throws ApiException 토글 OFF(003)/정원 부족(004)/종료 사업장·정합 깨짐(001)/소진 경합(006) 시 — 트랜잭션 롤백.
     */
    @Transactional
    public List<DailyUserResult> reactivateAndOccupy(DailyUserResult r, String approvedReqId) {
        final String cmpnyCd = r.cmpnyCd();
        final String siteCd = r.siteCd();
        final String userCd = r.userCd();

        // 1) 계정등록 토글(TB_DAILY_USER_LINK_POLICY.USE_YN='Y') 게이트 — OFF 면 차단(§4: 003 재사용).
        String linkPolicyUseYn = dailyJoinMapper.selectLinkPolicyUseYn(LinkPolicyQuery.of(cmpnyCd, siteCd));
        if (linkPolicyUseYn == null || !"Y".equals(linkPolicyUseYn)) {
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_003);
        }

        // 2) 보강2 — 재활성(USE_YN N→Y) 직전, 같은 회사·같은 휴대폰 HMAC 의 다른 활성 일용직 존재 검사.
        //    UX_TB_DAILY_USER_MBL(활성 한정 유니크) 위반으로 인한 미처리 500 을 사전 차단한다.
        //    비번 일치 이후 단계이고 통합 메시지(001)로만 응답하므로 enumeration 안전.
        if (r.mblNoHmac() != null && !r.mblNoHmac().isBlank()) {
            int activeMblDup = dailyJoinMapper.selectActiveDailyUserMblDupleCntExcept(cmpnyCd, r.mblNoHmac(), userCd);
            if (activeMblDup > 0) {
                throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
            }
        }

        // 2-1) prafta-daily-blacklist: 블랙리스트 게이트 — 차단 휴대폰이면 재활성(UPDATE) 전에 차단.
        //      비번 일치 이후 단계이고 사유(블랙리스트)는 노출하지 않는 통합성 메시지(005)로만 안내한다.
        if (r.mblNoHmac() != null && !r.mblNoHmac().isBlank()
                && dailyJoinMapper.selectActiveBlacklistCnt(cmpnyCd, r.mblNoHmac()) > 0) {
            log.info("일용직 로그인 자동 재활성 차단 - 블랙리스트 휴대폰 cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_005);
        }

        // 3) 보강1(b) — 사용자 행 재활성을 슬롯 조회보다 먼저 수행(행 락으로 동시 재입장 직렬화).
        //    같은 비활성 계정 동시 2요청은 여기서 직렬화되어, 후행 요청이 보강1(a)에서 선행 점유를 관측하게 된다.
        //    TB_DAILY_USER/TB_USER 재활성 + SITE_AUTH upsert. 기존 행 값(ID/이름/비번 해시/휴대폰 파생)을
        //    그대로 write-back 해 정보 손실 없이 활성으로 복귀. 이후 정원없음 throw 시 본 재활성도 함께 롤백.
        InsertDailyUserCommand command = InsertDailyUserCommand.ofReactivate(
                cmpnyCd, siteCd, userCd
                , r.userId(), r.userNm(), r.userPw()
                , r.mblNoEnc(), r.mblNoHmac(), r.mblNoLast4());

        dailyJoinMapper.reactivateDailyUser(command);
        int tbUserUpd = dailyJoinMapper.reactivateTbUser(command);
        if (tbUserUpd <= 0) {
            // 통합형 이전 레거시(TB_USER 행 부재) → 신규 INSERT 폴백.
            dailyJoinMapper.insertDailyUserToTbUser(command);
        }
        dailyJoinMapper.upsertTbUserSiteAuth(cmpnyCd, userCd, siteCd);

        // 4) 보강1(a) — 이 사용자가 이미 점유 중인 슬롯이 있으면 그 슬롯을 재사용(신규 점유/이력 적재 생략 = 멱등).
        //    없으면 빈 슬롯을 확보(없으면 정원 부족 004)해 조건부 UPDATE 로 점유한다.
        //    1차 방어(보강1a) + 사용자 행 락(보강1b)으로 1인 다중 슬롯 점유를 차단한다.
        String slotNo = dailyJoinMapper.selectSlotNoOccupiedByUser(cmpnyCd, siteCd, userCd);
        boolean alreadyOccupied = (slotNo != null && !slotNo.isBlank());
        if (!alreadyOccupied) {
            slotNo = dailyJoinMapper.selectFirstEmptySlotNo(EmptySlotQuery.of(cmpnyCd, siteCd));
            if (slotNo == null || slotNo.isBlank()) {
                throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_004);
            }
            // 빈 슬롯 점유(조건부 UPDATE). 0행이면 조회~점유 사이 선점됨 → 정원 부족(004)으로 롤백.
            int slotUpd = dailyJoinMapper.updateDailyUserSlotCurrUserCd(
                    DailyUserSlotUpdCommand.of(cmpnyCd, siteCd, slotNo, userCd));
            if (slotUpd <= 0) {
                throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_004);
            }
        }

        // 5) 슬롯 지정부서(NODE_CD) → TB_USER.NODE_CD 매칭(§1: null 이면 no-op = 복귀자 기존 부서 유지).
        String slotNodeCd = dailyJoinMapper.selectSlotNodeCd(cmpnyCd, siteCd, slotNo);
        if (slotNodeCd != null && !slotNodeCd.isBlank()) {
            dailyJoinMapper.updateTbUserNodeCdFromSlot(cmpnyCd, userCd, slotNodeCd);
        }

        // 6) 슬롯 점유 이력 적재 — 신규 점유 시에만(기존 슬롯 재사용 시 중복 이력 방지).
        if (!alreadyOccupied) {
            String hisId = dailyJoinMapper.selectDailySlotHisId(cmpnyCd);
            dailyJoinMapper.insertSlotHis(InsertSlotHisCommand.of(
                    hisId, cmpnyCd, siteCd, slotNo, LocalDate.now().format(YMD), r.userId()));
        }

        // 6-1) 입장 승인제(D6) — 승인요청 소진('02'→'05' 조건부 UPDATE, 같은 트랜잭션).
        //      0행 = 판정~소진 사이 상태 변경(동시 로그인 소진/자정 만료 경합) → 전체 롤백(재활성/슬롯 점유 원복).
        //      승인 대기 안내(006)로 응답하며, 재시도 시 판정이 처음부터 다시 수행된다.
        int consumed = dailyEntryService.consumeApprovedRequest(cmpnyCd, approvedReqId, userCd);
        if (consumed <= 0) {
            log.info("일용직 입장 승인요청 소진 경합 — userCd={}, reqId={} (전체 롤백)", userCd, approvedReqId);
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_006);
        }

        // 7) 활성 단건 재조회(원자성 가드) — USE_YN='Y' + 사업장 개방(B.USE_YN='Y') 통과 행만.
        //    종료 사업장/정합 깨짐이면 0건 → 통합 차단(001) 으로 전체 롤백(슬롯 점유까지 원복).
        List<DailyUserResult> active = dailyLoginMapper.selectDailyUserForLogin(
                new DailyLoginQuery(r.userId(), cmpnyCd));
        if (active == null || active.size() != 1) {
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
        }

        // 8) baim05-slot-default-sch — 슬롯 기본 근무타입 → TB_USER.DEFAULT_SCH_CD 복사.
        //    NODE_CD 매칭(§1)과 동일 규칙: 슬롯 미지정(NULL)이면 no-op(복귀자 기존 값/본인 선택 폴백),
        //    지정돼 있으면 관리자 지정값이 우선(덮어씀). 저장은 로그인 게이트(setDefaultSch) 미러 —
        //    updateUserDefaultSch(설정시점 포함) + 즉시 자동생성(applyDefaultSchChange, 실패 격리).
        //    ★자동생성은 REQUIRES_NEW(별도 커밋)라, 이후 단계 롤백 시 계획만 남는 정합 깨짐을 피하기 위해
        //      모든 롤백 유발 단계(승인 소진/활성 재조회) 통과 후 마지막에 수행한다.
        applySlotDefaultSchToUser(cmpnyCd, siteCd, slotNo, userCd);

        // PII(휴대폰) 평문 로그 금지 — 식별 키만 남긴다.
        log.info("일용직 로그인 승인 재활성 완료 — userCd={}, slotNo={}, reqId={}", userCd, slotNo, approvedReqId);
        return active;
    }

    /**
     * 슬롯 기본 근무타입 → 점유 사용자 TB_USER.DEFAULT_SCH_CD 복사(baim05-slot-default-sch).
     *
     * <p>Baim05ServiceImpl.applySlotDefaultSchToUser(QR 발급 경로)와 동일 규칙:
     * 슬롯 미지정이면 no-op / 사용자 현재값과 동일하면 생략(멱등) / 복사 시점 화이트리스트
     * 재검증 실패 시 생략(fail-open — 로그인 자체를 막지 않음) / 자동생성 실패는 격리.
     * 자가 로그인 경로이므로 설정 조작자(operatorNo)는 본인 USER_CD.
     */
    private void applySlotDefaultSchToUser(String cmpnyCd, String siteCd, String slotNo, String userCd) {
        String slotSchCd = dailyJoinMapper.selectSlotDefaultSchCd(cmpnyCd, siteCd, slotNo);
        if (slotSchCd == null || slotSchCd.isBlank()) {
            return;
        }

        // 사용자 현재값과 동일하면 갱신/재생성 생략(재입장 반복 시 불필요한 계획 재생성 방지)
        com.prafta.common.cmm.sch.vo.DefaultSchUserVO current = defaultSchGenMapper.selectDefaultSchUser(cmpnyCd, userCd);
        if (current != null && slotSchCd.equals(current.defaultSchCd())) {
            return;
        }

        // 복사 시점 화이트리스트 재검증(근무타입 비활성화 등 이후 변경 대비). 실패 시 복사 생략(fail-open).
        if (!defaultSchOptionService.isValidDefaultSch(cmpnyCd, siteCd, slotSchCd)) {
            log.warn("슬롯 기본 근무타입 복사 생략(사업장 활성 근무타입 아님) - siteCd={}, slotNo={}, schCd={}",
                    siteCd, slotNo, slotSchCd);
            return;
        }

        defaultSchGenMapper.updateUserDefaultSch(cmpnyCd, userCd, slotSchCd, userCd);

        // 즉시 자동생성 — 운영 게이트 on 일 때만. 실패는 격리(재입장 트랜잭션을 롤백시키지 않음).
        if (defaultSchGenEnabled) {
            try {
                defaultSchGenService.applyDefaultSchChange(cmpnyCd, siteCd, userCd, slotSchCd);
            } catch (Exception e) {
                log.error("슬롯 기본 근무타입 복사 — 자동생성 실패(설정은 저장됨) — userCd={}, schCd={}", userCd, slotSchCd, e);
            }
        }

        log.info("슬롯 기본 근무타입 복사 완료 - siteCd={}, slotNo={}, userCd={}, schCd={}", siteCd, slotNo, userCd, slotSchCd);
    }
}
