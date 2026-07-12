package com.prafta.platform.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.PasswordHasher;
import com.prafta.platform.common.PlatformConstants;
import com.prafta.platform.common.command.CompanyInsertCommand;
import com.prafta.platform.common.command.PlatformUserInsertCommand;
import com.prafta.platform.company.mapper.CompanyProvisionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 최초 플랫폼 운영자 계정 부트스트랩 러너(A안: 1회용 플래그).
 *
 * <p>{@code prafta.platform.bootstrap.enabled=true} 일 때만 빈으로 등록되어 기동 시 1회 동작한다.
 * 운영자 회사({@code prafta_system_admin})와 운영자 계정을 멱등 생성한다(이미 존재하면 skip).
 * 생성 직후 "플래그를 끄라"는 경고 로그를 남긴다.
 *
 * <p>평문 비밀번호/휴대폰은 절대 로그에 남기지 않는다(USER_ID·휴대폰 last4 만 출력).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "prafta.platform.bootstrap.enabled", havingValue = "true")
public class PlatformOperatorBootstrapRunner implements ApplicationRunner {

    private final CompanyProvisionMapper companyProvisionMapper;
    private final AesGcmCrypto aesGcmCrypto;
    private final HmacSigner hmacSigner;
    private final PasswordHasher passwordHasher;

    @Value("${prafta.platform.bootstrap.operator-id:}")
    private String operatorId;

    @Value("${prafta.platform.bootstrap.operator-pw:}")
    private String operatorPw;

    @Value("${prafta.platform.bootstrap.operator-nm:}")
    private String operatorNm;

    @Value("${prafta.platform.bootstrap.operator-mbl:}")
    private String operatorMbl;

    private static final int PHONE_MIN_DIGITS = 10;
    private static final int PHONE_MAX_DIGITS = 11;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {

        // 0) 필수 프로퍼티 검증(부재 시 기동 중단 없이 skip — 잘못된 플래그 방어).
        if (isBlank(operatorId) || isBlank(operatorPw) || isBlank(operatorNm) || isBlank(operatorMbl)) {
            log.error("[부트스트랩] 운영자 프로퍼티 누락으로 건너뜀 - operator-id/pw/nm/mbl 을 모두 지정하세요.");
            return;
        }

        String phoneNorm = Normalizers.normalizePhone(operatorMbl);
        if (phoneNorm == null
                || phoneNorm.length() < PHONE_MIN_DIGITS
                || phoneNorm.length() > PHONE_MAX_DIGITS) {
            log.error("[부트스트랩] 운영자 휴대폰 형식 오류로 건너뜀(자리수 불일치).");
            return;
        }

        String cmpnyCd = PlatformConstants.PLATFORM_CMPNY_CD;

        // 1) 운영자 회사 멱등 생성(없을 때만).
        if (companyProvisionMapper.selectCmpnyExists(cmpnyCd) == 0) {
            companyProvisionMapper.insertCmpny(new CompanyInsertCommand(
                    cmpnyCd
                    , "PRAFTA 운영"
                    , "PLATFORM"   // 운영사 자체 회사 — 실 사업자번호 없음(placeholder).
                    , "Y"
                    , "N"
                    , null
                    , "SYSTEM"
            ));
            log.info("[부트스트랩] 운영자 회사 생성 - cmpnyCd={}", cmpnyCd);
        } else {
            log.info("[부트스트랩] 운영자 회사 이미 존재 - 생성 건너뜀. cmpnyCd={}", cmpnyCd);
        }

        // 2) 운영자 계정 멱등 생성(USER_ID 기준).
        if (companyProvisionMapper.selectUserIdExists(cmpnyCd, operatorId.trim()) > 0) {
            log.info("[부트스트랩] 운영자 계정 이미 존재 - 생성 건너뜀. userId={}", operatorId.trim());
            log.warn("[부트스트랩] 멱등 skip 완료. ★ prafta.platform.bootstrap.enabled 플래그를 끄세요(false).");
            return;
        }

        // 3) USER_CD 채번 + master 계정 생성(즉시 활성 '01', 사이트/노드 없음).
        String userCd = companyProvisionMapper.selectNextUserCd(cmpnyCd);

        String phoneEnc = aesGcmCrypto.encrypt(phoneNorm);
        String phoneHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
        String phoneLast4 = Normalizers.last4(phoneNorm);
        String userPw = passwordHasher.hash(operatorPw);

        companyProvisionMapper.insertUser(new PlatformUserInsertCommand(
                cmpnyCd
                , userCd
                , operatorId.trim()
                , operatorNm.trim()
                , userPw
                , null   // SITE_CD — 운영자는 사업장 비소속.
                , null   // NODE_CD — 운영자는 부서 비소속.
                , AuthRoleUtils.AUTH_MASTER
                , "01"   // 즉시 활성.
                , phoneEnc
                , phoneHmac
                , phoneLast4
                , "SYSTEM"
        ));

        log.info("[부트스트랩] 운영자 계정 생성 완료 - cmpnyCd={}, userCd={}, userId={}, mblLast4={}",
                cmpnyCd, userCd, operatorId.trim(), phoneLast4);
        log.warn("[부트스트랩] ★ 부트스트랩 완료. 즉시 prafta.platform.bootstrap.enabled 플래그를 끄세요(false). "
                + "재기동 시 멱등 skip 되지만, 플래그는 보안상 비활성으로 되돌리는 것이 원칙입니다.");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
