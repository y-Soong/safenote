package com.prafta.web.attd.attd07.dto.response;

import java.util.List;

import com.prafta.web.attd.attd07.result.AttdCloseHistResult;

import lombok.Builder;
import lombok.Value;

/**
 * 근태 마감 상태 + 차단 사유 현황 응답.
 *
 * <p>prafta-019-C 근태 마감. 차단 사유 3종(§13.3) 카운트와 마감 가능 여부, 이력을 함께 전달한다.
 */
@Value
@Builder
public class AttdCloseStatusResponse {

    String closeYm;
    /** 마감 스코프 부서 노드 ('*' = 전체 사업장) */
    String nodeCd;
    /** 하위부서 포함 여부 (Y/N) */
    String incSubYn;
    /** OPEN 미마감 / CLOSED 마감 */
    String closeStatus;
    boolean closed;

    String closeDtime;
    String closeUserCd;
    String uncloseDtime;
    String uncloseUserCd;
    String closeDesc;

    // ===== 차단 사유 현황 (§13.3) =====
    /** 미결(대기) 요청 건수 (초과근무 제외) */
    int pendingReqCnt;
    /** GPS 미확인(Mock 위치 등) 건수 */
    int gpsUnconfirmedCnt;
    /** 미승인 초과근무(초과근무 신청 대기) 건수 */
    int unapprovedOtCnt;

    /** 미결 연차 변경(이동/삭제) 요청 건수 — REQUESTED(근로자 응답대기) + AGREED(관리자 확인대기) */
    int pendingLeaveChangeCnt;
    /** 차단 사유 합계 */
    int blockTotalCnt;

    /** 마감 실행 가능 여부 (미마감 + 차단 사유 0건) */
    boolean closable;

    /** 마감/해제 이력 */
    List<AttdCloseHistResult> histList;
}
