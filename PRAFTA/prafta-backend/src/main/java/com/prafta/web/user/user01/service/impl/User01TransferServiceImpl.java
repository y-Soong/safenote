package com.prafta.web.user.user01.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.push.TransferReservedNotiService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.user.user01.application.command.TransferReservationInsertCommand;
import com.prafta.web.user.user01.application.param.TransferEligibilityParam;
import com.prafta.web.user.user01.application.param.TransferReservationParam;
import com.prafta.web.user.user01.dto.response.TransferEligibilityResponse;
import com.prafta.web.user.user01.dto.response.TransferNoticeResponse;
import com.prafta.web.user.user01.dto.response.TransferReservationResponse;
import com.prafta.web.user.user01.mapper.User01Mapper;
import com.prafta.web.user.user01.mapper.UserTransferMapper;
import com.prafta.web.user.user01.result.TransferBlockReason;
import com.prafta.web.user.user01.result.TransferNoticeResult;
import com.prafta.web.user.user01.result.UserTransferBasicResult;
import com.prafta.web.user.user01.service.User01TransferService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 소속이동(Terminal A) 서비스 구현 — PRAFTA-WEB_001-1.
 *
 * <p>권한 = master/hr({@link AuthRoleUtils#isManager}). 회사 스코프/등록자/권한은 토큰 도출값만 신뢰한다(IDOR/cross-tenant 방지).
 * 정규직 정의 = EMPLOYMENT_TYPE != 'DAILY'(NULL 포함 정규직). 5종 불가케이스는 정규직만 적용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class User01TransferServiceImpl implements User01TransferService {

    private final UserTransferMapper userTransferMapper;
    private final UserTransferValidator userTransferValidator;
    // 일용직 분기의 노드 관리자 존재 검사(USER_400_056)는 기존 쿼리를 재사용한다(중복 구현 금지).
    private final User01Mapper user01Mapper;
    // PRAFTA-WEB_001-3 (Terminal C): 등록 즉시 PUSH 생산자(afterCommit + REQUIRES_NEW 격리).
    private final TransferReservedNotiService transferReservedNotiService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 소속이동 사유 최대 길이(DDL varchar(500) 정합 — truncation/500 방지). */
    private static final int REASON_MAX_LEN = 500;

    /** 로그인 안내 팝업 advisory 문구(이동 전 진행중 결재 종료 안내). */
    private static final List<String> NOTICE_GUIDE_MESSAGES =
            List.of("소속이동 시점에 진행 중인 결재/요청은 종료(반려/취소) 처리됩니다.");

    @Override
    public TransferEligibilityResponse checkEligibility(TransferEligibilityParam param) {
        log.info("소속이동 가능여부 판정 진입 - cmpnyCd={}, userCd={}", param.gvCmpnyCd(), param.userCd());

        // 권한 게이트(master/hr 전용).
        assertManager(param.gvAuthCd(), param.gvUserCd());

        // 대상 사용자 조회(회사 스코프). 없으면 404(존재 비노출).
        UserTransferBasicResult target = userTransferMapper.selectUserBasic(param.gvCmpnyCd(), param.userCd());
        if (target == null) {
            throw new ApiException(UserErrorCode.USER_404_004);
        }

        // 일용직은 5종 불가케이스 미적용 → 항상 eligible(부서 관리자 존재 검사는 등록 시점에 수행).
        if (isDaily(target.employmentType())) {
            return TransferEligibilityResponse.builder()
                    .userCd(target.userCd())
                    .employmentType(target.employmentType())
                    .eligible(true)
                    .blockReasons(List.of())
                    .build();
        }

        List<TransferBlockReason> reasons = userTransferValidator.evaluate(
                param.gvCmpnyCd(), target, param.toDefaultSchCd(), param.toSiteCd(), param.moveDate());

        log.info("소속이동 가능여부 판정 종료 - userCd={}, eligible={}, blockCnt={}",
                param.userCd(), reasons.isEmpty(), reasons.size());

        return TransferEligibilityResponse.builder()
                .userCd(target.userCd())
                .employmentType(target.employmentType())
                .eligible(reasons.isEmpty())
                .blockReasons(reasons)
                .build();
    }

    @Override
    @Transactional
    public TransferReservationResponse reserveTransfer(TransferReservationParam param) {
        log.info("소속이동 예약 등록 진입 - cmpnyCd={}, 대상userCd={}, 등록자={}",
                param.gvCmpnyCd(), param.userCd(), param.gvUserCd());

        // 0) 권한 게이트(master/hr 전용).
        assertManager(param.gvAuthCd(), param.gvUserCd());

        // 1) 대상 식별 + 필수값 검증(fail-fast).
        if (param.userCd() == null || param.userCd().isBlank()) {
            throw new ApiException(UserErrorCode.USER_404_004);
        }
        if (isBlank(param.toSiteCd())) {
            throw new ApiException(UserErrorCode.USER_400_062);
        }
        if (isBlank(param.toNodeCd())) {
            throw new ApiException(UserErrorCode.USER_400_063);
        }
        String moveDate = normalizeYmd(param.moveDate());
        if (moveDate == null || !isAfterToday(moveDate)) {
            throw new ApiException(UserErrorCode.USER_400_061);
        }
        if (isBlank(param.moveReason())) {
            throw new ApiException(UserErrorCode.USER_400_060);
        }
        // 길이 초과(>500)는 미입력(060)과 구분해 전용 코드(071)로 반환. DDL varchar(500) 정합.
        if (param.moveReason().length() > REASON_MAX_LEN) {
            throw new ApiException(UserErrorCode.USER_400_071);
        }

        // 1-1) 이동 목적지 실재 검증(daily/regular 공통) — 타 회사/비존재 사업장·부서 예약 차단(cross-tenant/위변조 방지).
        //      일용직의 selectNodeHasAdmin(관리자 존재)은 실재를 간접 보장하나, 정규직 경로는 누락되므로 공통 가드로 선행한다.
        if (userTransferMapper.selectSiteExists(param.gvCmpnyCd(), param.toSiteCd()) <= 0) {
            throw new ApiException(UserErrorCode.USER_400_062);
        }
        if (userTransferMapper.selectNodeExists(param.gvCmpnyCd(), param.toSiteCd(), param.toNodeCd()) <= 0) {
            throw new ApiException(UserErrorCode.USER_400_063);
        }

        // 2) 대상 사용자 조회(회사 스코프). 없으면 404(존재 비노출).
        UserTransferBasicResult target = userTransferMapper.selectUserBasic(param.gvCmpnyCd(), param.userCd());
        if (target == null) {
            throw new ApiException(UserErrorCode.USER_404_004);
        }

        boolean daily = isDaily(target.employmentType());
        // 정규직만 기본 근무타입 필수. 일용직은 기본 근무타입을 저장하지 않는다(null 고정).
        String toDefaultSchCd = daily ? null : trimToNull(param.toDefaultSchCd());

        if (daily) {
            // 3-a) 일용직: 이동 부서에 관리자(정/부) 존재해야 지정 가능(기존 가드 재사용, USER_400_056).
            int nodeHasAdmin = user01Mapper.selectNodeHasAdmin(param.gvCmpnyCd(), param.toSiteCd(), param.toNodeCd());
            if (nodeHasAdmin <= 0) {
                throw new ApiException(UserErrorCode.USER_400_056);
            }
        } else {
            // 3-b) 정규직: 기본 근무타입 필수.
            if (toDefaultSchCd == null) {
                throw new ApiException(UserErrorCode.USER_400_064);
            }
            // 4) 5종 불가케이스 사전검증(정규직 한정). 불가 시 첫 사유로 차단(fail-closed).
            List<TransferBlockReason> reasons = userTransferValidator.evaluate(
                    param.gvCmpnyCd(), target, toDefaultSchCd, param.toSiteCd(), moveDate);
            if (!reasons.isEmpty()) {
                TransferBlockReason first = reasons.get(0);
                log.info("소속이동 예약 차단(불가케이스) - 대상userCd={}, code={}, blockCnt={}",
                        param.userCd(), first.code(), reasons.size());
                throw new ApiException(UserErrorCode.valueOf(first.code()));
            }
        }

        // 5) 중복 예약(활성 RESERVED) 사전 차단. DB UNIQUE(활성 한정)가 동시성 백스톱.
        if (userTransferMapper.selectActiveReservationCnt(param.gvCmpnyCd(), param.userCd()) > 0) {
            throw new ApiException(UserErrorCode.USER_400_070);
        }

        // 6) 채번('TR' + YYYYMMDD + 시퀀스).
        String reservationId = userTransferMapper.selectReservationId(param.gvCmpnyCd());

        TransferReservationInsertCommand command = new TransferReservationInsertCommand(
                param.gvCmpnyCd()
                , reservationId
                , target.userCd()
                , target.siteCd()      // FROM_SITE_CD 스냅샷
                , target.nodeCd()      // FROM_NODE_CD 스냅샷
                , param.toSiteCd()
                , param.toNodeCd()
                , toDefaultSchCd       // 일용직 null
                , moveDate
                , param.moveReason()
                , target.employmentType()
                , param.gvUserCd());   // 등록자(master/hr)

        // 7) INSERT — 동시 등록으로 UNIQUE(활성 한정) 위반 시 사용자 친화 메시지로 변환.
        try {
            userTransferMapper.insertReservation(command);
        } catch (DuplicateKeyException e) {
            log.info("소속이동 예약 등록 - 활성 중복(UNIQUE) cmpnyCd={}, 대상userCd={}", param.gvCmpnyCd(), param.userCd());
            throw new ApiException(UserErrorCode.USER_400_070);
        }

        // 8) 등록 즉시 PUSH(Terminal C) — 대상자에게 안내 발송 예약(afterCommit). best-effort:
        //    발송/적재 실패가 등록(본 트랜잭션)을 롤백·차단하면 안 되므로 추가로 try-catch 격리한다
        //    (com-001 체크인 훅 패턴). 명칭은 등록 직후 동일 트랜잭션 내 가시 행을 조인 조회해 스냅샷.
        try {
            TransferNoticeResult names = userTransferMapper.selectTransferNoticeByReservation(
                    param.gvCmpnyCd(), target.userCd(), reservationId);
            transferReservedNotiService.notifyTransferReserved(
                    param.gvCmpnyCd()
                    , param.toSiteCd()                    // outbox SITE_CD = 이동 사업장
                    , target.userCd()                     // 대상자 본인
                    , reservationId
                    , moveDate                            // YYYYMMDD
                    , names != null ? names.toSiteNm() : null
                    , names != null ? names.toNodeNm() : null
                    , names != null ? names.defaultSchNm() : null   // 일용직 null
                    , param.moveReason()
                    , param.gvUserCd());                  // 등록자(master/hr)
        } catch (Exception e) {
            // 발송 훅 실패는 등록 성공에 영향 없음(로그만 — 본 트랜잭션은 정상 커밋).
            log.error("소속이동 예약 PUSH 발송 예약 실패(등록은 정상) - reservationId={}, 대상userCd={}",
                    reservationId, param.userCd(), e);
        }

        log.info("소속이동 예약 등록 종료 - cmpnyCd={}, reservationId={}, 대상userCd={}, moveDate={}",
                param.gvCmpnyCd(), reservationId, param.userCd(), moveDate);

        return TransferReservationResponse.builder()
                .reservationId(reservationId)
                .build();
    }

    @Override
    public TransferNoticeResponse getMyTransferNotice(String cmpnyCd, String userCd) {
        // 대상은 토큰 도출값만 신뢰(IDOR 방지). 토큰 누락/무효는 거부.
        if (isBlank(cmpnyCd) || isBlank(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        TransferNoticeResult notice = userTransferMapper.selectUnackedTransferNotice(cmpnyCd, userCd);
        if (notice == null) {
            return TransferNoticeResponse.builder()
                    .hasNotice(false)
                    .reservation(null)
                    .build();
        }

        TransferNoticeResponse.Reservation reservation = TransferNoticeResponse.Reservation.builder()
                .reservationId(notice.reservationId())
                .moveDate(formatYmdDisplay(notice.moveDate()))
                .toSiteNm(notice.toSiteNm())
                .toNodeNm(notice.toNodeNm())
                .defaultSchNm(notice.defaultSchNm())
                .moveReason(notice.moveReason())
                .guideMessages(NOTICE_GUIDE_MESSAGES)
                .build();

        log.info("소속이동 안내 조회 - cmpnyCd={}, userCd={}, reservationId={}", cmpnyCd, userCd, notice.reservationId());

        return TransferNoticeResponse.builder()
                .hasNotice(true)
                .reservation(reservation)
                .build();
    }

    @Override
    @Transactional
    public void ackTransferNotice(String cmpnyCd, String userCd, String reservationId) {
        // 대상은 토큰 도출값만 신뢰(IDOR 방지).
        if (isBlank(cmpnyCd) || isBlank(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (isBlank(reservationId)) {
            throw new ApiException(UserErrorCode.USER_404_004);
        }

        // 회사 + 예약 ID + 본인(USER_CD) 못박은 UPDATE — 타인 예약은 0행(404, 존재 비노출). 멱등(재확인도 1행).
        int affected = userTransferMapper.ackTransferNotice(cmpnyCd, userCd, reservationId);
        if (affected <= 0) {
            log.info("소속이동 안내 확인 대상 없음(미존재/타인) - cmpnyCd={}, userCd={}, reservationId={}",
                    cmpnyCd, userCd, reservationId);
            throw new ApiException(UserErrorCode.USER_404_004);
        }

        log.info("소속이동 안내 확인 완료 - cmpnyCd={}, userCd={}, reservationId={}", cmpnyCd, userCd, reservationId);
    }

    /** "YYYYMMDD" → 표시용 "YYYY-MM-DD". 형식 불량이면 원문 폴백. */
    private String formatYmdDisplay(String ymd) {
        if (ymd == null || ymd.length() != 8 || !ymd.chars().allMatch(Character::isDigit)) {
            return ymd;
        }
        return ymd.substring(0, 4) + "-" + ymd.substring(4, 6) + "-" + ymd.substring(6, 8);
    }

    /** master/hr 권한 게이트. 미보유 시 USER_403_002. */
    private void assertManager(String authCd, String userCd) {
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("소속이동 권한 없음(master/hr 외 차단) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(UserErrorCode.USER_403_002);
        }
    }

    /** 고용형태 일용직 판정(EMPLOYMENT_TYPE='DAILY'). NULL 포함 그 외는 정규직. */
    private boolean isDaily(String employmentType) {
        return AuthRoleUtils.isDailyWorker(employmentType);
    }

    /** moveDate(YYYYMMDD)가 오늘보다 미래(내일 이후)인지. */
    private boolean isAfterToday(String ymd) {
        try {
            return LocalDate.parse(ymd, YMD).isAfter(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    /** "YYYY-MM-DD"/"YYYYMMDD" 입력을 YYYYMMDD 8자리로 정규화. 형식 불량이면 null. */
    private String normalizeYmd(String raw) {
        if (raw == null) {
            return null;
        }
        String d = raw.replace("-", "").trim();
        if (d.length() != 8 || !d.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return d;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
