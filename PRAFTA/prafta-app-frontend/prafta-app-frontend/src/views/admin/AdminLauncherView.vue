<!--
  AdminLauncherView.vue — 관리자 모드 메인 (세로 섹션 + 하단 탭바 + 우상단 설정)
  - 작업 ID: 001-P1-F1 (UI 재설계: .claude/requests/app_requests/001-phase1-admin-ui-redesign.md)
  - 권한 단일 출처: 001_관리자모드-권한매트릭스.md §1(진입조건) · §3(모듈×역할 활성/비활성) · §3.1(현장전환)
  - 재설계 골자(사용자 확정 2026-06-06):
      · 2열 그리드 폐기 → 사용자 모드(MainView) 본문 패턴인 "세로 섹션 스크롤"로 교체.
      · 헤더를 AdminHeader(좌:사업장명+현장전환 / 우:[설정] 버튼)로 분리. 알림벨 없음.
      · 하단 AdminTabBar(승인/근태/홈/안전/TBM 5탭) 추가. 홈=대시보드 런처(중앙).
      · 본문 섹션 순서(위→아래): 대시보드(placeholder) → 승인 → 근태상세 → 안전 → TBM → 현장처리 → 게시판(준비중).
      · 설정은 본문 섹션 제외(우상단 버튼으로 이동). 현장처리/게시판/설정은 탭에 없음(의도).
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만.
  - developer 라운드 스코프(TODO):
      (1) GET /appApi/admin/access-context 조회 → canEnterAdmin/moduleActiveMap/moduleScopedMap/
          accessibleSites/siteSwitchEnabled/currentSiteCd 바인딩(기존 골격 로직 그대로 재사용 가능)
      (2) 본문 섹션(enabled) 선택 → 해당 모듈 라우팅(Phase 1=ComingSoon, Phase 2~8 실화면 교체)
      (3) 하단 탭(enabled) 선택 → 해당 모듈 라우팅/ComingSoon(홈=현 화면 유지)
      (4) 우상단 [설정] → 설정 진입(Phase 1=ComingSoon)
      (5) 현장 전환 시트 선택 → 사업장 전파(D5) + access-context 재조회
  - ⚠️ C1 원칙: 섹션/탭 활성·스코프는 서버(moduleActiveMap/moduleScopedMap)만 신뢰.
    클라이언트에서 역할(AUTH_CD) 기반 분기 로직을 두지 않는다(본 골격에 역할 판정 없음).
  - 디자인 토큰: MainView 세트를 .admin-launcher-view 루트에 1회 선언.
    자식(AdminHeader/AdminTabBar/AdminModuleSection/AdminSiteSwitchSheet)은 scoped 상태로 var(--...) 상속.
  - 진입: 사용자 모드 진입점(마이페이지 등, D7) → router.push('/AdminHome') (보호 라우트).
