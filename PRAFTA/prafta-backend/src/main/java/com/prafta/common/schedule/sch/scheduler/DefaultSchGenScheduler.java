package com.prafta.common.schedule.sch.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper;
import com.prafta.common.cmm.sch.service.DefaultSchGenService;
import com.prafta.common.cmm.sch.vo.DefaultSchUserVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기본근무 자동 스케줄 생성 배치 (PRAFTA-COM-008-E-3 트리거1). <b>기본 비활성(게이트)</b>.
 *
 * <p>정책서: {@code .claude/context/policies/attd/06-schedule.md} §6.1~§6.2,
 *   prafta-com-008-E-default-worktype.md §4-2(연차부여 스케줄러와 동반).
 *
 * <p>매년 1/1 00:35 실행(연차부여 {@code LeaveGrantScheduler} 00:30 직후). 게이트는 독립
 *   {@code prafta.default-sch.gen.enabled}(기본 false). 운영에서 검증 후 켠다(D-E6: 강결합 회피).
 *   기본 근무타입 설정 사용자 중 교대 비소속자에게 "그해 1/1~12/31" 빈 평일을 일괄 생성한다(멱등).
 *
 * <p>한 사용자 실패가 다른 사용자/회사를 막지 않도록 사용자 단위로 예외를 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSchGenScheduler {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DefaultSchGenMapper defaultSchGenMapper;
    private final DefaultSchGenService defaultSchGenService;

    /** 게이트: 기본 false(비활성). 운영 검증 후 true. 연차부여 게이트와 독립. */
    @Value("${prafta.default-sch.gen.enabled:false}")
    private boolean genEnabled;

    /**
     * 기본 매년 1/1 00:35 실행(연차부여 00:30 직후). {@code prafta.default-sch.gen.cron} 으로 재정의 가능.
     * cron: 초 분 시 일 월 요일. Spring Scheduling 은 서버 기본 TimeZone(Asia/Seoul)을 따른다.
     */
    @Scheduled(cron = "${prafta.default-sch.gen.cron:0 35 0 1 1 *}")
    public void runYearlyGeneration() {
        if (!genEnabled) {
            log.debug("기본근무 자동생성 배치 비활성(prafta.default-sch.gen.enabled=false) — 건너뜀");
            return;
        }
        LocalDate now = LocalDate.now();
        String fromYmd = now.withDayOfYear(1).format(YMD);          // 그해 1/1
        String toYmd = now.withMonth(12).withDayOfMonth(31).format(YMD); // 그해 12/31
        log.info("기본근무 자동생성 배치 시작 — 범위 {}~{}", fromYmd, toYmd);

        int totalUsers = 0;
        int totalDays = 0;
        List<String> companies = defaultSchGenMapper.selectDefaultSchCompanyCds();
        for (String cmpnyCd : companies) {
            List<DefaultSchUserVO> users = defaultSchGenMapper.selectDefaultSchUsers(cmpnyCd);
            for (DefaultSchUserVO u : users) {
                try {
                    int created = defaultSchGenService.generateForUser(
                            u.cmpnyCd(), u.siteCd(), u.userCd(), u.defaultSchCd(), fromYmd, toYmd);
                    totalDays += created;
                    totalUsers++;
                } catch (Exception e) {
                    // 사용자 단위 격리(해당 사용자 트랜잭션만 롤백).
                    log.error("기본근무 자동생성 사용자 실패 — cmpnyCd={}, userCd={}", u.cmpnyCd(), u.userCd(), e);
                }
            }
        }
        log.info("기본근무 자동생성 배치 완료 — 회사 {}곳, 대상 {}명, 생성 {}일",
                companies.size(), totalUsers, totalDays);
    }
}
