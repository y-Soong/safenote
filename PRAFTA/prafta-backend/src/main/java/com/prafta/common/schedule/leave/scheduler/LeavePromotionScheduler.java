package com.prafta.common.schedule.leave.scheduler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.leave.promotion.mapper.LeavePromotionMapper;
import com.prafta.common.cmm.leave.promotion.result.PromotionTargetResult;
import com.prafta.common.cmm.leave.promotion.result.PromotionTargetResult.PromotionStage;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionNotiService;
import com.prafta.common.cmm.leave.promotion.service.LeavePromotionService;
import com.prafta.common.cmm.leave.promotion.vo.PromotionLogInsertVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 사용촉진 일배치 (PRAFTA-COM-008-A-1 트리거). <b>기본 비활성(게이트)</b>.
 *
 * <p>정책서: attd/08-leave §8.5.2(AXIS7)·§8.5.8(멱등), 공통 §10.2(예정일 도래 리마인더).
 *   작업지시서 prafta-com-008-A-promotion.md §1-1(단일 일배치).
 *
 * <p>매일 1회 실행. 게이트 {@code prafta.leave-promotion.enabled}(기본 false) — 운영 검증 후 켠다
 *   (E {@code DefaultSchGenScheduler} 관례). cron 은 연차부여/기본스케줄 배치 인접 시각(00:40).
 *
 * <p>흐름: 기준일(today) 1회 산출 → {@link LeavePromotionService#resolveDueTargets} 로 1차/2차
 *   도래자 산출 → 1차는 {@link LeavePromotionNotiService#notifyFirstPromotion}(마스터+PUSH),
 *   2차는 마스터에 STAGE2_TARGET_DAYS 기록(웹 A-4 화면이 조회). 사용자 단위 예외 격리.
 *
 * <p><b>도래 판정은 구간 판정</b>이다(작업지시서_연차촉진-1차현황-화면-및-배치활성화 §4, D5·D6·D8).
 *   1차 = 회차 FIRST 마스터 부재 + today 가 [만료-6개월, 만료-2개월) 구간,
 *   2차 = 회차 FIRST 마스터 존재 + today 가 max(만료-3개월, 통지일+10일) 이상이며 만료-2개월 미만.
 *   배치가 특정 하루를 놓쳐도 다음 실행에서 흡수되므로(소급 통지 D6) 별도 백필 스크립트가 필요 없다.
 *   중복 통지는 UNIQUE(CMPNY_CD, DEDUP_KEY) + {@code DuplicateKeyException} 흡수로 차단된다(멱등 불변).
 *
 * <p>결정성: {@code today} 를 본 배치에서 1회 산출해 서비스에 주입한다(서비스는 now() 미호출).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeavePromotionScheduler {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SYSTEM_OPERATOR = "SYSTEM";

    private final LeavePromotionService leavePromotionService;
    private final LeavePromotionNotiService leavePromotionNotiService;
    private final LeavePromotionMapper leavePromotionMapper;

    /** 게이트: 기본 false(비활성). 운영 검증 후 true. */
    @Value("${prafta.leave-promotion.enabled:false}")
    private boolean promotionEnabled;

    /**
     * 기본 매일 00:40 실행(연차부여 00:30 / 기본스케줄 00:35 직후).
     * {@code prafta.leave-promotion.cron} 으로 재정의 가능. Asia/Seoul.
     */
    @Scheduled(cron = "${prafta.leave-promotion.cron:0 40 0 * * *}")
    public void runDailyPromotion() {
        if (!promotionEnabled) {
            log.debug("연차 사용촉진 배치 비활성(prafta.leave-promotion.enabled=false) — 건너뜀");
            return;
        }
        LocalDate today = LocalDate.now();
        String todayYmd = today.format(YMD);
        log.info("연차 사용촉진 배치 시작 — 기준일 {}", todayYmd);

        List<PromotionTargetResult> targets = leavePromotionService.resolveDueTargets(today);

        int firstNotified = 0;
        int secondRecorded = 0;
        for (PromotionTargetResult t : targets) {
            try {
                if (t.stage() == PromotionStage.FIRST) {
                    firstNotified += leavePromotionNotiService.notifyFirstPromotion(t, todayYmd, SYSTEM_OPERATOR);
                } else if (t.stage() == PromotionStage.SECOND) {
                    secondRecorded += recordSecondStageTarget(t);
                }
            } catch (Exception e) {
                // 사용자 단위 격리(다른 도래자 처리 계속).
                log.error("연차 사용촉진 처리 실패 — cmpnyCd={}, userCd={}, stage={}",
                        t.cmpnyCd(), t.userCd(), t.stage(), e);
            }
        }
        log.info("연차 사용촉진 배치 완료 — 도래 {}건, 1차 통지 {}건, 2차 대상 기록 {}건",
                targets.size(), firstNotified, secondRecorded);
    }

    /**
     * 2차 도래자의 직권지정 대상 잔여(STAGE2_TARGET_DAYS)를 마스터에 멱등 기록한다.
     * 통지(PUSH)는 실제 직권 지정 시점(A-4)에 발송하므로 여기서는 적재만 한다.
     * 단일 INSERT(원자적)라 별도 트랜잭션 경계가 필요 없다.
     *
     * @return 신규 기록 1, 멱등 스킵 0
     */
    private int recordSecondStageTarget(PromotionTargetResult t) {
        String dedupKey = "PROMO_DESIG_" + t.userCd() + "_" + t.baseAvailToDate();
        PromotionLogInsertVO vo = new PromotionLogInsertVO();
        vo.setPromoId(leavePromotionMapper.selectNextPromoId(t.cmpnyCd()));
        vo.setCmpnyCd(t.cmpnyCd());
        vo.setSiteCd(t.siteCd());
        vo.setUserCd(t.userCd());
        vo.setBaseGrantId(t.baseGrantId());
        vo.setBaseAvailToDate(t.baseAvailToDate());
        vo.setPromoStage("SECOND");
        vo.setNoticedDate(null);
        vo.setStage1DesignatedDays(BigDecimal.ZERO);
        vo.setStage2TargetDays(t.remainingDays());
        vo.setStage2DesignatedDate(null);
        // 도래만 기록(직권 지정 전). 실제 지정은 A-4 가 STATUS=DESIGNATED 로 갱신/적재.
        vo.setStatus("NOTICED");
        vo.setDedupKey(dedupKey);
        vo.setInsertNo(SYSTEM_OPERATOR);
        try {
            leavePromotionMapper.insertPromotionLog(vo);
            log.info("[leavePromotion] 2차 대상 잔여 기록 — userCd={}, availTo={}, 대상잔여={}",
                    t.userCd(), t.baseAvailToDate(), t.remainingDays());
            return 1;
        } catch (DuplicateKeyException dup) {
            log.info("[leavePromotion] 2차 대상 잔여 멱등 스킵(이미 기록됨) — userCd={}, availTo={}",
                    t.userCd(), t.baseAvailToDate());
            return 0;
        }
    }
}
