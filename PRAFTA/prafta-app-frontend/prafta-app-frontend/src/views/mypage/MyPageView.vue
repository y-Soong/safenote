<!--
  MyPageView.vue — 마이페이지 메인 (모바일 앱)
  - 작업 ID: PRAFTA-APP-010-10 (분해: .claude/requests/app_requests/prafta-app-010-plan.md)
  - UI 명세: UI-A010 (.claude/requests/app_requests/prafta-app-010-ui-spec.md)
  - 진입: MainView 우측 상단 아바타(onAvatarClick → router.push('/MyPage'))
  - planner 라운드 스코프: 프로필카드+메뉴2그룹+로그아웃버튼+탈퇴링크+버전 (template/style)
  - developer 라운드 스코프(아래 TODO): 프로필 조회(010-01, 마스킹), 로그아웃(010-06=기존 /comApi/login/logout 재사용 D3), 탈퇴(010-07), 라우팅
  - 디자인 토큰: MainView(.home-view)와 동일 세트를 .my-page-view 루트에 1회 선언. 하드코딩/Tailwind 금지.

  ── prafta-app-028 (planner 라운드) ──────────────────────────────────────────
  - UI 명세: UI-A028 (마이페이지 연차 요약 섹션 + 당겨서 새로고침)
  - 추가 1) 프로필 카드 아래 "연차 요약" 섹션(3 KPI: 남은/사용예정/사용, 일 단위).
           일용직(DAILY)은 미노출(MainView 잔여연차 카드와 동일 게이트). 섹션 탭 시 연차 현황(/MyLeaveSummaryView) 진입.
  - 추가 2) .mp-body 에 당겨서 새로고침(pull-to-refresh) — MainView .main 패턴 그대로 이식.
  - 데이터 매핑(단일 출처 = prafta-app-028 사용자 확정 사항):
      GET /appApi/leave01/my-leave-summary 응답 groups.TOTAL 기준
        · 남은 연차     = remaining (부여 − 총사용)
        · 사용예정 연차 = planned   (미래 확정 연차)
        · 사용 연차     = used      (총사용 − planned, 실제 소진분)
      → 신규 엔드포인트 없음. 기존 leave01/my-leave-summary 재사용. 그룹은 TOTAL 만(분리 토글 스코프 밖).
  - planner 라운드 스코프: 연차 요약 섹션 마크업 + scoped 스타일 + pull-to-refresh 인디케이터/핸들러 바인딩(template/style).
    script 의 API 호출/리팩터(조회 함수 묶기·pull 핸들러 본문·연차요약 매핑)는 아래 TODO(developer)로 표시만.

  ── PRAFTA-003(결재자선택UI 추가, 2026-08-27) ──────────────────────────────
  - "근무 정보" 바텀시트(DefaultSchEditSheet)가 결재선 선택 UI를 얹음에 따라, 시트 오픈 시점에
    프리셋(GET /appApi/mypage/approval-presets) + 결재선 분기 컨텍스트(GET /appApi/req09/approval-context)
    를 1회 로드해 props 로 전달한다(AttdRequestView.vue 의 loadPresets/loadApprovalContext 로드 패턴 미러).
