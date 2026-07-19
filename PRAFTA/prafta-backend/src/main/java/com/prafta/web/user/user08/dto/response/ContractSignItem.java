package com.prafta.web.user.user08.dto.response;

/**
 * 서명 이력 항목 (User_08 탭2 테이블 행).
 *
 * <p>이름은 서명 시점 스냅샷(USER_NM_SNAPSHOT), 휴대폰은 서버에서 복호화한 평문을
 * 하이픈 포맷("010-XXXX-XXXX")으로 노출한다(2026-07-17 마스킹 해제 결정 — User_05 전례 미러).
 * 파일 경로는 노출하지 않는다 — 열람/다운로드는 GET contract-sign-image 스트림만 사용.
 */
public record ContractSignItem(
    String signId
    , String userCd
    , String userNm            // 서명 시점 스냅샷
    , String mblNo             // 평문 하이픈 포맷(복호화 실패/계정 삭제 시 "-")
    , int contractVer
    , String firstWorkDate     // YYYYMMDD
    , String signDtime         // YYYY-MM-DD HH:mm
    , String mergedSha256      // 무결성 해시(화면에서 축약 표기)
) {
}
