<!--
  AdminEmployeeGpsMapSheet.vue — 외근 GPS 위치 바텀시트(카카오맵 다중 마커)
  - 작업 ID: PRAFTA-004 (UI 명세: UI-001)
  - 백엔드: GET /appApi/admin/employee-status/gps-trail?attdId= (attdIds 배열 각각 순차 호출 후 병합, PRAFTA-003)
  - 시트 셸: OffsiteReasonSheet.vue 이식. 지도 렌더(다중 마커/폴리라인/출퇴근 필터/Mock 배지):
    prafta-web-frontend AttdGpsCoordPanel.vue 알고리즘 이식(§UI 명세 참조, 좌표는 서버가 이미 복호화해 내려줌).
  - 디자인 토큰: 자급(부모 루트에 없을 수 있어 OffsiteReasonSheet.vue 처럼 필요한 토큰을 이 컴포넌트가 직접 선언).
-->
<template>
  <transition name="eegm-fade">
    <div
      v-if="modelValue"
      class="eegm__dimmer"
      role="dialog"
      aria-modal="true"
      :aria-label="`${userNm} 외근 위치`"
      @click.self="onClose"
    >
      <div class="eegm">
        <div class="eegm__handle" aria-hidden="true"></div>

        <header class="eegm__header">
          <h2 class="eegm__title">{{ userNm }} 외근 위치</h2>
          <button type="button" class="eegm__close" aria-label="닫기" @click="onClose">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="eegm__body">
          <!-- 출근/퇴근/전체 필터 -->
          <div v-if="!isLoading && !mapError && validTrail.length > 0" class="eegm__filter">
            <button
              type="button"
              class="eegm-filter-btn"
              :class="{ 'is-active': viewMode === 'all' }"
              @click="setViewMode('all')"
            >
              전체
            </button>
            <button
              type="button"
              class="eegm-filter-btn"
              :class="{ 'is-active': viewMode === '01' }"
              :disabled="startCount === 0"
              @click="setViewMode('01')"
            >
              출근
            </button>
            <button
              type="button"
              class="eegm-filter-btn"
              :class="{ 'is-active': viewMode === '02' }"
              :disabled="endCount === 0"
              @click="setViewMode('02')"
            >
              퇴근
            </button>
          </div>

          <!-- loading -->
          <div v-if="isLoading" class="eegm__map-wrap">
            <p class="eegm__map-fallback">GPS 정보를 불러오는 중...</p>
          </div>

          <!-- empty: 좌표 0건(외근이 아닌 인원을 열었을 때도 이 경로) -->
          <div v-else-if="validTrail.length === 0" class="eegm__map-wrap">
            <p class="eegm__map-fallback">수집된 GPS 좌표가 없습니다.</p>
          </div>

          <!-- error -->
          <div v-else-if="mapError" class="eegm__map-wrap">
            <p class="eegm__map-fallback">지도를 불러오지 못했습니다.</p>
          </div>

          <!-- success -->
          <div v-else class="eegm__map-wrap">
            <div
              id="eegmMap"
              ref="mapEl"
              class="eegm__map"
              role="img"
              aria-label="외근 위치 지도"
            ></div>
          </div>

          <p v-if="validTrail.length > 0 && !isLoading" class="eegm__summary">
            총 <b>{{ validTrail.length }}</b
            >건
            <span v-if="mockedCount > 0" class="eegm__mocked-warn"
              >(Mock 좌표 {{ mockedCount }}건 포함)</span
            >
          </p>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'

import api from '@/api/axios'
import { loadKakaoMapScript } from '@/utils/kakaoMap'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  userNm: { type: String, default: '' },
  // 대상 사용자의 그날 ATTD_ID 배열(최대 2개, PRAFTA-002 daily 응답의 attdIds) — 없으면 empty 상태.
  attdIds: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue'])

// 지도 표시 상태
const mapEl = ref(null)
const isLoading = ref(false)
const mapError = ref(false)

// 서버 gps-trail 응답 원본(여러 attdId 호출 결과 병합) — [{ gpsId, lat, lon, accuracy, apiCallDate, apiCallTime, isMocked, gpsInfoType }]
const trail = ref([])