-->
<template>
  <div class="my-page-view">
    <!-- 헤더 -->
    <header class="mp-hd">
      <button type="button" class="mp-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-mp-chev-left" />
        </svg>
      </button>
      <h1 class="mp-hd__title">마이페이지</h1>
      <!-- Q12 확정: 알림 아이콘 미노출(공지/알림 도메인 미구축). 도메인 구축 후 일괄 도입. -->
      <span class="mp-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤 영역) — prafta-app-028: 당겨서 새로고침 제스처 바인딩(공통 컴포저블 usePullToRefresh) -->
    <main
      class="mp-body"
      ref="mpBodyEl"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- prafta-app-028: 당겨서 새로고침 인디케이터 — 스크롤 최상단(프로필 카드 위)에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 로딩 -->
      <div v-if="isLoading" class="mp-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 프로필 카드 (탭 불가, 정보 확인용) -->
        <section class="mp-profile">
          <div class="mp-profile__avatar" aria-hidden="true">{{ avatarInitial }}</div>
          <div class="mp-profile__info">
            <p class="mp-profile__name">{{ userNm }}</p>
            <p class="mp-profile__meta">{{ siteNm }} · {{ nodeNm }}</p>
          </div>
        </section>

        <!-- prafta-app-028: 연차 요약 섹션 (3 KPI: 남은/사용예정/사용, 일 단위) -->
        <!--   일용직(DAILY)은 연차 해당없음 → 미노출(MainView 잔여연차 카드와 동일 게이트). -->
        <!--   연차요약 로드 실패는 비치명적: leaveSummaryFailed 면 섹션 자체 미노출(전체 화면 에러로 키우지 않음). -->
        <!--   소정-12(UI-E): 연차 기능 미노출 회사(자동부여 off + 부여이력 0)는 섹션 자체를 숨긴다. -->
        <section
          v-if="!isDailyWorker && leaveFeatureVisible && !leaveSummaryFailed"
          class="mp-leave"
          role="button"
          tabindex="0"
          aria-label="연차 현황 보기"
          @click="onLeaveSummaryClick"
          @keydown.enter="onLeaveSummaryClick"
          @keydown.space.prevent="onLeaveSummaryClick"
        >
          <div class="mp-leave__hd">
            <span class="mp-leave__title">연차 요약</span>
            <svg class="icon mp-leave__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </div>
          <!-- LC-11: 소수점 노출 금지 — 일은 큰 숫자, 시간·분은 보조 텍스트("N일 H시간 M분"). -->
          <div class="mp-leave__kpis">
            <div class="mp-leave__cell">
              <span class="mp-leave__lbl">남은 연차</span>
              <span
                class="mp-leave__val mp-leave__val--accent"
                :class="{ 'mp-leave__val--muted': leaveRemaining === 0 }"
              >
                {{ leaveRemainingParts.dayText }}<span class="mp-leave__unit">일</span>
                <span v-if="leaveRemainingParts.subText" class="mp-leave__sub">{{
                  leaveRemainingParts.subText
                }}</span>
              </span>
            </div>
            <div class="mp-leave__cell">
              <span class="mp-leave__lbl">사용예정</span>
              <span class="mp-leave__val" :class="{ 'mp-leave__val--muted': leavePlanned === 0 }">
                {{ leavePlannedParts.dayText }}<span class="mp-leave__unit">일</span>
                <span v-if="leavePlannedParts.subText" class="mp-leave__sub">{{
                  leavePlannedParts.subText
                }}</span>
              </span>
            </div>
            <div class="mp-leave__cell">
              <span class="mp-leave__lbl">사용</span>
              <span class="mp-leave__val" :class="{ 'mp-leave__val--muted': leaveUsed === 0 }">
                {{ leaveUsedParts.dayText }}<span class="mp-leave__unit">일</span>
                <span v-if="leaveUsedParts.subText" class="mp-leave__sub">{{
                  leaveUsedParts.subText
                }}</span>
              </span>
            </div>
          </div>
        </section>

        <!-- 관리자 모드 진입점 (001-Phase1-F4) — 서버 access-context.canEnterAdmin 시에만 노출. -->
        <template v-if="canEnterAdmin">
          <p class="mp-group-label">관리자</p>
          <nav class="mp-menu">
            <button type="button" class="mp-menu__row" @click="onAdminMode">
              <span class="mp-menu__text">관리자 모드</span>
              <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
                <use href="#i-mp-chev-right" />
              </svg>
            </button>
          </nav>
        </template>

        <!-- 계정 그룹 -->
        <p class="mp-group-label">계정</p>
        <nav class="mp-menu">
          <!-- 일용직(DAILY)은 개인정보 수정 미노출(필요 없는 기능). 비밀번호 변경은 유지. -->
          <button v-if="!isDailyWorker" type="button" class="mp-menu__row" @click="onProfileEdit">
            <span class="mp-menu__text">개인정보 수정</span>
            <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </button>
          <button type="button" class="mp-menu__row" @click="onPasswordChange">
            <span class="mp-menu__text">비밀번호 변경</span>
            <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </button>
          <!-- F-8-3: 근무 정보(기본 근무타입 자기변경) → PRAFTA-005(관리자 승인제): 대기중 요청 있으면
               현재값 대신 "승인 대기 중" 배지 노출(승인 전 미반영 — 현재값은 그대로 유효). -->
          <button type="button" class="mp-menu__row" @click="onDefaultSchClick">
            <span class="mp-menu__text">근무 정보</span>
            <span v-if="hasPendingDefaultSchReq" class="mp-menu__meta">승인 대기 중</span>
            <span v-else class="mp-menu__meta">{{ defaultSchLabel || '미설정' }}</span>
            <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </button>
          <!-- 일용직 계약서+승인제 T4: 내 서명 근로계약서 열람(교부 의무 §6-1) — 일용직(DAILY)에게만 노출.
               항상 노출(서명본 유무 무관) — 빈 상태는 MyContractView 가 자체 처리(UI-DC-04). -->
          <button v-if="isDailyWorker" type="button" class="mp-menu__row" @click="onMyContract">
            <span class="mp-menu__text">내 근로계약서</span>
            <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </button>
        </nav>

        <!-- BW-12(§7-1): 단시간(4시간·휴게 0) 근로자의 휴게 미이용 상시 요청(근기법 제54조① 단서).
             본인만 켜고 끈다(관리자 대리 불가). 노출 조건은 서버 판정값 eligibleYn 단독 —
             일용직 제외/정규직/기본 근무타입 240·0 판정을 화면이 다시 계산하지 않는다.
             조회 실패(loaded=false)면 행 자체를 노출하지 않는다(비치명적). -->
        <template
          v-if="!isDailyWorker && brkWaiveStanding.loaded && brkWaiveStanding.eligibleYn === 'Y'"
        >
          <p class="mp-group-label">근무 설정</p>
          <nav class="mp-menu">
            <div class="mp-terms-row mp-terms-row--stack">
              <div class="mp-terms-row__head">
                <button
                  type="button"
                  role="checkbox"
                  class="mp-terms-check"
                  :class="{ 'mp-terms-check--on': brkWaiveStanding.standingYn === 'Y' }"
                  :aria-checked="brkWaiveStanding.standingYn === 'Y' ? 'true' : 'false'"
                  aria-label="4시간 근무일 휴게 미이용 상시 요청"
                  :disabled="isBrkWaiveSaving"
                  @click="onToggleBrkWaiveStanding"
                >
                  <span class="mp-terms-check__box" aria-hidden="true">
                    <svg class="mp-terms-check__mark" viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <span class="mp-terms-check__label"
                    >4시간 근무 시 휴게 없이 바로 퇴근(휴게 미이용 상시 요청)</span
                  >
                </button>
                <span v-if="brkWaiveStanding.standingDtime" class="mp-menu__meta">{{
                  brkWaiveStanding.standingDtime
                }}</span>
              </div>
              <p class="mp-terms-row__desc">
                근로시간이 4시간인 날, 휴게 30분 없이 근무하고 바로 퇴근하기를 요청합니다. 켜고 끈
                시각이 기록돼요. (근로기준법 제54조① 단서)
              </p>
            </div>
          </nav>
        </template>

        <!-- 결재 그룹 — 일용직(DAILY)은 연차 결재선/결재 관리 모두 미노출(필요 없는 기능). -->
        <!--   소정-12(UI-E): 두 메뉴 모두 연차 전용이므로 연차 기능 미노출 회사에서도 함께 숨긴다. -->
        <template v-if="!isDailyWorker && leaveFeatureVisible">
          <p class="mp-group-label">결재</p>
          <nav class="mp-menu">
            <button type="button" class="mp-menu__row" @click="onPresetManage">
              <span class="mp-menu__text">연차 결재선 관리</span>
              <span class="mp-menu__meta">{{ presetCount }}개</span>
              <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
                <use href="#i-mp-chev-right" />
              </svg>
            </button>
            <!-- 사용자연차결재-04: 연차 결재 관리(내가 결재자인 연차 대기/처리 내역) -->
            <button type="button" class="mp-menu__row" @click="onLeaveApproval">
              <span class="mp-menu__text">연차 결재 관리</span>
              <span v-if="pendingApprovalCount > 0" class="mp-menu__meta"
                >{{ pendingApprovalCount }}건 대기</span
              >
              <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
                <use href="#i-mp-chev-right" />
              </svg>
            </button>
          </nav>
        </template>

        <!-- 약관 동의 설정 — 선택약관 + 위치정보 동의(005)를 같은 목록에 둔다.
             ★2026-09-02 UI 통일: 두 행 모두 [체크박스][(선택) 약관명][보기] 순서로 왼쪽 정렬한다.
               종전에는 006 만 우측 스위치, 005 는 좌측 들여쓰기 + 하단 버튼 2개라 같은 성격의
               동의인데 조작 형태가 서로 달랐다.
             ★위치정보의 '일시 중지 ↔ 재동의'는 되돌릴 수 있으므로 체크박스가 맡는다.
               반면 '동의 철회'는 좌표 파기를 동반해 되돌릴 수 없으므로 체크박스에 싣지 않고
               별도 버튼으로 남긴다(오조작 1회로 파기되면 복구 수단이 없다). -->
        <template v-if="optionalTerms.length > 0 || locationConsent.consentState">
          <p class="mp-group-label">약관 동의 설정</p>
          <nav class="mp-menu">
            <div v-for="terms in optionalTerms" :key="terms.termsId" class="mp-terms-row">
              <button
                type="button"
                role="checkbox"
                class="mp-terms-check"
                :class="{ 'mp-terms-check--on': terms.agrYn === 'Y' }"
                :aria-checked="terms.agrYn === 'Y' ? 'true' : 'false'"
                :aria-label="terms.termsNm + ' 동의'"
                :disabled="isTermsSaving"
                @click="onToggleOptionalTerms(terms)"
              >
                <span class="mp-terms-check__box" aria-hidden="true">
                  <svg class="mp-terms-check__mark" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M5 13l4 4L19 7" />
                  </svg>
                </span>
                <span class="mp-terms-check__label">{{ '(선택) ' + terms.termsNm }}</span>
              </button>
              <button type="button" class="mp-terms-row__view" @click="onViewTerms(terms)">
                보기
              </button>
            </div>

            <!-- 위치정보 동의(005) — 위 선택약관 행과 동일한 [체크박스][라벨][보기] 구조.
                 상태가 4가지(동의/일시중지/재동의필요/철회)라, 체크 해제 상태의 사유만
                 우측 배지로 덧붙인다.
                 ★해제(체크 해제)는 [일시 중지](되돌릴 수 있음)와 [동의 철회](좌표 파기, 복구
                   불가) 중 하나를 골라야 하므로, 행에 버튼을 늘어놓지 않고 해제 시트를 띄운다
                   (2026-09-02). 되돌리는 방향(재동의)만 행 버튼으로 남긴다. -->
            <div v-if="locationConsent.consentState" class="mp-terms-row mp-terms-row--stack">
              <div class="mp-terms-row__head">
                <button
                  type="button"
                  role="checkbox"
                  class="mp-terms-check"
                  :class="{ 'mp-terms-check--on': isLocationAgreed }"
                  :aria-checked="isLocationAgreed ? 'true' : 'false'"
                  :aria-label="(locationConsent.termsNm || '위치기반서비스 이용약관') + ' 동의'"
                  :disabled="isLocationSaving"
                  @click="onToggleLocationConsent"
                >
                  <span class="mp-terms-check__box" aria-hidden="true">
                    <svg class="mp-terms-check__mark" viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <span class="mp-terms-check__label">{{
                    '(선택) ' + (locationConsent.termsNm || '위치기반서비스 이용약관')
                  }}</span>
                </button>
                <button type="button" class="mp-terms-row__view" @click="onViewLocationTerms">
                  보기
                </button>
              </div>
              <!-- 상태 배지는 라벨 줄이 아니라 아랫줄에 둔다 — 같은 줄에 두면 배지 폭만큼
                   자리를 뺏겨 약관명이 두 줄로 접힌다(2026-09-02 실기기 확인). -->
              <div v-if="!isLocationAgreed" class="mp-loc__state">
                <span class="mp-loc-badge" :class="locationBadgeClass">
                  {{ locationStateLabel }}
                </span>
              </div>
            </div>
          </nav>
        </template>

        <!-- 로그아웃 (풀폭 secondary 버튼) -->
        <button type="button" class="mp-logout" @click="onLogoutClick">
          <svg class="icon" width="18" height="18" aria-hidden="true">
            <use href="#i-mp-logout" />
          </svg>
          로그아웃
        </button>

        <!-- 회원 탈퇴 (텍스트 링크, tertiary) -->
        <button type="button" class="mp-withdraw" @click="onWithdrawClick">회원 탈퇴</button>

        <!-- 앱 버전 + 로딩 소스(원격로딩 전환 §7 판정 수단: remote/bundle 표기) -->
        <p class="mp-version">{{ buildLabel }}</p>
      </template>
    </main>

    <!-- 하단 탭바 (마이 활성) — prafta-app-025 J1-2: 마이 탭의 목적지 화면이므로 공통 탭바 장착. -->
    <AppBottomTabBar :active-tab="'my'" />

    <!-- 로그아웃 확인 모달 -->
    <LogoutConfirmDialog v-model="logoutDialogOpen" @confirm="onLogoutConfirm" />

    <!-- 회원 탈퇴 확인 모달 -->
    <WithdrawalConfirmDialog
      v-model="withdrawDialogOpen"
      :user-nm="userNm"
      @confirm="onWithdrawConfirm"
    />

    <!-- F-8-3: 근무 정보(기본 근무타입 자기변경) 바텀시트 -->
    <!-- PRAFTA-003(결재자선택UI 추가): presets/approval-context 를 onDefaultSchClick 시점에 로드해 전달. -->
    <DefaultSchEditSheet
      v-model="defaultSchSheetOpen"
      :current-sch-cd="defaultSchCd"
      :current-label="defaultSchLabel"
      :presets="defaultSchPresets"
      :approval-context="defaultSchApprovalContext"
      @requested="onDefaultSchRequested"
    />

    <!-- 위치정보 동의 해제 방식 선택 시트 — 체크박스를 해제할 때 열린다(2026-09-02).
         닫으면 아무 전이도 일어나지 않으므로 체크는 그대로 유지된다. -->
    <LocationConsentOffSheet
      v-model="locationOffSheetOpen"
      :saving="isLocationSaving"
      @suspend="onSheetSuspendLocation"
      @withdraw="onSheetWithdrawLocation"
    />

    <!-- 인라인 SVG sprite (본 화면 전용) -->
    <svg width="0" height="0" class="mp-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-mp-chev-right"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol
          id="i-mp-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-mp-logout"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
          <polyline points="16 17 21 12 16 7" />
          <line x1="21" y1="12" x2="9" y2="12" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { useUserStore } from '@/stores/userStore'
