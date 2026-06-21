package com.prafta.web.attd.leaveflow.dto.request;

import java.util.List;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연차 신청 요청 (prafta-019-E). 신청자는 토큰으로 식별(요청 body의 사용자코드 신뢰 안 함).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveApplyRequest {

    @FieldLabel("연차코드")
    @NotBlank
    @Size(max = 20)
    private String leaveCd;

    /** 요청 LEAVE_TYPE (ANNUAL/HALF_AM/HALF_PM 등 성격 코드) */
    @FieldLabel("연차유형")
    @Size(max = 10)
    private String leaveType;

    @FieldLabel("근무일")
    @NotBlank
    @Pattern(regexp = "\\d{8}")
    private String workYmd;

    /** 사용 단위 [SYS025] 00=1일/01=반차/02=시간차2h/03=시간차1h/04=시간차30분 */
    @FieldLabel("사용단위")
    @NotBlank
    @Size(max = 2)
    private String useUnitType;

    @FieldLabel("시작시각")
    @Size(max = 4)
    private String startTime;

    @FieldLabel("종료시각")
    @Size(max = 4)
    private String endTime;

    @FieldLabel("사유")
    @Size(max = 500)
    private String reason;

    @FieldLabel("근무노드")
    @Size(max = 50)
    private String nodeCd;

    /** 결재 필요 시 사용자가 구성한 결재자 순서(1단계부터). 결재 불요면 비워둠. */
    private List<String> approverUserCds;

    /**
     * prafta-com-011-2: 가불(미래 연차 당겨쓰기) 신청 여부. 기본 false(미전송 시 false 취급).
     *
     * <p>true 면 시스템 법정 연차(월차/본연차)에 한해 잔여 부족분을 미래 발생 연차에서 당겨 차감한다.
     *   가불은 결재를 강제하며(결정 §4), 식별값은 토큰에서 강제하고 본 플래그만 본문 신뢰한다(IDOR 무관).
     */
    private Boolean isBorrow;
}
