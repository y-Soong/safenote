<!--
  TbmHubView.vue — TBM 허브 (3탭 컨테이너: 참석가능/교육중/교육완료)
  - 작업 ID: PRAFTA-TBM-HUB (분해: .claude/requests/app_requests/prafta-app-tbm-user-detail-plan.md §5)
  - UI 명세: UI-TBM-HUB
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만.
  - developer 라운드 스코프(아래 TODO): 탭 전환에 따른 리스트 조회 위임은 자식이 담당,
    여기서는 카드 선택 → 단계 라우팅(/TbmBeforeStart, /TbmInProgress, /TbmCompletedDetail)만 처리.
  - 디자인 토큰: TbmEntryView(.tbm-entry-view)와 동일 세트를 .tbm-hub-view 루트에 1회 선언.
    자식 컴포넌트(scoped)는 var(--...) 상속. 하드코딩 색상/픽셀 금지.
  - 진입: MainView › TbmAttendCard › `>` → router.push('/TbmHub') (보호 라우트, beforeEach 토큰 게이트).
-->
<template>
  <div class="tbm-hub-view">
    <!-- 헤더 -->
    <header class="tbm-hd">
      <button type="button" class="tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-tbm-chev-left" />
        </svg>
      </button>
      <h1 class="tbm-hd__title">TBM</h1>
      <span class="tbm-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 탭바 -->
    <nav class="tbm-tabs" role="tablist" aria-label="TBM 목록 탭">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        class="tbm-tabs__btn"
        :class="{ 'is-active': activeTab === t.key }"
        role="tab"
        :aria-selected="activeTab === t.key"
        @click="activeTab = t.key"
      >
        {{ t.label }}
      </button>
    </nav>

    <!-- 본문: 선택된 탭의 리스트 -->
    <main class="tbm-hub-body">
      <TbmAvailableList
        v-if="activeTab === 'AVAILABLE'"
        @select="onSelectAvailable"
      />
      <TbmInProgressList
        v-else-if="activeTab === 'IN_PROGRESS'"
        @select="onSelectInProgress"
      />
      <TbmCompletedList
        v-else
        @select="onSelectCompleted"
      />
    </main>

    <!-- 입실 비번 시트 (참석가능/교육중 카드 선택 시 오픈 → enter API) -->
    <TbmEntryPwdSheet
      v-model="entrySheetOpen"
      :title="selectedSession.title"
      :submitting="entrySubmitting"
      :error-msg="entryError"
      @submit="onEntrySubmit"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-tbm-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { requestGps } from '@/utils/gpsBridge'

import TbmAvailableList from './components/TbmAvailableList.vue'
import TbmInProgressList from './components/TbmInProgressList.vue'
import TbmCompletedList from './components/TbmCompletedList.vue'
import TbmEntryPwdSheet from './components/TbmEntryPwdSheet.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선, 없으면 window.alert) — MainView/TbmEntryView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 탭 정의(고정). AVAILABLE=참석가능(OPENED), IN_PROGRESS=교육중(내 출결 있는 IN_PROGRESS), COMPLETED=교육완료(내 이력)
const tabs = [
  { key: 'AVAILABLE', label: '참석가능' },
  { key: 'IN_PROGRESS', label: '교육중' },
  { key: 'COMPLETED', label: '교육완료' },
]
const activeTab = ref('AVAILABLE')

// 입실 비번 시트 상태 + 선택 세션 컨텍스트
const entrySheetOpen = ref(false)
const entrySubmitting = ref(false)
const entryError = ref('')
const selectedSession = ref({})
// 입실 성공 후 이동 목적지('/TbmBeforeStart' | '/TbmInProgress')
const entryTargetPath = ref('')

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/MainView')
}

// 참석가능 카드 선택 → 입실 비번 시트 오픈(성공 시 /TbmBeforeStart 이동)
const onSelectAvailable = (session) => {
  openEntrySheet(session, '/TbmBeforeStart')
}

// 교육중 카드 선택 → 입실 비번 재인증(enter 멱등). 성공 시 /TbmInProgress 이동
const onSelectInProgress = (session) => {
  openEntrySheet(session, '/TbmInProgress')
}

// 교육완료 카드 선택 → /TbmCompletedDetail 이동
const onSelectCompleted = (session) => {
  if (!session?.sessionCd) return
  router.push({ path: '/TbmCompletedDetail', query: { sessionCd: session.sessionCd } })
}

// 입실 비번 시트 오픈 공통(참석가능/교육중 공용)
const openEntrySheet = (session, targetPath) => {
  if (!session?.sessionCd) return
  selectedSession.value = session
  entryTargetPath.value = targetPath
  entryError.value = ''
  entrySheetOpen.value = true
}

