package com.prafta.common.cmm.location.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.consent.ConsentConst;
import com.prafta.common.cmm.consent.mapper.ConsentMapper;
import com.prafta.common.cmm.consent.mapper.result.ConsentStateResult;
import com.prafta.common.cmm.consent.service.ConsentHistoryRecorder;
import com.prafta.common.cmm.location.LocationConsentConst;
import com.prafta.common.cmm.location.application.command.LocationPurgeHistCommand;
import com.prafta.common.cmm.location.mapper.LocationPurgeMapper;
import com.prafta.common.cmm.location.mapper.result.LocationPurgeScopeResult;
import com.prafta.common.cmm.location.result.LocationConsentStatusResult;
import com.prafta.common.cmm.location.service.LocationConsentService;
import com.prafta.common.error.location.LocationErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위치정보 동의 상태 판정·전이 구현 — 위치정보 동의철회·중지 S3.
 *
 * <p>동의 상태 변경은 전부 {@link ConsentHistoryRecorder#recordAndUpsertState} 를 경유한다
 * (이력 우회 경로를 만들지 않는다 — 철회 이력은 책임 추궁의 근거다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationConsentServiceImpl implements LocationConsentService {

    private final ConsentMapper consentMapper;
    private final ConsentHistoryRecorder consentHistoryRecorder;
    private final LocationPurgeMapper locationPurgeMapper;

    /**
     * 이벤트 차단 게이트 — <b>기본 false(차단하지 않음)</b>.
     *
     * <h3>★왜 토글이 필요한가 (2026-09-02 운영 실사고)</h3>
     * 종전에는 로그인 게이트가 005 미동의자를 잡아 재동의를 강요했다. S2 에서 005 를
     * 게이트에서 빼자 그 강제가 사라졌고, 그 상태로 S4 이벤트 차단을 켜니
     * <b>약관 개정 후 재동의하지 않은 기존 사용자</b>가 출퇴근을 하지 못하게 됐다.
     * 운영 실측: 활성 40명 중 21명이 현재버전(v3) 동의 행 없음
     * (v2 만 동의 10명 + 005 행 자체가 없는 계정 11명).
     *
     * <p>이들은 "철회한 사람"이 아니라 <b>재동의 대기</b>일 뿐이다. 앱의 재동의 유도 UX 가
     * 실기기에서 검증되기 전까지는 차단을 켜지 않는다.
     *
     * <p>★수집 차단({@link #isCollectAllowed})은 이 토글과 <b>무관하게 항상 동작</b>한다 —
     * 미동의자의 좌표를 저장하지 않는 것은 법적 요건이라 게이트로 끄지 않는다.
     * 이 토글이 끄는 것은 "이벤트 자체를 막을지" 뿐이다.
     */
    @Value("${prafta.location.consent.event-block.enabled:false}")
    private boolean eventBlockEnabled;

    @Override
    public LocationConsentStatusResult resolveStatus(String cmpnyCd, String userCd) {
        String version = resolveCurrentVersion();
        return LocationConsentStatusResult.of(resolveState(cmpnyCd, userCd, version), version);
    }

    @Override
    public boolean isCollectAllowed(String cmpnyCd, String userCd) {
        return LocationConsentConst.STATE_AGREED.equals(
                resolveState(cmpnyCd, userCd, resolveCurrentVersion()));
    }

    @Override
    public boolean isEventAllowed(String cmpnyCd, String userCd) {
        // ★게이트가 꺼져 있으면 이벤트를 막지 않는다(수집 차단은 별개로 계속 동작).
        if (!eventBlockEnabled) {
            return true;
        }
        return isCollectAllowed(cmpnyCd, userCd);
    }

    @Override
    public boolean isEventAllowedForOngoing(String cmpnyCd, String userCd) {
        if (!eventBlockEnabled) {
            return true;
        }
        return isCollectAllowedForOngoing(cmpnyCd, userCd);
    }

    @Override
    public boolean isCollectAllowedForOngoing(String cmpnyCd, String userCd) {
        String state = resolveState(cmpnyCd, userCd, resolveCurrentVersion());

        // ★② 오버나이트 예외 — 약관 개정(회사 사정)으로 재동의 대기가 된 경우에 한해
        //    진행 중인 근태의 퇴근까지는 허용한다. 본인 의사인 중지/철회에는 적용하지 않는다.
        return LocationConsentConst.STATE_AGREED.equals(state)
                || LocationConsentConst.STATE_PENDING_REAGREE.equals(state);
    }

    @Override
    @Transactional
    public LocationConsentStatusResult withdraw(String cmpnyCd, String userCd, String userTypeCd) {

        String version = resolveCurrentVersion();

        // 1) 상태 전이(잠금 + 이력). 이미 WITHDRAWN 이면 0 → 파기까지 건너뛴다(멱등).
        //    ★전이를 먼저 하는 이유: recordAndUpsertState 가 FOR UPDATE 로 대상 행을 잠가
        //      동시 철회 요청을 직렬화한다. 파기를 먼저 하면 같은 요청 2건이 나란히 파기·이력을 남긴다.
        int affected = consentHistoryRecorder.recordAndUpsertState(
                cmpnyCd, userCd, LocationConsentConst.LOCATION_TERMS_ID, version
                , "N", LocationConsentConst.STATE_WITHDRAWN
                , ConsentConst.SOURCE_MYPAGE, cmpnyCd, userCd);

        if (affected == 0) {
            log.info("위치정보 철회 - 이미 철회 상태(멱등) cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            return LocationConsentStatusResult.of(LocationConsentConst.STATE_WITHDRAWN, version);
        }

        // 2) 파기 대상 집계 — ★반드시 파기 전에. 파기 후에는 대상이 사라져 집계할 수 없다.
        LocationPurgeScopeResult scope =
                locationPurgeMapper.selectPurgeScope(cmpnyCd, userCd, userTypeCd);

        // 3) 좌표 파기(행은 유지, 평문/암호문 쌍으로 NULL).
        int purgedAttd = locationPurgeMapper.purgeAttdGpsByUser(cmpnyCd, userCd);
        int purgedTbmAtt = locationPurgeMapper.purgeTbmAttendanceGpsByUser(cmpnyCd, userCd, userTypeCd);
        int purgedTbmSes = locationPurgeMapper.purgeTbmSessionGpsByUser(cmpnyCd, userCd);

        // 4) 파기 이력(append-only). ★좌표는 어떤 형태로도 남기지 않는다.
        //    파기할 게 없어도 기록한다 — "철회 요청을 받아 처리했다(대상 0건)"는 사실 자체가 근거다.
        locationPurgeMapper.insertPurgeHist(LocationPurgeHistCommand.of(
                cmpnyCd, userCd, userTypeCd
                , LocationConsentConst.PURGE_REASON_WITHDRAW, version
                , scope, purgedAttd, purgedTbmAtt, purgedTbmSes
                , cmpnyCd, userCd));

        log.info("위치정보 철회 완료 - cmpnyCd={}, userCd={}, 계통={}, 파기 출퇴근 {}건/TBM입실 {}건/TBM개설 {}건, 기간 {}~{}"
                , cmpnyCd, userCd, userTypeCd, purgedAttd, purgedTbmAtt, purgedTbmSes
                , scope == null ? null : scope.oldestCollected()
                , scope == null ? null : scope.latestCollected());

        return LocationConsentStatusResult.of(
                LocationConsentConst.STATE_WITHDRAWN, version, purgedAttd + purgedTbmAtt + purgedTbmSes);
    }

    @Override
    @Transactional
    public LocationConsentStatusResult suspend(String cmpnyCd, String userCd) {

        String version = resolveCurrentVersion();

        // ★철회한 사람이 중지로 되돌아가는 것은 막는다 — 좌표는 이미 파기됐고, 중지의 정의
        //   ("과거 유지")를 만족시킬 수 없어 상태 표시가 사실과 어긋난다. 재동의부터 해야 한다.
        String current = resolveState(cmpnyCd, userCd, version);
        if (LocationConsentConst.STATE_WITHDRAWN.equals(current)) {
            throw new ApiException(LocationErrorCode.LOCATION_400_002);
        }

        consentHistoryRecorder.recordAndUpsertState(
                cmpnyCd, userCd, LocationConsentConst.LOCATION_TERMS_ID, version
                , "N", LocationConsentConst.STATE_SUSPENDED
                , ConsentConst.SOURCE_MYPAGE, cmpnyCd, userCd);

        return LocationConsentStatusResult.of(LocationConsentConst.STATE_SUSPENDED, version);
    }

    @Override
    @Transactional
    public LocationConsentStatusResult resume(String cmpnyCd, String userCd) {

        String version = resolveCurrentVersion();

        consentHistoryRecorder.recordAndUpsertState(
                cmpnyCd, userCd, LocationConsentConst.LOCATION_TERMS_ID, version
                , "Y", LocationConsentConst.STATE_AGREED
                , ConsentConst.SOURCE_MYPAGE, cmpnyCd, userCd);

        log.info("위치정보 재동의 - cmpnyCd={}, userCd={}, ver={} (파기된 좌표는 복구되지 않음)"
                , cmpnyCd, userCd, version);

        return LocationConsentStatusResult.of(LocationConsentConst.STATE_AGREED, version);
    }

    /**
     * 현재 시행 중인 005 버전. 약관 행이 없거나 미사용이면 제도 자체가 가동 불가이므로 예외.
     */
    private String resolveCurrentVersion() {
        String version = consentMapper.selectTermsCurrentVersion(LocationConsentConst.LOCATION_TERMS_ID);
        if (version == null || version.isBlank()) {
            log.error("위치기반서비스 약관(005) 활성 행이 없다 - 위치정보 동의 판정 불가");
            throw new ApiException(LocationErrorCode.LOCATION_500_001);
        }
        return version;
    }

    /**
     * 4-state 판정.
     *
     * <pre>
     *   현재버전 행 있음 ┬ CONSENT_STATE 있음 → 그 값
     *                   └ CONSENT_STATE 없음 → AGR_YN='Y' ? AGREED : WITHDRAWN
     *   현재버전 행 없음 → PENDING_REAGREE
     * </pre>
     *
     * <p>★{@code CONSENT_STATE} 가 비어 있는 행이 생기는 경로: 가입 시 약관 동의는
     * {@code LoginMapper.insertTermsUserAgrMgmt} / {@code DailyJoinMapper} 가 직접 기록하며
     * 상태 컬럼을 채우지 않는다. 그 경로를 건드리지 않고도 정확히 판정되도록 폴백을 둔다
     * (무회귀 우선 — 가입 트랜잭션은 손대지 않는다).
     *
     * <p>★현재버전 행이 없으면 {@code PENDING_REAGREE} 다. 구버전에 동의했든(약관 개정)
     * 아예 응답이 없든 <b>재동의를 요구해야 한다는 점이 같고</b>, 무엇보다 이것을 철회로
     * 취급하면 <b>약관을 개정할 때마다 전 사용자의 위치정보가 파기된다.</b>
     */
    private String resolveState(String cmpnyCd, String userCd, String version) {

        ConsentStateResult row = consentMapper.selectUserAgrState(
                cmpnyCd, userCd, LocationConsentConst.LOCATION_TERMS_ID, version);

        if (row == null) {
            return LocationConsentConst.STATE_PENDING_REAGREE;
        }
        if (row.consentState() != null && !row.consentState().isBlank()) {
            return row.consentState();
        }
        return "Y".equals(row.agrYn())
                ? LocationConsentConst.STATE_AGREED
                : LocationConsentConst.STATE_WITHDRAWN;
    }
}
