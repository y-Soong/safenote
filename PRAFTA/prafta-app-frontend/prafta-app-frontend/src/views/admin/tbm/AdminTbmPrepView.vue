<!--
  AdminTbmPrepView.vue — 관리자 TBM 교육준비 화면 (OPENED)
  - 작업 ID: 051-03 (분해: 051-admin-tbm-statemachine-plan.md §1/§2/§4 UI-051-01)
  - 진입: 세션 상세(DRAFT)에서 "교육준비 시작"(/prepare) 성공 → /AdminTbmPrep?sessionCd=...
          또는 OPENED 세션 → 진입.
  - 구성: 입실비번 카드(entry-only) / 15분 자동 교육시작 카운트다운(+수동연장/지금시작)
          / 입실 근로자 리스트(거리·위치 + 이탈자 내보내기, D-3) / 추가 입실(정규직 대리·일용직 QR, D-4).
  - 백엔드:
      GET  /appApi/admin/tbm/sessions/{sessionCd}                         (상세 — prepStartAt/prepAutoStartAt/prepRemainSec/entryPwd)
      GET  /appApi/admin/tbm/sessions/{sessionCd}/attendees?phase=PREP     (입실자+거리/위치, E12)
      POST .../{sessionCd}/extend-prep                                     (수동연장, E3)
      POST .../{sessionCd}/start                                          (지금 교육시작, E4)
      POST .../{sessionCd}/regenerate-entry-password                       (입실비번 재발급, E6)
      POST .../{sessionCd}/attendees/{attendanceCd}/cancel-entry           (이탈자 내보내기=입실취소, E13)
      GET  .../{sessionCd}/eligible-regulars?keyword=                      (정규직 검색, E9)
      POST .../{sessionCd}/attendees/manager-direct  { userCd }            (정규직 대리입실, E10)
      POST .../{sessionCd}/attendees/qr-scan         { qrPayload }         (일용직 QR 입실, E11)
  - Flutter 브리지: SCAN_QR (qr_scan_page → web_app → window.flutter_inappwebview.callHandler) — §6.
  - 디자인 토큰: AdminTbmLiveView 세트를 .admin-tbm-prep-view 루트에 1회 선언(자식 상속).
  - C1: 권한/스코프/상태는 서버만 신뢰. 클라 역할 분기 없음. CSS 변수만, <style scoped>, TS 미사용.
  - developer R-A: loadDetail(E8 statusCd 분기 라우팅) / onExtend(E3) / onStartNow(E4) / onRegenerateEntry(E6) / 카운트다운.
  - developer R-B: 대리입실(E9/E10, onConfirmDirectEntry + DirectEntrySheet 검색).
  - developer R-C: loadAttendees(E12 phase=PREP 거리/좌표) / onOpenCancelEntry(E13 입실취소=물리삭제, 재입실 가능).
  - developer R-D: onScanQr(Flutter SCAN_QR 브리지 → qrPayload 가공 없이 E11 POST → loadAttendees).
      브리지 미존재(웹/dev) 폴백 안내. 파싱/식별/유효성은 백엔드 몫(역할 분담).