// gpsInfoType 이 '01'(출근)/'02'(퇴근) 인 유효 좌표만 — AttdGpsCoordPanel.validTrail 동일 규칙
const validTrail = computed(() =>
  trail.value.filter((g) => g.gpsInfoType === '01' || g.gpsInfoType === '02'),
)
const mockedCount = computed(
  () => validTrail.value.filter((g) => g.isMocked === 'Y' || g.isMocked === true).length,
)
const startCount = computed(() => validTrail.value.filter((g) => g.gpsInfoType === '01').length)
const endCount = computed(() => validTrail.value.filter((g) => g.gpsInfoType === '02').length)

const viewMode = ref('all')

// 카카오맵 인스턴스 핸들(반응형 불필요)
let kakaoMap = null
let kakaoMarkers = []
let kakaoPolyline = null

const onClose = () => {
  emit('update:modelValue', false)
}

// attdIds 각각에 대해 gps-trail 을 순차/병행 조회해 병합한다(웹과 동일하게 단건 계약 유지, §PRAFTA-003).
//   단건 실패는 전체를 무너뜨리지 않고 그 attdId 분만 빈 배열로 취급한다.
const loadTrail = async () => {
  const attdIdList = Array.isArray(props.attdIds) ? props.attdIds.filter((id) => id != null) : []
  trail.value = []

  if (attdIdList.length === 0) {
    return
  }

  isLoading.value = true
  try {
    const results = await Promise.all(
      attdIdList.map((attdId) =>
        api
          .get('/appApi/admin/employee-status/gps-trail', { params: { attdId } })
          .then(({ data }) => (Array.isArray(data?.trail) ? data.trail : []))
          .catch((e) => {
            console.error('[AdminEmployeeGpsMapSheet] GPS 동선 조회 실패', e?.message)
            return []
          }),
      ),
    )
    trail.value = results.flat()
  } finally {
    isLoading.value = false
  }
}

/*
 * 지도 렌더 — 웹 AttdGpsCoordPanel.vue renderMap 알고리즘 그대로 이식.
 * gpsInfoType('01'=출근 / '02'=퇴근) 기준 마커 색/라벨 구분 + 폴리라인 + LatLngBounds 자동 줌.
 */
const renderMap = async () => {
  mapError.value = false
  try {
    await loadKakaoMapScript()
  } catch (e) {
    console.error('[AdminEmployeeGpsMapSheet] 카카오 지도 로드 실패:', e?.message)
    mapError.value = true
    return
  }

  if (!mapEl.value) {
    // 시트가 그 사이 닫혔으면 무시.
    return
  }

  const points = validTrail.value
    .map((g) => {
      const lat = Number(g.lat)
      const lon = Number(g.lon)
      if (Number.isNaN(lat) || Number.isNaN(lon)) return null
      return {
        lat,
        lon,
        gpsInfoType: g.gpsInfoType,
        isMocked: g.isMocked === 'Y' || g.isMocked === true,
      }
    })
    .filter(Boolean)
    .sort((a, b) => a.gpsInfoType.localeCompare(b.gpsInfoType))

  if (points.length === 0) return

  // viewMode 필터 — '01'(출근)/'02'(퇴근) 단독 보기 시 해당 좌표만 표시(출근·퇴근 좌표가 겹칠 때 개별 확인용).
  const visible =
    viewMode.value === 'all' ? points : points.filter((p) => p.gpsInfoType === viewMode.value)
  if (visible.length === 0) return

  cleanupMap()

  const center = new window.kakao.maps.LatLng(visible[0].lat, visible[0].lon)
  kakaoMap = new window.kakao.maps.Map(mapEl.value, {
    center,
    level: 4,
  })

  // 마커 이미지 — 출근(초록)/퇴근(빨강) 구분(CSS 토큰과 동일 색)
  const buildPinImage = (color) =>
    new window.kakao.maps.MarkerImage(
      'data:image/svg+xml;base64,' +
        btoa(
          `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="32" viewBox="0 0 24 32"><path d="M12 0C5.4 0 0 5.4 0 12c0 9 12 20 12 20s12-11 12-20C24 5.4 18.6 0 12 0z" fill="${color}"/><circle cx="12" cy="12" r="5" fill="#fff"/></svg>`,
        ),
      new window.kakao.maps.Size(24, 32),
      { offset: new window.kakao.maps.Point(12, 32) },
    )
  const startImage = buildPinImage('#16a34a')
  const endImage = buildPinImage('#ef4444')

  const bounds = new window.kakao.maps.LatLngBounds()
  const path = []

  visible.forEach((p) => {
    const pos = new window.kakao.maps.LatLng(p.lat, p.lon)
    path.push(pos)
    bounds.extend(pos)

    const isStart = p.gpsInfoType === '01'

    const marker = new window.kakao.maps.Marker({
      map: kakaoMap,
      position: pos,
      image: isStart ? startImage : endImage,
    })
    kakaoMarkers.push(marker)

    const label = new window.kakao.maps.CustomOverlay({
      map: kakaoMap,
      position: pos,
      yAnchor: 2.2,
      content: `<div class="gps-pin-label ${isStart ? 'is-start' : 'is-end'}">${isStart ? '출근' : '퇴근'}</div>`,
    })
    kakaoMarkers.push(label)
  })

  // 출근·퇴근 2점 연결 폴리라인
  if (path.length >= 2) {
    kakaoPolyline = new window.kakao.maps.Polyline({
      path,
      strokeWeight: 3,
      strokeColor: '#16a34a',
      strokeOpacity: 0.8,
      strokeStyle: 'solid',
    })
    kakaoPolyline.setMap(kakaoMap)
  }

  // 자동 줌
  if (path.length === 1) {
    kakaoMap.setCenter(path[0])
    kakaoMap.setLevel(4)
  } else {
    kakaoMap.setBounds(bounds)
  }

  // 레이아웃 직후 relayout 으로 타일 깨짐 방지
  await nextTick()
  if (kakaoMap) {
    kakaoMap.relayout()
  }
}

