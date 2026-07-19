package com.prafta.web.subcon.subcon03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 수신 스냅샷 첨부 서빙 파라미터(PRAFTA-SUBCON-T7 §5-9 — IDOR 봉인).
 *
 * <p>회사 스코프(gvCmpnyCd)는 JWT 클레임에서만 도출한다(클라 파라미터 회사코드 금지 → 타사 파일 접근 불가).
 *    소유(snapshotId OWNER=gv) + 참조(fileMgmtCd 가 그 스냅샷 첨부 집합에 존재) 검증은 조회 SQL 안에서 강제한다.
 */
public record SnapshotFileParam(
    Long snapshotId
    , String fileMgmtCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static SnapshotFileParam from(Long snapshotId, String fileMgmtCd, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SnapshotFileParam(
            snapshotId
            , fileMgmtCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
