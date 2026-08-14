package com.prafta.app.leave.leaveflow.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-leavemulti: 연차 기간(From-To) 신청 요청 본문.
 *
 * <p><b>서버 계약은 범위가 아니라 "확정된 날짜 목록"이다.</b> From/To 는 화면이 목록을 만드는 입력일 뿐이며,
 * 사용자가 미리보기에서 체크한 결과가 그대로 {@code dates} 로 온다. 그래서 서버는 휴일을 재판정하지 않는다
 * — 체크가 곧 포함 의도이므로 해석 여지가 없다(주말·휴무일도 체크했으면 신청된다).
 *
 * <p>단 <b>체크 = 유효 보장은 아니다.</b> 서버는 날짜별로 기존 단일일 가드를 그대로 재검증하며,
 * 하나라도 막히면 전체 실패한다(정책 ②).
 *
 * <p>종일 전용이므로 {@code useUnitType} 은 받지 않는다(서버가 '00' 고정). 반차/시간차는 기존 단건 신청을 쓴다.
 * 가불({@code isBorrow})도 1차 범위에서 제외이므로 받지 않는다(서버가 false 고정).
 */
@Getter
@Setter
public class LeaveApplyMultiRequest {

    @NotBlank
    private String leaveCd;

    /** 연차 종류 구분[SYS021] — 단건 신청과 동일 의미. */
    private String leaveType;

    /** 신청 대상 날짜 목록(YYYYMMDD). 미리보기에서 사용자가 체크한 날짜만 온다. */
    @NotEmpty
    private List<String> dates;

    private String reason;

    /** 결재선 직접 지정(소유권은 서버 검증). */
    private List<String> approverUserCds;

    /** 결재선 프리셋 ID. */
    private String presetId;
}
