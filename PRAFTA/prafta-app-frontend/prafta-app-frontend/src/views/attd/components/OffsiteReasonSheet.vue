<!--
  OffsiteReasonSheet.vue — 외근(근무지 외) 출퇴근 사유 + 지도 모달
  - 작업 ID: APP008-N24 (UI 명세: UI-A0xx)
  - 정책: attd §7.2~§7.3 (지오펜스 밖 = 외근 태그, 사유 작성)
  - 트리거: MyAttendanceView / MainView 가 지오펜스 밖 출퇴근(서버 ATTD_400_086) 감지 시 open
  - 참조 패턴: views/req/components/BaseBottomSheet.vue
  - 지도: Kakao JS SDK(@/utils/kakaoMap.js) 런타임 동적 로드 + 현위치 1점 마커.
    주소는 services Geocoder 역지오코딩(가능 시), 실패하면 좌표 텍스트로 폴백.
-->
<template>
  <transition name="ofs-fade">
    <div
      v-if="modelValue"
      class="ofs__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="외근 사유 작성"
      @click.self="onCancel"
    >
      <div class="ofs">
        <div class="ofs__handle" aria-hidden="true"></div>

        <header class="ofs__header">
          <h2 class="ofs__title">
            {{ mode === 'checkOut' ? '외근 퇴근 등록' : '외근 출근 등록' }}
          </h2>
          <button type="button" class="ofs__close" aria-label="닫기" @click="onCancel">
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

        <div class="ofs__body">
          <!-- 안내 배너 -->
          <div class="ofs__notice">
            <svg
              class="ofs__notice-ic"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M21 10c0 7-9 12-9 12s-9-5-9-12a9 9 0 0 1 18 0z" />
              <circle cx="12" cy="10" r="3" />
            </svg>
            <span>근무지 범위 밖이에요. 외근 사유를 작성하면 외근으로 등록돼요.</span>
          </div>

          <!-- 지도 영역 (Kakao JS SDK) -->
          <div class="ofs__map-wrap">
            <div
              id="ofsMap"
              ref="mapEl"
              class="ofs__map"
              role="img"
              aria-label="현재 위치 지도"
            ></div>
            <p v-if="!mapReady && !mapError" class="ofs__map-fallback">지도를 불러오는 중…</p>
            <p v-else-if="mapError" class="ofs__map-fallback">지도를 불러오지 못했어요.</p>
          </div>

          <!-- 현위치 주소/좌표 요약 -->
          <p class="ofs__coord">현재 위치: {{ addressText || coordText }}</p>

          <!-- 사유 입력 -->
          <label class="ofs__label" for="ofsReason">외근 사유<span class="ofs__req">*</span></label>
          <textarea
            id="ofsReason"
            v-model="reason"
            class="ofs__textarea"
            rows="3"
            maxlength="500"
            placeholder="예: 거래처 방문, 현장 점검 등"
          ></textarea>
          <p class="ofs__count">{{ reason.length }}/500</p>
        </div>

        <footer class="ofs__footer">
          <button type="button" class="ofs__btn ofs__btn--ghost" @click="onCancel">취소</button>
          <button
            type="button"
            class="ofs__btn ofs__btn--primary"
            @click="onSubmit"
          >
            외근으로 등록
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount, getCurrentInstance } from 'vue'
import { loadKakaoMapScript } from '@/utils/kakaoMap'

// 전역 alert 프록시($alert 있으면 우선, 없으면 window.alert). 사유 미입력 안내용.
const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

// props: modelValue(v-model), mode('checkIn'|'checkOut'), lat/lon/accuracy(현위치 좌표)
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'checkIn' }, // 'checkIn' | 'checkOut'
  lat: { type: Number, default: null },
  lon: { type: Number, default: null },
  accuracy: { type: Number, default: null },
})

// emits: update:modelValue(닫기), submit({ reason }), cancel
const emit = defineEmits(['update:modelValue', 'submit', 'cancel'])

// 사유 입력 (UI 바인딩)
const reason = ref('')

// 지도 엘리먼트/준비 상태
const mapEl = ref(null)
const mapReady = ref(false)
const mapError = ref(false)

// 역지오코딩 결과 주소 (실패 시 빈 문자열 → 좌표 텍스트로 폴백)
const addressText = ref('')

// 카카오맵 인스턴스 핸들 (반응형 불필요 — 일반 변수)
let kakaoMap = null
let kakaoMarker = null

// 현위치 좌표 표시 텍스트
const coordText = computed(() => {
  if (props.lat == null || props.lon == null) return '확인 중…'
  return `${props.lat.toFixed(5)}, ${props.lon.toFixed(5)}`
})

const onCancel = () => {
  emit('cancel')
  emit('update:modelValue', false)
}

const onSubmit = () => {
  // 등록 버튼은 기본 활성 → 사유 빈값 제출 시 사유 안내(서버도 ATTD_400_086 으로 재검증).
  if (!reason.value.trim()) {
    showAlert('사유를 입력해 주세요.')
    return
  }
  emit('submit', { reason: reason.value.trim() })
  // 닫기/초기화는 부모(성공 후)가 modelValue=false 로 처리.
}

