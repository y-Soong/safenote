package com.prafta.common.cmm.sch.vo;

/**
 * 기본 근무타입 선택 옵션 단건 (PRAFTA-COM-008-E-5/E-8).
 *
 * <p>사업장 활성 근무타입(tb_sch_mgmt USE_YN='Y')의 select 옵션 표시용.
 * User_01 기본 근무타입 select / 로그인 게이트 팝업에서 공용으로 사용한다.
 *
 * <p>시각 필드는 'HHmm' 4자리 문자열(tb_sch_mgmt varchar(4)). 라벨 결합은 프론트에서 처리한다.
 * <p>⚠️ MyBatis 위치매핑 — record 필드 순서 = SELECT 컬럼 순서(별칭 매핑이지만 일관성 유지).
 */
public record SchOptionVO(
        String schCd
        , String schNo
        , String fstSchStrTime
        , String fstSchEndTime
        , String secSchStrTime
        , String secSchEndTime
) {
}
