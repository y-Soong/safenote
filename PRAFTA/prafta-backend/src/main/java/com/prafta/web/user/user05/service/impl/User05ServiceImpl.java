package com.prafta.web.user.user05.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.user.user05.application.param.DailyUserListParam;
import com.prafta.web.user.user05.application.query.DailyUserListQuery;
import com.prafta.web.user.user05.dto.response.DailyUserListResponse;
import com.prafta.web.user.user05.mapper.User05Mapper;
import com.prafta.web.user.user05.result.DailyUserListRaw;
import com.prafta.web.user.user05.result.DailyUserListResult;
import com.prafta.web.user.user05.service.User05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class User05ServiceImpl implements User05Service {

    private final User05Mapper user05Mapper;
    private final HmacSigner hmacSigner;
    // 휴대폰 평문 표시를 위한 AES-GCM 복호화(MBL_NO_ENC → 평문). 관리자 운영 화면 요구.
    private final AesGcmCrypto aesGcmCrypto;

    /** 조회 페이지 크기 상한(전수조회 방지, IDOR/DoS 완화). */
    private static final int PAGE_SIZE = 500;

    @Override
    public DailyUserListResponse selectDailyUserList(DailyUserListParam param) {
        log.info("일일사용자 관리 조회 진입 - cmpnyCd={}, siteCd={}, managerYn={}",
                param.gvCmpnyCd(), param.siteCd(), AuthRoleUtils.isManager(param.gvAuthCd()) ? "Y" : "N");

        boolean isManager = AuthRoleUtils.isManager(param.gvAuthCd());

        // 비권한자(master/hr 아님)가 특정 사업장을 지정한 경우, 해당 사업장 접근 권한을 선검증(cross-site IDOR 차단).
        // 사업장 미지정 시에는 SQL 스코프(TB_USER_SITE_AUTH IN)로 보유 사업장만 노출되므로 별도 차단 불필요.
        String siteCd = (param.siteCd() != null && !param.siteCd().isBlank()) ? param.siteCd() : null;
        if (!isManager && siteCd != null
                && user05Mapper.countUserSiteAuth(param.gvCmpnyCd(), param.gvUserCd(), siteCd) == 0) {
            log.warn("일일사용자 관리 조회 사업장 권한 없음 - userCd={}, siteCd={}", param.gvUserCd(), siteCd);
            throw new ApiException(CommonErrorCode.COMMON_403_001);
        }

        // 전화번호 검색: 평문 비교 금지. 정규화 후 HMAC(정확매칭)/LAST4(부분) 파생값만 쿼리에 전달.
        String mblNoHmac = null;
        String mblNoLast4 = null;
        if (param.mblNo() != null && !param.mblNo().isBlank()) {
            String phoneNorm = Normalizers.normalizePhone(param.mblNo());
            if (phoneNorm != null) {
                mblNoHmac = hmacSigner.hmacSha256Base64Url(phoneNorm);
                mblNoLast4 = Normalizers.last4(phoneNorm);
            } else {
                // 정규화 실패(자릿수 부족 등) → 매칭 불가능한 값으로 유도(빈 결과). LAST4 만 부분검색 시도.
                String digits = param.mblNo().replaceAll("\\D", "");
                if (digits.length() >= 4) {
                    mblNoLast4 = digits.substring(digits.length() - 4);
                }
            }
        }

        // 소속부서(nodeCd) 정규화 + 하위부서 포함 여부
        String nodeCd = (param.nodeCd() != null && !param.nodeCd().isBlank()) ? param.nodeCd() : null;
        String incSubNodeYn = "Y".equals(param.incSubNodeYn()) ? "Y" : "N";

        DailyUserListQuery query = new DailyUserListQuery(
            siteCd
            , nodeCd
            , incSubNodeYn
            , (param.userNm() != null && !param.userNm().isBlank()) ? param.userNm() : null
            , mblNoHmac
            , mblNoLast4
            , (param.occupyFrom() != null && !param.occupyFrom().isBlank()) ? param.occupyFrom() : null
            , (param.occupyTo() != null && !param.occupyTo().isBlank()) ? param.occupyTo() : null
            , isManager ? "Y" : "N"
            , PAGE_SIZE
            , 0
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );

        List<DailyUserListRaw> rawList = user05Mapper.selectDailyUserList(query);

        // 휴대폰(MBL_NO_ENC) 복호화 + 하이픈 포맷하여 평문 응답으로 변환(이름은 SQL 평문 그대로).
        List<DailyUserListResult> dailyUserList = rawList.stream()
                .map(r -> new DailyUserListResult(
                        r.hisId()
                        , r.userNm()
                        , formatMblNo(decryptMblNo(r.mblNoEnc()))
                        , r.siteNm()
                        , r.nodeNm()
                        , r.slotNo()
                        , r.occupyDtime()
                        , r.releaseDtime()))
                .toList();

        log.info("일일사용자 관리 조회 종료 - cmpnyCd={}, rows={}", param.gvCmpnyCd(), dailyUserList.size());

        return DailyUserListResponse.builder()
                .dailyUserList(dailyUserList)
                .build();
    }

    /**
     * MBL_NO_ENC(AES-GCM 암호문)를 평문 휴대폰 숫자열로 복호화한다.
     *
     * <p>암호문이 없으면 null. 복호화 실패(키 불일치/손상)는 조회 전체를 막지 않도록 격리하고
     * 평문(휴대폰)은 로그에 남기지 않는다(PII 평문 로그 금지).
     */
    private String decryptMblNo(String mblNoEnc) {
        if (mblNoEnc == null || mblNoEnc.isBlank()) {
            return null;
        }
        try {
            return aesGcmCrypto.decrypt(mblNoEnc);
        } catch (Exception e) {
            log.warn("일일사용자 휴대폰 복호화 실패(해당 행만 '-' 표시) - {}", e.getMessage());
            return null;
        }
    }

    /**
     * 숫자열 휴대폰을 하이픈 포맷으로 변환한다(11자리 010-XXXX-XXXX, 10자리 0XX-XXX-XXXX).
     * 없으면 '-', 그 외 자리수는 원문 그대로 반환한다.
     */
    private String formatMblNo(String digits) {
        if (digits == null || digits.isBlank()) {
            return "-";
        }
        String d = digits.replaceAll("\\D", "");
        if (d.length() == 11) {
            return d.substring(0, 3) + "-" + d.substring(3, 7) + "-" + d.substring(7);
        }
        if (d.length() == 10) {
            return d.substring(0, 3) + "-" + d.substring(3, 6) + "-" + d.substring(6);
        }
        return digits;
    }
}