// 지도 정리 — 마커/지도 핸들 해제.
const cleanupMap = () => {
  if (kakaoMarker) {
    try {
      kakaoMarker.setMap(null)
    } catch (_e) {
      void 0
    }
    kakaoMarker = null
  }
  kakaoMap = null
}

// 현위치 좌표로 역지오코딩(주소 조회). services 라이브러리 미가용/실패 시 조용히 폴백.
const reverseGeocode = (lat, lon) => {
  try {
    if (!window.kakao?.maps?.services?.Geocoder) return
    const geocoder = new window.kakao.maps.services.Geocoder()
    geocoder.coord2Address(lon, lat, (result, status) => {
      if (status !== window.kakao.maps.services.Status.OK || !result?.length) return
      const r = result[0]
      // 도로명 주소 우선, 없으면 지번 주소.
      addressText.value = r.road_address?.address_name || r.address?.address_name || ''
    })
  } catch (e) {
    console.error('[OffsiteReasonSheet] 역지오코딩 실패:', e?.message)
  }
}

// 지도 렌더 — props.lat/lon 중심 + 현위치 단일 마커.
const renderMap = async () => {
  if (props.lat == null || props.lon == null) {
    mapError.value = true
    return
  }
  mapError.value = false
  mapReady.value = false

  try {
    await loadKakaoMapScript()
  } catch (e) {
    console.error('[OffsiteReasonSheet] 카카오 지도 로드 실패:', e?.message)
    mapError.value = true
    return
  }

  if (!mapEl.value) {
    // 시트가 그 사이 닫혔으면 무시.
    return
  }

  cleanupMap()

  const center = new window.kakao.maps.LatLng(props.lat, props.lon)
  kakaoMap = new window.kakao.maps.Map(mapEl.value, {
    center,
    level: 4,
  })

  // 현위치 마커(기본 핀).
  kakaoMarker = new window.kakao.maps.Marker({
    map: kakaoMap,
    position: center,
  })

  // 레이아웃 직후 relayout 으로 타일 깨짐 방지 + 중심 재설정.
  await nextTick()
  if (kakaoMap) {
    kakaoMap.relayout()
    kakaoMap.setCenter(center)
  }

  mapReady.value = true

  // 주소 역지오코딩(비동기, 실패해도 무시).
  reverseGeocode(props.lat, props.lon)
}

// 열림/닫힘에 따른 사유 초기화 + 지도 초기화 트리거
watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      reason.value = ''
      addressText.value = ''
      mapReady.value = false
      mapError.value = false
      await nextTick()
      await renderMap()
    } else {
      cleanupMap()
    }
  },
)

// 열린 상태에서 좌표가 갱신되면 지도/주소 재렌더.
watch(
  () => [props.lat, props.lon],
  async () => {
    if (props.modelValue) {
      await nextTick()
      await renderMap()
    }
  },
)

onBeforeUnmount(() => {
  cleanupMap()
})
</script>

<style scoped>
.ofs__dimmer {
  /*
   * 토큰 자급(self-contained): 이 시트는 부모 화면(MainView/MyAttendanceView)에서
   * 띄워지는데, MainView 의 루트(.home-view)에는 --space-* / --color-overlay /
   * --radius-xl 등이 선언돼 있지 않다. 그 경우 padding 이 0 으로 무너져 답답해 보이므로
   * 이 컴포넌트 루트에서 필요한 토큰을 직접 선언해 부모와 무관하게 동작하도록 한다.
   */
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;
  --radius-md: 10px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --color-warning-border: #fde68a;

  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 110;
}
.ofs {
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
.ofs__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.ofs__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.ofs__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ofs__close {
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
.ofs__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.ofs__notice {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-warning-tint);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-warning-text);
}
.ofs__notice-ic {
  flex-shrink: 0;
  margin-top: 1px;
}
.ofs__map-wrap {
  position: relative;
  width: 100%;
  height: 200px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
}
.ofs__map {
  width: 100%;
  height: 100%;
}
.ofs__map-fallback {
  position: absolute;
  inset: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.ofs__coord {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ofs__label {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ofs__req {
  color: var(--color-danger);
  margin-left: 2px;
}
.ofs__textarea {
  width: 100%;
  box-sizing: border-box;
  /* flex 컨테이너(.ofs__body) 안에서 키보드로 높이가 줄어도 사유 입력칸은 크기 유지 */
  flex-shrink: 0;
  min-height: 84px;
  resize: none;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.ofs__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}
.ofs__count {
  margin: 0;
  text-align: right;
  font-size: 11px;
  color: var(--color-text-tertiary);
}
.ofs__footer {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.ofs__btn {
  flex: 1;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.ofs__btn--ghost {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.ofs__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.ofs__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ofs-fade-enter-active,
.ofs-fade-leave-active {
  transition: opacity 0.18s ease;
}
.ofs-fade-enter-from,
.ofs-fade-leave-to {
  opacity: 0;
}
</style>