-->
<template>
  <div class="admin-tbm-prep-view">
    <!-- 헤더 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-prep-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">TBM 교육준비</h1>
      <button type="button" class="admin-tbm-hd__refresh" aria-label="새로고침" @click="onRefresh">
        <svg class="icon" width="20" height="20" aria-hidden="true">
          <use href="#i-admin-tbm-prep-refresh" />
        </svg>
      </button>
    </header>

    <main
      class="admin-tbm-prep-body"
      ref="scrollRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <p v-if="isLoading" class="admin-tbm-state">불러오는 중…</p>

      <div v-else-if="loadError" class="admin-tbm-state">
        <p class="admin-tbm-state__msg">정보를 불러오지 못했어요.</p>
        <button type="button" class="admin-tbm-retry" @click="onRetry">다시 시도</button>
      </div>

      <template v-else-if="session">
        <!-- 제목 + 교육준비 배지 -->
        <div class="admin-tbm-prep__head">
          <span class="admin-tbm-prep__status">교육준비</span>
          <h2 class="admin-tbm-prep__title">{{ session.title || 'TBM 세션' }}</h2>
        </div>

        <!-- 입실 비밀번호(entry-only) -->
        <AdminTbmPwdCard
          mode="ENTRY"
          :entry-pwd="session.entryPwd"
          :can-regenerate="true"
          :regenerating="regenEntry"
          @regenerate="onRegenerateEntry"
        />

        <!-- 자동 교육시작 카운트다운 -->
        <section class="card">
          <p class="card__label">자동 교육시작</p>
          <p class="admin-tbm-prep__countdown">
            <template v-if="countdownText">{{ countdownText }} 후 자동 시작</template>
            <template v-else>곧 교육이 시작돼요…</template>
          </p>
          <div class="admin-tbm-prep__timer-actions">
            <button type="button" class="btn btn--ghost" :disabled="busy" @click="onExtend">
              수동 연장
            </button>
            <button type="button" class="btn btn--primary" :disabled="busy" @click="onStartNow">
              지금 교육시작
            </button>
          </div>
        </section>

        <!-- 입실 근로자 리스트 (거리/위치 + 이탈자 내보내기) -->
        <section class="card">
          <div class="card__head">
            <p class="card__label">입실 근로자</p>
            <span class="admin-tbm-prep__count">{{ attendees.length }}명</span>
          </div>

          <p v-if="attendeesLoading" class="admin-tbm-state admin-tbm-state--sm">불러오는 중…</p>
          <p v-else-if="!attendees.length" class="admin-tbm-state admin-tbm-state--sm">
            아직 입실한 근로자가 없어요
          </p>
          <ul v-else class="admin-tbm-prep__list">
            <li v-for="a in attendees" :key="a.attendanceCd">
              <AdminTbmAttendeeRow
                :attendee="a"
                variant="PREP"
                :radius-m="session.gpsVerifyRadiusM"
                @cancel-entry="onOpenCancelEntry"
              />
            </li>
          </ul>
        </section>

        <!-- 추가 입실 -->
        <section class="card">
          <p class="card__label">추가 입실</p>
          <div class="admin-tbm-prep__add-actions">
            <button type="button" class="btn btn--ghost" @click="onOpenDirectEntry">
              정규직 대리입실
            </button>
            <button type="button" class="btn btn--ghost" :disabled="scanning" @click="onScanQr">
              일용직 QR 입실
            </button>
          </div>
          <p class="admin-tbm-prep__hint">
            휴대전화 사용이 어려운 정규직은 검색해 대리입실, 일용직은 QR 코드를 스캔해 입실 처리해요.
          </p>
        </section>
      </template>
    </main>

    <!-- 정규직 대리입실 시트 -->
    <AdminTbmDirectEntrySheet
      :open="directEntryOpen"
      :session-cd="sessionCd"
      :submitting="directEntrySubmitting"
      @close="onCloseDirectEntry"
      @confirm="onConfirmDirectEntry"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-prep-chev-left"
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
          id="i-admin-tbm-prep-refresh"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="23 4 23 10 17 10" />
          <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { isKnownMissing } from '@/utils/shellCapability'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import AdminTbmPwdCard from './components/AdminTbmPwdCard.vue'
import AdminTbmAttendeeRow from './components/AdminTbmAttendeeRow.vue'
import AdminTbmDirectEntrySheet from './components/AdminTbmDirectEntrySheet.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 우선) — LiveView/SessionDetailView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ── 상태 ──────────────────────────────────────────────────────────
const sessionCd = computed(() => route.query.sessionCd || '')
const isLoading = ref(false)
const loadError = ref(false)
const attendeesLoading = ref(false)
const busy = ref(false) // 연장/시작 가드
const regenEntry = ref(false)
const scanning = ref(false)

const session = ref(null) // 상세(서버 SessionDetailItem + prepStartAt/prepAutoStartAt)
const attendees = ref([]) // [{ attendanceCd, userNm, userTypeCd, deptNm, entryAt, distanceM, ... }]

// 카운트다운 표시(서버 산출 남은 초 prepRemainSec 기준 — 클라는 표시만, 기기 시계 비의존)
const countdownText = ref('') // 'mm:ss' 또는 ''
let countdownTimer = null

// 대리입실 시트
const directEntryOpen = ref(false)
const directEntrySubmitting = ref(false)

