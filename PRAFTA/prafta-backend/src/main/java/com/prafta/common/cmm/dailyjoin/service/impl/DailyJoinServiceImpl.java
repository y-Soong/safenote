package com.prafta.common.cmm.dailyjoin.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.baseinfo.application.command.SmsAuthConsumeCommand;
import com.prafta.common.cmm.dailyjoin.application.command.DailyUserSlotUpdCommand;
import com.prafta.common.cmm.dailyjoin.application.command.InsertDailyUserCommand;
import com.prafta.common.cmm.dailyjoin.application.command.TermsUserAgrCommand;
import com.prafta.common.cmm.dailyjoin.application.param.InsertDailyUserParam;
import com.prafta.common.cmm.dailyjoin.application.param.SiteInfoParam;
import com.prafta.common.cmm.dailyjoin.application.param.UserIdDupleCheckParam;
import com.prafta.common.cmm.dailyjoin.application.query.EmptySlotQuery;
import com.prafta.common.cmm.dailyjoin.application.query.LinkPolicyQuery;
import com.prafta.common.cmm.dailyjoin.application.query.MblNoDupleQuery;
import com.prafta.common.cmm.dailyjoin.application.query.SiteInfoQuery;
import com.prafta.common.cmm.dailyjoin.application.query.SmsConsumeQuery;
import com.prafta.common.cmm.dailyjoin.application.query.UserIdDupleCheckQuery;
import com.prafta.common.cmm.dailyjoin.dto.response.InsertDailyUserResponse;
import com.prafta.common.cmm.dailyjoin.dto.response.SiteInfoResponse;
import com.prafta.common.cmm.dailyjoin.dto.response.UserIdDupleCheckResponse;
import com.prafta.common.cmm.dailyjoin.mapper.DailyJoinMapper;
import com.prafta.common.cmm.dailyjoin.result.RequiredTermsResult;
import com.prafta.common.cmm.dailyjoin.result.SiteInfoResult;
import com.prafta.common.cmm.dailyjoin.service.DailyJoinService;
import com.prafta.common.error.dailyjoin.DailyJoinErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.PasswordHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyJoinServiceImpl implements DailyJoinService {

    private final DailyJoinMapper dailyJoinMapper;
    private final HmacSigner hmacSigner;
    private final AesGcmCrypto aesGcmCrypto;
    private final PasswordHasher passwordHasher;

    @Override
    public SiteInfoResponse selectSiteInfo(SiteInfoParam param) {
        log.info("일일사용자 회원가입 - 회사/사업장 정보 조회 진입 cmpnyCd={}, siteCd={}", param.cmpnyCd(), param.siteCd());

        SiteInfoResult result = dailyJoinMapper.selectSiteInfo(SiteInfoQuery.from(param));

        if (result == null) {
            // 회사 미존재/USE_YN='N', 사업장 미존재 시 유효하지 않은 링크로 처리
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_404_001);
        }

        log.info("일일사용자 회원가입 - 회사/사업장 정보 조회 종료 cmpnyCd={}, siteCd={}", param.cmpnyCd(), param.siteCd());

        return SiteInfoResponse.builder()
                .cmpnyCd(result.cmpnyCd())
                .cmpnyNm(result.cmpnyNm())
                .siteCd(result.siteCd())
                .siteNm(result.siteNm())
                .build();
    }

    @Override
    public UserIdDupleCheckResponse checkUserIdDuple(UserIdDupleCheckParam param) {
        log.info("일일사용자 회원가입 - 사용자ID 중복체크 진입 cmpnyCd={}", param.cmpnyCd());

        int cnt = dailyJoinMapper.selectUserIdDupleCnt(UserIdDupleCheckQuery.from(param));
        String uniqueYn = (cnt > 0) ? "N" : "Y";

        log.info("일일사용자 회원가입 - 사용자ID 중복체크 종료 uniqueYn={}", uniqueYn);

        return UserIdDupleCheckResponse.builder()
                .uniqueYn(uniqueYn)
                .build();
    }

    @Override
    @Transactional
    public InsertDailyUserResponse insertDailyUser(InsertDailyUserParam param) {
        log.info("일일사용자 회원가입 진입 cmpnyCd={}, siteCd={}", param.cmpnyCd(), param.siteCd());

        // a. SMS 인증 가입 시점 재검증 (휴대폰 HMAC + 인증번호 기준 최근 인증완료 행 확인)
        String phoneNorm = Normalizers.normalizePhone(param.mblNo());
        if (phoneNorm == null) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_003);
        }
        String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);

        // 인증완료 레코드의 SMS_ID 확보 (소진 처리 대상)
        String smsId = dailyJoinMapper.selectSmsVerifiedSmsId(SmsConsumeQuery.of(phoneHmac, param.certNo()));
        if (smsId == null || smsId.isBlank()) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_003);
        }

        // SMS 인증코드 일회성 소진 (VERIFIED_YN='Y' -> 'C' 조건부 UPDATE, 영향행 0이면 롤백)
        int smsConsumed = dailyJoinMapper.consumeSmsAuth(SmsAuthConsumeCommand.from(smsId));
        if (smsConsumed <= 0) {
            // 동시 가입 등으로 이미 소진된 인증코드
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_003);
        }

        // g-1. 가입 전 계정등록 토글 검증 (TB_DAILY_USER_LINK_POLICY.USE_YN OFF면 가입 차단)
        String linkPolicyUseYn = dailyJoinMapper.selectLinkPolicyUseYn(LinkPolicyQuery.of(param.cmpnyCd(), param.siteCd()));
        if (linkPolicyUseYn == null || !"Y".equals(linkPolicyUseYn)) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_004);
        }

        // b. ID 중복(USE_YN='Y') 재확인
        int userIdCnt = dailyJoinMapper.selectUserIdDupleCnt(UserIdDupleCheckQuery.of(param.cmpnyCd(), param.userId()));
        if (userIdCnt > 0) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_001);
        }

        // b. 휴대폰 중복(CMPNY_CD + MBL_NO_HMAC 기준 USE_YN='Y') 확인
        int mblNoCnt = dailyJoinMapper.selectMblNoDupleCnt(MblNoDupleQuery.of(param.cmpnyCd(), phoneHmac));
        if (mblNoCnt > 0) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_002);
        }

        // b-2. PRAFTA-app-027-3'(통합형) — TB_USER 중복도 사전 차단(UX_TB_USER_ID/UX_TB_USER_MBL_NO).
        //      통합 후 같은 USER_ID/휴대폰이 정규 사용자와 겹치면 INSERT 가 UNIQUE 충돌하므로 사전 검증한다.
        int tbUserIdCnt = dailyJoinMapper.selectTbUserIdDupleCnt(param.cmpnyCd(), param.userId());
        if (tbUserIdCnt > 0) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_001);
        }
        int tbUserMblCnt = dailyJoinMapper.selectTbUserMblHmacDupleCnt(param.cmpnyCd(), phoneHmac);
        if (tbUserMblCnt > 0) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_002);
        }

        // g-2. 사업장 첫 빈 슬롯 확보 (없으면 가입 차단)
        String slotNo = dailyJoinMapper.selectFirstEmptySlotNo(EmptySlotQuery.of(param.cmpnyCd(), param.siteCd()));
        if (slotNo == null || slotNo.isBlank()) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_005);
        }

        // d. 휴대폰 암호화/HMAC/마지막4자리 파생 (신규/재활성 공통)
        String phoneEnc = aesGcmCrypto.encrypt(phoneNorm);
        String phoneLast4 = Normalizers.last4(phoneNorm);

        // e. 비밀번호 - 사용자 입력 비밀번호 해시(평문 로깅 금지, 난수 아님)
        String userPw = passwordHasher.hash(param.userPw());

        // 옵션2 — 같은 휴대폰의 비활성(만료) 일용직 행이 있으면 새 USER_CD 채번 대신 재활성한다.
        //         (활성 중복은 위 b/b-2 에서 이미 차단되었으므로 여기 도달 시 활성 충돌은 없음)
        String reuseUserCd = dailyJoinMapper.selectReactivatableDailyUserCd(param.cmpnyCd(), phoneHmac);

        final String userCd;
        if (reuseUserCd != null && !reuseUserCd.isBlank()) {
            // (가) 재활성 경로 — 기존 USER_CD 재사용. 워커당 1행을 유지하고 재가입 차단을 해소한다.
            //      같은 휴대폰 비활성 행이 여러 개(레거시)면 최신 1건만 재활성, 나머지는 비활성 유지.
            userCd = reuseUserCd;
            InsertDailyUserCommand command =
                    InsertDailyUserCommand.from(param, userCd, userPw, phoneEnc, phoneHmac, phoneLast4);

            // TB_DAILY_USER 기존 행 재활성(만료일/잠금 초기화 포함)
            dailyJoinMapper.reactivateDailyUser(command);

            // 통합형 TB_USER 재활성. 영향행 0이면 통합형 이전 레거시(TB_USER 없음) → 신규 INSERT 폴백.
            int tbUserUpd = dailyJoinMapper.reactivateTbUser(command);
            if (tbUserUpd <= 0) {
                dailyJoinMapper.insertDailyUserToTbUser(command);
            }

            // SITE_AUTH 는 사업장 변경 가능성을 고려해 upsert(기존 권한 행 회복 또는 신규 부여)
            dailyJoinMapper.upsertTbUserSiteAuth(param.cmpnyCd(), userCd, param.siteCd());
        } else {
            // (나) 신규 가입 경로 — USER_CD 채번 후 TB_DAILY_USER + 통합형 TB_USER/SITE_AUTH INSERT.
            userCd = dailyJoinMapper.selectDailyUserCd(EmptySlotQuery.of(param.cmpnyCd(), param.siteCd()));
            InsertDailyUserCommand command =
                    InsertDailyUserCommand.from(param, userCd, userPw, phoneEnc, phoneHmac, phoneLast4);

            // f. TB_DAILY_USER insert
            dailyJoinMapper.insertDailyUser(command);

            // f-2. PRAFTA-app-027-3'(통합형) — 같은 트랜잭션에서 TB_USER + TB_USER_SITE_AUTH 동시 INSERT.
            //      EMPLOYMENT_TYPE='DAILY'/AUTH_CD='99999'/NODE_CD=NULL/ACCOUNT_STATUS='01'/USER_PW=동일 hash.
            dailyJoinMapper.insertDailyUserToTbUser(command);
            dailyJoinMapper.insertTbUserSiteAuth(param.cmpnyCd(), userCd, param.siteCd());
        }

        // g-3. 빈 슬롯 점유 (조건부 UPDATE 로 동시성 충돌 방어) — 신규/재활성 공통.
        int slotUpd = dailyJoinMapper.updateDailyUserSlotCurrUserCd(
                DailyUserSlotUpdCommand.of(param.cmpnyCd(), param.siteCd(), slotNo, userCd));
        if (slotUpd <= 0) {
            // 조회와 점유 사이에 다른 가입자가 슬롯을 선점한 경우 → 트랜잭션 롤백
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_005);
        }

        // h. 약관 동의 이력 insert (필수약관 검증 후 요청 항목 저장)
        insertTermsAgreement(userCd, param);

        log.info("일일사용자 회원가입 완료 userCd={}, slotNo={}, 재활성여부={}",
                userCd, slotNo, (reuseUserCd != null && !reuseUserCd.isBlank()) ? "Y" : "N");

        // i. 응답으로 userId 반환
        return InsertDailyUserResponse.builder()
                .userId(param.userId())
                .build();
    }

    /**
     * 약관 동의 이력 저장.
     * 약관 버전은 클라이언트 값이 아닌 서버 조회값(TB_TERMS.TERMS_VERSION)을 저장한다.
     */
    private void insertTermsAgreement(String userCd, InsertDailyUserParam param) {
        List<RequiredTermsResult> requiredTermsList = dailyJoinMapper.selectRequiredTermsList();
        if (requiredTermsList == null || requiredTermsList.isEmpty()) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_500_001);
        }

        // termsId -> 서버 신뢰 약관버전 매핑
        Map<String, String> termsVersionMap = new HashMap<>();
        for (RequiredTermsResult required : requiredTermsList) {
            termsVersionMap.put(required.termsId(), required.termsVersion());
        }

        // 필수약관이 요청의 동의 목록에 모두 포함되어 있는지 검증
        for (RequiredTermsResult required : requiredTermsList) {
            boolean agreed = param.agrTermsList().stream()
                    .anyMatch(agr -> required.termsId().equals(agr.termsId()));
            if (!agreed) {
                throw new ApiException(DailyJoinErrorCode.DAILYJOIN_500_001);
            }
        }

        // 필수약관 동의 이력 저장 (서버 조회 버전으로 저장)
        for (RequiredTermsResult required : requiredTermsList) {
            String termsVersion = termsVersionMap.get(required.termsId());
            dailyJoinMapper.insertTermsUserAgrMgmt(
                    TermsUserAgrCommand.of(userCd, required.termsId(), termsVersion));
        }
    }
}
