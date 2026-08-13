package com.prafta.platform.customer.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회사 통상근로시간 기준값 변경 요청 DTO (POST /platformApi/customer/std-work-policy).
 *
 * <p>Platform_03(고객 리스트) 행의 [통상근로시간] 셀에서 호출한다. 회사 등록(Platform_01) 시점에만
 * 입력할 수 있던 기준값을 <b>등록 후에도 운영자가 정정</b>할 수 있게 하는 진입점이다.
 *
 * <p><b>모드로 표현하는 이유</b> — "미지정(행 없음)"과 "직접 지정"은 의미가 다르다. 미지정은
 * 코드 폴백 2400분(주 40시간)을 쓰며, 이를 0/null 값으로 뭉뚱그리면 화면·서버 어느 쪽에서도
 * 의도를 구분할 수 없다(AI 토큰 한도의 quotaMode 와 동일 설계).
 */
@Getter
@Setter
@NoArgsConstructor
public class StdWorkPolicyUpdateRequest {

    /** 대상 회사코드(필수). */
    private String cmpnyCd;

    /**
     * 기준값 방식.
     * <ul>
     *   <li>{@code DIRECT} — 직접 지정. {@code weekStdMinutes} 필수.</li>
     *   <li>{@code DEFAULT} — 지정 해제(행 삭제). 코드 폴백 2400분(주 40시간)으로 되돌린다.</li>
     * </ul>
     */
    private String policyMode;

    /** DIRECT 모드 전용 — 주 소정근로 분(0 초과 ~ 2400). 값 범위 검증은 StdWorkHoursService 가 수행. */
    private Integer weekStdMinutes;
}
