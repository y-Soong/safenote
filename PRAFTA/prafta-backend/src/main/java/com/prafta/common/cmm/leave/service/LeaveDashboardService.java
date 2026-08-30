package com.prafta.common.cmm.leave.service;

import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.cmm.leave.command.CoverGrantCommand;
import com.prafta.common.cmm.leave.command.ManualGrantCommand;
import com.prafta.common.cmm.leave.vo.CoverGrantResultVO;
import com.prafta.common.cmm.leave.vo.HireDateGrantResultVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardResultVO;
import com.prafta.common.cmm.leave.vo.LeaveDetailResultVO;
import com.prafta.common.cmm.leave.vo.LeaveRecallResultVO;
import com.prafta.common.cmm.leave.vo.LeaveTypeOptionVO;
import com.prafta.common.cmm.leave.vo.LeaveUsageHistoryRowVO;
import com.prafta.common.cmm.leave.vo.ManualGrantResultVO;
import com.prafta.common.cmm.leave.vo.ShortfallListResultVO;

import java.util.List;

/**
 * 연차 현황 대시보드/상세/수동 부여(attd09) 비즈니스 서비스.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 *
 * <p>채널(웹/배치) 비종속 비즈니스 로직을 모은다. attd09 모듈의 {@code Attd09ServiceImpl}은
 * 본 서비스로 위임하는 어댑터다(baim07 ↔ LeavePolicyService 동일 패턴).
 *
 * <p>스코프/권한 (정책서 §8.5.7):
 * <ul>
 *   <li>목록/상세/수동부여 종류 조회: AUTH_MASTER OR AUTH_HR_MANAGER + CMPNY_CD 스코프.
 *       대시보드 조회는 전 직원/특정 직원 PII를 노출하므로 진입부에서 관리자 권한을 강제한다
 *       (요청 body의 cmpnyCd 미신뢰, JWT만).</li>
 *   <li>수동 부여(단일/일괄): AUTH_MASTER OR AUTH_HR_MANAGER 진입부 강제.</li>
 * </ul>
 */
public interface LeaveDashboardService {

    /**
     * 연차 현황 대시보드 목록 + 메트릭 조회.
     *
     * @param cmpnyCd      회사 코드 (JWT)
     * @param authCd       수행자 권한 코드 (JWT) — MASTER/HR 진입부 강제 (§8.5.7)
     * @param siteCd       사업장 코드 필터 (NULL/빈값=전체, CMPNY_CD 스코프 내)
     * @param nodeCd       소속부서 노드 필터 (NULL/빈값=전체)
     * @param incSubNodeYn 하위부서 포함 여부 Y/N (Y면 nodeCd 서브트리 포함 — attd08 패턴)
     * @param userNm       사용자명 검색어 (NULL/빈값=전체)
     * @param page         페이지(1-based)
     * @param size         페이지 크기
     */
    LeaveDashboardResultVO getDashboard(String cmpnyCd, String authCd, String siteCd, String nodeCd,
                                        String incSubNodeYn, String userNm, int page, int size);

    /**
     * 직원별 연차 상세 조회. 대상 직원이 스코프 밖이면 ApiException(NOT_FOUND).
     *
     * @param cmpnyCd 회사 코드 (JWT)
     * @param authCd  수행자 권한 코드 (JWT) — MASTER/HR 진입부 강제 (§8.5.7)
     * @param userCd  대상 직원 코드
     */
    LeaveDetailResultVO getDetail(String cmpnyCd, String authCd, String userCd);

    /**
     * 직원별 연도별 연차 사용 이력 조회(일자 전개, dateYmd 오름차순). 대상 직원이 스코프 밖이면 ApiException(NOT_FOUND).
     *
     * <p>Attd_16 연차 사용 현황 캘린더와 동일한 일자 전개 방식을 단일 사용자 스코프로 재사용한다.
     * 상태(status)는 dateYmd를 오늘과 비교해 'USED'(사용) / 'SCHEDULED'(사용예정)로 산출한다.
     *
     * @param cmpnyCd 회사 코드 (JWT)
     * @param authCd  수행자 권한 코드 (JWT) — MASTER/HR 진입부 강제 (§8.5.7)
     * @param userCd  대상 직원 코드
     * @param year    조회 연도 (YYYY, 4자리)
     */
    List<LeaveUsageHistoryRowVO> getUsageHistory(String cmpnyCd, String authCd, String userCd, String year);