-->
<template>
  <div class="admin-launcher-view">
    <!-- 아이콘 스프라이트(본 화면 전역 use 참조 전제) -->
    <svg width="0" height="0" class="admin-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-admin-mappin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="11" r="3" /><path d="M17.657 16.657L13.414 20.9a2 2 0 0 1-2.827 0l-4.244-4.243a8 8 0 1 1 11.314 0z" />
        </symbol>
        <symbol id="i-admin-chev-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol id="i-admin-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-admin-chev-down" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="6 9 12 15 18 9" />
        </symbol>
        <!-- 모듈/탭 아이콘 (라인 아이콘) -->
        <symbol id="i-admin-home" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 11l9-8 9 8" /><path d="M5 10v10h14V10" />
        </symbol>
        <symbol id="i-admin-approval" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
        </symbol>
        <symbol id="i-admin-attd" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
        </symbol>
        <symbol id="i-admin-safety" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" />
        </symbol>
        <symbol id="i-admin-tbm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="3" width="20" height="14" rx="2" /><path d="M8 21h8M12 17v4" />
        </symbol>
        <symbol id="i-admin-siteops" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><path d="M14 17.5h7M17.5 14v7" />
        </symbol>
        <symbol id="i-admin-board" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 4h16v12H7l-3 3z" /><path d="M8 9h8M8 12h5" />
        </symbol>
        <symbol id="i-admin-settings" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 0 1-4 0v-.1A1.6 1.6 0 0 0 7 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H1a2 2 0 0 1 0-4h.1A1.6 1.6 0 0 0 2.6 7a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1A1.6 1.6 0 0 0 7 2.6 1.6 1.6 0 0 0 8 1.1V1a2 2 0 0 1 4 0v.1A1.6 1.6 0 0 0 17 2.6a1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V8a1.6 1.6 0 0 0 1.1 1.5H23a2 2 0 0 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z" />
        </symbol>
      </defs>
    </svg>

    <!-- 헤더: 좌(사용자 모드 복귀 + 사업장명+현장전환) / 우([설정]) -->
    <AdminHeader
      :site-name="siteName"
      :site-switch-enabled="siteSwitchEnabled"
      @click:back="onBackToUser"
      @click:site-switch="onOpenSiteSwitch"
      @click:settings="onSettings"
    />

    <!-- 본문 (세로 섹션 스크롤) -->
    <main
      class="admin-body"
      ref="bodyEl"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출(공통 컴포저블) -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 로딩 -->
      <div v-if="isLoading" class="admin-loading" aria-live="polite">불러오는 중...</div>

      <!-- 권한 없음(직접 진입 등 방어 — 정상 흐름은 진입점에서 게이팅) -->
      <div v-else-if="!canEnterAdmin" class="admin-empty" aria-live="polite">
        <p class="admin-empty__title">관리자 권한이 없습니다</p>
        <p class="admin-empty__desc">관리자 모드를 사용할 수 없는 계정입니다.</p>
      </div>

      <template v-else>
        <!-- 연차 변경 확인 대기 배너 — 근로자가 동의(AGREED)해 관리자 확정만 남은 건이 있을 때 노출.
             탭하면 확인 화면(/AdminLeaveChangeConfirm)으로 이동. 스코프/권한은 서버가 단일 출처로 판정. -->
        <button
          v-if="pendingConfirmCount > 0"
          type="button"
          class="confirm-banner"
          @click="onConfirmBannerClick"
        >
          <span class="confirm-banner__icon" aria-hidden="true">!</span>
          <span class="confirm-banner__text">
            근로자가 동의한 연차 변경/삭제 확인이
            <strong>{{ pendingConfirmCount }}</strong>건 있어요
          </span>
          <span class="confirm-banner__cta" aria-hidden="true">확인 ▸</span>
        </button>

        <!-- 1) 대시보드 (최상단) — prafta-app-025 J1-10 B-5: 요약 위젯 4종(자체 조회).
             currentSiteCd 를 prop 으로 전달해 현장 전환 시 재조회되게 한다(C1: 서버 산출만 신뢰). -->
        <AdminDashboard ref="dashboardRef" :site-cd="currentSiteCd" />

        <!-- 2~7) 모듈 세로 섹션 — enabled/scoped/note 는 서버 맵에서만(C1) -->
        <div class="module-list">
          <AdminModuleSection
            v-for="m in modules"
            :key="m.key"
            :title="m.title"
            :icon-id="m.iconId"
            :enabled="moduleActiveMap[m.key] === true"
            :scoped="moduleScopedMap[m.key] === true"
            :note="m.note"
            :badge-count="moduleBadgeMap[m.key] || 0"
            @select="onModuleSelect(m)"
          />
        </div>
      </template>
    </main>

    <!-- 하단 탭바: 승인 / 근태 / 홈(중앙) / 안전 / TBM. 비활성 탭은 회색 무동작. -->
    <AdminTabBar
      :active-tab="'home'"
      :module-active-map="moduleActiveMap"
      @click:tab="onTabClick"
    />

    <!-- 현장 전환 셀렉터 시트(유지/재사용) — 헤더 현장전환 트리거에서 호출 -->
    <AdminSiteSwitchSheet
      v-model="siteSheetOpen"
      :sites="accessibleSites"
      :current-site-cd="currentSiteCd"
      @select="onSelectSite"
    />
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'

