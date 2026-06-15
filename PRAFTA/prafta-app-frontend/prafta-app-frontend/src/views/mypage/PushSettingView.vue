<!--
  PushSettingView.vue — 푸시 알림 설정 (모바일 앱)
  - 작업 ID: PRAFTA-APP-021-5 (분해: .claude/requests/app_requests/prafta-app-021-plan.md)
  - UI 명세: UI-A021 (.claude/requests/app_requests/prafta-app-021-ui-spec.md)
  - 진입: MainView 우측 상단 아바타(onAvatarClick → router.push('/PushSetting')) — prafta-app-021 라우팅 스왑
  - planner 라운드 스코프(본 파일): template + scoped style + 상태/토글 정의 자리 (값은 비움)
  - developer 라운드 스코프(아래 TODO): 설정 조회(021-1 GET), 토글 저장(021-1 PUT),
    isAdmin 반영, 낙관적 토글/원복, 에러 폴백, 라우팅
  - 디자인 토큰: MyPageView(.my-page-view)와 동일 세트를 .push-setting-view 루트에 1회 선언. 하드코딩/Tailwind 금지.
  - 설계 메모: 마스터 OFF → 모든 타입 스위치 disabled + dimmed (UI 게이팅, 허용 범위).
    토글키(W1~M5)는 BE PushNotiTypeConst 와 1:1 일치해야 한다(1 토글 = 1+ NOTI_TYPE 은 BE 가 전개).
-->
<template>
  <div class="push-setting-view">
    <!-- 헤더 -->
    <header class="ps-hd">
      <button type="button" class="ps-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-ps-chev-left" />
        </svg>
      </button>
      <h1 class="ps-hd__title">알림 설정</h1>
      <span class="ps-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤 영역) -->
    <main class="ps-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="ps-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 마스터 스위치 -->
        <section class="ps-master">
          <div class="ps-master__text">
            <p class="ps-master__title">푸시 알림 받기</p>
            <p class="ps-master__desc">끄면 모든 알림을 받지 않아요.</p>
          </div>
          <button
            type="button"
            role="switch"
            class="ps-switch"
            :class="{ 'ps-switch--on': masterOn }"
            :aria-checked="masterOn ? 'true' : 'false'"
            aria-label="푸시 알림 전체"
            @click="onToggleMaster"
          >
            <span class="ps-switch__knob" aria-hidden="true"></span>
          </button>
        </section>

        <!-- 근로자 알림 -->
        <p class="ps-group-label">근로자 알림</p>
        <nav class="ps-menu" :class="{ 'ps-menu--disabled': !masterOn }">
          <div
            v-for="item in workerToggles"
            :key="item.key"
            class="ps-row"
            :class="{ 'ps-row--disabled': !masterOn, 'ps-row--readonly': item.savable === false }"
          >
            <div class="ps-row__text">
              <span class="ps-row__label">{{ item.label }}</span>
              <span v-if="item.desc" class="ps-row__desc">{{ item.desc }}</span>
            </div>
            <button
              type="button"
              role="switch"
              class="ps-switch"
              :class="{ 'ps-switch--on': item.on }"
              :aria-checked="item.on ? 'true' : 'false'"
              :aria-label="item.label"
              :disabled="!masterOn || item.savable === false"
              @click="onToggleType(item.key)"
            >
              <span class="ps-switch__knob" aria-hidden="true"></span>
            </button>
          </div>
        </nav>

        <!-- 관리자 알림 (isAdmin 일 때만) -->
        <template v-if="isAdmin">
          <p class="ps-group-label">관리자 알림</p>
          <nav class="ps-menu" :class="{ 'ps-menu--disabled': !masterOn }">
            <div
              v-for="item in adminToggles"
              :key="item.key"
              class="ps-row"
              :class="{ 'ps-row--disabled': !masterOn, 'ps-row--readonly': item.savable === false }"
            >
              <div class="ps-row__text">
                <span class="ps-row__label">{{ item.label }}</span>
                <span v-if="item.desc" class="ps-row__desc">{{ item.desc }}</span>
              </div>
              <button
                type="button"
                role="switch"
                class="ps-switch"
                :class="{ 'ps-switch--on': item.on }"
                :aria-checked="item.on ? 'true' : 'false'"
                :aria-label="item.label"
                :disabled="!masterOn || item.savable === false"
                @click="onToggleType(item.key)"
              >
                <span class="ps-switch__knob" aria-hidden="true"></span>
              </button>
            </div>
          </nav>
        </template>
      </template>
    </main>

    <!-- 인라인 SVG sprite (본 화면 전용) -->
    <svg width="0" height="0" class="ps-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-ps-chev-left"
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
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (앱 전역 $alert 우선) — MyPageView/MainView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ───────────────────────────────────────────────────────────
// 상태 (developer: GET /appApi/notiset01/settings 응답 주입 보완 필요)
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)

