package com.prafta.web.user.user10.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소정-10: 소정근로시간 이력 등록/정정 요청 (User_10 · StdWorkHoursEditPop 공용).
 *
 * <p><b>등록(register)</b> = 계약 변경. 직전 열린 행이 자동 마감되고 새 행이 쌓인다.
 * <b>정정(correct)</b> = 오입력 수정. 적용 시작일은 키라서 변경할 수 없다.
 *
 * <p>단축 사유(육아기·임신기·가족돌봄)는 <b>적용 종료일이 필수</b>다 — 검증은 서버
 * ({@code StdWorkHoursService})가 단일 출처로 수행한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class StdWorkSaveRequest {

    /** 대상 사용자 코드 (필수). 사업장/부서는 서버가 재조회해 권한 게이트에 쓴다. */
    private String userCd;

    /** 적용 시작일 (YYYYMMDD, 필수). 정정 시에는 대상 행을 지목하는 키. */
    private String applyStrDate;

    /** 적용 종료일 (YYYYMMDD, 무기한이면 빈 값). 단축 사유는 필수. */
    private String applyEndDate;

    /** 주 소정근로 분 (필수, 2400 = 주 40시간) */
    private Integer weekStdMinutes;

    /** 사유코드 [SYS083] (필수) */
    private String reasonCd;

    /** 사유 상세 (선택, 500자 이내) */
    private String reasonDetail;
}
