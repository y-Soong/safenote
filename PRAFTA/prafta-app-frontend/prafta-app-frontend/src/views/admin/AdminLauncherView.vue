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
    <main class="admin-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="admin-loading" aria-live="polite">불러오는 중...</div>

      <!-- 권한 없음(직접 진입 등 방어 — 정상 흐름은 진입점에서 게이팅) -->
      <div v-else-if="!canEnterAdmin" class="admin-empty" aria-live="polite">
        <p class="admin-empty__title">관리자 권한이 없습니다</p>
        <p class="admin-empty__desc">관리자 모드를 사용할 수 없는 계정입니다.</p>
      </div>

      <template v-else>
        <!-- 1) 대시보드 (최상단, Phase 1=placeholder 영역만) -->
        <section class="dashboard">
          <h2 class="dashboard__title">대시보드</h2>
          <div class="dashboard__placeholder" aria-live="polite">
            <p class="dashboard__ph-text">실시간 근태 / 안전점검 / 위험성 발굴</p>
            <p class="dashboard__ph-sub">준비 중입니다</p>
          </div>
        </section>

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

import AdminHeader from './components/AdminHeader.vue'
import AdminTabBar from './components/AdminTabBar.vue'
import AdminModuleSection from './components/AdminModuleSection.vue'
import AdminSiteSwitchSheet from './components/AdminSiteSwitchSheet.vue'

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
  { key: 'ATTD_DETAIL', title: '근태 상세', iconId: 'i-admin-attd', route: '/ComingSoon', note: '' },
  { key: 'SAFETY', title: '안전 관리', iconId: 'i-admin-safety', route: '/ComingSoon', note: '' },
  { key: 'TBM', title: 'TBM 관리', iconId: 'i-admin-tbm', route: '/AdminTbm', note: '' },
  { key: 'SITE_OPS', title: '현장 처리', iconId: 'i-admin-siteops', route: '/ComingSoon', note: '' },
  { key: 'BOARD', title: '게시판', iconId: 'i-admin-board', route: '/ComingSoon', note: '준비중' },
]

// 현장 전환 시트 토글 (UI 토글 — 허용 범위)
const siteSheetOpen = ref(false)

// ── 진입판정 조회 ─────────────────────────────────────────────────────────────
// access-context 조회. siteCd 지정 시 현장전환 재조회(D5 — 서버가 USE_YN='Y' 검증).
//   401/403 토큰 에러는 axios 인터셉터가 처리. 그 외 실패는 진입 차단 + 안내.
const loadAccessContext = async (siteCd) => {
  isLoading.value = true
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
  } catch (e) {
    console.warn('[AdminLauncher] access-context 조회 실패:', e?.message)
    // F6/D8: 진입 불가로 처리(템플릿 empty 상태 노출). 서버가 최종 판정이므로 클라 가드는 UX 보조.
    canEnterAdmin.value = false
    await showAlert('관리자 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
}

// ── 핸들러 ────────────────────────────────────────────────────────────────────

// 사용자 모드 복귀(좌측 상단). 관리자→사용자 모드는 히스토리를 쌓지 않도록 replace 로 전환.
const onBackToUser = () => {
  router.replace('/MainView')
}

// 본문 섹션 선택 — enabled 섹션만 emit 됨(AdminModuleSection 이 disabled 차단).
//   Phase 1 = ComingSoon 으로 라우팅하며 query.module 로 모듈 키 전달(Phase 2~8 에서 실화면 교체).
const onModuleSelect = (module) => {
  if (!module?.route) return
  router.push({ path: module.route, query: { module: module.key } })
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

  // Phase 1 = ComingSoon 으로 라우팅(query.module 로 모듈 키 전달, Phase 2~8 실화면 교체).
  router.push({ path: '/ComingSoon', query: { module: moduleKey } })
}

// 우상단 설정 진입. Phase 1 = ComingSoon(개인 알림/푸시 on/off 등 추후).
const onSettings = () => {
  router.push({ path: '/ComingSoon', query: { module: 'SETTINGS' } })
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

  min-height: 100%;
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
  padding: var(--space-sm) var(--space-lg) 88px;
  overflow-y: auto;
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
.dashboard__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 120px;
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}
.dashboard__ph-text {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.dashboard__ph-sub {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
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
