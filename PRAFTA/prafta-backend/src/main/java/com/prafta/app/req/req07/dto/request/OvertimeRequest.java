package com.prafta.app.req.req07.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-007: 초과근무 신청 등록 body (POST /appApi/req07/overtime).
 *
 * <p>REQ_TYPE='03' 고정 (초과근무 생성 요청). 수정은 본 작업 범위 외 (원본 요청서 명시).
 * plan §4.3 PRAFTA-APP-007-4 명세.
 */
@Getter
@Setter
@NoArgsConstructor
public class OvertimeRequest {

    /** 대상 근무일 (YYYYMMDD). */
    private String workYmd;

    /** 본인의 해당 일자 노드 코드. */
    private String nodeCd;

    /**
     * 구간 배열 (1~2). 각 slot 의 workSeq + startDate/startTime/endDate/endTime 사용.
     * prafta-043: 초과근무 유형(OT_TYPE) 전면 파기 — 유형 입력/저장 없음.
     */
    private List<SlotRequest> slots;

    /** 신청 사유 (필수, 최대 500자). */
    private String reqReason;

    /**
     * prafta-app-009: 'N' 결재선 결재자 순서 목록(1차). 비면 presetId 폴백.
     * 'Y'/즉시승인 케이스에서는 무시/빈 허용(서버가 노드 SELF_ATTD_APPRV_YN 으로 분기).
     */
    private List<String> approverUserCds;

    /** prafta-app-009: approverUserCds 가 비었을 때 전개할 본인 소유 프리셋 ID(없으면 null). */
    private String presetId;

    /**
     * 소정-07 - 근로자 명시 청구 확인 값 ('Y' 만 확인으로 인정).
     *
     * <p>육아기·가족돌봄 근로시간 단축 기간의 연장근로는 사업주가 요구할 수 없고 근로자가 명시적으로
     * 청구한 경우에만 가능하다(위반 시 1천만원 이하 벌금). 단축 기간이 아닌 근로자(대다수)에게는
     * 값이 무엇이든 아무 영향이 없다 — 게이트 진입 자체가 없다.
     *
     * <p><b>additive 필드</b>: 구버전 앱이 미전송하면 null → 확인 없음 → 단축 기간 한정 거부
     * (ATTD_400_201). 허용이 아니라 거부가 기본값인 fail-safe 방향이다.
     * (앱 화면의 확인 체크박스는 소정-12 범위 — 여기서는 서버 계약만 연다.)
     */
    private String reducedWorkOtClaimYn;
}
