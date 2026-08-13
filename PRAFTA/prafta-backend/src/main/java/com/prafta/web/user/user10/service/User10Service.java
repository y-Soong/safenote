package com.prafta.web.user.user10.service;

import com.prafta.web.user.user10.application.param.StdWorkSaveParam;
import com.prafta.web.user.user10.application.param.StdWorkUserListParam;
import com.prafta.web.user.user10.dto.response.StdWorkHistoryResponse;
import com.prafta.web.user.user10.dto.response.StdWorkReasonOptionsResponse;
import com.prafta.web.user.user10.dto.response.StdWorkSaveResponse;
import com.prafta.web.user.user10.dto.response.StdWorkUserListResponse;

/**
 * 소정-10: 소정근로시간 관리·이력 서비스 (User_10).
 *
 * <p>지시서 §0단계 "관리 화면(등록·변경·이력 조회, 15h 미만 경고·사유별 범위 검증,
 * canManageNode 게이트 필수)" / plan §3 UI-C · §4 소정-10.
 *
 * <p><b>역할 경계</b> — 검증·이력 규칙(겹침·단축 종료일 필수·복귀 행 자동 생성/이동·경고)은
 * 전부 공용 {@code StdWorkHoursService} 가 단일 출처로 수행한다. 본 서비스는
 * <b>인가 + 화면용 조회/응답 변환</b>만 담당한다(규칙 복제 금지).
 *
 * <p><b>인가</b> — 본인 스코프 EP({@code /comApi/leave-feature/std-work-summary})와 달리
 * <b>userCd 를 받는 타인 조회/쓰기</b>이므로 사업장 인가 + 부서 게이트
 * ({@code canManageNodeExcludeSafe})를 전 EP 에 건다. 계약 근로시간은 인사 정보라
 * safe(안전관리자)는 전사 통과에서 제외한다(User_09 와 동일 기준).
 */
public interface User10Service {

    /** 관리 대상 근로자 목록 + 오늘 기준 유효 소정(미입력이면 null 로 내려 화면이 배지 처리). */
    StdWorkUserListResponse selectStdWorkUserList(StdWorkUserListParam param);

    /** 특정 근로자의 이력 타임라인 + 오늘 기준 요약(출처/단시간 파생/대상 여부). */
    StdWorkHistoryResponse selectStdWorkHistory(String gvCmpnyCd, String gvAuthCd, String gvUserCd,
                                                String gvSiteCd, String targetUserCd);

    /**
     * 등록/정정 팝업 옵션 — 통상 기준값 + SYS083 전 사유(단축 포함) + 경고 임계값.
     *
     * @param siteCd 대상 사업장(선택). 지정 시 사업장 오버라이드 기준값을 내려주며, 다른 사업장
     *               기준값 열람을 막기 위해 사업장 인가를 건다. 미지정이면 회사 기본값.
     */
    StdWorkReasonOptionsResponse getReasonOptions(String cmpnyCd, String gvAuthCd, String gvUserCd,
                                                  String gvSiteCd, String siteCd);

    /** 이력 등록(계약 변경 — 직전 열린 행 자동 마감 + 신규 행). 결과의 경고/복귀 행 정보를 그대로 반환. */
    StdWorkSaveResponse registerStdWorkHours(StdWorkSaveParam param);

    /** 이력 정정(오입력 수정 — 적용 시작일 불변). 복귀 행 동기화 결과를 그대로 반환. */
    StdWorkSaveResponse correctStdWorkHours(StdWorkSaveParam param);
}
