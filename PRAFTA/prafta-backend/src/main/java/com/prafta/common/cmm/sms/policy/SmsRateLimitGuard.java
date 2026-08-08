package com.prafta.common.cmm.sms.policy;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.sms.AuthCodeSmsDispatcher;
import com.prafta.common.cmm.sms.policy.mapper.SmsSendPolicyMapper;
import com.prafta.common.config.SmsProperties;
import com.prafta.common.error.ApiErrorCode;
import com.prafta.common.error.sms.SmsErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SMS 발송 다층 상한 가드(SMS2-B1 · B3 · B4).
 *
 * <p>진입점 3곳(A: 무인증 셀프가입·계정찾기 / B: 앱 휴대폰 변경 / C: 플랫폼 위치열람)이 공통으로 사용한다.
 * 호출부는 {@link #guardAndInsert} 한 줄로 "상한 판정 + 인증코드 INSERT" 를 위임한다.
 *
 * <h3>★TOCTOU 봉인 설계 (요청서 2차 B-2)</h3>
 * 카운트 조회와 INSERT 사이에 잠금이 없으면 동시 요청이 전부 카운트를 통과한다(1차 qa D-6).
 * 본 클래스는 <b>정책행 1건을 배타 잠금</b>해 전역 직렬화하고, 그 트랜잭션 안에서
 * [정책 읽기 → 킬스위치 판정 → 4축 카운트 → 인증코드 INSERT] 를 수행한다.
 * 시간당 500건 규모라 전역 직렬화 비용은 무시할 수 있고, 번호·IP·사용자·전역 4축을 한 번에 봉인한다.
 *
 * <h3>★★잠금 보유 중 금지 사항 (3차 / sec N-1 · qa Q-1 — 재발 방지)</h3>
 * <b>이 트랜잭션이 정책행 X 잠금을 보유한 동안, 같은 행({@code TB_SMS_SEND_POLICY})을
 * <u>다른 커넥션</u>으로 건드리는 호출을 절대 만들지 말 것.</b>
 * <p>2차 코드는 {@link SmsKillSwitchService#fire} 를 {@code REQUIRES_NEW} 로 호출했다.
 * {@code REQUIRES_NEW} 는 별도 커넥션을 잡으므로, 부모가 쥐고 있는 바로 그 행을 UPDATE 하려다
 * <b>자기 자신의 잠금을 기다리는 자기 교착</b>이 됐다. 대기자가 하나뿐이라 InnoDB 데드락 감지에
 * 걸리지 않아 1213 이 아니라 {@code innodb_lock_wait_timeout}(기본 50초) 만료 후 1205 로 실패했고,
 * 그 결과 <b>킬스위치가 한 번도 발동하지 못한 채</b> 요청당 커넥션 2~3개를 50초씩 점유해
 * 과금 폭주 국면에서 전 서비스를 함께 무너뜨렸다.
 * <p>★아래 절("외부 HTTP 를 트랜잭션에 넣지 않는다")의 확신이 이 문제를 가렸다.
 * <b>외부 HTTP 만이 위험한 것이 아니다 — 잠금 보유 중의 "다른 커넥션" 자체가 위험하다.</b>
 * 새 호출을 추가할 때는 그 안쪽이 {@code REQUIRES_NEW}/별도 DataSource/비동기 실행으로
 * 커넥션을 갈아타지 않는지 반드시 확인할 것.
 * <p>현재 잠금 구간 안의 중첩 트랜잭션은 감사로그({@code AuditLogService}, {@code REQUIRES_NEW})뿐이며
 * 대상 테이블이 {@code TB_AUDIT_LOG} 라 충돌하지 않는다.
 *
 * <h3>★{@code @Transactional} 을 새로 도입하는데 요청서 §7-3 위반이 아닌 이유</h3>
 * §7-3 이 금지한 것은 <b>외부 HTTP 호출을 트랜잭션 안에 넣는 것</b>이다.
 * 본 트랜잭션 범위는 [정책행 잠금 → 정책 SELECT → 4축 카운트 → 인증코드 INSERT] 이며 외부 HTTP 가 없다.
 * {@code AuthCodeSmsDispatcher.dispatch()} 는 이 트랜잭션이 <b>커밋된 뒤</b> 호출되어야 한다
 * (그래야 {@code SmsSendResultRecorder}(REQUIRES_NEW)가 해당 행을 볼 수 있고,
 *  발송 실패로 인증코드 행이 롤백되지도 않는다).
 * 잠금 보유 중 네트워크를 기다리면 전역 직렬화가 그대로 전체 장애가 되므로 이 경계는 반드시 지킬 것.
 *
 * <h3>★게이트 OFF 드라이런 (요청서 2차 B-5 / 3차 qa Q-2 로 부분 개정)</h3>
 * {@code prafta.sms.enabled=false} 면 <b>시간·일·전역 축은 판정만 하고 차단하지 않는다</b>.
 * 근거: 게이트 OFF 는 문자가 나가지 않는 상태라 과금 보호 대상이 애초에 없고,
 * 무회귀 기준선이 "SMS 연동 이전 = 서버측 제한 전무" 이기 때문이다.
 * 차단하지 않은 판정 결과는 {@code [SMS상한:드라이런]} 프리픽스로 남겨 임계값 튜닝 근거로 쓴다.
 * <p>★★<b>예외 1 — 번호 창(55초)은 전 진입점에서 게이트와 무관하게 항상 강제한다</b>(3차 / qa Q-2).
 * 2차는 진입점 A·B 의 창까지 드라이런으로 돌렸는데, 진입점 A 는 {@code @NoAuth} 라 그 순간
 * 서버측 상한이 <b>전면 소멸</b>했다(1차 코드는 1분 창으로 차단했으므로 1차 대비 후퇴다).
 * 결과적으로 무인증 요청 하나로 {@code TB_SMS_AUTH_CODE} 무한 증식 +
 * 전역 정책행 직렬화(모든 SMS 흐름이 한 행 뒤에 줄서기)가 가능했다. 과금이 0 이어도 가용성이 무너진다.
 * 진입점 C 의 창은 애초에 SMS 연동 이전부터 존재하던 보안 통제였다(보안 리뷰 V-2, {@code PLATFORM_400_013}).
 * <p>★예외 2 — 인증번호 <b>대입</b> 방어({@link SmsVerifyGuard})는 본 클래스와 무관하며
 * 게이트와 상관없이 항상 동작한다. 대입 공격은 문자 발송 없이 성립하므로 게이트로 가려선 안 된다.
 *
 * <h3>★★★잠금 안 재검사가 "최신값" 을 보게 하는 조건 (4차 / sec T-1 · qa R-1 — 재발 방지)</h3>
 * <b>이 트랜잭션의 첫 <u>비잠금</u> SELECT 가 REPEATABLE READ 의 read view(스냅샷)를 고정한다.</b>
 * {@code SELECT ... FOR UPDATE} 는 locking read 라 항상 최신값을 읽지만 read view 를 <b>재생성하지 않는다</b>.
 * <p>3차는 성능을 위해 잠금 <b>앞에</b> 선검사({@code selectPolicyNoLock})를 넣었다. 그 순간
 * 스냅샷이 {@code FOR UPDATE} 획득 <b>이전</b>으로 앞당겨져, 잠금 안에서 재실행하는 4축·전역 카운트가
 * 전부 <b>stale</b> 이 됐다 — 동시 요청이 각자 스냅샷을 뜬 뒤 줄을 서고, 차례로 잠금을 얻어도
 * 앞선 요청의 커밋이 보이지 않아 카운트가 0 → <b>전원 통과·전원 INSERT</b>.
 * 2차 B-2 / 1차 qa D-6 이 세운 TOCTOU 봉인이 <b>선검사 한 줄로 무너졌다</b>.
 * <p>→ 4차는 {@code isolation = READ_COMMITTED} 로 근본 원인을 제거한다.
 * READ_COMMITTED 에서는 <b>statement 마다 새 read view</b> 가 생성되므로, 잠금 대기 중 커밋된 건이
 * 잠금 획득 후의 재검사에 반드시 보인다. 정책행 X 잠금에 의한 직렬화 자체는 격리수준과 무관하게 동작한다.
 * <p>★<b>성능 최적화를 정확성 경로에 넣을 때는 그 최적화가 만드는 "순서·가시성 변화" 를 반드시 따져볼 것.</b>
 * 3차 주석은 "정확성은 잠금 안 재검사가 담당한다" 고 단언했지만, 그 재검사가 무엇을 보는지는 검토되지 않았다.
 * <p>★전제: {@code binlog_format=ROW}(RDS MySQL 8.4 기본). STATEMENT 이면 READ_COMMITTED 에서
 * 쓰기가 1665 로 거부된다 — 배포 절차서의 선행 확인 항목이다.
 *
 * <h3>★캐시를 두지 않는 이유 (요청서 2차 C-3)</h3>
 * 차단 판정 경로가 어차피 정책행을 잠금 조회하므로 <b>캐시로 절약되는 쿼리가 0</b>이다.
 * 캐시를 넣으면 이득 없이 "화면에서 바꾼 임계값이 반영되지 않는" 리스크만 생긴다.
 * 무캐시라 다중 인스턴스로 스케일아웃해도 정합성이 자동 유지된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsRateLimitGuard {

    /**
     * 전역 소진율 사전 경고 임계(%). [3차 / sec N-5]
     * ★상한 자체가 아니라 "경고를 언제 띄울지" 만 정하는 관측 상수라 정책 테이블로 올리지 않았다
     *   (차단 판정에 관여하지 않는다).
     */
    private static final int GLOBAL_WARN_PERCENT = 80;

    private final SmsSendPolicyMapper smsSendPolicyMapper;
    private final SmsKillSwitchService smsKillSwitchService;
    private final AuthCodeSmsDispatcher authCodeSmsDispatcher;

    /**
     * [4차 / sec T-7] 정책행이 <b>운영 중</b> 사라졌을 때 발송 게이트를 즉시 내리기 위해 주입한다.
     * 부팅 검증({@code SmsPolicyBootstrapValidator})은 기동 1회뿐이라 기동 이후의 소실을 잡지 못한다.
     */
    private final SmsProperties smsProperties;

    /**
     * 상한을 판정하고 통과 시 인증코드 INSERT 를 실행한다.
     *
     * <p>{@code insertAction} 에는 각 진입점의 기존 INSERT 호출(및 SMS2-D4 의 기존 코드 만료)을 그대로 넣는다.
     * 진입점 코드의 diff 를 최소화하고 INSERT 를 잠금 구간 안으로 들여오기 위한 콜백이다.
     *
     * <p>★{@code REQUIRES_NEW} 인 이유: 진입점 C 처럼 호출자 트랜잭션이 존재할 수 있는 곳에서도
     *    잠금 보유 범위를 이 메서드 안으로 한정하기 위함이다.
     *
     * <p>★★{@code READ_COMMITTED} 인 이유(4차 / sec T-1 · qa R-1): 클래스 주석
     *    "잠금 안 재검사가 최신값을 보게 하는 조건" 절 참조. <b>제거하면 4축·전역 TOCTOU 가 즉시 재개방된다.</b>
     *    선검사(⓪)를 남겨두는 한 이 격리수준은 필수다. 되돌릴 거면 선검사부터 지울 것.
     *
     * @param ctx          축 값(목적/번호HMAC/IP해시/사용자코드)
     * @param insertAction 인증코드 INSERT(및 기존 코드 만료). 잠금 구간 안에서 실행된다
     * @throws ApiException 상한 초과 / 킬스위치 발동(게이트 ON 인 경우에만)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW
            , isolation = Isolation.READ_COMMITTED
            , rollbackFor = Exception.class)
    public void guardAndInsert(SmsSendContext ctx, Runnable insertAction) {

        // 게이트 ON 이면 시간·일·전역 축을 실제 차단, OFF 면 판정만 하고 로그만 남긴다.
        // ★번호 창(55초)은 이 값과 무관하게 항상 강제된다(3차 / qa Q-2 — 아래 ③).
        final boolean enforce = authCodeSmsDispatcher.isSendEnabled();

        // ⓪ [3차 / qa Q-2] 선검사 — ★정책행 잠금을 잡기 <b>전에</b> 한다.
        //    무인증 EP 로 요청이 몰려도 대부분 여기서 끊겨 전역 직렬화 지점(FOR UPDATE)의 경합 자체가 줄어든다.
        //    ★특히 킬스위치 발동 중에는 어떤 요청도 잠금을 잡지 않게 된다(가장 부하가 큰 국면이 정확히 그때다).
        //    ★이 검사는 성능용이고 정확성은 보장하지 않는다(잠금 밖 읽기라 동시 요청이 함께 통과할 수 있다).
        //      통과한 요청은 아래 ②·③ 에서 잠금 안에서 반드시 재검사한다.
        //    ★★[4차 / sec T-1] 이 선검사가 트랜잭션의 "첫 비잠금 SELECT" 라 REPEATABLE READ 였다면
        //      스냅샷이 여기서 고정되어 아래 재검사가 전부 stale 이 된다. 그래서 이 메서드의 격리수준을
        //      READ_COMMITTED 로 낮췄다(클래스 주석 참조). 둘은 한 쌍이다 — 한쪽만 되돌리지 말 것.
        preCheckBeforeLock(ctx, enforce);

        // ① 정책행 배타 잠금 + 조회(같은 트랜잭션이라 항상 최신값 — 화면 수정이 다음 요청부터 즉시 반영된다).
        SmsSendPolicy policy = smsSendPolicyMapper.selectPolicyForUpdate();

        if (policy == null) {
            // ★fail-open. 시드 누락으로 5개 인증 흐름이 통째로 죽는 사고를 막는다(최종 방어는 발송 게이트).
            // ★★[4차 / sec T-7] 그 "최종 방어" 를 여기서 실제로 내린다.
            //   부팅 검증(SmsPolicyBootstrapValidator)은 기동 1회뿐이라 <b>운영 중</b> 정책행이 사라지면
            //   다시 fail-open 으로 돌아갔다(= 상한 없이 발송). 발송 경로가 직접 게이트를 내려
            //   fail-open 의 결과가 "상한 없이 발송" 이 아니라 "발송 안 함(SKIPPED)" 이 되게 한다.
            //   ★되돌리는 경로는 재기동뿐이다(원인 미확인 상태의 자동 재개 금지 — 킬스위치와 동일 원칙).
            if (smsProperties.forceDisable("SMS 상한 정책행 부재(TB_SMS_SEND_POLICY.POLICY_ID='DEFAULT')")) {
                log.error("[SMS상한] 정책행 부재 - 발송 게이트를 강제 OFF 한다(이후 전 흐름 SKIPPED)."
                        + " 상한 없이 발송되는 것보다 발송하지 않는 쪽이 안전하다."
                        + " prafta-sms-ppurio-2-rate-limit-policy.sql 의 시드 INSERT 를 적용한 뒤 재기동할 것");
            }
            insertAction.run();
            return;
        }

        // ② 킬스위치 — 발동 상태면 발송 자체를 막는다.
        if (policy.isKillSwitchFired()) {
            if (enforce) {
                log.warn("[SMS상한] 킬스위치 발동 상태로 발송 차단 - 목적={}", ctx.purposeCd());
                throw new ApiException(SmsErrorCode.SMS_503_001);
            }
            // ★게이트 OFF + 킬스위치 ON → 기존과 동일한 200(무회귀 최우선).
            //   게이트 OFF 는 애초에 발송하지 않으므로 킬스위치가 개입할 이유가 없다.
            log.info("[SMS상한:드라이런] 축=KILL_SWITCH → 게이트 ON 이었다면 차단");
        }

        // ③ 번호 축 — 연속 발송 간격 창(잠금 안 재검사).
        //    ★★[3차 / qa Q-2] 이 축은 게이트 상태와 무관하게 <b>항상</b> 차단한다(전 진입점 공통).
        //      게이트 OFF 에서 이 창까지 풀면 무인증 EP(진입점 A)에 서버측 상한이 전면 소멸해
        //      인증코드 테이블 무한 증식 + 전역 정책행 직렬화로 가용성이 무너진다(1차 대비 후퇴이기도 하다).
        if (ctx.mblNoHmac() != null && policy.phoneWindowSec() > 0) {
            int windowCnt = smsSendPolicyMapper.selectPhoneWindowCnt(
                    ctx.mblNoHmac(), ctx.purposeCd(), policy.phoneWindowSec());
            if (windowCnt > 0) {
                reject(true, "PHONE_WINDOW", windowCnt, 1, ctx.windowErrorCode());
            }
        }

        // ④ 번호 축 — 시간/일 상한(목적별).
        if (ctx.mblNoHmac() != null && hasAnyLimit(policy.phoneHourLimit(), policy.phoneDayLimit())) {
            SmsAxisCount phone = smsSendPolicyMapper.selectPhoneAxisCount(ctx.mblNoHmac(), ctx.purposeCd());
            checkAxis(enforce, "PHONE_HOUR", phone.hourCnt(), policy.phoneHourLimit(), SmsErrorCode.SMS_400_003);
            checkAxis(enforce, "PHONE_DAY", phone.dayCnt(), policy.phoneDayLimit(), SmsErrorCode.SMS_400_003);
        }

        // ⑤ IP 축 — ipHash 가 null 이면 축 스킵(fail-open).
        //    ★1단계는 관측 전용이라 IP_AXIS_ENABLED_YN='N' 인 동안 차단하지 않는다.
        if (ctx.ipHash() != null && hasAnyLimit(policy.ipHourLimit(), policy.ipDayLimit())) {
            boolean ipEnforce = enforce && policy.isIpAxisEnforced();
            SmsAxisCount ip = smsSendPolicyMapper.selectIpAxisCount(ctx.ipHash());
            checkAxis(ipEnforce, "IP_HOUR", ip.hourCnt(), policy.ipHourLimit(), SmsErrorCode.SMS_400_004);
            checkAxis(ipEnforce, "IP_DAY", ip.dayCnt(), policy.ipDayLimit(), SmsErrorCode.SMS_400_004);
        }

        // ⑥ 사용자 축 — userCd 가 null(무인증 흐름)이면 축 스킵.
        if (ctx.userCd() != null && hasAnyLimit(policy.userHourLimit(), policy.userDayLimit())) {
            SmsAxisCount user = smsSendPolicyMapper.selectUserAxisCount(ctx.userCd());
            checkAxis(enforce, "USER_HOUR", user.hourCnt(), policy.userHourLimit(), SmsErrorCode.SMS_400_004);
            checkAxis(enforce, "USER_DAY", user.dayCnt(), policy.userDayLimit(), SmsErrorCode.SMS_400_004);
        }

        // ⑦ 인증코드 INSERT(잠금 구간 안 — 여기까지가 원자적이다).
        insertAction.run();

        // ⑧ 전역 축 재확인 → 초과면 킬스위치 발동(다음 요청부터 차단).
        //    ★"INSERT 후 판정" 인 이유: 방금 요청까지 포함해야 501번째에서 정확히 발동한다.
        //      방금 건은 통과시키고 그 다음부터 막는다(경계 1건 오차는 허용).
        checkGlobal(enforce, policy.globalHourLimit());
    }

    // ------------------------------------------------------------------
    // 내부 판정
    // ------------------------------------------------------------------

    /**
     * [3차 / qa Q-2] 정책행 잠금 <b>이전</b>의 조기 반려 검사.
     *
     * <p>목적은 <b>잠금 경합 완화</b>다. 무인증 EP 로 요청이 몰려도 상당수가
     * {@code SELECT ... FOR UPDATE} 에 도달하기 전에 끊긴다.
     * <p>검사 순서는 잠금 구간(②·③)과 <b>동일하게</b> 유지한다 — 킬스위치가 창보다 앞이다.
     * 순서가 어긋나면 같은 상황에서 요청마다 다른 에러코드가 나간다.
     *
     * <p>★정확성은 이 검사로 보장되지 않는다(잠금 밖 읽기라 동시 요청이 함께 통과할 수 있다).
     *    최종 판정은 잠금 구간 안의 재검사가 담당한다. 여기서는 <b>반려만</b> 하고 통과는 결정하지 않는다.
     * <p>★정책행이 없으면(fail-open) 선검사를 건너뛴다 — 잠금 구간의 fail-open 과 동일 규칙.
     */
    private void preCheckBeforeLock(SmsSendContext ctx, boolean enforce) {

        SmsSendPolicy preview = smsSendPolicyMapper.selectPolicyNoLock();
        if (preview == null) {
            return;
        }

        // ①-1 킬스위치(게이트 ON 일 때만) — 발동 중에는 잠금을 아예 잡지 않는다.
        if (enforce && preview.isKillSwitchFired()) {
            log.warn("[SMS상한] 킬스위치 발동 상태로 발송 차단(선검사) - 목적={}", ctx.purposeCd());
            throw new ApiException(SmsErrorCode.SMS_503_001);
        }

        // ①-2 번호 창 — 게이트와 무관하게 항상 강제(Q-2).
        if (ctx.mblNoHmac() == null || preview.phoneWindowSec() <= 0) {
            return;
        }
        int windowCnt = smsSendPolicyMapper.selectPhoneWindowCnt(
                ctx.mblNoHmac(), ctx.purposeCd(), preview.phoneWindowSec());
        if (windowCnt > 0) {
            log.warn("[SMS상한] 발송 차단(선검사) - 축=PHONE_WINDOW, 현재={}, 코드={}",
                    windowCnt, ctx.windowErrorCode().code());
            throw new ApiException(ctx.windowErrorCode());
        }
    }

    /** 임계값이 하나라도 유효(1 이상)한지. 전부 0 이하면 그 축은 무제한이므로 쿼리조차 하지 않는다. */
    private boolean hasAnyLimit(int hourLimit, int dayLimit) {
        return hourLimit > 0 || dayLimit > 0;
    }

    /** 한 축의 카운트를 상한과 비교한다. 상한이 0 이하면 무제한(판정 생략). */
    private void checkAxis(boolean enforce, String axis, int cnt, int limit, ApiErrorCode errorCode) {
        if (limit <= 0) {
            return;
        }
        if (cnt >= limit) {
            reject(enforce, axis, cnt, limit, errorCode);
        }
    }

    /**
     * 전역 축 판정 + 킬스위치 발동.
     *
     * <p>★게이트 OFF 에서는 <b>발동시키지 않는다</b>. 발송 0건 상태에서 발동시키면
     *    나중에 게이트를 켜는 순간 이미 잠긴 상태가 되어 버린다.
     */
    private void checkGlobal(boolean enforce, int globalHourLimit) {
        if (globalHourLimit <= 0) {
            return;
        }

        if (!enforce) {
            // 게이트 OFF 면 전 행이 SKIPPED 라 실발송 카운트가 항상 0 이다 → 드라이런 전용 카운트를 쓴다.
            int dryRunCnt = smsSendPolicyMapper.selectGlobalDryRunCnt();
            if (dryRunCnt > globalHourLimit) {
                log.warn("[SMS상한:드라이런] 축=GLOBAL_HOUR, 현재={}, 상한={} → 게이트 ON 이었다면 킬스위치 발동",
                        dryRunCnt, globalHourLimit);
            }
            return;
        }

        int sentCnt = smsSendPolicyMapper.selectGlobalSentCnt();
        if (sentCnt > globalHourLimit) {
            // ★같은 트랜잭션(REQUIRED)에서 실행된다 — 이 트랜잭션이 정책행 X 잠금을 이미 보유하고 있으므로
            //   잠금 보유자 본인이 UPDATE 해야 한다. 클래스 주석 "잠금 보유 중 금지 사항" 절 참조(sec N-1).
            smsKillSwitchService.fire(sentCnt, globalHourLimit);
            return;
        }

        // [3차 / sec N-5] 사전 경고 — 킬스위치가 걸린 뒤에야 알게 되면 이미 전 고객사 인증 흐름이 멈춘 상태다.
        //   ★알림 채널은 신설하지 않는다(워크스페이스에 운영자 알림 채널이 없다).
        //     grep/CloudWatch 로그 메트릭 필터로 잡을 수 있게 고정 프리픽스만 제공한다.
        if (sentCnt >= (long) globalHourLimit * GLOBAL_WARN_PERCENT / 100) {
            log.warn("[SMS상한:소진경고] 전역 시간당 상한 {}% 초과 - 현재={}, 상한={}."
                    + " 초과하면 킬스위치가 자동 발동하며 해제는 Platform_05 화면에서 수동으로만 가능하다.",
                    GLOBAL_WARN_PERCENT, sentCnt, globalHourLimit);
        }
    }

    /**
     * 상한 위반 처리. 게이트 ON 이면 차단(예외), OFF 면 드라이런 로그만.
     *
     * <p>★사용자에게 어느 축인지 알리지 않는다(우회 힌트 방지) — 축은 서버 로그·에러코드로만 구분한다.
     */
    private void reject(boolean enforce, String axis, int cnt, int limit, ApiErrorCode errorCode) {
        if (!enforce) {
            log.warn("[SMS상한:드라이런] 축={}, 현재={}, 상한={} → 게이트 ON 이었다면 차단", axis, cnt, limit);
            return;
        }
        log.warn("[SMS상한] 발송 차단 - 축={}, 현재={}, 상한={}, 코드={}", axis, cnt, limit, errorCode.code());
        throw new ApiException(errorCode);
    }
}
