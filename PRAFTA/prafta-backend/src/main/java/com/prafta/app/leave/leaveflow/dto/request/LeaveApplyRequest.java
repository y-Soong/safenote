package com.prafta.app.leave.leaveflow.dto.request;

import java.util.List;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-018-B: 앱 연차 신청 요청(camelCase JSON).
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.dto.request.LeaveApplyRequest} 미러.
 * 신청자(cmpny/site/user)는 토큰으로 식별하며 본문의 식별값은 신뢰하지 않는다(IDOR 차단).
 *   nodeCd/WORK_SEQ 등 위치/구간 식별값도 본문으로 받지 않는다(웹과 달리 앱은 nodeCd 본문 미수신 — 서버 null 저장).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveApplyRequest {

    @FieldLabel("연차코드")
    @NotBlank
    @Size(max = 20)
    private String leaveCd;

    /** 요청 LEAVE_TYPE(ANNUAL/HALF_AM/HALF_PM 등 성격 코드). 표시·분류용(보안 비민감), 미전송 시 null 저장(웹 미러). */
    @FieldLabel("연차유형")
    @Size(max = 10)
    private String leaveType;

    @FieldLabel("근무일")
    @NotBlank
    @Pattern(regexp = "\\d{8}")
    private String workYmd;

    /**
     * 사용 단위 [SYS025] 00=1일/01=반차/02=시간차2h/03=시간차1h/04=시간차30분.
     * ('05' 반반차는 2026-08-07 폐지 — 신청·검증 경로에서 거부, 과거 데이터 조회만 존치)
     */
    @FieldLabel("사용단위")
    @NotBlank
    @Size(max = 2)
    private String useUnitType;

    /**
     * 반차(01) 전용 파트. {@code START}=시작기준(늦게 출근) / {@code END}=종료기준(일찍 퇴근).
     * 반차 신청 시 필수(미지정·오값이면 서버 fail-closed 거부 ATTD_400_195).
     * 다른 사용 단위에서는 무시된다(반차 시간대 도입 HB-02).
     */
    @FieldLabel("반차구분")
    @Size(max = 5)
    private String halfPart;

    @FieldLabel("시작시각")
    @Size(max = 4)
    private String startTime;

    @FieldLabel("종료시각")
    @Size(max = 4)
    private String endTime;

    @FieldLabel("사유")
    @Size(max = 500)
    private String reason;

    /** 결재 필요 시 사용자가 구성한 결재자 순서(1단계부터). 결재 불요면 비워둠. */
    private List<String> approverUserCds;

    /** (보조) 본인 소유 결재선 프리셋 ID. approverUserCds 가 비었고 결재 필요 시 서버에서 본인 프리셋을 전개한다. */
    @FieldLabel("결재선프리셋")
    @Size(max = 20)
    private String presetId;

    /**
     * prafta-com-011-2: 가불(미래 연차 당겨쓰기) 신청 여부. 기본 false(미전송 시 false 취급).
     *
     * <p>웹 {@code com.prafta.web.attd.leaveflow.dto.request.LeaveApplyRequest} 미러.
     *   true 면 시스템 법정 연차(월차/본연차)에 한해 잔여 부족분을 미래 발생 연차에서 당겨 차감한다(결재 강제).
     */
    private Boolean isBorrow;
}
