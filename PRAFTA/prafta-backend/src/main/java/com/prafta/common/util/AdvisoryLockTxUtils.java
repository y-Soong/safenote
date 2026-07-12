package com.prafta.common.util;

import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * MySQL advisory lock(GET_LOCK) 해제를 트랜잭션 완료(afterCompletion) 시점으로 미루는 헬퍼.
 *
 * <p><b>왜 필요한가 (보안리뷰 Medium):</b> {@code @Transactional} 메서드의 finally 는 커밋
 * <i>이전</i>에 실행된다. 락을 finally 에서 해제하면 "해제 ~ 커밋" 사이 창에서 후속 요청이
 * 미커밋 스냅샷(REPEATABLE READ)으로 누적/한도 가드를 판정해 직렬화가 우회될 수 있다.
 * 본 헬퍼는 해제를 {@link TransactionSynchronization#afterCompletion(int)} — 커밋/롤백
 * 불문 항상 호출 — 로 미뤄 이 창을 제거한다.
 *
 * <p><b>같은 커넥션 보장 근거:</b> RELEASE_LOCK 은 GET_LOCK 을 잡은 커넥션(세션)에서
 * 실행해야 한다. Spring 의 afterCompletion 은 {@code AbstractPlatformTransactionManager}
 * 가 doCommit/doRollback 직후, {@code cleanupAfterCompletion}(커넥션 스레드 언바인딩·풀
 * 반환) <i>이전</i>에 호출한다. 따라서 이 시점엔 트랜잭션 커넥션이 여전히 스레드에
 * 바인딩되어 있어, MyBatis 매퍼 호출(SqlSessionTemplate → DataSourceUtils.getConnection)
 * 이 GET_LOCK 을 잡았던 동일 커넥션에서 RELEASE_LOCK 을 실행한다.
 *
 * <p><b>해제 실패 안전망:</b> release 콜백(각 호출부의 기존 release* 메서드)은 예외를
 * 삼키고 warn 로그만 남긴다 — GET_LOCK 은 세션 단위라 커넥션(세션) 종료 시 자동
 * 해제되는 안전망이 있다.
 *
 * <p><b>기존 관례 미러:</b> {@code isSynchronizationActive()} 가드 + 등록 실패 시 폴백
 * 패턴은 커밋 후 PUSH 훅(common.cmm.push.impl.*, Tbm01ServiceImpl 등) 다수에서 이미
 * 검증된 방식이다. 단, 커밋 후 훅(afterCommit)과 달리 락 해제는 롤백 시에도 필요하므로
 * afterCompletion 을 사용한다.
 */
public final class AdvisoryLockTxUtils {

    private AdvisoryLockTxUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * advisory lock 해제를 현재 트랜잭션의 afterCompletion(커밋/롤백 불문)으로 등록한다.
     *
     * <p>중첩(join) 트랜잭션에서는 최외곽 트랜잭션 완료 시점에 해제된다 — 락 보호 구간이
     * 실제 데이터가 보이는 시점(외부 커밋)까지 자연 연장되므로 의도된 동작이다.
     *
     * @param lockKey GET_LOCK 에 사용한 키 (null 이면 등록하지 않음)
     * @param release 해제 콜백(호출부의 기존 release* 메서드 — 예외 삼킴/warn 로깅 책임 포함)
     * @return true = afterCompletion 에 해제 등록됨(호출부 finally 해제 생략).
     *         false = 트랜잭션 동기화 비활성(이론상 없음, 방어) — 호출부가 기존 finally 에서 직접 해제.
     */
    public static boolean deferReleaseToAfterCompletion(String lockKey, Consumer<String> release) {
        if (lockKey == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                release.accept(lockKey);
            }
        });
        return true;
    }
}