// 마스터 스위치 + 관리자 노출 플래그 (서버 응답으로 주입). 기본 opt-out → true 초기값.
const masterOn = ref(true)
const isAdmin = ref(false)

// 근로자 토글 정의 (고정 UI — key 는 BE PushNotiTypeConst 와 1:1 일치).
//   on 초기값은 opt-out 정책상 true(미설정=수신). 서버 응답(items.on)으로 덮어쓴다.
//   savable=false 항목(R1~R3)은 plan §8-R 7 읽기전용(ON 고정, 끌 수 없음): 노출하되 저장 거부.
const workerToggles = ref([
  { key: 'W1_LEAVE_RECALL', label: '연차 회수 알림', desc: '', on: true, savable: true },
  { key: 'W2_REQUEST_RESULT', label: '신청 처리 결과', desc: '승인·반려 결과를 알려드려요', on: true, savable: true },
  { key: 'W3_TBM', label: 'TBM 교육 알림', desc: '교육 시작·종료를 알려드려요', on: true, savable: true },
  { key: 'W4_CHECKIN_REMIND', label: '출근 시간 알림', desc: '출근 5분 전', on: true, savable: true },
  { key: 'W5_CHECKOUT_REMIND', label: '퇴근 시간 알림', desc: '퇴근 5분 전', on: true, savable: true },
  // 읽기전용 안내 항목(끌 수 없음, ON 고정). 서버도 화이트리스트로 저장 거부한다.
  { key: 'R1_REFUSAL_NOTICE', label: '노무수령거부 통지', desc: '항상 받아요', on: true, savable: false },
  { key: 'R2_LEAVE_CHANGE_CONFIRMED', label: '연차 변경 확정 결과', desc: '항상 받아요', on: true, savable: false },
  { key: 'R3_LEAVE_CHANGE_REJECTED', label: '연차 변경 반려 결과', desc: '항상 받아요', on: true, savable: false },
])

// 관리자 토글 정의 (isAdmin 일 때만 노출 + 저장).
const adminToggles = ref([
  { key: 'M1_LATE_EARLY', label: '직원 지각·조기퇴근 알림', desc: '', on: true, savable: true },
  { key: 'M2_APPROVAL', label: '결재 요청 알림', desc: '근태·초과근무·연차', on: true, savable: true },
  { key: 'M3_REFUSAL_CHECKIN', label: '노무수령거부일 출근 감지', desc: '', on: true, savable: true },
  { key: 'M4_NEAR_MISS', label: '아차사고 보고 알림', desc: '', on: true, savable: true },
  { key: 'M5_RISK_REQUEST', label: '위험성평가 요청 알림', desc: '', on: true, savable: true },
])

// ───────────────────────────────────────────────────────────
// 라우팅 (UI 이동 — 허용)
// ───────────────────────────────────────────────────────────
const onBack = () => {
  router.push('/MainView')
}

// ───────────────────────────────────────────────────────────
// 저장 직렬화 가드 (낙관적 토글 + PUT). 동시 PUT 경합을 막기 위해 진행 중 플래그 유지.
// ───────────────────────────────────────────────────────────
const isSaving = ref(false)

// 토글키 → ref 항목 조회(worker/admin 양쪽 탐색). 없으면 null.
const findToggle = (key) => {
  return (
    workerToggles.value.find((t) => t.key === key) ||
    adminToggles.value.find((t) => t.key === key) ||
    null
  )
}

