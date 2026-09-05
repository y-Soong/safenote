package com.prafta.web.attd.leaveflow.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 예상 차감액 미리보기 요청 (연차 시간차 환산 개편 LC-07 — T3).
 *
 * <p>신청 폼에서 단위/시간 선택 변경 시 호출한다. INSERT 없음(조회 전용).
 * 신청자는 토큰으로 식별(본문 식별값 비신뢰 — IDOR). 검증 가드(휴게 가로지름·스케줄 내 등)는
 * 신청({@code /leaveflow/apply})과 동일하게 태워 "신청하면 거부될 값"을 미리 보여주지 않는다.
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

    /** 휴게시간 무시 요청(BW-04, 앱 미러). 'Y'/'N', 미전송=N. */
    @FieldLabel("휴게시간무시")
    @Size(max = 1)
    @Pattern(regexp = "[YN]")
    private String brkWaiveYn;

    /** v2(BW2-04, 앱 미러): 넘길 휴게 분량 W(분). 미전송=0, 15분 배수, 반차 전용. */
    @FieldLabel("휴게넘김분")
    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(720)
    private Integer brkWaiveMin;

    /** v2(BW2-04, §7 Q3, 앱 미러): 반차 preview 파트(START/END, 선택). 미전송이면 종전(END 미체크 + 양 파트 요약). */
    @FieldLabel("반차구분")
    @Size(max = 5)
    private String halfPart;
}
