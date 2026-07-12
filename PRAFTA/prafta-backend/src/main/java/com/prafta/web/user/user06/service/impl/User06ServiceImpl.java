package com.prafta.web.user.user06.service.impl;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.web.user.user06.application.command.BlacklistInsertCommand;
import com.prafta.web.user.user06.application.command.BlacklistReleaseCommand;
import com.prafta.web.user.user06.application.param.BlacklistListParam;
import com.prafta.web.user.user06.application.param.BlacklistRegParam;
import com.prafta.web.user.user06.application.param.BlacklistReleaseParam;
import com.prafta.web.user.user06.application.query.BlacklistListQuery;
import com.prafta.web.user.user06.dto.response.BlacklistListResponse;
import com.prafta.web.user.user06.dto.response.BlacklistRegResponse;
import com.prafta.web.user.user06.mapper.User06Mapper;
import com.prafta.web.user.user06.result.BlacklistRaw;
import com.prafta.web.user.user06.result.BlacklistResult;
import com.prafta.web.user.user06.service.User06Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class User06ServiceImpl implements User06Service {

    private final User06Mapper user06Mapper;
    private final HmacSigner hmacSigner;
    private final AesGcmCrypto aesGcmCrypto;

    /** 조회 페이지 크기 상한(전수조회 방지, DoS 완화). */
    private static final int PAGE_SIZE = 500;

    /** 본 화면 메뉴 식별자(서버측 역할 게이트 기준). */
    private static final String MENU_D_ID = "User_06";
    /** 메뉴 버튼 권한 종류(고정 상수 — 동적 컬럼 주입 금지). */
    private static final String BTN_SRCH = "SRCH";
    private static final String BTN_NEW = "NEW";
    private static final String BTN_DELT = "DELT";

    /** 등록 사유 최대 길이(DDL varchar(200) 정합). */
    private static final int REASON_MAX_LEN = 200;

    @Override
    public BlacklistListResponse selectBlacklistList(BlacklistListParam param) {
        log.info("일일계정 블랙리스트 조회 진입 - cmpnyCd={}", param.gvCmpnyCd());

        // 서버측 메뉴 권한 게이트(조회) — User_06 BTN_SRCH 보유 역할만 허용(JWT authCd 기준).
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SRCH);

        // 전화번호 검색: 평문 비교 금지. 정규화 후 HMAC(정확매칭)/LAST4(부분) 파생값만 쿼리에 전달.
        String mblNoHmac = null;
        String mblNoLast4 = null;
        if (param.mblNo() != null && !param.mblNo().isBlank()) {
            String phoneNorm = Normalizers.normalizePhone(param.mblNo());
            if (phoneNorm != null) {
                mblNoHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
                mblNoLast4 = Normalizers.last4(phoneNorm);
            } else {
                // 정규화 실패 → LAST4 부분검색만 시도(매칭 불가 시 빈 결과).
                String digits = param.mblNo().replaceAll("\\D", "");
                if (digits.length() >= 4) {
                    mblNoLast4 = digits.substring(digits.length() - 4);
                }
            }
        }

        String useYn = ("Y".equals(param.useYn()) || "N".equals(param.useYn())) ? param.useYn() : null;

        BlacklistListQuery query = new BlacklistListQuery(
                param.gvCmpnyCd()
                , mblNoHmac
                , mblNoLast4
                , useYn
                , PAGE_SIZE
                , 0);

        List<BlacklistRaw> rawList = user06Mapper.selectBlacklistList(query);

        // 휴대폰은 평문 노출 금지 → 서버 마스킹(복호 후 가운데 마스킹, 복호 실패 시 LAST4 기반 폴백).
        List<BlacklistResult> blacklist = rawList.stream()
                .map(r -> new BlacklistResult(
                        r.blacklistId()
                        , maskMblNo(decryptMblNo(r.mblNoEnc()), r.mblNoLast4())
                        , r.reason()
                        , r.useYn()
                        , r.insertNm()
                        , r.insertDate()))
                .toList();

        log.info("일일계정 블랙리스트 조회 종료 - cmpnyCd={}, rows={}", param.gvCmpnyCd(), blacklist.size());

        return BlacklistListResponse.builder()
                .blacklist(blacklist)
                .build();
    }

    @Override
    @Transactional
    public BlacklistRegResponse insertBlacklist(BlacklistRegParam param) {
        log.info("일일계정 블랙리스트 등록 진입 - cmpnyCd={}", param.gvCmpnyCd());

        // 0) 서버측 메뉴 권한 게이트(등록) — User_06 BTN_NEW 보유 역할만 허용(JWT authCd 기준).
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_NEW);

        // 1) 사유 필수 + 길이 제한(DDL varchar(200) — truncation/500 방지).
        if (param.reason() == null || param.reason().isBlank()) {
            throw new ApiException(UserErrorCode.USER_400_040);
        }
        if (param.reason().length() > REASON_MAX_LEN) {
            throw new ApiException(UserErrorCode.USER_400_059);
        }

        // 2) 휴대폰 정규화 + 형식 검증 — 한국 전화번호(10~11자리, '0' 시작)만 허용(프론트 validatePhoneNumber 정합).
        //    normalizePhone 은 숫자만 남길 뿐 형식을 보장하지 않으므로("123" 통과) 서버에서 자릿수/접두 검증한다.
        String phoneNorm = Normalizers.normalizePhone(param.mblNo());
        if (phoneNorm == null || phoneNorm.length() < 10 || phoneNorm.length() > 11 || !phoneNorm.startsWith("0")) {
            throw new ApiException(UserErrorCode.USER_400_049);
        }

        // 3) 파생값(HMAC/ENC/LAST4)
        String mblNoHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
        String mblNoEnc = aesGcmCrypto.encrypt(phoneNorm);
        String mblNoLast4 = Normalizers.last4(phoneNorm);

        // 4) 활성 중복 사전 차단(사용자 친화 메시지). DB UNIQUE 가 동시성 백스톱.
        if (user06Mapper.selectActiveBlacklistCnt(param.gvCmpnyCd(), mblNoHmac) > 0) {
            throw new ApiException(UserErrorCode.USER_400_058);
        }

        // 5) 채번('B' + YYYYMMDD + 시퀀스)
        String blacklistId = user06Mapper.selectBlacklistId(param.gvCmpnyCd());

        BlacklistInsertCommand command = new BlacklistInsertCommand(
                param.gvCmpnyCd()
                , blacklistId
                , mblNoEnc
                , mblNoHmac
                , mblNoLast4
                , param.reason()
                , param.gvUserCd());

        // 6) INSERT — 동시 등록으로 UNIQUE(활성 한정) 위반 시 사용자 친화 메시지로 변환.
        try {
            user06Mapper.insertBlacklist(command);
        } catch (DuplicateKeyException e) {
            log.info("일일계정 블랙리스트 등록 - 활성 중복(UNIQUE) cmpnyCd={}", param.gvCmpnyCd());
            throw new ApiException(UserErrorCode.USER_400_058);
        }

        log.info("일일계정 블랙리스트 등록 종료 - cmpnyCd={}, blacklistId={}", param.gvCmpnyCd(), blacklistId);

        return BlacklistRegResponse.builder()
                .blacklistId(blacklistId)
                .build();
    }

    @Override
    @Transactional
    public void releaseBlacklist(BlacklistReleaseParam param) {
        log.info("일일계정 블랙리스트 해제 진입 - cmpnyCd={}, blacklistId={}", param.gvCmpnyCd(), param.blacklistId());

        // 서버측 메뉴 권한 게이트(해제) — User_06 BTN_DELT 보유 역할만 허용(JWT authCd 기준).
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_DELT);

        if (param.blacklistId() == null || param.blacklistId().isBlank()) {
            throw new ApiException(UserErrorCode.USER_400_040);
        }

        // 회사 스코프 + 활성행 한정 조건부 UPDATE. 0행이면 미존재/타 회사/이미 해제 → 404(존재 비노출).
        int updated = user06Mapper.releaseBlacklist(
                new BlacklistReleaseCommand(param.gvCmpnyCd(), param.blacklistId(), param.gvUserCd()));
        if (updated <= 0) {
            throw new ApiException(UserErrorCode.USER_404_003);
        }

        log.info("일일계정 블랙리스트 해제 종료 - cmpnyCd={}, blacklistId={}", param.gvCmpnyCd(), param.blacklistId());
    }

    /**
     * 서버측 메뉴 권한 게이트 — User_06 메뉴의 지정 버튼권한(BTN_SRCH/NEW/DELT)을 보유한 역할만 통과.
     *
     * <p>권한 판정은 AuthRoleUtils 단독이 아니라 TB_SYST_AUTH_MENU 조회 기반이다(시드의 노드직책
     * AUTH_CD 00001/00004/00006/00008 까지 포함하기 위함). authCd 는 JWT 클레임 도출값만 신뢰한다.
     * 미보유(미부여 인증 사용자/일용직 99999 등) 시 USER_403_001 로 차단(회사 전역 임의 등록/해제 봉인).
     */
    private void assertMenuButton(String cmpnyCd, String authCd, String userCd, String btnType) {
        if (authCd == null || authCd.isBlank()
                || user06Mapper.selectMenuButtonAuthCnt(cmpnyCd, authCd, MENU_D_ID, btnType) <= 0) {
            log.warn("일일계정 블랙리스트 권한 없음(역할 게이트 차단) - userCd={}, authCd={}, btnType={}", userCd, authCd, btnType);
            throw new ApiException(UserErrorCode.USER_403_001);
        }
    }

    /**
     * MBL_NO_ENC(AES-GCM)를 평문 숫자열로 복호화한다. 암호문 없으면 null,
     * 복호 실패(키 불일치/손상)는 조회 전체를 막지 않도록 격리하고 평문은 로그에 남기지 않는다.
     */
    private String decryptMblNo(String mblNoEnc) {
        if (mblNoEnc == null || mblNoEnc.isBlank()) {
            return null;
        }
        try {
            return aesGcmCrypto.decrypt(mblNoEnc);
        } catch (Exception e) {
            log.warn("블랙리스트 휴대폰 복호화 실패(LAST4 기반 마스킹으로 대체) - {}", e.getMessage());
            return null;
        }
    }

    /**
     * 휴대폰을 가운데 마스킹 형태로 변환한다(평문 노출 금지).
     * 복호 성공: 11자리 010-****-XXXX / 10자리 0XX-***-XXXX.
     * 복호 실패/없음: LAST4 기반 폴백("***-****-XXXX"), LAST4 도 없으면 "-".
     */
    private String maskMblNo(String digits, String last4) {
        if (digits != null && !digits.isBlank()) {
            String d = digits.replaceAll("\\D", "");
            if (d.length() == 11) {
                return d.substring(0, 3) + "-****-" + d.substring(7);
            }
            if (d.length() == 10) {
                return d.substring(0, 3) + "-***-" + d.substring(6);
            }
        }
        if (last4 != null && !last4.isBlank()) {
            return "***-****-" + last4;
        }
        return "-";
    }
}