import { forceLogout } from '@/composables/useAuth'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
// prafta-app-028: 일용직(DAILY) 게이트 — 연차 요약 섹션 노출 판정(MainView 잔여연차 카드와 동일 게이트).
import { isDailyWorker as isDailyWorkerFn } from '@/utils/employment'
// 소정-12(UI-E): 연차 기능 노출 판정(회사 단위) — 연차 요약 섹션·연차 결재 메뉴 노출 게이트.
import { leaveFeatureVisible, ensureLeaveFeatureVisibility } from '@/utils/leaveFeature'
// 연차 일수 표기 공용 유틸 — 2026-08-09 규약: 잔여는 일 단위 단독(splitLeaveDaysOnly).
// HB-13(F-3 §20-2): 사용/사용예정은 역환산 대신 반차 건수·시간차 실분 병기(splitLeaveDaysWithHourly).
import { splitLeaveDaysOnly, splitLeaveDaysWithHourly } from '@/utils/leaveFormat'
// PRAFTA-SUBCON-T4: 연동 회사 제3자 제공 동의(006) 식별 — 철회(Y→N) 확인 팝업 판별용.
import { THIRD_PARTY_CONSENT_TERMS_ID } from '@/utils/termsGate'
// ★상태 부연 설명(구 LOCATION_STATE_DESC)은 2026-09-02 UI 정리로 상시 노출을 폐지했다 —
//   중지/철회의 차이는 해제 시트(LocationConsentOffSheet)가 그 시점에 설명한다.
import {
  LOCATION_STATE_LABEL,
  LOCATION_WITHDRAW_CONFIRM,
  LOCATION_SUSPEND_CONFIRM,
} from '@/utils/locationConsent'
import { getShellInfo } from '@/utils/shellCapability'

import LogoutConfirmDialog from './components/LogoutConfirmDialog.vue'
import WithdrawalConfirmDialog from './components/WithdrawalConfirmDialog.vue'
import DefaultSchEditSheet from './components/DefaultSchEditSheet.vue'
import LocationConsentOffSheet from './components/LocationConsentOffSheet.vue'
import AppBottomTabBar from '@/components/common/AppBottomTabBar.vue'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (앱 전역 $alert 우선) — MainView/MyLeaveSummaryView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
// 공통: confirm 폴백 (TermsAgreeView 패턴 동일) — SUBCON-T4 철회 확인 팝업에서 사용.
const showConfirm = (message) => {
  if (proxy?.$confirm) return proxy.$confirm(message)
  return Promise.resolve(window.confirm(message))
}

// ───────────────────────────────────────────────────────────
// 상태 (developer: 초기값/응답 주입 보완 필요)
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)

// GET /appApi/mypage/profile (마스킹 응답 D1) 매핑 (userNm/siteNm/nodeNm/presetCount)
const userNm = ref('')
const siteNm = ref('')
const nodeNm = ref('')
const presetCount = ref(0)

// F-8-3: 근무 정보(기본 근무타입 자기변경). 현재값은 profile 보강 응답(defaultSchCd/No/StrTime/EndTime).
const defaultSchCd = ref('')
const defaultSchLabel = ref('')
const defaultSchSheetOpen = ref(false)
// PRAFTA-005(기본근무타입-승인제): 대기중 요청 존재 여부(메뉴 행 배지). 비치명적 → 실패는 false 폴백(미노출).
const hasPendingDefaultSchReq = ref(false)
// PRAFTA-003(결재자선택UI 추가): 결재선 프리셋/분기 컨텍스트 — 시트 오픈 시점에 로드해 props 로 전달.
const defaultSchPresets = ref([])
const defaultSchApprovalContext = ref(null)

// 사용자연차결재-04: "연차 결재 관리" 대기 건수 배지(경량 조회, 비치명적). 실패 시 0(미노출).
const pendingApprovalCount = ref(0)

// 001-Phase1-F4: 관리자 모드 진입점 노출 판정(서버 access-context.canEnterAdmin).
//   판정 출처를 별도 경량 엔드포인트로 분리하지 않고 진입판정 단일 출처(access-context)를 재사용한다
//   — 판정 일관성 유지 + 엔드포인트 중복 방지. 프로필 조회와 병렬 호출(아래 onMounted)로 추가 지연 최소화.
const canEnterAdmin = ref(false)

