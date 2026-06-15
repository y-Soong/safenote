package com.prafta.app.notiset.notiset01.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.app.notiset.notiset01.dto.request.PushSettingSaveRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 푸시 설정 저장(PUT) Param (PRAFTA-APP-021-1).
 *
 * <p>cmpnyCd/userCd/authCd 는 JWT 클레임에서만 도출(IDOR 차단). 본문은 masterOn + 토글 변경 목록만.
 *
 * <p>입력 검증:
 *   <ul>
 *     <li>tokenInfo/식별자 누락 → COMMON_400_003 (진짜 인증 결함만 003 — 앱 인터셉터 강제 로그아웃).</li>
 *     <li>request null → COMMON_400_001 (본문 검증 실패는 003/600 금지, 강제 로그아웃 회피).</li>
 *   </ul>
 *
 * <p>masterOn 이 null 이면 마스터 미변경. toggleKey/on 이 null 인 항목은 무시(서비스에서 화이트리스트 필터).
 */
public record PushSettingSaveParam(
    String cmpnyCd
    , String userCd
    , String authCd
    , Boolean masterOn
    , List<ToggleChange> toggleChanges
){
    /** 토글 변경 1건(서비스에서 화이트리스트 검증 후 NOTI_TYPE 행으로 전개). */
    public record ToggleChange(String toggleKey, boolean on) {
    }

    public static PushSettingSaveParam from(PushSettingSaveRequest request, TokenInfo tokenInfo) {
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        List<ToggleChange> changes = new ArrayList<>();
        if (request.getItems() != null) {
            for (PushSettingSaveRequest.PushSettingToggleItem item : request.getItems()) {
                if (item == null || item.getToggleKey() == null || item.getToggleKey().isBlank()
                        || item.getOn() == null) {
                    // 불완전 항목은 무시(부분 저장 허용, 검증 실패로 전체 거부하지 않음).
                    continue;
                }
                changes.add(new ToggleChange(item.getToggleKey(), item.getOn()));
            }
        }

        return new PushSettingSaveParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , request.getMasterOn()
            , changes
        );
    }
}
