package com.prafta.common.cmm.sch.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 근무일 기준 유효 스케줄(effective-dating) 판정 SQL fragment 전용 홀더 매퍼.
 *
 * <p><b>메서드가 없다.</b> 동명 XML 의 {@code <sql>} fragment
 * ({@code effectiveSchVersion} / {@code effectiveSchApplyKeys}) 를 앱·웹 양쪽 매퍼가
 * 완전수식 {@code refid}(namespace.fragmentId)로 참조하기 위한 네임스페이스 홀더다.
 * 원 위치는 {@code AppAdminApprovalMapper.xml} 이었고 2026-08-14 이관했다(판정 규칙 단일 출처).
 *
 * <p>★패키지가 {@code .mapper} 로 끝나야 {@code MainApplication} 의
 *    {@code @MapperScan("com.prafta.**.**.mapper")} 에 잡힌다.
 * <p>★XML 은 인터페이스 FQN 과 1:1 경로에 둔다
 *    ({@code resources/com/prafta/common/cmm/sch/mapper/SchEffectiveMapper.xml}).
 *    {@code DBConfig} 의 {@code classpath*:mapper/**}{@code /*.xml} 패턴은 {@code com/prafta/...} 를
 *    잡지 못하며, 실제로는 MyBatis 가 인터페이스 FQN 과 같은 경로의 XML 을 자동 로딩하는 규칙으로
 *    매핑된다. 즉 <b>이 인터페이스가 없으면 XML 자체가 로딩되지 않는다.</b>
 *
 * <p>★<b>삭제 금지 — 실패 시점 주의(qa 2026-08-14 정정).</b> 이 파일을 지우면 "빈 인터페이스라
 *    지워도 되겠지" 와 달리 <b>기동은 성공한다.</b> {@code XMLMapperBuilder.buildStatementFromContext}
 *    가 {@code IncompleteElementException} 을 삼켜 {@code incompleteStatements} 로 보류하기 때문이다.
 *    실제 폭발은 {@code Configuration.getMappedStatement} → {@code buildAllStatements()} 가 도는
 *    <b>최초 매퍼 호출 시점</b>이며, 그 시점에 스케줄 쿼리뿐 아니라 <b>앱 전체가 실패</b>한다.
 *    바꿔 말하면 배포 전 <b>아무 API 1회 스모크 호출</b>만으로 이 위험을 확정적으로 검출할 수 있다.
 */
@Mapper
public interface SchEffectiveMapper {
    // 의도적으로 비어 있음 - XML <sql> fragment 로딩 목적의 홀더.
}