    /**
     * 사용 이력 증빙 파일 열람 (연차 신청 증빙 필수화 2026-08-29). 스코프 밖이면 ApiException.
     *
     * <p>Attd_09 상세와 동일한 MASTER/HR 게이트({@code ensureManager}) + "해당 파일이 이 회사·이 직원의
     * 연차 사용 건에 실제로 첨부된 FILE_TYPE=008 파일"인지 검증 후 인증 스트림으로만 서빙한다
     * (결재선 밖 인사 관리자의 증빙 확인 경로 — 2026-08-30 사용자 확정. 무결재 자동확정 건 포함).
     *
     * @param cmpnyCd    회사 코드 (JWT)
     * @param authCd     수행자 권한 코드 (JWT) — MASTER/HR 진입부 강제 (§8.5.7)
     * @param userCd     대상 직원 코드
     * @param fileMgmtCd 증빙 파일 코드
     */
    FileBytesResult getUsageEvidenceFile(String cmpnyCd, String authCd, String userCd, String fileMgmtCd);

    /**
     * 수동 부여 가능 휴가 종류 옵션.
     *
     * @param cmpnyCd 회사 코드 (JWT)
     * @param authCd  수행자 권한 코드 (JWT) — MASTER/HR 진입부 강제 (§8.5.7)
     */
    List<LeaveTypeOptionVO> getManualGrantTypes(String cmpnyCd, String authCd);

    /**
     * 연차 수동 부여(단일/일괄 공통). @Transactional, 권한 MASTER/HR.
     *
     * @param cmpnyCd 회사 코드 (JWT)
     * @param command 부여 입력 (userCds/leaveCd/grantDays/availFromDate/reason)
     * @param authCd  수행자 권한 코드 (JWT)
     * @param userCd  수행자 사용자 코드 (INSERT_NO 기록용)
     */
    ManualGrantResultVO manualGrant(String cmpnyCd, ManualGrantCommand command, String authCd, String userCd);

    /**
     * 관리자 수동 부여 연차 회수(soft cancel, PRAFTA-031). @Transactional, 권한 MASTER/HR.
     *
     * <p>정책서 §8.5.7(권한) / §8.5.8(소프트 취소·사용 이력 불변). 회수 대상은 관리자 수동 부여건
     * (GRANT_BY_TYPE='02', GRANT_TYPE 무관 — 경력인정 이원화 Phase 2 §2-3, 2026-08-21로 MANUAL_% 접두
     * 제한 제거, 법정 수기부여(_COVER)도 포함)이며, 사용 전(STATUS='ACTIVE' AND
     * USED_DAYS=0 AND DEL_YN='N')일 때만 가능하다. 회수 = STATUS='CANCELED' 전환 + 회수 메타 기록.
     * <b>USED_DAYS는 절대 갱신하지 않는다.</b> 회수 성공 시 알림 outbox에 1건 적재(발송은 추후).
     *
     * @param cmpnyCd         회사 코드 (JWT)
     * @param grantId         회수 대상 부여 ID (PathVariable)
     * @param reason          회수 사유 (필수, 최대 500자)
     * @param authCd          수행자 권한 코드 (JWT) — MASTER/HR 진입부 강제
     * @param operatorUserCd  수행자 사용자 코드 (CANCEL_BY / outbox INSERT_NO 기록용)
     */
    LeaveRecallResultVO recallGrant(String cmpnyCd, String grantId, String reason, String authCd,
                                    String operatorUserCd);

