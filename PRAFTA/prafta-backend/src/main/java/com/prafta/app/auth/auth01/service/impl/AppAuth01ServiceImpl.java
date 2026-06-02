package com.prafta.app.auth.auth01.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.app.auth.auth01.application.param.WithdrawParam;
import com.prafta.app.auth.auth01.dto.response.WithdrawResponse;
import com.prafta.app.auth.auth01.mapper.AppAuth01Mapper;
import com.prafta.app.auth.auth01.service.AppAuth01Service;
import com.prafta.common.cmm.login.application.param.LogoutParam;
import com.prafta.common.cmm.login.service.LoginService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-010-07: 회원 탈퇴 서비스 구현 (D5).
 *
 * <p>단일 트랜잭션:
 * <ol>
 *   <li>본인 USER_ID 조회(이력 PK 용)</li>
 *   <li>본인 소유 프리셋 hard delete(디테일 → 마스터)</li>
 *   <li>tb_user: ACCOUNT_STATUS='03' + PII 마스킹/무효화 + WITHDRAWAL_DATE + USER_PW 무효화 + USER_NM 마스킹</li>
 *   <li>tb_del_user INSERT(마스킹 이름)</li>
 *   <li>토큰 폐기·선점 해제(기존 로그아웃 흐름 재사용)</li>
 * </ol>
 *
 * <p>제외(D5): 연차 자동취소, 결재자 알림. 출퇴근/근태/안전/TBM 기록은 보존(USER_CD 유지, 근로기준법 3년).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuth01ServiceImpl implements AppAuth01Service {

    private final AppAuth01Mapper appAuth01Mapper;
    private final LoginService loginService;

    // 탈퇴 시 USER_NM 마스킹 고정값(plan §4 ②/③: 김○○).
    private static final String MASKED_USER_NM = "김○○";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdraw(WithdrawParam param) {
        TokenInfo tokenInfo = param.tokenInfo();
        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();

        // 0) 게이트: 명시적 동의 필수.
        if (!param.confirmed()) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        // 1) 이력 PK 용 USER_ID 확보(마스킹 전).
        String userId = appAuth01Mapper.selectUserId(cmpnyCd, userCd);
        if (userId == null || userId.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_004);
        }

        // 2) 본인 소유 프리셋 hard delete (디테일 → 마스터).
        appAuth01Mapper.deleteMyPresetSteps(cmpnyCd, userCd);
        appAuth01Mapper.deleteMyPresetMasters(cmpnyCd, userCd);

        // 3) tb_user 탈퇴 처리.
        int updated = appAuth01Mapper.withdrawUser(cmpnyCd, userCd, MASKED_USER_NM);
        if (updated == 0) {
            throw new ApiException(CommonErrorCode.COMMON_400_004);
        }

        // 4) tb_del_user 이력 INSERT(마스킹 이름).
        appAuth01Mapper.insertDelUser(cmpnyCd, userId, MASKED_USER_NM, userCd);

        // 5) 토큰 폐기·선점 해제(기존 로그아웃 흐름 재사용). APP 클라이언트.
        loginService.logout(LogoutParam.from("APP", tokenInfo));

        log.info("앱 회원 탈퇴 완료 - userCd={}", userCd);

        return WithdrawResponse.builder().success(true).build();
    }
}
