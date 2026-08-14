package com.prafta.app.leave.leaveflow.service;

import java.util.List;

import com.prafta.app.leave.leaveflow.dto.response.LeaveApplyMultiPreviewResponse;
import com.prafta.common.dto.TokenInfo;

/**
 * prafta-leavemulti: 연차 기간(From-To) 신청 오케스트레이터 (종일 전용).
 *
 * <p><b>설계 — 기존 단일일 로직을 수정하지 않고 "호출"만 한다.</b>
 * 1 신청을 날짜별 단일일 신청 N회로 분해해 {@code AppLeaveFlowService.submitLeave} 를 그대로 태운다.
 * 그래서 검증·차감·결재·알림이 전부 기존 경로이며 신규 판정 로직이 없다.
 *
 * <p>대안이었던 "REQ 1건 + leave_use 날짜별 N행" 은 채택하지 않았다 — REQ 단위 대표행 dedupe
 * (MIN(LEAVE_ID))가 9개 매퍼 11곳에 날짜 상관조건 없이 박혀 있어, 날짜별 N행을 만들면
 * 첫 날짜만 남고 나머지가 화면에서 사라진다.
 *
 * <p><b>★ 이 서비스는 반드시 별도 빈이어야 한다.</b> 같은 빈에서 {@code submitLeave} 를 호출하면
 * self-invocation 으로 {@code @Transactional} 이 무시되어 각 INSERT 가 autocommit 되고,
 * 정책 ②(전체 실패 = 전체 롤백)가 성립하지 않는다.
 */
public interface MultiDayLeaveApplyService {

    /**
     * 미리보기 — 구간의 날짜별 선택 가능 여부·기본 체크 상태와 잔여 배정 결과를 계산한다(읽기 전용).
     *
     * @param fromYmd 구간 시작(YYYYMMDD)
     * @param toYmd   구간 종료(YYYYMMDD)
     */
    LeaveApplyMultiPreviewResponse preview(TokenInfo tokenInfo, String leaveCd,
                                           String fromYmd, String toYmd);

    /**
     * 제출 — 2-Phase.
     * <ol>
     *   <li>Phase 1(읽기 전용): 전 날짜 검증 + 잔여 배정 시뮬레이션. 하나라도 막히면
     *       <b>아무것도 만들지 않고</b> 막힌 날짜 <b>전부</b>를 담아 거부한다.</li>
     *   <li>Phase 2(단일 트랜잭션): 날짜별 {@code submitLeave} 호출. 하나라도 실패하면 전체 롤백.</li>
     * </ol>
     *
     * @return 생성된 묶음 ID
     */
    String applyMulti(TokenInfo tokenInfo, String leaveCd, String leaveType, List<String> dates,
                      String reason, List<String> approverUserCds, String presetId);
}