// ── 조회 ──────────────────────────────────────────────────────────
// 세션 상세 로드(E8). statusCd 검증 후 OPENED 가 아니면 상태별 분기 라우팅:
//   DRAFT→back / IN_PROGRESS→/AdminTbmLive replace / COMPLETED→/AdminTbmCompleted replace.
//   서버가 지연평가(D-1)로 자동전이했을 수 있으므로 응답 statusCd 로 라우팅 판단.
//   응답의 prepRemainSec(서버 산출 남은 초)으로 카운트다운 시작·재시작(연장 후 재조회 포함).
const loadDetail = async () => {
  if (!sessionCd.value) {
    loadError.value = true
    return
  }
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}`,
    )
    const detail = data?.session || null
    if (!detail) {
      loadError.value = true
      return
    }

    // statusCd 분기: 교육준비 화면은 OPENED 전용. 그 외 상태는 적절한 화면으로 이동.
    const statusCd = detail.statusCd
    if (statusCd === 'IN_PROGRESS') {
      stopCountdown()
      router.replace({ path: '/AdminTbmLive', query: { sessionCd: sessionCd.value } })
      return
    }
    if (statusCd === 'COMPLETED') {
      stopCountdown()
      router.replace({ path: '/AdminTbmCompleted', query: { sessionCd: sessionCd.value } })
      return
    }
    if (statusCd !== 'OPENED') {
      // DRAFT/CANCELLED 등: 교육준비 화면 대상이 아님 → 이전 화면 복귀.
      stopCountdown()
      router.back()
      return
    }

    session.value = detail
    startCountdown()
  } catch (e) {
    console.error('[AdminTbmPrepView] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// 입실자 로드(E12, phase=PREP) — 입실 거리(distanceM)/좌표 포함. 거리 배지/이탈자 판단은 AttendeeRow 가 수행.
// 비치명적: 실패해도 화면 전체를 막지 않고 빈 리스트로 둔다(상세는 loadDetail 이 관장).
const loadAttendees = async () => {
  if (!sessionCd.value) return
  attendeesLoading.value = true
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/attendees`,
      { params: { phase: 'PREP' } },
    )
    attendees.value = Array.isArray(data?.attendees) ? data.attendees : []
  } catch (e) {
    console.error('[AdminTbmPrepView] 입실자 조회 실패:', e?.message)
    attendees.value = []
  } finally {
    attendeesLoading.value = false
  }
}

const onRetry = () => {
  loadDetail()
  loadAttendees()
}
const onRefresh = () => {
  loadDetail()
  loadAttendees()
}

// 화면 진입 후 흐른 시간 측정용 단조 시계(monotonic). 기기 시계 변경/NTP 보정에 영향받지 않는다.
// performance.now() 미지원 환경(구형 웹뷰)만 Date.now() 로 폴백.
const monotonicNow = () =>
  typeof performance !== 'undefined' && typeof performance.now === 'function'
    ? performance.now()
    : Date.now()

// 카운트다운: 서버가 산출한 "남은 초"(prepRemainSec)를 기준값으로 잡고, 경과시간만큼 빼서 표시한다.
// 도달(0) 시 자동전이 감지 위해 상세 재조회.
// (표시·만료감지만 — 실제 전이는 서버 지연평가. 클라는 비즈로직 금지.)
//
// ★기기 시계에 의존하지 않는다: 종전에는 서버가 준 절대시각에서 이 기기의 Date.now() 를 뺐는데,
//   기기 시계가 서버보다 느리면 상한(15:00)을 넘는 값(예: 15:02)이 표시되고 폰·PC 숫자가 서로
//   어긋났다(08-30 실측: PC 약 1.3초, 폰은 그 이상 느림). 이제 서버가 0 으로 클램프한 남은 초를
//   내려주고 클라는 거기서 경과분만 감산하므로 15:00 을 넘길 수 없다.
// ★과거 사고 이력: 08-23 즉시만료(타임존 표기 없는 벽시계를 로컬 파싱) / 08-30 555분 밀림
//   ("DB=UTC" 가정으로 벽시계에 Z 부착) / 이번 15:02(기기 시계 의존). 클라에서 타임존 보정이나
//   절대시각 재계산을 다시 도입하지 말 것.
const startCountdown = () => {
  stopCountdown()

  // 기준값: 서버 산출 남은 초. 숫자가 아니면(구버전 서버 응답 등) 종전 절대시각 방식으로 폴백.
  let baseRemainSec = null
  const serverRemain = session.value?.prepRemainSec
  if (typeof serverRemain === 'number' && Number.isFinite(serverRemain)) {
    baseRemainSec = Math.max(0, Math.floor(serverRemain))
  } else {
    // [폴백 경로] prepRemainSec 미수신 시에만 사용 — 기기 시계 의존(상한 초과 표시 가능).
    const target = session.value?.prepAutoStartAt
    const targetMs = target ? new Date(String(target).replace(' ', 'T')).getTime() : NaN
    if (Number.isFinite(targetMs)) {
      baseRemainSec = Math.max(0, Math.floor((targetMs - Date.now()) / 1000))
    }
  }

  if (baseRemainSec === null) {
    countdownText.value = ''
    return
  }

  const anchorMs = monotonicNow()
  const tick = () => {
    const elapsedSec = Math.floor((monotonicNow() - anchorMs) / 1000)
    const remainSec = baseRemainSec - elapsedSec
    if (remainSec <= 0) {
      countdownText.value = ''
      stopCountdown()
      // 만료 → 서버 자동전이 반영 위해 상세 재조회(라우팅은 loadDetail 의 statusCd 분기).
      // 재조회 성공 시 loadDetail 이 startCountdown 을 다시 불러 기준값·anchor 가 갱신된다.
      loadDetail()
      return
    }
    const mm = String(Math.floor(remainSec / 60)).padStart(2, '0')
    const ss = String(remainSec % 60).padStart(2, '0')
    countdownText.value = `${mm}:${ss}`
  }
  tick()
  countdownTimer = setInterval(tick, 1000)
}
const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => router.back()