// 지도 정리 — 마커/폴리라인 해제(마운트 해제·트레일 변경·시트 닫힘 시 호출).
const cleanupMap = () => {
  if (kakaoPolyline) {
    try {
      kakaoPolyline.setMap(null)
    } catch (_e) {
      void 0
    }
    kakaoPolyline = null
  }
  for (const m of kakaoMarkers) {
    try {
      m.setMap(null)
    } catch (_e) {
      void 0
    }
  }
  kakaoMarkers = []
  kakaoMap = null
}

// 출근/퇴근/전체 필터 버튼 — 선택한 모드로 지도를 다시 그린다.
const setViewMode = async (mode) => {
  if (viewMode.value === mode) return
  viewMode.value = mode
  cleanupMap()
  if (isLoading.value || validTrail.value.length === 0) return
  await nextTick()
  await renderMap()
}

// 열림/닫힘에 따른 좌표 조회 + 지도 렌더/정리 트리거.
watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      viewMode.value = 'all'
      mapError.value = false
      await loadTrail()
      if (validTrail.value.length > 0) {
        await nextTick()
        await renderMap()
      }
    } else {
      cleanupMap()
      trail.value = []
    }
  },
)

onBeforeUnmount(() => {
  cleanupMap()
})
</script>

<style scoped>
.eegm__dimmer {
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;
  --radius-md: 10px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-border: #e5e7eb;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;

  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 110;
}
.eegm {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 88vh;
}
.eegm__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.eegm__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.eegm__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.eegm__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.eegm__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.eegm__filter {
  display: flex;
  gap: var(--space-xs);
}
.eegm-filter-btn {
  flex: 0 0 auto;
  height: 30px;
  padding: 0 var(--space-md);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  cursor: pointer;
  font-family: inherit;
}
.eegm-filter-btn.is-active {
  color: var(--color-surface);
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.eegm-filter-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.eegm__map-wrap {
  position: relative;
  width: 100%;
  height: 260px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}
.eegm__map {
  width: 100%;
  height: 100%;
}
.eegm__map-fallback {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.eegm__summary {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.eegm__mocked-warn {
  color: var(--color-danger);
  font-weight: 600;
  margin-left: var(--space-xs);
}
.eegm-fade-enter-active,
.eegm-fade-leave-active {
  transition: opacity 0.18s ease;
}
.eegm-fade-enter-from,
.eegm-fade-leave-to {
  opacity: 0;
}
:deep(.gps-pin-label) {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-surface);
  white-space: nowrap;
}
:deep(.gps-pin-label.is-start) {
  background: var(--color-primary);
}
:deep(.gps-pin-label.is-end) {
  background: var(--color-danger);
}
</style>