// PUT 저장 공통기. body = { masterOn, items }. 실패 시 false 반환(호출부가 원복).
const putSettings = async (body) => {
  if (isSaving.value) return false
  isSaving.value = true
  try {
    // 응답으로 최신 설정을 받아 동기화(서버가 화이트리스트로 거부한 항목까지 정합화).
    const { data } = await api.put('/appApi/notiset01/settings', body)
    applyServerSettings(data)
    return true
  } catch (e) {
    console.warn('[PushSetting] 설정 저장 실패:', e?.message)
    return false
  } finally {
    isSaving.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 토글 조작 (낙관적 토글 + PUT 저장, 실패 시 원복).
// ───────────────────────────────────────────────────────────
const onToggleMaster = async () => {
  const prev = masterOn.value
  // 낙관적 토글.
  masterOn.value = !prev
  const ok = await putSettings({ masterOn: masterOn.value })
  if (!ok) {
    masterOn.value = prev
    showAlert('설정을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

const onToggleType = async (key) => {
  // 마스터 OFF면 개별 토글 비활성(template :disabled 로도 차단되나 가드 1중 더).
  if (!masterOn.value) return
  const item = findToggle(key)
  if (!item) return
  // 읽기전용 항목(R*)은 끌 수 없음 — 클릭 무시(ON 고정 유지, 서버도 거부).
  if (item.savable === false) return

  const prev = item.on
  // 낙관적 토글.
  item.on = !prev
  const ok = await putSettings({ items: [{ toggleKey: key, on: item.on }] })
  if (!ok) {
    item.on = prev
    showAlert('설정을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

// ───────────────────────────────────────────────────────────
// 서버 응답 → 화면 상태 반영 (GET/PUT 공통). 응답 형태:
//   { masterOn, isAdmin, items: [{ toggleKey, group, on, savable }] }
// ───────────────────────────────────────────────────────────
const applyServerSettings = (data) => {
  if (!data) return
  masterOn.value = data.masterOn !== false
  isAdmin.value = data.isAdmin === true

  const items = Array.isArray(data.items) ? data.items : []
  // 서버가 내려준 토글만 on/savable 을 덮어쓴다(미관리자는 관리자 토글 미포함 → 기존값 유지하나 미노출).
  items.forEach((srv) => {
    const item = findToggle(srv.toggleKey)
    if (!item) return
    item.on = srv.on !== false
    if (typeof srv.savable === 'boolean') item.savable = srv.savable
  })
}

// ───────────────────────────────────────────────────────────
// 진입 시 1회 조회 (설정 + isAdmin). 401/403 은 axios 인터셉터 처리.
//   실패 시 기본값(전부 ON, 비관리자) 폴백 + 안내.
// ───────────────────────────────────────────────────────────
const loadSettings = async () => {
  try {
    const { data } = await api.get('/appApi/notiset01/settings')
    applyServerSettings(data)
  } catch (e) {
    console.warn('[PushSetting] 설정 조회 실패:', e?.message)
    // 폴백: opt-out 기본(전부 ON) 유지. 관리자 섹션은 미노출(isAdmin=false).
    masterOn.value = true
    isAdmin.value = false
    showAlert('설정을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadSettings)
</script>

<style scoped>
/*
 * 디자인 토큰 — MyPageView(.my-page-view)와 동일 세트를 본 화면 루트에 1회 선언.
 * 자식은 var(--...) 상속. 하드코딩/Tailwind 금지.
 */
.push-setting-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.ps-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
}
.ps-hd__back {
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
.ps-hd__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ps-hd__spacer {
  width: 44px;
}

/* 본문 */
.ps-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-sm) var(--space-lg) 40px;
}
.ps-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 마스터 스위치 카드 */
.ps-master {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--space-md);
}
.ps-master__text {
  min-width: 0;
}
.ps-master__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ps-master__desc {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 그룹 라벨 */
.ps-group-label {
  margin: var(--space-lg) 0 var(--space-sm) var(--space-xs);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 메뉴 카드 */
.ps-menu {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.ps-menu--disabled {
  opacity: 0.55;
}

/* 토글 행 */
.ps-row {
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.ps-row:last-child {
  border-bottom: 0;
}
/*
 * 읽기전용 행(R1~R3): plan §8-R 7 — "수정 안되더라도 목록 표시". ON 고정·끌 수 없음을
 * 시각적으로 잠금(스위치 dimmed·기본 커서)으로 표현해 일반 토글과 구분한다. 마스터 ON/OFF 무관.
 */
.ps-row--readonly .ps-switch {
  opacity: 0.45;
  cursor: default;
}
.ps-row--readonly .ps-row__label {
  color: var(--color-text-secondary);
}
.ps-row__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.ps-row__label {
  font-size: 15px;
  color: var(--color-text-primary);
}
.ps-row__desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 스위치 (role=switch 버튼) */
.ps-switch {
  position: relative;
  flex-shrink: 0;
  width: 48px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--color-switch-off);
  cursor: pointer;
  transition: background 0.18s ease;
}
.ps-switch--on {
  background: var(--color-primary);
}
.ps-switch:disabled {
  cursor: default;
}
.ps-switch__knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
  transition: transform 0.18s ease;
}
.ps-switch--on .ps-switch__knob {
  transform: translateX(20px);
}

.ps-sprite {
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