    /**
     * 입사일 기준 연차 부여(테스트/검증용). @Transactional, 권한 MASTER/HR.
     *
     * <p>선택 직원 각각의 HIRE_DATE 기준 근속 개월 수로 법정 연차를 산정해 부여한다.
     * <ul>
     *   <li>근속 12개월 미만: 법정 월차(경과 개월수, 최대 11) → SYS_MONTHLY</li>
     *   <li>근속 12개월 이상: 본연차 15일(SYS_ANNUAL) + 활성 정책 근속가산(SYS_TENURE_BONUS)</li>
     * </ul>
     * 입사일 미입력 직원이 1명이라도 있으면 전건 부여하지 않고 예외(§입력 가드).
     * 동일 직원·연도·종류로 이미 부여된 건은 멱등 처리(중복 부여 차단, 건너뜀).
     *
     * @param cmpnyCd 회사 코드 (JWT)
     * @param userCds 대상 직원 코드 목록
     * @param authCd  수행자 권한 코드 (JWT)
     * @param userCd  수행자 사용자 코드 (INSERT_NO 기록용)
     */
    HireDateGrantResultVO hireDateGrant(String cmpnyCd, List<String> userCds, String authCd, String userCd);

    /**
     * 입사일 기준 차액 조회 목록 (경력인정 이원화 Phase 2 §2-2, read-only). 권한 = master/hr 전용
     * ({@code ensureManager} — P-13, Attd_09 본문과 동일 게이트. safe·부서 관리자는 403) +
     * 사업장 필터 지정 시 {@code assertSiteAccess} 방어선 유지.
     *
     * <p>AXIS1=FISCAL_YEAR 회사가 아니면 rows 없이 {@code fiscalYearYn='N'}만 반환한다(에러 아님 — 탭
     * 비노출 판정용). 정답 누적/실제 부여 누적/기보전 합은 사용자별 산정(엔진 computeHireBasisAccrual +
     * P-12 live 법정 부여 총량 selectStatutoryGrantedLiveTotal + selectCoverGrantTotal).
     *
     * @param cmpnyCd      회사 코드 (JWT)
     * @param authCd       수행자 권한 코드 (JWT)
     * @param gvUserCd     수행자 사용자 코드 (JWT) — assertSiteAccess 판정용
     * @param gvSiteCd     수행자 토큰 사업장 코드 (JWT) — assertSiteAccess 판정용
     * @param siteCd       사업장 필터 (NULL/빈값=전체)
     * @param nodeCd       소속부서 필터 (NULL/빈값=전체)
     * @param incSubNodeYn 하위부서 포함 여부 Y/N
     * @param userNm       사용자명 검색어
     * @param baseYmd      조회 기준일 (YYYYMMDD, 필수 — 퇴사(예정)일 입력 시 퇴직정산 참고 조회)
     * @param page         페이지(1-based)
     * @param size         페이지 크기
     */
    ShortfallListResultVO getShortfallList(String cmpnyCd, String authCd, String gvUserCd, String gvSiteCd,
                                           String siteCd, String nodeCd, String incSubNodeYn, String userNm,
                                           String baseYmd, int page, int size);

    /**
     * 입사일 기준 차액 보전(법정 수기부여, {@code _COVER}) 실행 (경력인정 이원화 Phase 2 §2-3).
     * {@code @Transactional}, 권한 AUTH_MASTER OR AUTH_HR_MANAGER(기존 수동 부여 관례).
     *
     * <p>{@code GRANT_TYPE='STATUTORY_ANNUAL'}, {@code GRANT_BY_TYPE='02'}, 멱등키는 수동 부여 키 체계
     * ({@code PAYLOAD_HASH8+WINDOW}) + 전용 접미사 {@code _COVER}(★R-6 — 엔진 표준키 형식 절대 금지).
     * 상한(요청량 ≤ 서버 재계산 남은 부족분)·소정-05(법정 자동부여 OFF) 게이트를 서버가 강제한다.
     *
     * @param cmpnyCd        회사 코드 (JWT)
     * @param command        부여 입력 (userCd/grantDays/reason/baseYmd)
     * @param authCd         수행자 권한 코드 (JWT)
     * @param operatorUserCd 수행자 사용자 코드 (INSERT_NO 기록용)
     */
    CoverGrantResultVO coverGrant(String cmpnyCd, CoverGrantCommand command, String authCd, String operatorUserCd);
}