// ───────────────────────────────────────────────────────────
// prafta-app-028: 연차 요약(3 KPI) 상태
//   데이터 출처 = GET /appApi/leave01/my-leave-summary 응답 groups.TOTAL (단일 출처).
//     · 남은 연차     = remaining
//     · 사용예정 연차 = planned
//     · 사용 연차     = used
//   developer: loadLeaveSummary() 에서 groups.TOTAL 을 아래 ref 들에 주입한다(매핑 그대로).
//   leaveSummaryFailed: 연차요약 로드 실패 플래그(비치명적). true 면 섹션 자체 미노출(전체 화면 에러 금지).
// ───────────────────────────────────────────────────────────
const leaveRemaining = ref(0)
const leavePlanned = ref(0)
const leaveUsed = ref(0)
const leaveSummaryFailed = ref(false)
// 1일 환산시간(분, 서버 권위 — my-leave-summary.convMinutes, E4 참고 분모). 미제공 시 480.
//   2026-08-09 규약: 잔여 표기가 일 단위 단독으로 전환되어 시간 환산 분모로는 더 이상 쓰지 않는다
//   — WithHourly 계열(사용/사용예정)의 dayPart 캐리 방어(decompose) 인자로만 잔존.
const leaveConvMinutes = ref(480)
// HB-13(F-3 §20-2): 사용/사용예정 셀의 부가 항목 원값(서버 additive 필드, 구 응답이면 0 폴백).
//   시간차는 "실사용 분"(START_DATE <= 오늘 / > 오늘 로 분리), 반차는 "일수"(건수 아님 — 분할차감 대응).
//   이 값들은 법정/법정 외 구분이 없는 전체 합계라 groups.TOTAL 에만 병기할 수 있는데,
//   본 화면은 애초에 groups.TOTAL 만 사용하므로(위 매핑 주석) MyLeaveSummaryView 의 전체-토글 한정 분기와 정합한다.
const leaveHourlyMinutesPast = ref(0)
const leaveHourlyMinutesPlanned = ref(0)
const leaveHalfDayDaysPast = ref(0)
const leaveHalfDayDaysPlanned = ref(0)

// 앱 버전 + 로딩 소스 표기 — 셸이 주입한 __SHELL__(T1) 기반. 구버전 셸/브라우저(__SHELL__
// 부재)에서는 기존 고정 표기를 유지한다(무회귀). loadSource 는 remote/bundle 판정 수단(§7).
const buildLabel = computed(() => {
  const shell = getShellInfo()
  if (!shell) return 'PRAFTA v1.0.0'
  const ver = shell.appVersion ? `v${shell.appVersion}` : 'v1.0.0'
  const parts = [`PRAFTA ${ver}`]
  if (shell.loadSource) parts.push(shell.loadSource)
  // 원격 배포 산출물에만 배포 스크립트(T5)가 __APP_BUILD__ 를 주입한다 — 해시로 배포본 식별(§7).
  const buildCommit = window.__APP_BUILD__ && window.__APP_BUILD__.commit
  if (buildCommit) parts.push(buildCommit)
  return parts.join(' · ')
})

// 선택약관 동의 설정 — GET /appApi/terms01/optional-terms 응답(현재버전 + agrYn).
//   비치명적: 실패 시 빈 목록(섹션 미노출). 토글은 POST /appApi/terms01/optional-terms-agree.
const optionalTerms = ref([])

// 위치정보 동의(005) 상태 — GET /comApi/consent/location-consent.
//   consentState 가 비어 있으면(조회 실패) 섹션 자체를 그리지 않는다(비치명적).
const locationConsent = ref({
  consentState: '',
  termsVersion: '',
  collectAllowed: false,
  termsId: '',
  termsNm: '',
})
const isLocationSaving = ref(false)

const locationStateLabel = computed(
  () => LOCATION_STATE_LABEL[locationConsent.value.consentState] || '',
)
const isLocationAgreed = computed(() => locationConsent.value.consentState === 'AGREED')

// 상태 배지 색 — 체크박스 옆에 놓이므로 시각 무게를 맞춘다(작은 pill).
const locationBadgeClass = computed(() => ({
  'is-agreed': locationConsent.value.consentState === 'AGREED',
  'is-suspended': locationConsent.value.consentState === 'SUSPENDED',
  'is-pending': locationConsent.value.consentState === 'PENDING_REAGREE',
  'is-withdrawn': locationConsent.value.consentState === 'WITHDRAWN',
}))

// 약관 전문 보기 — 006 의 onViewTerms 와 동일 경로/파라미터 계약.
const onViewLocationTerms = () => {
  router.push({
    path: '/TermsDetail',
    query: {
      termsId_p: locationConsent.value.termsId || '005',
      termsNm_p: locationConsent.value.termsNm || '위치기반서비스 이용약관',
    },
  })
}
// 토글 저장 직렬화 가드(동시 PUT 경합 방지).
const isTermsSaving = ref(false)

// ── BW-12(§7-1) 휴게 미이용 상시 요청 ─────────────────────────────────────
//   loaded/eligibleYn/standingYn/standingDtime 전부 서버(GET /appApi/mypage/brk-waive-standing) 값.
//   화면은 노출 조건도 시각 포맷도 재계산하지 않는다(서버 산출 표시 전용).
const brkWaiveStanding = ref({ loaded: false, standingYn: 'N', standingDtime: '', eligibleYn: 'N' })
const isBrkWaiveSaving = ref(false)

// 일용직(DAILY) 여부 — 연차 요약 섹션 노출 게이트(라운드트립 없이 세션값으로 판정).
const isDailyWorker = computed(() => isDailyWorkerFn())

// 2026-08-09 규약: 남은 연차는 일 단위 단독(splitLeaveDaysOnly — subText 항상 '') —
//   구 E4 역환산 참고치(splitLeaveDays + leaveConvMinutes) 제거.
//   내부 계산값(leaveRemaining 등 숫자 ref)은 그대로 두고 표시만 교체(muted 판정 회귀 없음).
const leaveRemainingParts = computed(() => splitLeaveDaysOnly(leaveRemaining.value))
// HB-13(F-3 §20-2): 사용예정/사용은 반차 건수 + 시간차 실분을 보조 텍스트에 병기(일수→시간 역환산 제거).
//   상세 화면(MyLeaveSummaryView 의 LeaveBalanceCard/LeaveSplitKpi)과 동일 규칙·동일 원값을 쓴다
//   — 메인 카드와 상세 화면의 수치가 어긋나지 않도록(F-3 이 화면 간 모순으로 번지는 것을 차단).
//   반차·시간차가 모두 0 이면 splitLeaveDays 와 완전히 동일한 결과라 기존 표기 회귀가 없다.
const leavePlannedParts = computed(() =>
  splitLeaveDaysWithHourly(
    leavePlanned.value,
    leaveConvMinutes.value,
    leaveHourlyMinutesPlanned.value,
    leaveHalfDayDaysPlanned.value,
  ),
)
const leaveUsedParts = computed(() =>
  splitLeaveDaysWithHourly(
    leaveUsed.value,
    leaveConvMinutes.value,
    leaveHourlyMinutesPast.value,
    leaveHalfDayDaysPast.value,
  ),
)

// 모달 토글 (UI 상태 — 허용 범위)
const logoutDialogOpen = ref(false)
const withdrawDialogOpen = ref(false)

// 탈퇴 진행 중 (중복 제출 방지)
const isWithdrawing = ref(false)

// 아바타 이니셜 (이름 앞 2자, 빈값이면 '?')
const avatarInitial = computed(() => (userNm.value ? userNm.value.slice(0, 2) : '?'))