import AdminHeader from './components/AdminHeader.vue'
import AdminTabBar from './components/AdminTabBar.vue'
import AdminModuleSection from './components/AdminModuleSection.vue'
import AdminSiteSwitchSheet from './components/AdminSiteSwitchSheet.vue'
import AdminDashboard from './components/AdminDashboard.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — MainView/MyPageView 패턴 동일.
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 진입판정/로딩 (GET /appApi/admin/access-context 응답으로 채움) ───────
const isLoading = ref(true)
const canEnterAdmin = ref(false)

// 헤더/현장 (access-context.currentSiteCd / accessibleSites / siteSwitchEnabled)
const siteName = ref('') // accessibleSites 중 currentSiteCd 의 siteNm (또는 토큰 gv_siteNm)
const currentSiteCd = ref('')
const siteSwitchEnabled = ref(false)
const accessibleSites = ref([]) // [{ siteCd, siteNo, siteNm }]

// 모듈 활성/스코프 맵 (서버 산출값 — 클라이언트 역할 분기 금지, C1)
const moduleActiveMap = ref({}) // { HOME:true, APPROVAL:false, ... }
const moduleScopedMap = ref({}) // { APPROVAL:true, ... } → 🔵 배지

// 본문 세로 섹션 표시 구성(정적 — 표시명/아이콘/라우트는 프레젠테이션 설정).
//   설정(SETTINGS)·홈(HOME)은 본문 섹션에서 제외(설정=우상단 버튼, 홈=대시보드 placeholder).
//   enabled/scoped 값은 서버 맵에서만(위 moduleActiveMap/moduleScopedMap).
//   route 는 Phase 1 빈 골격(ComingSoon) → Phase 2~8 실화면으로 developer 가 교체.
const modules = [
  { key: 'APPROVAL', title: '승인 관리', iconId: 'i-admin-approval', route: '/AdminApproval', note: '' },
  // 일용직 계약서+승인제 T4: 일용직 입장 승인(활성=서버 moduleActiveMap.ENTRY, master∥hr).
  //   아이콘은 기존 스프라이트(i-admin-approval) 재사용 — template(스프라이트) 무변경.
  { key: 'ENTRY', title: '입장 승인', iconId: 'i-admin-approval', route: '/AdminEntryApproval', note: '' },
  // A8: 셀프가입(회원가입) 승인. 활성=서버 moduleActiveMap.SELF_JOIN(master∥hr∥노드관리자).
  //   아이콘은 기존 스프라이트(i-admin-approval) 재사용 — template(스프라이트) 무변경.
  {
    key: 'SELF_JOIN',
    title: '가입 승인',
    iconId: 'i-admin-approval',
    route: '/AdminSelfJoin',
    note: '',
  },
  { key: 'ATTD_DETAIL', title: '근태 상세', iconId: 'i-admin-attd', route: '/AdminAttdDetail', note: '' },
  { key: 'SAFETY', title: '안전 관리', iconId: 'i-admin-safety', route: '/AdminSafety', note: '' },
  { key: 'TBM', title: 'TBM 관리', iconId: 'i-admin-tbm', route: '/AdminTbm', note: '' },
  { key: 'SITE_OPS', title: '현장 처리', iconId: 'i-admin-siteops', route: '/AdminSiteOps', note: '' },
  { key: 'BOARD', title: '게시판', iconId: 'i-admin-board', route: '/AdminBoard', note: '' },
]

// 현장 전환 시트 토글 (UI 토글 — 허용 범위)
const siteSheetOpen = ref(false)

// 연차 변경 확인 대기(AGREED) 건수 — 진입 배너 노출용. 서버 스코프 판정 결과만 신뢰(빈 목록=0).
const pendingConfirmCount = ref(0)

// 모듈별 미처리 건수 배지 — { APPROVAL: n, ENTRY: n }.
//   홈에서 신규 요청 유무를 알 수 있게 한다(기존에는 화면에 직접 들어가야만 확인 가능).
//   ⚠️ C1: 건수도 서버 산출값만 신뢰한다. 조회 실패/비권한은 0(배지 비노출)이며 진입을 막지 않는다.
const moduleBadgeMap = ref({})

