package com.prafta.web.attd.attd07.application.model;

public record UpdateUserAttdInfosModel(
	String attdId
    , String siteCd
    , String nodeCd
    , String userCd
    , String userId
    , String workSeq
    , String workYmd
    
    , String oriCheckInDate
    , String oriCheckInTime
    , String oriCheckOutDate
    , String oriCheckOutTime
    
    , String checkInDate
    , String checkInTime
    , String checkInMethod
    , String checkOutDate
    , String checkOutTime
    , String checkOutMethod
    , String reason
    , String gvCmpnyCd
    , String gvUserCd
    // [보안 하드닝] 관리자 직접 근태 생성/수정 인가 게이트용 토큰 권위 필드.
    //   gvAuthCd: 역할 게이트(전사역할 통과 + 노드 관리자 판정)에 사용(JWT 기반, body 위조 불가).
    //   gvSiteCd: 호출자 세션 사업장(cross-site IDOR 차단 검증에 사용).
    , String gvAuthCd
    , String gvSiteCd
) {
}
