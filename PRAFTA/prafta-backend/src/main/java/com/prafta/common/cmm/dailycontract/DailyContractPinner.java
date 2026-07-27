package com.prafta.common.cmm.dailycontract;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.dailycontract.mapper.DailyContractMapper;
import com.prafta.common.cmm.dailycontract.result.ActiveContractResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 승인 시점 계약서 버전 확정(pin) 해석 전용 컴포넌트 — 승인 시점 버전확정 T1.
 *
 * <p><b>존재 이유(순환 의존 회피, plan §7 R1)</b>: 승인 트랜잭션({@code DailyEntryServiceImpl})이
 * 활성 계약서 버전을 알아야 하는데, {@code DailyContractServiceImpl} 은 이미
 * {@code DailyEntryService}(사업장 인가 가드 재사용)를 주입받고 있다. 여기서 승인 쪽이
 * {@code DailyContractService} 를 주입하면 Spring 순환 참조로 <b>애플리케이션 기동이 실패</b>한다
 * (Boot 2.6+ 는 순환 참조 기본 금지). 그래서 매퍼만 의존하는 얇은 컴포넌트를 두고,
 * 의존 방향을 단방향으로 고정한다.
 *
 * <pre>
 * DailyContractServiceImpl -> DailyEntryService -> DailyContractPinner -> DailyContractMapper
 * </pre>
 *
 * <p>본 클래스는 <b>서비스 Bean 을 주입받지 않는다</b>(신규 의존 추가 금지 — 추가하면 순환이 되살아난다).
 * 계약서 SQL 을 {@code DailyEntryMapper} 에 넣는 대안은 도메인 경계를 흐리므로 채택하지 않았다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyContractPinner {

    private final DailyContractMapper dailyContractMapper;

    /**
     * 승인 시점에 확정(pin)할 계약서 버전 — 사업장 활성(USE_YN='Y') 버전, 없으면 센티넬 {@code 0}.
     *
     * <p>센티넬 0 의 의미는 "승인 시점에 계약서가 미등록이었다"(K4)이며, 그 사이클은 서명 게이트를
     * 건너뛴다(이후 관리자가 등록해도 그 사이클에는 적용하지 않고 다음 승인 사이클부터 적용).
     * {@code NULL}(= 컬럼 미기록)은 <b>배포 전 레거시 승인 전용</b> 의미이므로 이 메서드는 절대
     * null 을 반환하지 않는다 — 두 의미를 값으로 구분해야 배포 기준시각을 코드에 박지 않아도 된다.
     *
     * <p>버전 채번이 {@code IFNULL(MAX(CONTRACT_VER),0)+1} 이라 실제 버전은 1부터 시작한다.
     * 따라서 0 은 실존 버전과 충돌하지 않는다.
     */
    public int resolveActiveVerForPin(String cmpnyCd, String siteCd) {
        ActiveContractResult active = dailyContractMapper.selectActiveContract(cmpnyCd, siteCd);
        if (active == null) {
            log.info("승인 시점 활성 계약서 없음(pin=0, 해당 사이클 서명 게이트 스킵) — cmpnyCd={}, siteCd={}",
                    cmpnyCd, siteCd);
            return 0;
        }
        return active.contractVer();
    }
}