// 대시보드 컴포넌트 ref — 당겨서 새로고침 시 대시보드 재조회를 명시 호출하기 위함.
//   (현장 전환은 props.siteCd watch 로 자동 재조회되지만, 당겨서 새로고침은 siteCd 가
//    바뀌지 않으므로 expose 된 refresh() 를 직접 호출한다.)
const dashboardRef = ref(null)

// ── 당겨서 새로고침 (공통 컴포저블 usePullToRefresh) ───────────────────────────
//   스크롤 컨테이너(.admin-body) 최상단에서 아래로 당기면 access-context + 대시보드를 재조회.
//   access-context 는 진입 로딩(isLoading)을 켜지 않고 인디케이터만 쓰도록 silent 호출.
//   대시보드는 currentSiteCd 가 그대로라 watch 가 발화하지 않으므로 expose refresh 를 직접 호출.
const bodyEl = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(bodyEl, async () => {
  await Promise.all([
    loadAccessContext(undefined, { silent: true }),
    dashboardRef.value?.refresh?.(),
  ])
})

// ── 진입판정 조회 ─────────────────────────────────────────────────────────────
// access-context 조회. siteCd 지정 시 현장전환 재조회(D5 — 서버가 USE_YN='Y' 검증).
//   401/403 토큰 에러는 axios 인터셉터가 처리. 그 외 실패는 진입 차단 + 안내.
//   silent=true(당겨서 새로고침): 전체 로딩(isLoading)을 켜지 않아 본문 카드를 유지하고
//     자체 인디케이터만 노출한다(MainView showLoading=false 패턴).
const loadAccessContext = async (siteCd, { silent = false } = {}) => {
  if (!silent) isLoading.value = true
  try {
    const params = siteCd ? { siteCd } : {}
    const { data } = await api.get('/appApi/admin/access-context', { params })

    canEnterAdmin.value = data?.canEnterAdmin === true
    currentSiteCd.value = data?.currentSiteCd || ''
    siteSwitchEnabled.value = data?.siteSwitchEnabled === true
    accessibleSites.value = Array.isArray(data?.accessibleSites) ? data.accessibleSites : []
    // 서버 산출 맵만 신뢰(클라이언트 역할 분기 없음).
    moduleActiveMap.value = data?.moduleActiveMap || {}
    moduleScopedMap.value = data?.moduleScopedMap || {}

    // 현재 사업장명: accessibleSites 에서 currentSiteCd 매칭 → 폴백 토큰 gv_siteNm.
    const matched = accessibleSites.value.find((s) => s.siteCd === currentSiteCd.value)
    siteName.value = matched?.siteNm || sessionStorage.getItem('gv_siteNm') || ''

    // 진입 가능 관리자면 연차 변경 확인 대기(AGREED) 건수 조회(배너용). 비관리자는 서버가 빈 목록/403 → 0.
    if (canEnterAdmin.value) {
      loadPendingConfirms()
      // 모듈 배지는 moduleActiveMap 이 채워진 뒤에 조회한다(비활성 모듈 호출 스킵 판단에 필요).
      loadModuleBadges()
    } else {
      pendingConfirmCount.value = 0
      moduleBadgeMap.value = {}
    }
  } catch (e) {
    console.warn('[AdminLauncher] access-context 조회 실패:', e?.message)
    // F6/D8: 진입 불가로 처리(템플릿 empty 상태 노출). 서버가 최종 판정이므로 클라 가드는 UX 보조.
    canEnterAdmin.value = false
    await showAlert('관리자 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    if (!silent) isLoading.value = false
  }
}

// 모듈별 미처리 건수 조회(배지용).
//   기존 목록/카운트 엔드포인트가 이미 집계를 돌려주므로 전용 API 를 신설하지 않는다.
//     · 승인 관리: GET /appApi/admin/approval/pending → counts.ALL (pageSize=1 로 목록 부하 최소화)
//     · 입장 승인: GET /appApi/entryadmin01/pending-lists → totalCount
//     · 가입 승인: GET /appApi/admin/self-join/pending → 목록 길이
//     · 안전 관리: 위험성평가(001 평가요청) + 아차사고(100 접수) 합산 — 아래 SAFETY 분기 참조
//   ATTD_DETAIL(조회 전용, 근태결재는 승인 관리로 통합됨) · TBM · SITE_OPS(QR 스캔 액션) ·
//   BOARD(관리자 발행 자료실, 근로자 요청 큐 없음) 는 "근로자 요청 대응" 성격이 아니라서 배지 대상 제외.
//   비활성 모듈은 호출 자체를 건너뛴다(불필요한 403 방지). 실패는 0 으로 두고 조용히 넘어간다 —
//   배지는 보조 정보이므로 홈 진입을 막거나 알림을 띄우지 않는다.
const loadModuleBadges = async () => {
  const next = {}

  const tasks = []
  if (moduleActiveMap.value.APPROVAL === true) {
    tasks.push(
      api
        .get('/appApi/admin/approval/pending', { params: { group: 'ALL', page: 1, pageSize: 1 } })
        .then(({ data }) => {
          next.APPROVAL = Number(data?.counts?.ALL ?? data?.totalCount) || 0
        })
        .catch(() => {
          next.APPROVAL = 0
        }),
    )
  }
  if (moduleActiveMap.value.ENTRY === true) {
    tasks.push(
      api
        .get('/appApi/entryadmin01/pending-lists')
        .then(({ data }) => {
          next.ENTRY =
            Number(data?.totalCount) ||
            (Array.isArray(data?.pendingList) ? data.pendingList.length : 0)
        })
        .catch(() => {
          next.ENTRY = 0
        }),
    )
  }

  // A8: 셀프가입 승인 대기 건수. 대기 목록은 페이징이 없어 목록 길이가 곧 건수다
  //   (전용 count EP 를 신설하지 않는다).
  //   ⚠️ 한계: 대기 목록 EP 에는 부서 게이트가 있어 노드관리자가 nodeCd 없이 호출하면 403 이다.
  //      → 노드관리자의 배지는 0 으로 남는다(진입은 막지 않는다). 정확한 건수는 화면 대기 탭이 보여준다.
  //      v1 은 전사역할(master/hr)만 배지 정확으로 둔다(홈 진입 왕복을 늘리지 않기 위해).
  if (moduleActiveMap.value.SELF_JOIN === true) {
    tasks.push(
      api
        .get('/appApi/admin/self-join/pending', {
          params: currentSiteCd.value ? { siteCd: currentSiteCd.value } : {},
        })
        .then(({ data }) => {
          next.SELF_JOIN = Array.isArray(data?.selfJoinList) ? data.selfJoinList.length : 0
        })
        .catch(() => {
          next.SELF_JOIN = 0
        }),
    )
  }

  // 안전 관리 배지 = 위험성평가(001 평가요청, 관리자 첫 대응 대기) + 아차사고(100 접수, 조치 시작 전) 합산.
  //   순회점검 결과는 조회 전용이라 집계 대상이 아니다(AdminSafetyView 카드 3개 중 2개만 대응 필요 성격).
  //   ⚠️ 한계: 아차사고 /appApi/nearmiss/status-counts 는 siteCd 파라미터를 받지 않고 토큰 사업장(gv_siteCd)
  //      고정이라, 관리자 모드에서 현장을 전환해도 배지는 로그인 사업장 기준으로 남는다(위험성평가는
  //      siteCd 를 지원해 currentSiteCd 로 정확히 반영됨 — 근본 해결은 근태결재선통합류의 백엔드 대응 필요).
  if (moduleActiveMap.value.SAFETY === true) {
    tasks.push(
      Promise.all([
        api
          .get('/appApi/admin/safety/risk-findings', {
            params: {
              assessmentStatus: '001',
              ...(currentSiteCd.value ? { siteCd: currentSiteCd.value } : {}),
            },
          })
          .then(({ data }) => (Array.isArray(data?.findings) ? data.findings.length : 0))
          .catch(() => 0),
        api
          .get('/appApi/nearmiss/status-counts')
          .then(({ data }) => Number(data?.statusCount?.receivedCnt) || 0)
          .catch(() => 0),
      ]).then(([riskCnt, nearMissCnt]) => {
        next.SAFETY = riskCnt + nearMissCnt
      }),
    )
  }

  await Promise.all(tasks)
  moduleBadgeMap.value = next
}

// 연차 변경 확인 대기(AGREED) 건수 조회. 실패/비권한은 0(배너 미노출) — 진입 차단 금지.
const loadPendingConfirms = async () => {
  try {
    const { data } = await api.get('/appApi/leavechange/admin/pending-confirms')
    pendingConfirmCount.value = Array.isArray(data?.list) ? data.list.length : 0
  } catch (e) {
    // 비관리자(403)/조회 실패는 배너 미노출(0). 정상 흐름 영향 없음.
    pendingConfirmCount.value = 0
  }
}

// ── 핸들러 ────────────────────────────────────────────────────────────────────

// 연차 변경 확인 배너 → 확인 화면 진입.
const onConfirmBannerClick = () => {
  router.push('/AdminLeaveChangeConfirm')
}

// 사용자 모드 복귀(좌측 상단). 관리자→사용자 모드는 히스토리를 쌓지 않도록 replace 로 전환.
const onBackToUser = () => {
  router.replace('/MainView')
}

// 본문 섹션 선택 — enabled 섹션만 emit 됨(AdminModuleSection 이 disabled 차단).
//   Phase 1 = ComingSoon 으로 라우팅하며 query.module 로 모듈 키 전달(Phase 2~8 에서 실화면 교체).
const onModuleSelect = (module) => {
  if (!module?.route) return

  const query = { module: module.key }
  // A8: 셀프가입 승인 화면만 현장 전환 컨텍스트(siteCd)를 넘긴다.
  //   관리자 모드는 현장 전환 시 토큰을 재발급하지 않아 화면이 조회 사업장을 알아야 한다.
  //   ★전 모듈 공통으로 siteCd 를 붙이지 않은 이유: 기존 화면들이 query 를 어떻게 소비하는지에
  //     의존하지 않기 위해서다(회귀 위험 0 을 택했다). 서버는 값이 없으면 토큰 gv_siteCd 로 폴백한다.
  if (module.key === 'SELF_JOIN' && currentSiteCd.value) {
    query.siteCd = currentSiteCd.value
  }

  router.push({ path: module.route, query })
}

// 탭 key ↔ access-context 모듈 key 매핑(단순 식별자 매핑 — 역할 분기 아님, C1 무관).
//   AdminTabBar 의 tabs 정의(moduleKey)와 정합: 승인/근태/홈/안전/TBM.
const TAB_MODULE_KEY = {
  approval: 'APPROVAL',
  attd: 'ATTD_DETAIL',
  home: 'HOME',
  safety: 'SAFETY',
  tbm: 'TBM',
}

// 하단 탭 선택 — enabled 탭만 emit 됨(AdminTabBar 가 disabled 차단). 홈=대시보드(현 화면 유지).
//   부모에서도 moduleActiveMap 비활성 탭은 방어적으로 무동작(서버 맵만 신뢰, C1).
const onTabClick = (tabKey) => {
  // 홈 = 현 화면(대시보드)이므로 라우팅 없이 종료.
  if (tabKey === 'home') return

  const moduleKey = TAB_MODULE_KEY[tabKey]
  // 매핑되지 않은 탭 또는 서버 맵 비활성 탭은 무동작(방어).
  if (!moduleKey || moduleActiveMap.value[moduleKey] !== true) return

  // 001-Phase5: TBM 탭은 실화면(/AdminTbm)으로 라우팅. 나머지는 Phase 1 빈 골격(ComingSoon) 유지.
  if (moduleKey === 'TBM') {
    router.push({ path: '/AdminTbm' })
    return
  }

  // 001-Phase2: 승인 탭은 실화면(/AdminApproval)으로 라우팅.
  if (moduleKey === 'APPROVAL') {
    router.push({ path: '/AdminApproval' })
    return
  }

  // prafta-app-025 J1-5: 근태 탭은 실화면(/AdminAttdDetail)으로 라우팅.
  if (moduleKey === 'ATTD_DETAIL') {
    router.push({ path: '/AdminAttdDetail' })
    return
  }

  // prafta-app-025 J1-6: 안전 탭은 실화면(/AdminSafety 안전 허브)으로 라우팅.
  if (moduleKey === 'SAFETY') {
    router.push({ path: '/AdminSafety' })
    return
  }

  // Phase 1 = ComingSoon 으로 라우팅(query.module 로 모듈 키 전달, Phase 2~8 실화면 교체).
  router.push({ path: '/ComingSoon', query: { module: moduleKey } })
}

// 우상단 설정 진입 — prafta-app-025 J1-9(MVP): 관리자 "설정"=관리자 개인 환경설정(푸시 on/off 등)으로 정의.
//   기존 본인 푸시 알림 설정 화면(/PushSetting, prafta-app-021)을 재사용한다(USER_CD=JWT 본인 설정).
//   푸시 외 추가 설정 항목(기본 진입 사업장 등)이 필요하면 후속에서 AdminSettingsView 신설.
const onSettings = () => {
  router.push('/PushSetting')
}

// 현장 전환 트리거 → 시트 오픈
const onOpenSiteSwitch = () => {
  siteSheetOpen.value = true
}

// 현장 전환 시트에서 사업장 선택 → 시트 닫고 선택 사업장으로 재조회(D5 전파).
const onSelectSite = (site) => {
  siteSheetOpen.value = false
  if (!site?.siteCd || site.siteCd === currentSiteCd.value) return
  // 선택 사업장으로 access-context 재조회(D5 전파 — 서버가 USE_YN='Y' 검증).
  loadAccessContext(site.siteCd)
}

// 진입 시 1회 조회(토큰 사업장 기준).
onMounted(() => {
  loadAccessContext()
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(MainView .home-view 세트) — 자식 scoped 가 상속 */
.admin-launcher-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-disabled-bg: #f3f4f6;
  --color-disabled-text: #9ca3af;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  /* 앱 셸은 뷰포트 높이로 '고정'한다(min-height 가 아님). 그래야 내부 .admin-body 가
     실제 스크롤 컨테이너가 되고, 내용이 길어도 문서(body)로 스크롤이 새지 않는다
     (당겨서 새로고침의 scrollTop 판정 정확도 보장). dvh 미지원 환경은 vh 로 폴백. */
  height: 100vh;
  height: 100dvh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 본문 — 헤더(56)와 탭바(72) 사이, 탭바에 가려지지 않도록 하단 패딩(MainView .main 패턴) */
.admin-body {
  flex: 1;
  /* flex 자식의 기본 min-height:auto 는 내용이 길면 축소를 막아 overflow-y 스크롤이
     컨테이너 대신 문서로 새는 원인이 된다(스크롤 위치 오판 → 당겨서 새로고침 오작동).
     min-height:0 으로 .admin-body 를 실제 스크롤 컨테이너로 고정한다(MainView .main 동일). */
  min-height: 0;
  padding: var(--space-sm) var(--space-lg) 88px;
  overflow-y: auto;
}

/* 당겨서 새로고침 인디케이터 — 당김 거리에 따라 높이가 늘어났다 줄어든다(MainView 이식) */
.pull-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  height: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}
/* 손가락을 뗀 뒤(또는 새로고침 중)에는 부드럽게 높이 전환, 당기는 중에는 즉시 반응 */
.pull-refresh--animating {
  transition: height 0.2s ease;
}
.pull-refresh__text {
  padding: 8px 0;
}

.admin-loading,
.admin-empty {
  padding: 48px 16px;
  text-align: center;
  color: var(--color-text-secondary);
}
.admin-empty__title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-empty__desc {
  margin: 0;
  font-size: 13px;
}

/* 대시보드 (placeholder) */
.dashboard {
  margin-bottom: var(--space-lg);
}
.dashboard__title {
  margin: 0 0 var(--space-sm);
  padding: 0 var(--space-xs);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* 연차 변경 확인 대기 배너 — 근로자 동의 후 관리자 확정만 남은 건 알림(경고 톤). 탭하면 확인 화면. */
.confirm-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 14px;
  margin-bottom: var(--space-md);
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning-text);
  border-radius: var(--radius-md);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.confirm-banner__icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: var(--color-warning-text);
  color: var(--color-surface);
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
}
.confirm-banner__text {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--color-warning-text);
}
.confirm-banner__text strong {
  font-weight: 700;
}
.confirm-banner__cta {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-warning-text);
}

/* 모듈 세로 섹션 리스트 */
.module-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 스프라이트 */
.admin-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