// 시트 submit({ entryPwd }) → (GPS 유형 확인) → 좌표 획득 → POST /appApi/tbm/enter
//   성공(enter 멱등: 이미 입실이어도 진행) 시 목적지로 query 동반 이동.
//   GPS 정책: 세션의 gpsVerifyTypeCd 가 'AUTO' 일 때만 GPS 필수(측위 실패 시 차단).
//     그 외(DISABLED/MANUAL 등)는 GPS 비요구 → 좌표 없이도 입실 진행(좌표는 best-effort).
//     세션 리스트에는 GPS 유형이 없어 입실 컨텍스트(entry-context)로 조회한다.
//     (DISABLED vs OFF 등 코드 불일치에 안전: 'AUTO' 가 아니면 전부 비요구 처리.)
const onEntrySubmit = async ({ entryPwd }) => {
  if (entrySubmitting.value) return
  const sessionCd = selectedSession.value.sessionCd
  if (!sessionCd) return
  entryError.value = ''

  entrySubmitting.value = true
  try {
    // 1) 세션 GPS 검증 유형 확인. 조회 실패해도 입실은 시도(서버가 최종 판정).
    let gpsVerifyType = ''
    try {
      const ctx = await api.get('/appApi/tbm/entry-context', { params: { sessionCd } })
      gpsVerifyType = ctx?.data?.gpsVerifyTypeCd || ''
    } catch (e) {
      console.warn('[TbmHub] entry-context 조회 실패(무시, 입실 계속):', e?.message)
    }
    const gpsRequired = gpsVerifyType === 'AUTO'

    // 2) 좌표 획득. AUTO 면 측위/권한 실패 시 차단, 그 외엔 좌표 없이 진행.
    let lat = null
    let lon = null
    const gps = await requestGps()
    if (gps.status === 'OK') {
      // AUTO 에서 Mock 위치는 차단(부정확/위변조 입실 방지). 비요구 세션은 좌표 자체를 안 보냄.
      if (gpsRequired && gps.isMocked) {
        showAlert('위치 위변조가 감지되어 입실할 수 없어요.')
        return
      }
      if (!gps.isMocked) {
        lat = gps.lat
        lon = gps.lon
      }
    } else if (gpsRequired) {
      // AUTO 인데 측위 실패 → 차단(서버도 좌표 없으면 거부).
      if (gps.status === 'PERMISSION_DENIED' || gps.status === 'SERVICE_DISABLED') {
        showAlert('위치 권한 또는 위치 서비스가 꺼져 있어요. 설정에서 위치를 허용해 주세요.')
      } else {
        // TIMEOUT / BRIDGE_UNAVAILABLE 등 측위 실패.
        showAlert('현재 위치를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.')
      }
      return
    }
    // 비요구(DISABLED/MANUAL) + 측위 실패 → 좌표(null) 없이 입실 진행.

    // 3) 입실 호출(좌표는 있으면 동봉, 없으면 null).
    await api.post('/appApi/tbm/enter', {
      sessionCd,
      entryPwd,
      lat,
      lon,
    })
    // enter 멱등: 이미 입실이어도 정상 진행. 목적지로 세션 동반 이동.
    // title/managerUserNm 은 표시 보조용(별도 세션 메타 endpoint 없음) — 선택 카드에서 전달.
    entrySheetOpen.value = false
    router.push({
      path: entryTargetPath.value,
      query: {
        sessionCd,
        title: selectedSession.value.title || '',
        managerUserNm: selectedSession.value.managerUserNm || '',
      },
    })
  } catch (e) {
    console.error('[TbmHub] enter 실패:', e?.message)
    // 비번 불일치/잠금/상태/거리초과 등은 서버 메시지를 시트 인라인으로 표시.
    entryError.value = e?.response?.data?.message || '입실하지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    entrySubmitting.value = false
  }
}
</script>

<style scoped>
/* 디자인 토큰 1회 선언(TbmEntryView 세트와 동일) — 자식 scoped 가 상속 */
.tbm-hub-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
}

/* 헤더 */
.tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.tbm-hd__back {
  width: 36px;
  height: 36px;
  margin-left: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.tbm-hd__spacer {
  width: 36px;
}

/* 탭바 */
.tbm-tabs {
  display: flex;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.tbm-tabs__btn {
  flex: 1;
  height: 44px;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.tbm-tabs__btn.is-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}

/* 본문 */
.tbm-hub-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
}

/* 스프라이트 */
.tbm-sprite {
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