// ───────────────────────────────────────────────────────────
// 메뉴 라우팅 (UI 토글/이동 — 허용)
// ───────────────────────────────────────────────────────────
// 뒤로가기: 메인 화면(MainView)으로 복귀
const onBack = () => {
  router.push('/MainView')
}
// prafta-app-028: 연차 요약 섹션 탭 → 연차 현황(MyLeaveSummaryView) 진입.
const onLeaveSummaryClick = () => {
  router.push('/MyLeaveSummaryView')
}
const onProfileEdit = () => {
  router.push('/ProfileEdit')
}
const onPasswordChange = () => {
  router.push('/PasswordChange')
}
// F-8-3: 근무 정보 항목 탭 → 바텀시트 오픈.
//   PRAFTA-003(결재자선택UI 추가): 오픈 전 프리셋/결재선 컨텍스트를 1회 로드해 시트에 props 로 전달.
const onDefaultSchClick = () => {
  loadDefaultSchPresets()
  loadDefaultSchApprovalContext()
  defaultSchSheetOpen.value = true
}
// PRAFTA-005(기본근무타입-승인제): 바텀시트 신청 완료(requested) → "승인 전 미반영" 정책에 따라
//   defaultSchLabel/defaultSchCd 는 갱신하지 않는다(현재값 그대로 유효). 대신 대기 배지를 재조회해 반영한다.
const onDefaultSchRequested = (/* reqId */) => {
  loadPendingDefaultSchChangeReq()
}
// 'HHmm' → 'HH:mm' (근무 정보 메뉴 항목 표시용)
const fmtSchTime = (t) => {
  if (!t || t.length < 4) return t || ''
  return `${t.substring(0, 2)}:${t.substring(2, 4)}`
}
// 일용직 계약서+승인제 T4: 내 서명 근로계약서 열람 진입(일용직 전용 노출).
const onMyContract = () => {
  router.push('/MyContract')
}
const onPresetManage = () => {
  router.push('/ApprovalPresetList')
}
// 사용자연차결재-04: 연차 결재 관리 진입(결재자 본인 스코프).
const onLeaveApproval = () => {
  router.push('/LeaveApproval')
}
// 001-Phase1-F4: 관리자 모드 진입(보호 라우트). 서버가 최종 진입 판정.
const onAdminMode = () => {
  router.push('/AdminHome')
}
// 선택약관 (보기) — 기존 TermsDetail 재사용(query termsId_p/termsNm_p).
const onViewTerms = (terms) => {
  router.push({
    path: '/TermsDetail',
    query: {
      termsId_p: terms.termsId,
      termsNm_p: terms.termsNm,
    },
  })
}

// ───────────────────────────────────────────────────────────
// 로그아웃 / 탈퇴 모달 열기 (UI 토글 — 허용)
// ───────────────────────────────────────────────────────────
const onLogoutClick = () => {
  logoutDialogOpen.value = true
}
const onWithdrawClick = () => {
  withdrawDialogOpen.value = true
}

// ───────────────────────────────────────────────────────────
// 로그아웃 확정 — 모달 confirm 후 처리
// ───────────────────────────────────────────────────────────
const onLogoutConfirm = async () => {
  // D3: 기존 로그아웃 인프라 재사용. forceLogout()이 서버 로그아웃(/comApi/login/logout) +
  //   clearSession + removeRefreshToken 을 일괄 처리한다(@/composables/useAuth).
  await forceLogout()
  // axios 기본 Authorization 헤더 제거 + userStore 초기화
  delete api.defaults.headers.common.Authorization
  try {
    userStore.logout()
  } catch (e) {
    console.warn('[MyPage] userStore logout skip:', e?.message)
  }
  router.replace('/')
}

