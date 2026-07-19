package com.prafta.web.subcon.subcon03.service;

import java.util.Collection;
import java.util.Set;

/**
 * 제3자 제공 동의 필터 확장점(마스터 §1-7 — PRAFTA-SUBCON-T3 §5-5 / D7).
 *
 * <p>T3 는 <b>인터페이스만</b> 정의한다. 구현체 빈이 없으면(현재) 스냅샷 대상 근로자는 전원 포함된다
 * (ObjectProvider 주입 — T1 RelationTerminationHandler 훅 패턴 승계). T4(동의 관리)가 @Component 를
 * 등록하면 T3 코드 수정 없이 연결된다.
 *
 * <p><b>T4 연결 시 반드시 재검토할 것(security 인계 §11-8)</b>: 구현체가 예외를 던지거나 조회에 실패했을 때
 * "전원 포함"으로 fallback 하면 미동의자 데이터가 반출된다(fail-open). 구현체는 fail-closed 여야 하며,
 * 호출측(Subcon03ServiceImpl)은 예외를 삼키지 않고 승인 트랜잭션 전체를 롤백한다.
 */
public interface ThirdPartyConsentProvider {

    /**
     * 제3자 제공에 동의한 근로자만 남겨 반환한다.
     *
     * @param cmpnyCd    제공측 회사코드
     * @param siteCd     제공 대상 사업장코드
     * @param workerType 근로자 구분(REGULAR / DAILY — 일용직 동의 경로 분기용)
     * @param userCds    후보 근로자 사용자코드 집합
     * @return 동의한 근로자의 사용자코드 집합(부분집합)
     */
    Set<String> filterConsented(String cmpnyCd, String siteCd, String workerType, Collection<String> userCds);
}
