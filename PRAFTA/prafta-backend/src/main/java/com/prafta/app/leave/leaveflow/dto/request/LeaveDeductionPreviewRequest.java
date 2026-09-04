package com.prafta.app.leave.leaveflow.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 예상 차감액 미리보기 요청 (연차 시간차 환산 개편 LC-07 — T3).
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.dto.request.LeaveDeductionPreviewRequest} 미러.
 * 신청 폼에서 단위/시간 선택 변경 시 호출. INSERT 없음(조회 전용). 신청자는 토큰 식별(IDOR).
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveDeductionPreviewRequest {

    @FieldLabel("연차코드")
    @NotBlank
    @Size(max = 20)
    private String leaveCd;

    @FieldLabel("근무일")
    @NotBlank
    @Pattern(regexp = "\\d{8}")
    private String workYmd;

    /** 사용 단위 [SYS025] 00=1일/01=반차/02=시간차2h/03=시간차1h/04=시간차30분/05=반반차 */
    @FieldLabel("사용단위")
    @NotBlank
    @Size(max = 2)
    private String useUnitType;

    /** 시간차(02/03/04)일 때만 필수(HHMM). */
    @FieldLabel("시작시각")
    @Size(max = 4)
    private String startTime;

    /** 시간차(02/03/04)일 때만 필수(HHMM). */
    @FieldLabel("종료시각")
    @Size(max = 4)
    private String endTime;

    /** 휴게시간 무시 요청(BW-04). 'Y'/'N', 미전송=N. submit 과 동일 게이트(ATTD_400_217/219)로 사전 차단. */
    @FieldLabel("휴게시간무시")
    @Size(max = 1)
    @Pattern(regexp = "[YN]")
    private String brkWaiveYn;
}