// 입실비번 재발급(OPENED, E6)
const onRegenerateEntry = async () => {
  if (regenEntry.value) return
  const ok = await askConfirm('입실 비밀번호를 재발급할까요? 이미 입실한 근로자는 그대로 유지돼요.')
  if (!ok) return
  regenEntry.value = true
  try {
    const { data } = await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/regenerate-entry-password`,
    )
    if (data?.entryPwd && session.value) {
      session.value.entryPwd = data.entryPwd
    }
  } catch (e) {
    const msg = e?.response?.data?.message || '재발급에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    regenEntry.value = false
  }
}

// 수동 연장(E3) / 지금 교육시작(E4)
const onExtend = async () => {
  if (busy.value) return
  busy.value = true
  try {
    await api.post(`/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/extend-prep`)
    await loadDetail() // prepAutoStartAt 갱신 → 카운트다운 재시작
  } catch (e) {
    const msg = e?.response?.data?.message || '연장에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    busy.value = false
  }
}
const onStartNow = async () => {
  if (busy.value) return
  const ok = await askConfirm('지금 교육을 시작할까요? 시작하면 더 이상 입실할 수 없어요.')
  if (!ok) return
  busy.value = true
  try {
    await api.post(`/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/start`)
    stopCountdown()
    router.replace({ path: '/AdminTbmLive', query: { sessionCd: sessionCd.value } })
  } catch (e) {
    const msg = e?.response?.data?.message || '교육 시작에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    busy.value = false
  }
}

// 이탈자 내보내기(입실취소, D-3/E13) — confirm 후 입실취소(물리삭제). 재입실 가능. 성공 시 리스트 갱신.
const onOpenCancelEntry = async (attendee) => {
  if (!attendee?.attendanceCd) return
  const ok = await askConfirm(
    `${attendee.userNm || '해당 근로자'}님을 내보낼까요? 입실이 취소되며 다시 입실할 수 있어요.`,
  )
  if (!ok) return
  try {
    await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}` +
        `/attendees/${encodeURIComponent(attendee.attendanceCd)}/cancel-entry`,
    )
    await loadAttendees()
  } catch (e) {
    // 멱등(이미 취소/없음)·상태 충돌 등은 서버 메시지를 그대로 안내한 뒤 리스트를 동기화.
    const msg = e?.response?.data?.message || '내보내기에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
    await loadAttendees()
  }
}

