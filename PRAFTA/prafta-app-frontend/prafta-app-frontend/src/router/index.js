// src/router/index.js (APP)
import { createRouter, createWebHashHistory } from 'vue-router'
import { buildDynamicRoutes } from './dynamicRoutes'
import api from '@/api/axios'

const routes = [
  { path: '/', name: 'Login', component: () => import('@/views/login/LoginView.vue') },
  { path: '/MainView', name: 'MainView', component: () => import('@/views/main/MainView.vue') },

  // prafta-app-002: 내 근태 조회 (오늘/이번주/이번달)
  {
    path: '/MyAttendance',
    name: 'MyAttendance',
    component: () => import('@/views/attd/MyAttendanceView.vue'),
  },

  // prafta-app-004-C4: TBM 입실/종료 (보호 — beforeEach 토큰 게이트). 진입: /TbmEntry?sessionCd=...
  {
    path: '/TbmEntry',
    name: 'TbmEntry',
    component: () => import('@/views/tbm/TbmEntryView.vue'),
  },

  // PRAFTA-TBM-HUB: 사용자 TBM 허브 트리 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: MainView › TbmAttendCard › `>` → /TbmHub (3탭: 참석가능/교육중/교육완료)
  {
    path: '/TbmHub',
    name: 'TbmHub',
    component: () => import('@/views/tbm/TbmHubView.vue'),
  },
  // 입실(enter) 성공 후 진입: /TbmBeforeStart?sessionCd=...
  {
    path: '/TbmBeforeStart',
    name: 'TbmBeforeStart',
    component: () => import('@/views/tbm/TbmBeforeStartView.vue'),
  },
  // 관리자 시작(IN_PROGRESS) 확인 또는 교육중 탭 재참여 후 진입: /TbmInProgress?sessionCd=...
  {
    path: '/TbmInProgress',
    name: 'TbmInProgress',
    component: () => import('@/views/tbm/TbmInProgressView.vue'),
  },
  // 교육완료 탭 카드 선택 후 진입: /TbmCompletedDetail?sessionCd=...
  {
    path: '/TbmCompletedDetail',
    name: 'TbmCompletedDetail',
    component: () => import('@/views/tbm/TbmCompletedDetailView.vue'),
  },

  // prafta-app-025 J1-3: 안전 허브 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: MainView › SafetyActivityCard › 헤더 ">"(onSafetyDetail), 바텀 탭바 '안전' 탭.
  //   안전 활동 3종(안전점검/위험성 발굴/아차사고 보고) 진입 허브. 근무중 게이트는 화면 자체 산출.
  {
    path: '/SafetyHub',
    name: 'SafetyHub',
    component: () => import('@/views/safety/SafetyHubView.vue'),
  },

  // prafta-app-025 J1-10 B-6: 내 안전활동 이력 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: MainView › SafetyActivityCard › 헤더 ">"(onSafetyDetail) → /MySafetyHistory.
  //   본인이 처리한 순회점검 + 본인이 등록한 위험성평가 이력(시간순 합본). 본인 필터는 서버가 JWT 로 강제.
  //   하단 "안전" 탭(/SafetyHub)은 허브로 유지 — 역할 분리(허브 vs 본인 이력 조회).
  {
    path: '/MySafetyHistory',
    name: 'MySafetyHistory',
    component: () => import('@/views/safety/MySafetyHistoryView.vue'),
  },

  // prafta-app-005: 연차 현황 (본인 잔여연차 상세)
  {
    path: '/MyLeaveSummaryView',
    name: 'MyLeaveSummaryView',
    component: () => import('@/views/leave/MyLeaveSummaryView.vue'),
  },

  // prafta-app-018-C: 연차 신청 폼 (보호 — beforeEach 토큰 게이트, publicPaths 미포함)
  //   진입: /LeaveApply (연차현황) 또는 /LeaveApply?workYmd=YYYYMMDD&nodeCd=N001 (내 근태 액션시트)
  {
    path: '/LeaveApply',
    name: 'LeaveApply',
    component: () => import('@/views/leave/LeaveApplyView.vue'),
  },

  // PRAFTA-COM-008-C: 근로자 발의 연차 이동 요청 (취소 불가, 관리자 승인 대상)
  {
    path: '/LeaveMoveRequest',
    name: 'LeaveMoveRequest',
    component: () => import('@/views/leave/LeaveMoveRequestView.vue'),
  },

  // PRAFTA-COM-008-A-7: 연차 사용촉진 1차 계획서 화면 (보호 — beforeEach 토큰 게이트, publicPaths 미포함)
  //   진입: 메인 로그인 안내 팝업 "계획 등록" → /LeavePromotionPlan.
  {
    path: '/LeavePromotionPlan',
    name: 'LeavePromotionPlan',
    component: () => import('@/views/leave/LeavePromotionPlanView.vue'),
  },

  // PRAFTA-APP-006: 내 승인 요청 목록 화면
  {
    path: '/MyRequests',
    name: 'MyRequests',
    component: () => import('@/views/req/MyRequestsView.vue'),
  },

  // PRAFTA-APP-007: 근태 요청 폼 (스케줄 수정 / 근태 보정 / 초과근무)
  {
    path: '/AttdRequest',
    name: 'AttdRequest',
    component: () => import('@/views/req/AttdRequestView.vue'),
  },

  // 001-Phase1: 관리자 모드 런처 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: 마이페이지 '관리자 모드' row(canEnterAdmin 시) → /AdminHome.
  //   서버 access-context 가 진입 최종 판정(클라 가드는 UX 보조).
  {
    path: '/AdminHome',
    name: 'AdminHome',
    component: () => import('@/views/admin/AdminLauncherView.vue'),
  },
  // 001-Phase1: 관리자 모듈 빈 골격(Phase 2~8 실화면 교체 전). query.module 로 모듈 키 전달.
  {
    path: '/ComingSoon',
    name: 'ComingSoon',
    component: () => import('@/views/_common/ComingSoon.vue'),
  },

  // 001-Phase5: 관리자 모드 TBM 관리 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: AdminLauncherView/AdminTabBar 의 TBM → /AdminTbm. 서버 access-context 가 진입 최종 판정.
  {
    path: '/AdminTbm',
    name: 'AdminTbm',
    component: () => import('@/views/admin/tbm/AdminTbmView.vue'),
  },
  // 세션 상세(교육관리 카드/개설 성공 후 진입): /AdminTbmSessionDetail?sessionCd=...
  {
    path: '/AdminTbmSessionDetail',
    name: 'AdminTbmSessionDetail',
    component: () => import('@/views/admin/tbm/AdminTbmSessionDetailView.vue'),
  },
  // 세션 수정(교육내용 위주): 세션 상세 "수정" → /AdminTbmSessionEdit?sessionCd=...
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). DRAFT/OPENED 만 서버가 수정 허용.
  {
    path: '/AdminTbmSessionEdit',
    name: 'AdminTbmSessionEdit',
    component: () => import('@/views/admin/tbm/AdminTbmSessionEditView.vue'),
  },
  // prafta-051 R-A: TBM 교육준비 화면(OPENED) — 세션 상세 "교육준비 시작"(/prepare 성공)/"교육준비 화면으로" 진입.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 진입: /AdminTbmPrep?sessionCd=...
  {
    path: '/AdminTbmPrep',
    name: 'AdminTbmPrep',
    component: () => import('@/views/admin/tbm/AdminTbmPrepView.vue'),
  },
  // 001-Phase5 R3: TBM 진행 화면(IN_PROGRESS) — 세션 상세 "교육 시작"/"진행 화면으로" 진입.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 진입: /AdminTbmLive?sessionCd=...
  {
    path: '/AdminTbmLive',
    name: 'AdminTbmLive',
    component: () => import('@/views/admin/tbm/AdminTbmLiveView.vue'),
  },
  // 001-Phase5 R3: TBM 종료 화면(COMPLETED) — 진행화면 종료 후/세션 상세 "종료 화면으로" 진입.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 진입: /AdminTbmCompleted?sessionCd=...
  {
    path: '/AdminTbmCompleted',
    name: 'AdminTbmCompleted',
    component: () => import('@/views/admin/tbm/AdminTbmCompletedView.vue'),
  },
  // 001-Phase5 R5: 교육자료 상세/미리보기 — 자료 탭 카드 선택 진입.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 진입: /AdminTbmMaterialDetail?mtrlCd=...
  {
    path: '/AdminTbmMaterialDetail',
    name: 'AdminTbmMaterialDetail',
    component: () => import('@/views/admin/tbm/AdminTbmMaterialDetailView.vue'),
  },
  // 001-Phase5 R5: 교육자료 등록/수정 — "자료 등록"(신규) 또는 상세 "수정"(mtrlCd 있음) 진입.
  //   보호 라우트. 진입: /AdminTbmMaterialForm 또는 /AdminTbmMaterialForm?mtrlCd=...
  {
    path: '/AdminTbmMaterialForm',
    name: 'AdminTbmMaterialForm',
    component: () => import('@/views/admin/tbm/AdminTbmMaterialFormView.vue'),
  },
  // 001-Phase5 R6: TBM 이력 상세(출결 명단, 조회 전용) — 이력 탭 카드 선택 진입.
  //   보호 라우트. 진입: /AdminTbmHistoryDetail?sessionCd=...
  {
    path: '/AdminTbmHistoryDetail',
    name: 'AdminTbmHistoryDetail',
    component: () => import('@/views/admin/tbm/AdminTbmHistoryDetailView.vue'),
  },

  // 001-Phase2: 관리자 모드 승인 관리 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: AdminLauncherView/AdminTabBar 의 APPROVAL → /AdminApproval. 서버 access-context 가 진입 최종 판정.
  {
    path: '/AdminApproval',
    name: 'AdminApproval',
    component: () => import('@/views/admin/approval/AdminApprovalView.vue'),
  },
  // 승인 상세(대기/이력 카드 선택 후 진입): /AdminApprovalDetail?reqId=...&group=...
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트).
  {
    path: '/AdminApprovalDetail',
    name: 'AdminApprovalDetail',
    component: () => import('@/views/admin/approval/AdminApprovalDetailView.vue'),
  },

  // 일용직 계약서+승인제 T4: 관리자 모드 일용직 입장 승인 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: AdminLauncherView 본문 ENTRY 섹션(moduleActiveMap.ENTRY===true) → /AdminEntryApproval.
  //   진입 게이팅은 서버 access-context(ENTRY=master∥hr), 처리 인가는 서버 EP(entryadmin01)가 최종 판정.
  {
    path: '/AdminEntryApproval',
    name: 'AdminEntryApproval',
    component: () => import('@/views/admin/entry/AdminEntryApprovalView.vue'),
  },

  // 관리자 연차 변경/삭제 최종 확인 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: AdminLauncherView 상단 "연차 변경 확인 대기 N건" 배너 → /AdminLeaveChangeConfirm.
  //   스코프/권한은 서버(공유 Attd13Service)가 단일 출처로 재강제(비관리자 fail-closed).
  {
    path: '/AdminLeaveChangeConfirm',
    name: 'AdminLeaveChangeConfirm',
    component: () => import('@/views/admin/leavechange/AdminLeaveChangeConfirmView.vue'),
  },

  // prafta-app-025 J1-5: 관리자 모드 근태 상세 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: AdminLauncherView/AdminTabBar 의 ATTD_DETAIL → /AdminAttdDetail. 서버 access-context 가 진입 최종 판정.
  //   화면 진입 후 조회 EP(/appApi/admin/attd-detail/*)가 노드 스코프를 서버에서 재강제(C1).
  {
    path: '/AdminAttdDetail',
    name: 'AdminAttdDetail',
    component: () => import('@/views/admin/attd/AdminAttdDetailView.vue'),
  },

  // prafta-app-025 J1-7: 관리자 모드 현장 처리(일용직 QR 출퇴근 등록) (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: AdminLauncherView 본문 SITE_OPS 섹션(moduleActiveMap.SITE_OPS===true) → /AdminSiteOps.
  //   진입 게이트/사업장 스코프/대상 유효성/멱등은 서버 EP(/appApi/admin/site-ops/*)가 최종 판정(C1).
  {
    path: '/AdminSiteOps',
    name: 'AdminSiteOps',
    component: () => import('@/views/admin/siteops/AdminSiteOpsView.vue'),
  },

  // prafta-app-025 J1-6: 관리자 모드 안전 관리 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: AdminLauncherView 본문 SAFETY 섹션/하단 탭바 '안전' 탭(moduleActiveMap.SAFETY===true) → /AdminSafety.
  //   안전 허브(순회점검 결과 / 위험성평가 / 아차사고). 사업장 스코프/상태 전이는 서버 EP(/appApi/admin/safety/*)가 최종 판정(C1).
  {
    path: '/AdminSafety',
    name: 'AdminSafety',
    component: () => import('@/views/admin/safety/AdminSafetyView.vue'),
  },
  // 순회점검 결과(조회 전용): 월 선택 → 포인트 리스트 → 상세 시트(일자별 답변 + 불량 사진/비고).
  {
    path: '/AdminSafetyInspection',
    name: 'AdminSafetyInspection',
    component: () => import('@/views/admin/safety/AdminSafetyInspectionView.vue'),
  },
  // 위험성평가(조회 + 상태전환): 상태 필터 → 목록 → 상세/상태전환 시트.
  {
    path: '/AdminSafetyRisk',
    name: 'AdminSafetyRisk',
    component: () => import('@/views/admin/safety/AdminSafetyRiskView.vue'),
  },

  // prafta-app-025 J1-8: 관리자 모드 게시판 = 안전자료실(Archive, notice02) — 조회 + 등록(작성+첨부).
  //   진입: AdminLauncherView 본문 BOARD 섹션(moduleActiveMap.BOARD===true) → /AdminBoard.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 등록 권한(master/hr/safe)은 서버 EP 가 최종 강제(C1).
  {
    path: '/AdminBoard',
    name: 'AdminBoard',
    component: () => import('@/views/admin/board/AdminBoardView.vue'),
  },
  // 자료 상세(목록 행 선택 후 진입, 읽기 전용 + 첨부 다운로드): /AdminBoardDetail?noticeId=...
  {
    path: '/AdminBoardDetail',
    name: 'AdminBoardDetail',
    component: () => import('@/views/admin/board/AdminBoardDetailView.vue'),
  },
  // 자료 등록 폼(헤더 "등록" 진입): /AdminBoardForm. 수정/삭제는 웹 위임(앱 미신설).
  {
    path: '/AdminBoardForm',
    name: 'AdminBoardForm',
    component: () => import('@/views/admin/board/AdminBoardFormView.vue'),
  },

  // PRAFTA-APP-010: 마이페이지 (인증 필수 — publicPaths 미포함)
  {
    path: '/MyPage',
    name: 'MyPage',
    component: () => import('@/views/mypage/MyPageView.vue'),
  },

  // PRAFTA-APP-021: 푸시 알림 설정 (인증 필수 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: MainView 우상단 아바타(onAvatarClick → /PushSetting). 본인(USER_CD=JWT) 설정만 조회/저장.
  {
    path: '/PushSetting',
    name: 'PushSetting',
    component: () => import('@/views/mypage/PushSettingView.vue'),
  },
  {
    path: '/ProfileEdit',
    name: 'ProfileEdit',
    component: () => import('@/views/mypage/ProfileEditView.vue'),
  },
  {
    path: '/PasswordChange',
    name: 'PasswordChange',
    component: () => import('@/views/mypage/PasswordChangeView.vue'),
  },
  {
    path: '/ApprovalPresetList',
    name: 'ApprovalPresetList',
    component: () => import('@/views/mypage/ApprovalPresetListView.vue'),
  },
  {
    path: '/ApprovalPresetEdit',
    name: 'ApprovalPresetEdit',
    component: () => import('@/views/mypage/ApprovalPresetEditView.vue'),
  },

  // 일용직 계약서+승인제 T4: 내 서명 근로계약서 열람 (보호 — publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: MyPageView "내 근로계약서"(일용직 전용 노출) → /MyContract. 본인 스코프는 서버가 JWT 로 강제.
  {
    path: '/MyContract',
    name: 'MyContract',
    component: () => import('@/views/mypage/MyContractView.vue'),
  },

  // 사용자연차결재-02: 연차 결재 관리(결재 대기/처리 내역 2탭) — 보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트).
  //   진입: 마이페이지 결재 그룹 "연차 결재 관리" → /LeaveApproval.
  {
    path: '/LeaveApproval',
    name: 'LeaveApproval',
    component: () => import('@/views/mypage/LeaveApprovalView.vue'),
  },
  // 사용자연차결재-03: 연차 결재 상세 + 결정(승인/반려) — 보호 라우트.
  //   진입: 결재 카드 선택 → /LeaveApprovalDetail?reqId=...&approvalStep=...
  {
    path: '/LeaveApprovalDetail',
    name: 'LeaveApprovalDetail',
    component: () => import('@/views/mypage/LeaveApprovalDetailView.vue'),
  },

  // 퍼블릭
  {
    path: '/ActInfoSrch',
    name: 'ActInfoSrch',
    component: () => import('@/views/login/ActInfoSrch.vue'),
  },
  { path: '/TermsInfo', name: 'TermsInfo', component: () => import('@/views/login/TermsInfo.vue') },
  {
    path: '/TermsDetail',
    name: 'TermsDetail',
    component: () => import('@/views/login/TermsDetail.vue'),
  },
  {
    path: '/QrScanner',
    name: 'QrScanner',
    component: () => import('@/views/_common/QrScanner.vue'),
  },
  { path: '/ChkLst', name: 'ChkLst', component: () => import('@/views/chkLst/ChkLst.vue') },
  // prafta-app-011 화면 C: 안전점검 저장 완료 (요약 query 전달)
  {
    path: '/SafetyInspectSaved',
    name: 'SafetyInspectSaved',
    component: () => import('@/views/chkLst/SafetyInspectSavedView.vue'),
  },
  { path: '/Risk_01', name: 'Risk_01', component: () => import('@/views/risk/Risk_01.vue') },

  // prafta-app-012: 아차사고/사건 보고 (인증 필수 — publicPaths 미포함, beforeEach 토큰 게이트)
  //   근로자 보고 / 관리자(안전직군) 목록·상세. 서버가 사업장 권한(assertSiteAccess) 최종 판정.
  {
    path: '/NearMissReport',
    name: 'NearMissReport',
    component: () => import('@/views/nearmiss/NearMissReport.vue'),
  },
  {
    path: '/NearMissManageList',
    name: 'NearMissManageList',
    component: () => import('@/views/nearmiss/NearMissManageList.vue'),
  },
  {
    path: '/NearMissManageDetail',
    name: 'NearMissManageDetail',
    component: () => import('@/views/nearmiss/NearMissManageDetail.vue'),
  },

  // prafta-app-023: 공지 표시 (인증 필수 — publicPaths 미포함, beforeEach 토큰 게이트)
  //   진입: 메인 홈 공지카드 행/전체보기, 로그인 팝업 → 상세/전체목록.
  //   대상 노출 판정은 서버(countNoticeVisibleToUser)가 최종(403 NOTICE_403_003).
  {
    path: '/NoticeDetail',
    name: 'NoticeDetail',
    component: () => import('@/views/notice/NoticeDetailView.vue'),
  },
  {
    path: '/NoticeList',
    name: 'NoticeList',
    component: () => import('@/views/notice/NoticeListView.vue'),
  },

  { path: '/JoinUser', name: 'JoinUser', component: () => import('@/views/login/JoinUser.vue') },

  // PRAFTA-037-F3: 인증대기(SYS013='04') 계정의 휴대폰 본인인증 화면.
  // 임시 scope=PHONE_AUTH 토큰만으로 SMS 발송/검증 호출. 정식 토큰 부재 → public.
  {
    path: '/PhoneAuth',
    name: 'PhoneAuth',
    component: () => import('@/views/login/PhoneAuthView.vue'),
  },

  // PRAFTA-COM-008-E-8c: 기본 근무타입 미설정(교대 비소속) 로그인 게이트.
  // 임시 scope=DEFAULT_SCH 토큰만으로 옵션조회/저장 호출. 정식 토큰 부재 → public.
  {
    path: '/DefaultSchGate',
    name: 'DefaultSchGate',
    component: () => import('@/views/login/DefaultSchGateView.vue'),
  },

  // 필수약관 미동의 게이트 — 로그인 직후(정식 토큰 보유) 미동의 필수약관이 있으면 진입.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 정식 토큰으로 동의 EP 호출.
  //   동의 거부/뒤로가기 → 로그아웃 후 로그인 복귀(화면 자체 가드).
  {
    path: '/TermsAgree',
    name: 'TermsAgree',
    component: () => import('@/views/login/TermsAgreeView.vue'),
  },

  // 일용직 계약서+승인제 T4: 근로계약서 서명 게이트 — termsGate 체인 ①-b(일용직 + signRequiredYn='Y') 진입.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 정식 토큰으로 서명 EP 호출.
  //   서명 거부/뒤로가기 → 로그아웃 후 로그인 복귀(화면 자체 가드 — TermsAgree 미러).
  {
    path: '/DailyContractSign',
    name: 'DailyContractSign',
    component: () => import('@/views/login/DailyContractSignView.vue'),
  },
  // 일용직 계약서+승인제 T4: 입장 승인 대기/거부 안내 — 로그인 실패(DAILYLOGIN_400_006/007) 후 진입.
  //   비보호 라우트(publicPaths 포함) — 로그인 전 안내 전용(API 호출 없음, R4).
  {
    path: '/DailyEntryPending',
    name: 'DailyEntryPending',
    component: () => import('@/views/login/DailyEntryPendingView.vue'),
  },
  // 소정-12: 셀프가입 승인 대기/거부 안내 — 로그인 시 ACCOUNT_STATUS '06 가입승인대기'/'07 가입거부' 분기 진입.
  //   비보호 라우트(publicPaths 포함) — 로그인 전 안내 전용(토큰 미발급, API 호출 없음).
  {
    path: '/JoinApprovalPending',
    name: 'JoinApprovalPending',
    component: () => import('@/views/login/JoinApprovalPendingView.vue'),
  },

  // PRAFTA-SUBCON-T4: 연동 회사 제3자 제공 동의 게이트 — 활성 연동 사업장 소속 + 006 미응답 시 진입.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 정식 토큰으로 동의 EP 호출.
  //   ★ 필수약관 게이트와 달리 강제가 아니다: 동의/미동의 모두 정상 통과(로그아웃·이탈가드 없음).
  {
    path: '/ThirdPartyConsent',
    name: 'ThirdPartyConsent',
    component: () => import('@/views/login/ThirdPartyConsentView.vue'),
  },

  // prafta-app-033: 강제 비밀번호 변경 게이트 — 로그인 응답 nextStep='PASSWORD_CHANGE'(정식 토큰 보유) 시 진입.
  //   보호 라우트(publicPaths 미포함, beforeEach 토큰 게이트). 정식 토큰으로 비번변경 EP(/appApi/mypage/password) 호출.
  //   변경 거부/뒤로가기 → 로그아웃 후 로그인 복귀(화면 자체 가드). 성공 → routeAfterLogin(약관 게이트 → 메인).
  {
    path: '/ForcedPasswordChange',
    name: 'ForcedPasswordChange',
    component: () => import('@/views/login/ForcedPasswordChangeView.vue'),
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/_common/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

let dynamicInjected = false

/** 퍼블릭 허용 경로 목록 */
const publicPaths = [
  '/',
  // '/MainView', // 너 정책상 public로 열어둔 상태 유지 (원하면 제거 가능)
  '/ActInfoSrch',
  '/TermsInfo',
  '/TermsDetail',
  '/QrScanner',
  '/JoinUser',
  '/PhoneAuth', // PRAFTA-037-F3: 인증대기 단계는 정식 토큰 미발급 → public 라우트로 취급
  '/DefaultSchGate', // PRAFTA-COM-008-E-8c: 기본 근무타입 게이트 — 임시 토큰만, 정식 토큰 미발급 → public
  '/DailyEntryPending', // 일용직 계약서+승인제 T4: 승인 대기/거부 안내 — 로그인 실패 후 진입(토큰 미발급) → public
  '/JoinApprovalPending', // 소정-12: 셀프가입 승인 대기/거부 안내 — 로그인 분기 진입(토큰 미발급) → public
]

// ✅ refresh 동시 호출 방지
let bootstrapping = null

async function ensureAccessToken() {
  const token = sessionStorage.getItem('token')
  if (token) return token

  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  if (!bootstrapping) {
    bootstrapping = (async () => {
      try {
        const res = await api.post('/comApi/auth/refresh', { refreshToken })
        const newToken = res.data?.token
        if (newToken) {
          sessionStorage.setItem('token', newToken)
          api.defaults.headers.common.Authorization = `Bearer ${newToken}`
        }
        return newToken || null
      } catch (e) {
        sessionStorage.clear()
        localStorage.removeItem('refreshToken')
        return null
      } finally {
        bootstrapping = null
      }
    })()
  }

  return bootstrapping
}

/** DB에서 메뉴 조회 */
async function fetchAppRoutes() {
  try {
    // 백엔드 kebab 리팩터 정렬: GET /comApi/baseinfo/app-menu-lists (cmpnyCd/userCd 는 JWT 클레임에서 도출 → 파라미터 불필요)
    // 응답은 { appMenuResultList: [...] } 래핑(빈 목록이면 본문 null)
    const response = await api.get('/comApi/baseinfo/app-menu-lists')
    if (response.status === 200) return response.data?.appMenuResultList || []
  } catch (err) {
    console.error('메뉴 조회 실패:', err)
  }
  return []
}

/** 동적 라우트 주입 */
async function injectDynamicRoutes() {
  if (dynamicInjected) return

  const dbRoutes = await fetchAppRoutes()
  if (!dbRoutes || dbRoutes.length === 0) {
    dynamicInjected = true
    return
  }

  const dynamicRoutes = buildDynamicRoutes(dbRoutes)

  dynamicRoutes.forEach((r) => {
    if (!r.meta) r.meta = {}
    if (typeof r.meta.requiresAuth === 'undefined') r.meta.requiresAuth = true

    if (!router.hasRoute(r.name)) {
      router.addRoute(r)
      console.log('동적라우트 추가:', r.path, r.name, 'requiresAuth:', r.meta.requiresAuth)
    }
  })

  dynamicInjected = true
}

/** beforeEach */
router.beforeEach(async (to, from, next) => {
  // ✅ 이미 로그인 상태인데 로그인 화면(/)으로 가려 하면 메인으로 보내기 (replace)
  if (to.path === '/') {
    const ensured = await ensureAccessToken() // 세션 토큰 없으면 refresh로 확보 시도
    if (ensured) {
      // redirect 쿼리가 있으면 그쪽으로, 없으면 메인
      const target = to.query?.redirect || '/MainView'
      return next({ path: target, replace: true })
    }
    // 토큰 없으면 그냥 로그인 화면 허용
    return next()
  }

  // 1) 퍼블릭 경로인지 체크
  const isPublic = publicPaths.some((p) => {
    if (p === to.path) return true
    if (to.path.startsWith(p + '/')) return true
    return false
  })

  if (isPublic) {
    // 퍼블릭 접근 허용
    // 동적 라우트 주입은 토큰이 있을 때만 — 비로그인 상태에서 /app-menu-lists(@NoAuth 미적용)를
    // 호출하면 AuthAspect 가 토큰 부재로 500 을 던진다(가입하기·약관·인증대기 등 public 경로 진입 시 발생).
    if (!dynamicInjected && sessionStorage.getItem('token')) {
      await injectDynamicRoutes()
    }
    return next()
  }

  // 2) 퍼블릭이 아니면 토큰 확보 시도(새로고침/웹뷰 재로딩 대응)
  const ensured = await ensureAccessToken()
  if (!ensured) {
    return next({ path: '/', query: { redirect: to.fullPath } })
  }

  // 3) 토큰 확보 후 동적 라우트 주입
  if (!dynamicInjected) {
    await injectDynamicRoutes()
  }

  // 4) meta.requiresAuth 세밀 분기(필요시)
  if (to.meta?.requiresAuth === false) return next()

  return next()
})

export default router
