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
import com.prafta.common.cmm.dailylogin.application.query.DailyLoginQuery;
import com.prafta.common.cmm.dailylogin.mapper.DailyLoginMapper;
import com.prafta.common.cmm.dailylogin.result.DailyUserResult;
import com.prafta.common.error.dailylogin.DailyLoginErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-032 B — 일용직 로그인 자동 재활성(무마찰 재입장) 트랜잭션 처리기.
 *
 * <p>비밀번호 검증/실패카운트 누적은 {@link DailyLoginServiceImpl#login} 측(비-Transactional)에서 끝낸 뒤,
 * <b>비밀번호가 일치하고 재활성 대상</b>인 경우에만 본 처리기를 호출한다. 재활성 + 슬롯 점유 + 이력 적재는
 * 하나의 트랜잭션으로 묶여, 토글 OFF/정원 부족/종료 사업장/정합 깨짐 시 전체 롤백된다(부분 점유 방지).
 *
 * <p>self-invocation 으로는 {@code @Transactional} 이 적용되지 않으므로, 로그인 서비스와 분리된
 * 별도 빈으로 둔다.
 *
 * <p>결정(§1~§4): 빈 슬롯 NODE_CD null 이면 TB_USER.NODE_CD no-op(복귀자 부서 유지),
 * 값 있으면 갱신 / 재활성·슬롯 SQL 은 DailyJoinMapper 재사용(복제 금지) / 정원없음=DAILYLOGIN_400_004,
 * 토글 OFF=DAILYLOGIN_400_003.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReentryProcessor {

    // §3 — 재활성/슬롯 statement 는 DailyJoinMapper 를 재사용한다(복제 금지).
    private final DailyJoinMapper dailyJoinMapper;
    // 재활성 후 활성 단건 재조회(원자적 정합 가드)용.
    private final DailyLoginMapper dailyLoginMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 비활성 일용직 행 {@code r} 을 재활성하고 빈 슬롯을 점유한다.
     *
     * @return 재활성 후 활성 로그인 대상 단건(USE_YN='Y' 가드 통과). 토큰 발급 등 후속은 호출자가 진행.
     * @throws ApiException 토글 OFF(003)/정원 부족(004)/종료 사업장·정합 깨짐(001) 시 — 트랜잭션 롤백.
     */
    @Transactional
    public List<DailyUserResult> reactivateAndOccupy(DailyUserResult r) {
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

        // 7) 활성 단건 재조회(원자성 가드) — USE_YN='Y' + 사업장 개방(B.USE_YN='Y') 통과 행만.
        //    종료 사업장/정합 깨짐이면 0건 → 통합 차단(001) 으로 전체 롤백(슬롯 점유까지 원복).
        List<DailyUserResult> active = dailyLoginMapper.selectDailyUserForLogin(
                new DailyLoginQuery(r.userId(), cmpnyCd));
        if (active == null || active.size() != 1) {
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_400_001);
        }

        // PII(휴대폰) 평문 로그 금지 — 식별 키만 남긴다.
        log.info("일용직 로그인 자동 재활성 완료 — userCd={}, slotNo={}", userCd, slotNo);
        return active;
    }
}