// 정규직 대리입실 시트
const onOpenDirectEntry = () => {
  directEntryOpen.value = true
}
const onCloseDirectEntry = () => {
  directEntryOpen.value = false
}
const onConfirmDirectEntry = async (userCd) => {
  if (!userCd || directEntrySubmitting.value) return
  directEntrySubmitting.value = true
  try {
    await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/attendees/manager-direct`,
      { userCd },
    )
    directEntryOpen.value = false
    // 입실자 리스트 갱신(R-C 에서 실연동 — 미구현 동안에도 안전하게 호출).
    await loadAttendees()
    await showAlert('대리입실 처리됐어요.')
  } catch (e) {
    // 멱등(이미 입실)/대상 부적합/상태 충돌 등은 서버 메시지를 그대로 안내.
    const msg = e?.response?.data?.message || '대리입실에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    directEntrySubmitting.value = false
  }
}

// 일용직 QR 입실(D-4/E11) — Flutter SCAN_QR 브리지 호출 → qrPayload(가공 없이) → 서버. R-D 범위.
//   브리지(스캔·전달만) ↔ 서버(파싱/식별/입실) 역할 분담. 웹/dev 브라우저(브리지 미존재)는 폴백 안내 후 no-op.
const onScanQr = async () => {
  if (scanning.value) return

  // 브리지 미주입 환경(웹/dev 브라우저) 폴백: QR 스캔은 앱 전용.
  const bridge = window.flutter_inappwebview
  if (!bridge || typeof bridge.callHandler !== 'function') {
    await showAlert('앱에서만 QR 스캔이 가능해요.')
    return
  }

  // 셸이 SCAN_QR 을 모른다고 선언(원격 Vue + 구버전 셸 스큐) → 브리지 없음과 동일 안내.
  // ★ SCAN_QR 에는 타임아웃을 걸지 않는다 — 네이티브 스캔 화면에서 사용자가 오래 머무는 것이 정상 흐름.
  if (isKnownMissing('SCAN_QR')) {
    await showAlert('앱에서만 QR 스캔이 가능해요.')
    return
  }

  scanning.value = true
  try {
    // Flutter SCAN_QR: {status:'OK', payload} | 'CANCELLED' | 'PERMISSION_DENIED' | 'ERROR'
    const res = await bridge.callHandler('SCAN_QR')
    const status = res?.status
    if (status !== 'OK' || !res?.payload) {
      if (status === 'CANCELLED') return // 사용자 취소 — 조용히 종료
      if (status === 'PERMISSION_DENIED') {
        await showAlert('카메라 권한이 필요해요. 설정에서 카메라 권한을 허용해 주세요.')
        return
      }
      // ERROR / 그 외(빈 payload 포함)
      await showAlert('QR 스캔에 실패했어요. 잠시 후 다시 시도해 주세요.')
      return
    }

    // QR raw(JSON 문자열)를 가공 없이 서버로 전달 — 파싱/식별/유효성 검증은 백엔드(E11).
    await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/attendees/qr-scan`,
      { qrPayload: res.payload },
    )
    await loadAttendees()
    await showAlert('QR 입실 처리됐어요.')
  } catch (e) {
    // 멱등(이미 입실)/대상 부적합/QR 형식오류/상태 충돌 등은 서버 메시지를 그대로 안내.
    const msg = e?.response?.data?.message || 'QR 입실에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    scanning.value = false
  }
}

// 당겨서 새로고침 — 즉시 1회 재조회(상세+입실자). loadDetail 이 카운트다운을 재기동(idempotent)하므로
//   별도 타이머 조작 없이 기존 onRefresh 와 동일한 조회 쌍만 호출한다.
const scrollRef = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(
  scrollRef,
  async () => {
    await Promise.all([loadDetail(), loadAttendees()])
  },
)

onMounted(() => {
  loadDetail()
  loadAttendees()
})
onUnmounted(stopCountdown)
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminTbmLiveView 세트) — 자식(PwdCard/AttendeeRow) scoped 가 상속 */
.admin-tbm-prep-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
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

/* 헤더 */
.admin-tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-tbm-hd__back,
.admin-tbm-hd__refresh {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.admin-tbm-hd__back {
  margin-left: -8px;
}
.admin-tbm-hd__refresh {
  margin-right: -8px;
  color: var(--color-text-secondary);
}
.admin-tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* 본문 */
.admin-tbm-prep-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 제목 + 상태 */
.admin-tbm-prep__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.admin-tbm-prep__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-prep__status {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}

/* 카드 */
.card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}
.card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-sm);
}
.card__label {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.card__head .card__label {
  margin: 0;
}

/* 카운트다운 */
.admin-tbm-prep__countdown {
  margin: 0 0 var(--space-md);
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}
.admin-tbm-prep__timer-actions,
.admin-tbm-prep__add-actions {
  display: flex;
  gap: var(--space-sm);
}

/* 입실자 리스트 */
.admin-tbm-prep__count {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}
.admin-tbm-prep__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

/* 안내 */
.admin-tbm-prep__hint {
  margin: var(--space-sm) 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-tertiary);
}

/* 상태 메시지 */
.admin-tbm-state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.admin-tbm-state--sm {
  margin: 0;
  font-size: 13px;
}
.admin-tbm-state__msg {
  margin: 0 0 var(--space-sm);
}
.admin-tbm-retry {
  height: 36px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 버튼 */
.btn {
  flex: 1;
  height: 44px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

/* 스프라이트 */
.admin-tbm-sprite {
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