// ───────────────────────────────────────────────────────────
// 회원 탈퇴 확정 — 체크박스 게이트 통과 후 처리
// ───────────────────────────────────────────────────────────
const onWithdrawConfirm = async () => {
  // D5: 탈퇴는 연차 자동취소/결재자 알림을 하지 않는다(서버 트랜잭션도 동일).
  //   서버가 ACCOUNT_STATUS='03' + PII 마스킹 + 토큰 폐기까지 처리한다.
  if (isWithdrawing.value) return
  isWithdrawing.value = true
  try {
    await api.post('/appApi/auth/withdraw', { confirmed: true })
    // 인사말 후 로컬 세션/토큰 초기화 + 로그인 화면 이동.
    await showAlert('그동안 PRAFTA를 이용해 주셔서 감사합니다.')
    await forceLogout()
    delete api.defaults.headers.common.Authorization
    try {
      userStore.logout()
    } catch (e) {
      console.warn('[MyPage] userStore logout skip:', e?.message)
    }
    router.replace('/')
  } catch (e) {
    const msg =
      e?.response?.data?.message || '탈퇴 처리 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.'
    showAlert(msg)
  } finally {
    isWithdrawing.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 진입 시 1회 조회 (프로필 + presetCount). 401/403은 axios 인터셉터 처리.
// ───────────────────────────────────────────────────────────
// 001-Phase1-F4: 관리자 진입점 노출 판정(경량). 실패는 비치명적 → 미노출 폴백.
const loadAdminEntryFlag = async () => {
  try {
    const { data } = await api.get('/appApi/admin/access-context')
    canEnterAdmin.value = data?.canEnterAdmin === true
  } catch (e) {
    canEnterAdmin.value = false
    console.warn('[MyPage] 관리자 진입판정 실패:', e?.message)
  }
}

// 사용자연차결재-04: 연차 결재 대기 건수 배지(경량 조회). 비치명적 → 실패는 0 폴백(미노출).
const loadPendingApprovalCount = async () => {
  try {
    const { data } = await api.get('/appApi/leaveflow/approval/pending')
    pendingApprovalCount.value = data?.totalCount ?? 0
  } catch (e) {
    pendingApprovalCount.value = 0
    console.warn('[MyPage] 연차 결재 대기 건수 조회 실패:', e?.message)
  }
}

// PRAFTA-005(기본근무타입-승인제): 근무 정보 메뉴 "승인 대기 중" 배지(경량 조회, PRAFTA-004 확장분 재사용).
//   비치명적 → 실패는 false 폴백(미노출, 서버가 최종 중복 검증).
const loadPendingDefaultSchChangeReq = async () => {
  try {
    const { data } = await api.get('/appApi/req06/my', {
      params: { reqTypes: '14', reqStatuses: '01', limit: 1 },
    })
    const items = Array.isArray(data?.items) ? data.items : []
    hasPendingDefaultSchReq.value = items.length > 0
  } catch (e) {
    hasPendingDefaultSchReq.value = false
    console.warn('[MyPage] 기본 근무타입 변경 대기 조회 실패:', e?.message)
  }
}

// PRAFTA-003(결재자선택UI 추가): 결재선 프리셋 로드(req07 AttdRequestView.vue loadPresets 미러).
//   실패해도 시트는 빈 프리셋으로 동작(무차단).
const loadDefaultSchPresets = async () => {
  try {
    const { data } = await api.get('/appApi/mypage/approval-presets')
    defaultSchPresets.value = Array.isArray(data?.presets) ? data.presets : []
  } catch (e) {
    console.error('[MyPage] 결재선 프리셋 로드 실패:', e?.message)
    defaultSchPresets.value = []
  }
}

// PRAFTA-003(결재자선택UI 추가): 결재선 분기 컨텍스트 로드(req07 AttdRequestView.vue
//   loadApprovalContext 미러 — workYmd 없이 호출 가능함을 확인함, AppReq09Controller 는 workYmd 를
//   실제로 읽지 않는다). 실패 시 null → 시트가 결재선 노출 폴백.
const loadDefaultSchApprovalContext = async () => {
  try {
    const { data } = await api.get('/appApi/req09/approval-context')
    defaultSchApprovalContext.value = data || null
  } catch (e) {
    console.error('[MyPage] 결재선 컨텍스트 로드 실패:', e?.message)
    defaultSchApprovalContext.value = null
  }
}

// ───────────────────────────────────────────────────────────
// prafta-app-028: 연차 요약 로드 (GET /appApi/leave01/my-leave-summary, 기존 EP 재사용)
//   비치명적: 실패하면 leaveSummaryFailed=true → 섹션 미노출(전체 화면 에러로 키우지 않음).
//   일용직(DAILY)은 호출 자체를 생략해도 무방(섹션이 isDailyWorker 게이트로 미노출).
//   ── developer 라운드 스코프 ──
// ───────────────────────────────────────────────────────────
const loadLeaveSummary = async () => {
  // 일용직(DAILY)은 연차 해당없음 → 조회 자체를 생략(섹션도 isDailyWorker 게이트로 미노출).
  if (isDailyWorker.value) return
  try {
    const { data } = await api.get('/appApi/leave01/my-leave-summary')
    const total = data?.groups?.TOTAL
    // 데이터 없음도 비치명적 미노출(섹션 자체를 숨김, 전체 화면 에러로 키우지 않음).
    if (!total) {
      leaveSummaryFailed.value = true
      return
    }
    leaveRemaining.value = total.remaining ?? 0 // 남은 연차(부여 − 총사용)
    leavePlanned.value = total.planned ?? 0 // 사용예정 연차(미래 확정 연차)
    leaveUsed.value = total.used ?? 0 // 사용 연차(실제 소진분)
    // LC-11: 표기 분모 — 서버 미제공(구버전 응답) 시 480 폴백.
    leaveConvMinutes.value = Number(data?.convMinutes) > 0 ? Number(data.convMinutes) : 480
    // HB-13(F-3 §20-2): 사용/사용예정 병기용 원값(서버 additive 필드). 구 응답이면 0 → 기존 표기로 폴백.
    leaveHourlyMinutesPast.value = Number(data?.hourlyUsedMinutesPast) || 0
    leaveHourlyMinutesPlanned.value = Number(data?.hourlyUsedMinutesPlanned) || 0
    leaveHalfDayDaysPast.value = Number(data?.halfDayUsedDaysPast) || 0
    leaveHalfDayDaysPlanned.value = Number(data?.halfDayUsedDaysPlanned) || 0
    // 새로고침 재호출 대비: 성공 경로에서 실패 플래그를 명시적으로 리셋(이전 실패 상태 박제 방지).
    leaveSummaryFailed.value = false
  } catch (e) {
    // 비치명적: 섹션만 미노출. showAlert 금지(전체 화면 에러로 키우지 않음).
    leaveSummaryFailed.value = true
    console.warn('[MyPage] 연차 요약 조회 실패:', e?.message)
  }
}

// ───────────────────────────────────────────────────────────
// 위치정보 동의(005) 상태 로드 (GET /comApi/consent/location-consent).
//   비치명적: 실패하면 섹션 미노출. 전체 화면 에러로 키우지 않는다.
// ───────────────────────────────────────────────────────────
const loadLocationConsent = async () => {
  try {
    const { data } = await api.get('/comApi/consent/location-consent')
    locationConsent.value = {
      consentState: data?.consentState || '',
      termsVersion: data?.termsVersion || '',
      collectAllowed: !!data?.collectAllowed,
      termsId: data?.termsId || '',
      termsNm: data?.termsNm || '',
    }
  } catch (e) {
    locationConsent.value = {
      consentState: '',
      termsVersion: '',
      collectAllowed: false,
      termsId: '',
      termsNm: '',
    }
    console.warn('[MyPage] 위치정보 동의 상태 조회 실패:', e?.message)
  }
}

// 위치정보 상태 전이 공통 호출기. 성공하면 서버가 돌려준 상태로 갱신한다
//   (낙관적 갱신을 쓰지 않는다 — 철회는 파기를 동반해 되돌릴 수 없으므로 서버 확정값만 믿는다).
//   반환값 = 전이 성공 여부. 해제 시트는 이 값이 true 일 때만 닫는다(실패 시 다시 고를 수 있게).
const callLocationConsent = async (path, successMsg) => {
  if (isLocationSaving.value) return false
  isLocationSaving.value = true
  try {
    const { data } = await api.post(`/comApi/consent/location-consent/${path}`)
    // ★약관명은 전이 응답에 없을 수 있다 — 기존 값을 보존해 [보기] 버튼이 죽지 않게 한다.
    locationConsent.value = {
      consentState: data?.consentState || '',
      termsVersion: data?.termsVersion || '',
      collectAllowed: !!data?.collectAllowed,
      termsId: data?.termsId || locationConsent.value.termsId,
      termsNm: data?.termsNm || locationConsent.value.termsNm,
    }
    await showAlert(typeof successMsg === 'function' ? successMsg(data) : successMsg)
    return true
  } catch (e) {
    console.warn('[MyPage] 위치정보 동의 전이 실패:', e?.message)
    showAlert(e?.response?.data?.message || '처리하지 못했어요. 잠시 후 다시 시도해 주세요.')
    return false
  } finally {
    isLocationSaving.value = false
  }
}

// 해제 시트 열림 상태 — 체크를 해제하려 할 때만 연다.
const locationOffSheetOpen = ref(false)

// 일시 중지 — 과거 기록은 유지된다(법 제24조②).
//   철회와 마찬가지로 실행 전 확인을 받는다(오탭 방지). 취소하면 시트는 열린 채로 남는다.
const onSheetSuspendLocation = async () => {
  const confirmed = await showConfirm(LOCATION_SUSPEND_CONFIRM)
  if (!confirmed) return
  const ok = await callLocationConsent(
    'suspend',
    // ★두 문장을 줄로 나눠 보여 준다(모달이 white-space:pre-line 이라 \n 이 개행으로 렌더링됨).
    '위치정보 수집을 중지했어요.\n지금까지의 기록은 그대로 있어요.',
  )
  if (ok) locationOffSheetOpen.value = false
}

// 동의 철회 — ★수집된 위치정보를 전부 파기한다. 되돌릴 수 없으므로 시트 설명과 별개로
//   확인 팝업을 유지한다(파기 직전 마지막 관문). 취소하면 시트는 열린 채로 남는다.
const onSheetWithdrawLocation = async () => {
  const confirmed = await showConfirm(LOCATION_WITHDRAW_CONFIRM)
  if (!confirmed) return
  const ok = await callLocationConsent('withdraw', (data) => {
    const purged = Number(data?.purgedRows || 0)
    return purged > 0
      ? `동의를 철회하고 위치정보 ${purged}건을 삭제했어요.`
      : '동의를 철회했어요. 삭제할 위치정보는 없었어요.'
  })
  if (ok) locationOffSheetOpen.value = false
}

// 재동의 — 철회로 파기된 좌표는 복구되지 않는다.
const onResumeLocation = async () => {
  await callLocationConsent('resume', '위치정보 제공에 다시 동의했어요.')
}

// 체크박스 토글 — 선택약관 행과 조작 형태를 맞춘 진입점(2026-09-02 UI 통일).
//   ★체크 해제는 그 자리에서 처리하지 않는다. 중지(되돌릴 수 있음)와 철회(파기, 복구 불가)는
//     결과가 전혀 달라 사용자가 골라야 하기 때문이다 → 해제 시트를 띄운다.
//   체크(재동의)는 잃는 것이 없으므로 즉시 처리한다.
const onToggleLocationConsent = async () => {
  if (isLocationSaving.value) return
  if (isLocationAgreed.value) {
    locationOffSheetOpen.value = true
  } else {
    // SUSPENDED / PENDING_REAGREE / WITHDRAWN 어느 상태에서도 resume 가능(서버 계약).
    await onResumeLocation()
  }
}

// ───────────────────────────────────────────────────────────
// 선택약관 동의 설정 로드 (GET /appApi/terms01/optional-terms).
//   비치명적: 실패하면 빈 목록(섹션 미노출). 전체 화면 에러로 키우지 않음.
// ───────────────────────────────────────────────────────────
const loadOptionalTerms = async () => {
  try {
    const { data } = await api.get('/appApi/terms01/optional-terms')
    const list = Array.isArray(data?.terms) ? data.terms : []
    optionalTerms.value = list.map((t) => ({
      termsId: t.termsId,
      termsNm: t.termsNm,
      termsVersion: t.termsVersion,
      agrYn: t.agrYn === 'Y' ? 'Y' : 'N',
    }))
  } catch (e) {
    // 비치명적: 섹션 미노출. showAlert 금지.
    optionalTerms.value = []
    console.warn('[MyPage] 선택약관 조회 실패:', e?.message)
  }
}

// 선택약관 토글(낙관적 토글 + POST 저장, 실패 시 원복).
//   PRAFTA-SUBCON-T4: 연동 회사 제3자 제공 동의(006)의 '철회'(Y→N)는 확인 팝업을 거친다.
//   철회는 소급되지 않는다(이미 제공된 스냅샷은 회수 불가) — 사용자에게 반드시 고지.
//   N→Y(동의) 및 그 외 선택약관은 기존대로 즉시 저장(팝업 없음).
const onToggleOptionalTerms = async (terms) => {
  if (isTermsSaving.value) return
  const prev = terms.agrYn
  const next = prev === 'Y' ? 'N' : 'Y'

  if (terms.termsId === THIRD_PARTY_CONSENT_TERMS_ID && prev === 'Y' && next === 'N') {
    const ok = await showConfirm(
      '연동 회사 자료 제공에 대한 동의를 철회할까요?\n\n' +
        '철회하면 이후 생성되는 제공분부터 제외됩니다.\n' +
        '다만 이미 제공된 자료는 회수되지 않습니다.',
    )
    // 취소: 낙관적 토글 이전이므로 스위치 상태 변경 없음(서버 호출도 없음).
    if (!ok) return
  }

  // 낙관적 토글.
  terms.agrYn = next
  isTermsSaving.value = true
  try {
    await api.post('/appApi/terms01/optional-terms-agree', {
      termsId: terms.termsId,
      agrYn: next,
    })
  } catch (e) {
    // 실패 시 원복 + 안내.
    terms.agrYn = prev
    console.warn('[MyPage] 선택약관 토글 실패:', e?.message)
    showAlert(e?.response?.data?.message || '설정을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isTermsSaving.value = false
  }
}

// ───────────────────────────────────────────────────────────
// BW-12(§7-1): 휴게 미이용 상시 요청 로드 (GET /appApi/mypage/brk-waive-standing).
//   비치명적: 실패하면 loaded=false 유지 → 행 미노출(선택약관 조회와 동일 관례). showAlert 금지.
//   일용직은 서버 대상 자체가 아니므로 호출을 생략한다(행도 isDailyWorker 게이트로 미노출).
// ───────────────────────────────────────────────────────────
const loadBrkWaiveStanding = async () => {
  if (isDailyWorker.value) return
  try {
    const { data } = await api.get('/appApi/mypage/brk-waive-standing')
    brkWaiveStanding.value = {
      loaded: true,
      standingYn: data?.standingYn === 'Y' ? 'Y' : 'N',
      standingDtime: data?.standingDtime || '',
      eligibleYn: data?.eligibleYn === 'Y' ? 'Y' : 'N',
    }
  } catch (e) {
    // 비치명적: 행 미노출(구서버·DDL 미적용 환경 포함).
    brkWaiveStanding.value = { loaded: false, standingYn: 'N', standingDtime: '', eligibleYn: 'N' }
    console.warn('[MyPage] 휴게 미이용 상시 요청 조회 실패:', e?.message)
  }
}

// 상시 요청 토글 — 확인 팝업 없음(되돌릴 수 있는 토글. 위치정보 '일시 중지'와 같은 급).
//   낙관적 토글 후 PUT, 실패 시 원복 + 안내. 시각은 서버 응답값으로만 갱신(클라 시계 신뢰 금지).
const onToggleBrkWaiveStanding = async () => {
  if (isBrkWaiveSaving.value) return
  const prev = brkWaiveStanding.value.standingYn
  const prevDtime = brkWaiveStanding.value.standingDtime
  const next = prev === 'Y' ? 'N' : 'Y'

  brkWaiveStanding.value = { ...brkWaiveStanding.value, standingYn: next }
  isBrkWaiveSaving.value = true
  try {
    const { data } = await api.put('/appApi/mypage/brk-waive-standing', { standingYn: next })
    brkWaiveStanding.value = {
      loaded: true,
      standingYn: data?.standingYn === 'Y' ? 'Y' : 'N',
      standingDtime: data?.standingDtime || '',
      eligibleYn: data?.eligibleYn === 'Y' ? 'Y' : 'N',
    }
  } catch (e) {
    brkWaiveStanding.value = {
      ...brkWaiveStanding.value,
      standingYn: prev,
      standingDtime: prevDtime,
    }
    console.warn('[MyPage] 휴게 미이용 상시 요청 저장 실패:', e?.message)
    showAlert(e?.response?.data?.message || '설정을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isBrkWaiveSaving.value = false
  }
}

// ───────────────────────────────────────────────────────────
// prafta-app-028: 진입/새로고침 공통 조회 묶음.
//   onMounted 와 pull-to-refresh(onPullEnd) 가 모두 호출한다.
//   { showLoading } — true: 최초 진입(전체 로딩 표시). false: 당겨서 새로고침(카드 유지 + 자체 인디케이터).
//   ── developer 라운드 스코프 ──
// ───────────────────────────────────────────────────────────
const loadAll = async ({ showLoading = true } = {}) => {
  // 최초 진입(showLoading=true)에서만 전체 로딩 표시. 당겨서 새로고침은 카드 유지 + 자체 인디케이터.
  if (showLoading) isLoading.value = true

  // 비치명적 4종은 먼저 병렬 시작(각자 내부에서 예외 흡수 → reject 없음).
  const adminP = loadAdminEntryFlag()
  const pendingP = loadPendingApprovalCount()
  const leaveP = loadLeaveSummary()
  const termsP = loadOptionalTerms()
  const locConsentP = loadLocationConsent()
  // PRAFTA-005(기본근무타입-승인제): 근무 정보 메뉴 배지(비치명적).
  const defaultSchPendingP = loadPendingDefaultSchChangeReq()
  // BW-12(§7-1): 휴게 미이용 상시 요청 행(비치명적).
  const brkWaiveP = loadBrkWaiveStanding()

  // 프로필(주 데이터)은 try/catch 로 직접 처리.
  //   GET /appApi/mypage/profile (마스킹 응답 D1). 메인 화면은 마스킹 PII를 사용하지 않고
  //   이름/사업장/부서/프리셋개수만 표시한다.
  try {
    const { data } = await api.get('/appApi/mypage/profile')
    userNm.value = data?.userNm || ''
    siteNm.value = data?.siteNm || ''
    nodeNm.value = data?.nodeNm || ''
    presetCount.value = data?.presetCount ?? 0
    // F-8-3: 현재 기본 근무타입 표시(미설정이면 defaultSchNo 가 없어 라벨은 빈 문자열 → "미설정" 표기).
    defaultSchCd.value = data?.defaultSchCd || ''
    defaultSchLabel.value = data?.defaultSchNo
      ? `${data.defaultSchNo} (${fmtSchTime(data.defaultSchStrTime)}~${fmtSchTime(data.defaultSchEndTime)})`
      : ''
  } catch (e) {
    // 401/403 등 토큰 에러는 axios 인터셉터가 처리. 그 외 실패는 세션값으로 최소 폴백 표시.
    console.warn('[MyPage] 프로필 조회 실패:', e?.message)
    userNm.value = sessionStorage.getItem('gv_userNm') || ''
    siteNm.value = sessionStorage.getItem('gv_siteNm') || ''
    nodeNm.value = sessionStorage.getItem('gv_nodeNm') || ''
    showAlert('정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    // 프로필 기준으로 로딩 표시를 먼저 푼다(아래 Promise.all 보다 앞).
    if (showLoading) isLoading.value = false
  }

  // 비치명적 조회 완료 대기(내부에서 예외 흡수되어 reject 없음).
  await Promise.all([adminP, pendingP, leaveP, termsP, locConsentP, defaultSchPendingP, brkWaiveP])
}

onMounted(() => {
  // 소정-12(UI-E): 연차 노출 판정 확보(캐시 있으면 라운드트립 없음). 실패는 비차단(노출 폴백).
  ensureLeaveFeatureVisibility()
  // prafta-app-028: 진입 시 프로필/관리자판정/대기건수/연차요약을 loadAll 로 일괄 조회(중복 호출 제거).
  loadAll()
})

// ───────────────────────────────────────────────────────────
// prafta-app-028: 당겨서 새로고침 — 공통 컴포저블(usePullToRefresh)로 추출.
//   .mp-body 가 실제 스크롤 컨테이너(overflow-y:auto, flex:1, min-height:0)이며
//   스크롤 최상단(프로필 카드 위)에서 아래로 더 당기면 loadAll({showLoading:false}) 재조회.
//   기존 새로고침 콜백(프로필/관리자판정/대기건수/연차요약 일괄 갱신)은 그대로 유지.
// ───────────────────────────────────────────────────────────
const mpBodyEl = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(
  mpBodyEl,
  async () => {
    // 새로고침 시 프로필/관리자판정/대기건수/연차요약을 함께 갱신(비치명적 항목은 자체 폴백).
    await loadAll({ showLoading: false })
  },
)
</script>

<style scoped>
/*
 * 디자인 토큰 — MainView(.home-view)와 동일 세트를 본 화면 루트에 1회 선언.
 * 자식 컴포넌트(scoped)는 var(--...) 를 상속받아 사용. 하드코딩/Tailwind 금지.
 */
.my-page-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-on-danger: #ffffff;
  /* 위치정보 동의 S4: primary 버튼 전경색(기존 on-danger 와 짝). */
  --color-on-primary: #ffffff;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-switch-off: #d1d5db;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: relative;
  /* prafta-app-025 J1-2: 하단 공통 탭바를 뷰포트 바닥에 고정하기 위해 화면 높이를 뷰포트로 고정. */
  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.mp-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
}
.mp-hd__back {
  width: 44px;
  height: 44px;
  margin-left: -10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.mp-hd__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.mp-hd__spacer {
  width: 44px;
}

/* 본문 — 하단 공통 탭바(72px)에 가려지지 않도록 하단 패딩(88px).
   prafta-app-028: min-height:0 으로 .mp-body 를 실제 스크롤 컨테이너로 고정(당겨서 새로고침 스크롤 위치 오판 방지). */
.mp-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-sm) var(--space-lg) 88px;
}
.mp-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 프로필 카드 */
.mp-profile {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--space-lg);
}
.mp-profile__avatar {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  border: 0.5px solid var(--color-primary-tint-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
}
.mp-profile__info {
  min-width: 0;
}
.mp-profile__name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.mp-profile__meta {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* prafta-app-028: 연차 요약 섹션 (탭 가능 카드) */
.mp-leave {
  margin-bottom: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  font-variant-numeric: tabular-nums;
}
.mp-leave__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}
.mp-leave__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.mp-leave__chev {
  color: var(--color-text-tertiary);
}
.mp-leave__kpis {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
}
.mp-leave__cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: 0 var(--space-xs);
  position: relative;
}
.mp-leave__cell + .mp-leave__cell::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 1px;
  background: var(--color-border-light);
}
.mp-leave__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.mp-leave__val {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}
/* 남은 연차는 강조(primary). */
.mp-leave__val--accent {
  color: var(--color-primary);
}
/* 0값은 muted 로 노이즈 감소(LeaveSplitKpi 패턴). accent 보다 우선 적용. */
.mp-leave__val--muted {
  color: var(--color-text-tertiary);
}
.mp-leave__unit {
  margin-left: 2px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-tertiary);
}
/* LC-11: 시간·분 보조 텍스트 ("3시간 30분") — 일 단위 옆 축소 표기(줄바꿈 허용). */
.mp-leave__sub {
  display: block;
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

/* 그룹 라벨 */
.mp-group-label {
  margin: var(--space-lg) 0 var(--space-sm) var(--space-xs);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 메뉴 리스트 */
.mp-menu {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.mp-menu__row {
  width: 100%;
  min-height: 52px;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 0 var(--space-lg);
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--color-border-light);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.mp-menu__row:last-child {
  border-bottom: 0;
}
.mp-menu__text {
  flex: 1;
  font-size: 15px;
  color: var(--color-text-primary);
}
.mp-menu__meta {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.mp-menu__chev {
  color: var(--color-text-tertiary);
}

/* 로그아웃 버튼 (풀폭 secondary) */
.mp-logout {
  width: 100%;
  height: 48px;
  margin-top: var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: var(--color-surface);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 회원 탈퇴 텍스트 링크 */
.mp-withdraw {
  display: block;
  margin: var(--space-lg) auto 0;
  padding: var(--space-sm);
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  font-size: 13px;
  font-weight: 500;
  text-decoration: underline;
  cursor: pointer;
  font-family: inherit;
}

/* 앱 버전 */
.mp-version {
  margin: var(--space-md) 0 0;
  text-align: center;
  font-size: 11px;
  color: var(--color-text-tertiary);
}

/* 약관 동의 설정 — 선택약관(006 등) + 위치정보(005) 공통 행.
   ★2026-09-02 UI 통일: 두 행 모두 [체크박스][(선택) 약관명][보기] 를 왼쪽 정렬한다.
     종전에는 006 만 우측 스위치였고, 005 는 하단 버튼 2개라 조작 형태가 서로 달랐다.
     색상/간격은 화면 루트에 선언한 CSS 변수만 사용한다(하드코딩 금지). */
/* 상태 배지 줄 — 체크 해제 사유(중지/재동의필요/철회)를 라벨 아랫줄에 왼쪽 정렬로 둔다.
   ★라벨과 같은 줄에 두지 않는다(배지 폭만큼 자리를 뺏겨 약관명이 접힌다). */
.mp-loc__state {
  display: flex;
  justify-content: flex-start;
  margin-top: var(--space-xs);
}
.mp-loc-badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  line-height: 1.6;
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.mp-loc-badge.is-agreed {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.mp-loc-badge.is-suspended,
.mp-loc-badge.is-pending {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.mp-loc-badge.is-withdrawn {
  background: var(--color-border-light);
  color: var(--color-danger);
}
/* (행 액션 버튼 .mp-loc__btn 3종은 2026-09-02 에 전부 사라졌다 —
   중지/철회는 해제 시트로, 재동의는 체크박스 자체로 흡수되어 사용처가 없다.) */

.mp-terms-row {
  min-height: 56px;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.mp-terms-row:last-child {
  border-bottom: 0;
}
/* 위치정보 행 — 설명/버튼이 아래로 쌓이는 변형.
   ★반드시 .mp-terms-row 뒤에 선언한다. 특이도가 같아 앞에 두면 base 의
     align-items:center 가 이겨 버리고, 컬럼 방향에서 그 값은 자식을 가로 중앙에
     밀어 넣는다(= 2026-09-02 이전까지 위치정보 행만 좌측 정렬이 안 되던 원인). */
.mp-terms-row--stack {
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-xs);
  padding-top: var(--space-md);
  padding-bottom: var(--space-md);
}
/* 라벨 줄은 행 폭을 다 써야 [보기]·상태 배지가 제자리에 놓인다. */
.mp-terms-row--stack > * {
  width: 100%;
}
.mp-terms-row__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  min-height: 24px;
}
/* 체크박스(role=checkbox 버튼) — 박스와 라벨을 한 버튼으로 묶어 탭 영역을 넓힌다.
   [보기]는 중첩 버튼이 될 수 없어 이 버튼 밖 형제로 둔다. */
.mp-terms-check {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  min-width: 0;
  min-height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.mp-terms-check:disabled {
  opacity: 0.5;
  cursor: default;
}
.mp-terms-check__box {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border: 1.5px solid var(--color-switch-off);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  transition:
    background 0.15s ease,
    border-color 0.15s ease;
}
.mp-terms-check--on .mp-terms-check__box {
  border-color: var(--color-primary);
  background: var(--color-primary);
}
.mp-terms-check__mark {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: var(--color-on-primary);
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0;
  transition: opacity 0.15s ease;
}
.mp-terms-check--on .mp-terms-check__mark {
  opacity: 1;
}
.mp-terms-check__label {
  font-size: 15px;
  color: var(--color-text-primary);
  word-break: keep-all;
}
.mp-terms-row__view {
  flex-shrink: 0;
  background: transparent;
  border: 0;
  padding: 0;
  font-size: 13px;
  color: var(--color-primary);
  text-decoration: underline;
  cursor: pointer;
  font-family: inherit;
}
/* BW-12(§7-1): 상시 요청 행의 설명 문단 — 라벨 아래 보조 텍스트(체크박스 폭만큼 들여쓰기). */
.mp-terms-row__desc {
  margin: 0;
  padding-left: calc(22px + var(--space-sm));
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-secondary);
  word-break: keep-all;
}

/* (구 .mp-switch 스위치 스타일은 2026-09-02 체크박스 전환으로 제거 — 사용처 0.) */

.mp-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
