<!--
  LeaveMoveRequestView.vue — 근로자 발의 연차 변경 요청 화면 (prafta-com-008-C-3 + 연차취소개방-02, 앱)
  유형: frontend-screen (모바일 앱, 근로자)
  연결 작업: PRAFTA-{C-5-app} → 연차취소개방-02 (이동/취소 유형 세그먼트 확장, 라우트 /LeaveMoveRequest 불변)
  참조 패턴: views/leave/LeaveApplyView.vue (헤더 + 본문 + 폼 + 디자인 토큰 루트 1회)
  역할 분담: 이동(MOVE) = 종전 동작 무수정. 취소(DELETE) = 신규 세그먼트(연차취소개방 지시서 §4-2).
  ※ 취소 발의는 촉진 지정(FIRST/SECOND) 연차 불가(서버 400_207 최종 방어 + 인라인 경고 배지).
    만료일(AVAIL_TO_DATE) 이내 + DIRECT_USE_KEY 충돌 + 마감월은 서버 강제. 본인 LEAVE_ID 한정(JWT).
-->
<template>
  <div class="leave-move-view">
    <header class="lmv-hd">
      <button type="button" class="lmv-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-lmv-chev-left" /></svg>
      </button>
      <h1 class="lmv-hd__title">연차 변경 요청</h1>
      <span class="lmv-hd__spacer" aria-hidden="true"></span>
    </header>

    <main
      class="lmv-body"
      ref="scrollRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <p v-if="isLoading" class="lmv-state">불러오는 중...</p>

      <div v-else-if="loadError" class="lmv-state lmv-state--err">
        <p>{{ loadError }}</p>
        <button type="button" class="lmv-retry" @click="loadMyLeaves">다시 시도</button>
      </div>

      <template v-else>
        <!-- 요청 유형 세그먼트 (연차취소개방-02) — 기본 이동(종전 동작 보존) -->
        <section class="lmv-section">
          <div class="lmv-seg" role="tablist" aria-label="요청 유형">
            <button
              type="button"
              class="lmv-seg__btn"
              :class="{ 'lmv-seg__btn--on': reqMode === 'MOVE' }"
              @click="reqMode = 'MOVE'"
            >이동</button>
            <button
              type="button"
              class="lmv-seg__btn"
              :class="{ 'lmv-seg__btn--on': reqMode === 'DELETE' }"
              @click="reqMode = 'DELETE'"
            >취소</button>
          </div>
        </section>

        <!-- 대상 연차일 선택 -->
        <section class="lmv-section">
          <h2 class="lmv-section__title">대상 연차일</h2>
          <select v-model="selectedLeaveId" class="lmv-select">
            <option value="" disabled>연차일을 선택하세요</option>
            <option v-for="lv in movableLeaves" :key="lv.leaveId" :value="lv.leaveId">
              {{ lv.startDate }} · {{ lv.leaveNm }}{{ lv.promotionStageNm ? ' (' + lv.promotionStageNm + ')' : '' }}
            </option>
          </select>
          <!-- 촉진 지정 건 + 취소 모드: 인라인 경고 배지(제출 버튼 disabled 숨김 금지 — 클릭 시 안내) -->
          <p v-if="reqMode === 'DELETE' && selectedIsPromotion" class="lmv-hint">
            촉진 지정 연차는 취소 요청이 불가합니다.
          </p>
        </section>

        <!-- 이동 대상일 (이동 모드에서만) -->
        <section v-if="reqMode === 'MOVE'" class="lmv-section">
          <h2 class="lmv-section__title">이동 대상일</h2>
          <!-- 공통 날짜 휠 필드(modelValue 'YYYY-MM-DD'). 만료일 이내/마감/충돌은 서버 강제. -->
          <DateStepperField v-model="moveTargetDate" placeholder="이동 대상일 선택" />
        </section>

        <!-- 연차이동확장-06: 이동 위치 선택(반차 파트/시간차 시각) — MOVE 모드 + 대상 선택 시 단위별 조건부.
             미지정=원 위치 유지(기본, 필드 미전송). 종일 건은 섹션 미노출. 교차/형식 검증은 서버(400_208) 최종 방어. -->
        <section
          v-if="reqMode === 'MOVE' && (isHalfSelected || isHourlySelected)"
          class="lmv-section"
        >
          <h2 class="lmv-section__title">이동 위치</h2>

          <!-- 반차(01): 파트 라디오 3옵션 — 기본「그대로 유지」(미전송) -->
          <template v-if="isHalfSelected">
            <div class="lmv-radios" role="radiogroup" aria-label="반차 위치 선택">
              <label class="lmv-radio">
                <input type="radio" name="lmv-half-part" value="" v-model="halfPartChoice" />
                <span>그대로 유지</span>
              </label>
              <label class="lmv-radio">
                <input type="radio" name="lmv-half-part" value="START" v-model="halfPartChoice" />
                <span>시작 기준(늦게 출근)</span>
              </label>
              <label class="lmv-radio">
                <input type="radio" name="lmv-half-part" value="END" v-model="halfPartChoice" />
                <span>종료 기준(일찍 퇴근)</span>
              </label>
            </div>
            <p class="lmv-help">별도로 선택하지 않으면 「그대로 유지」가 기본입니다.</p>
          </template>

          <!-- 시간차(02|03|04): 원 시각 유지(기본, 미전송) ↔ 시각 지정 + 30분 스텝 시각 필드 -->
          <template v-else>
            <div class="lmv-seg" role="tablist" aria-label="이동 시각 지정 여부">
              <button
                type="button"
                class="lmv-seg__btn"
                :class="{ 'lmv-seg__btn--on': timeChoice === 'KEEP' }"
                @click="timeChoice = 'KEEP'"
              >원 시각 유지</button>
              <button
                type="button"
                class="lmv-seg__btn"
                :class="{ 'lmv-seg__btn--on': timeChoice === 'SET' }"
                @click="timeChoice = 'SET'"
              >시각 지정</button>
            </div>
            <template v-if="timeChoice === 'SET'">
              <TimeStepperField v-model="moveStartTimeInput" :step="30" placeholder="시작 시각" />
              <p class="lmv-help">종료 시각은 원 분량({{ originMinutesText }})으로 자동 결정됩니다.</p>
            </template>
          </template>

          <!-- 선택 요약(지정 시에만 노출 — 표시 전용, 종료=시작+원 분량 클라 파생) -->
          <p v-if="positionSummary" class="lmv-summary">{{ positionSummary }}</p>
        </section>

        <!-- 사유 -->
        <section class="lmv-section">
          <h2 class="lmv-section__title">{{ reqMode === 'MOVE' ? '이동 사유' : '취소 사유' }}</h2>
          <textarea
            v-model="reason"
            class="lmv-textarea"
            rows="3"
            maxlength="500"
            :placeholder="reqMode === 'MOVE' ? '이동 사유를 입력하세요' : '취소 사유를 입력하세요'"
          ></textarea>
        </section>

        <!-- 취소 안내 (지시서 §4-2 원문) -->
        <p v-if="reqMode === 'DELETE'" class="lmv-notice">
          취소가 확정되면 해당 연차 차감이 복원됩니다. 다시 사용하려면 새로 신청해 주세요.
        </p>

        <button
          type="button"
          class="lmv-submit"
          :disabled="!submitEnabled || submitting"
          @click="handleSubmit"
        >
          {{ reqMode === 'MOVE' ? '이동 요청' : '취소 요청' }}
        </button>
      </template>
    </main>

    <svg width="0" height="0" class="lmv-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-lmv-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatYmdDisplay } from '@/utils/approvalFormat'
import DateStepperField from '@/components/common/DateStepperField.vue'
import TimeStepperField from '@/components/common/TimeStepperField.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 (developer: 조회/제출 보완) ─────────────────────────────────────
const isLoading = ref(true)
const loadError = ref('')
const movableLeaves = ref([])
const selectedLeaveId = ref('')
const moveTargetDate = ref('')
const reason = ref('')
const submitting = ref(false)

// 연차취소개방-02: 요청 유형(MOVE=이동 | DELETE=취소). 기본 이동 — 종전 동작·프리셀렉트 보존.
const reqMode = ref('MOVE')

// ── 연차이동확장-06: 이동 위치 선택 상태 ─────────────────────────────────
// 반차 파트: ''=그대로 유지(기본, 미전송) | 'START' | 'END'
const halfPartChoice = ref('')
// 시간차 시각: 'KEEP'=원 시각 유지(기본, 미전송) | 'SET'=시각 지정
const timeChoice = ref('KEEP')
// 지정 시작 시각 'HH:MM' (TimeStepperField v-model, 30분 스텝)
const moveStartTimeInput = ref('')

// 시간차 단위(SYS025 02:2시간 / 03:1시간 / 04:30분)
const HOURLY_UNITS = ['02', '03', '04']

// 선택된 대상 연차 행(원본 단위/시각/분 보존값 — toLeave 매핑)
const selectedLeave = computed(
  () => movableLeaves.value.find((it) => it.leaveId === selectedLeaveId.value) || null,
)
const isHalfSelected = computed(() => selectedLeave.value?.useUnitType === '01')
const isHourlySelected = computed(() => HOURLY_UNITS.includes(selectedLeave.value?.useUnitType))

// 원 분량 안내 문구("종료는 원 분량(30분)으로 자동 결정")
const originMinutesText = computed(() => {
  const n = Number(selectedLeave.value?.leaveMinutes)
  return Number.isFinite(n) && n > 0 ? `${n}분` : '원 사용 시간'
})

// 선택 요약 1줄(지정 시에만) — 종료=시작+원 분량 클라 파생(표시 전용, 확정 값은 서버 파생).
const positionSummary = computed(() => {
  if (isHalfSelected.value && halfPartChoice.value) {
    const label = halfPartChoice.value === 'START' ? '시작 기준(늦게 출근)' : '종료 기준(일찍 퇴근)'
    return `→ ${label}으로 이동`
  }
  if (isHourlySelected.value && timeChoice.value === 'SET' && moveStartTimeInput.value) {
    const [h, m] = moveStartTimeInput.value.split(':').map(Number)
    const mins = Number(selectedLeave.value?.leaveMinutes)
    if (Number.isFinite(h) && Number.isFinite(m) && Number.isFinite(mins) && mins > 0) {
      const end = (h * 60 + m + mins) % 1440 // 자정 넘김은 익일 표시(저장 규약은 서버)
      const endStr = `${String(Math.floor(end / 60)).padStart(2, '0')}:${String(end % 60).padStart(2, '0')}`
      return `→ ${moveStartTimeInput.value}~${endStr}으로 이동`
    }
    return `→ ${moveStartTimeInput.value} 시작으로 이동`
  }
  return ''
})

// 대상 연차/모드 변경 시 위치 선택 초기화 — 다른 단위의 잔존값 오전송 방지.
watch([selectedLeaveId, reqMode], () => {
  halfPartChoice.value = ''
  timeChoice.value = 'KEEP'
  moveStartTimeInput.value = ''
})

// 단순 필수값 검증만 골격에서. 만료/충돌/마감은 서버(developer).
const canSubmit = computed(
  () => !!selectedLeaveId.value && !!moveTargetDate.value && !!reason.value.trim(),
)

// 연차취소개방-02: 취소 모드 필수값 = 대상 연차 + 사유만(이동 대상일 제외).
const canSubmitDelete = computed(() => !!selectedLeaveId.value && !!reason.value.trim())

// 모드별 제출 가능 여부 — 이동 모드는 기존 canSubmit 그대로(무수정).
const submitEnabled = computed(() =>
  reqMode.value === 'MOVE' ? canSubmit.value : canSubmitDelete.value,
)

// 연차취소개방-02: 선택 연차의 촉진 지정 여부(promotionStage FIRST/SECOND) — 취소 모드 경고 배지용.
//   버튼 disabled 로 숨기지 않는다(안내 도달 불가 함정) — 클릭 시 안내 + 서버 400_207 최종 방어.
const selectedIsPromotion = computed(() => {
  const lv = movableLeaves.value.find((it) => it.leaveId === selectedLeaveId.value)
  return !!lv && lv.promotionStage != null && lv.promotionStage !== 'NONE'
})

const onBack = () => router.back()

// 촉진단계 코드 → 라벨 (서버 row 는 코드값만 반환)
const PROMOTION_STAGE_NM = { NONE: '', FIRST: '1차 촉진', SECOND: '2차 촉진' }

// YYYYMMDD → "YYYY.MM.DD" (셀렉트 표시, D1 점 통일)
const fmtYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ymd || ''
  return formatYmdDisplay(ymd)
}

// "YYYY-MM-DD"(input[type=date]) → YYYYMMDD
const toYmd8 = (v) => (v ? String(v).replace(/[^0-9]/g, '').slice(0, 8) : '')

// 서버 row → 셀렉트 표시 객체로 보강(이름 미제공 → 일수 라벨, 촉진단계 라벨)
const toLeave = (lv) => ({
  leaveId: lv.leaveId,
  startDate: fmtYmd(lv.startDate),
  leaveNm: lv.leaveDays != null ? `연차 ${lv.leaveDays}일` : '연차',
  promotionStageNm: PROMOTION_STAGE_NM[lv.promotionStage] || '',
  // 연차취소개방-02: 촉진 판정용 원본 코드 보존(FIRST/SECOND) — 표시 라벨과 별개.
  promotionStage: lv.promotionStage || 'NONE',
  // 연차이동확장-06: 단위별 조건부 UI·요약 파생용 원본 값 보존(movable-leaves 확장 필드).
  useUnitType: lv.useUnitType || '',
  startTime: lv.startTime || '',
  endTime: lv.endTime || '',
  leaveMinutes: lv.leaveMinutes ?? null,
})

// GET /appApi/leavechange/movable-leaves
//   본인(JWT) 미래 확정 연차일 목록(이동 가능 대상). → movableLeaves.value
//   작업지시서_연차변경화면_진입버튼: 쿼리파라미터 leaveId 가 있으면 목록 로드 후 프리셀렉트.
//     목록에 없으면(만료/이미 처리 등 레이스) 조용히 무시 — 에러 팝업 없음, placeholder 유지.
const loadMyLeaves = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await api.get('/appApi/leavechange/movable-leaves')
    const list = res?.data?.list || []
    movableLeaves.value = list.map(toLeave)
    const presetLeaveId = String(route.query.leaveId || '')
    if (presetLeaveId && movableLeaves.value.some((lv) => lv.leaveId === presetLeaveId)) {
      selectedLeaveId.value = presetLeaveId
    }
  } catch (err) {
    loadError.value = resolveApiErrorMessage(err, '연차 목록을 불러오지 못했어요.')
  } finally {
    isLoading.value = false
  }
}

// POST /appApi/leavechange/move-requests
//   body(대문자 키) = { TARGET_LEAVE_ID, MOVE_TARGET_DATE, REQ_REASON }
//   연차이동확장-06: 위치 지정 시에만 MOVE_TARGET_HALF_PART("START"|"END") 또는
//     MOVE_TARGET_START_TIME("HHMM") 선택 포함 — 미지정이면 필드 자체 미전송(R1 페이로드 동일성).
//   서버: 본인 LEAVE_ID 검증(body 비신뢰) + 만료/충돌/마감 + 단위 교차·근무시간 내 검증(400_208 등) →
//         initiatorType=WORKER, reqType=MOVE 요청 생성(관리자 승인 대상) + 관리자 PUSH.
const onSubmit = async () => {
  if (!canSubmit.value || submitting.value) return
  // 시각 지정 토글인데 시각 미선택 — disabled 숨김 대신 클릭 시 안내(안내 도달 불가 함정 회피).
  if (isHourlySelected.value && timeChoice.value === 'SET' && !moveStartTimeInput.value) {
    await showAlert('지정할 시작 시각을 선택해 주세요.')
    return
  }
  submitting.value = true
  try {
    const body = {
      TARGET_LEAVE_ID: selectedLeaveId.value,
      MOVE_TARGET_DATE: toYmd8(moveTargetDate.value),
      REQ_REASON: reason.value.trim(),
    }
    // 미지정 시 필드 자체 미전송(null 전송 아님) — 종전 페이로드와 바이트 동일.
    if (isHalfSelected.value && halfPartChoice.value) {
      body.MOVE_TARGET_HALF_PART = halfPartChoice.value
    }
    if (isHourlySelected.value && timeChoice.value === 'SET' && moveStartTimeInput.value) {
      body.MOVE_TARGET_START_TIME = moveStartTimeInput.value.replace(':', '')
    }
    await api.post('/appApi/leavechange/move-requests', body)
    await showAlert('이동 요청을 보냈어요. 관리자 승인 후 반영됩니다.')
    router.back()
  } catch (err) {
    await showAlert(resolveApiErrorMessage(err, '요청에 실패했어요.'))
  } finally {
    submitting.value = false
  }
}

// 연차취소개방-02: POST /appApi/leavechange/delete-requests
//   body(대문자 키) = { TARGET_LEAVE_ID, REQ_REASON } — 이동 대상일 미포함.
//   서버: 본인 LEAVE_ID(IDOR)·미도래(400_206)·촉진 지정(400_207)·활성요청 중복(400_128)·마감 검증 →
//         initiatorType=WORKER, reqType=DELETE 요청 생성(관리자 확인 대상, PUSH 없음 — B-1 확정).
const onSubmitDelete = async () => {
  if (!canSubmitDelete.value || submitting.value) return
  // 촉진 지정 건: disabled 숨김 대신 클릭 시 안내(최종 방어는 서버 400_207).
  if (selectedIsPromotion.value) {
    await showAlert('촉진 지정 연차는 취소 요청이 불가합니다. 관리자에게 문의해 주세요.')
    return
  }
  submitting.value = true
  try {
    await api.post('/appApi/leavechange/delete-requests', {
      TARGET_LEAVE_ID: selectedLeaveId.value,
      REQ_REASON: reason.value.trim(),
    })
    await showAlert('취소 요청을 보냈어요. 관리자 확인 후 반영됩니다.')
    router.back()
  } catch (err) {
    await showAlert(resolveApiErrorMessage(err, '요청에 실패했어요.'))
  } finally {
    submitting.value = false
  }
}

// 모드별 제출 디스패처 — 이동 모드는 기존 onSubmit 그대로 호출(무수정).
const handleSubmit = () => (reqMode.value === 'MOVE' ? onSubmit() : onSubmitDelete())

// 당겨서 새로고침 — 이동 가능한 연차 목록만 재조회(부작용 없는 조회).
const scrollRef = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(scrollRef, async () => {
  await loadMyLeaves()
})

onMounted(loadMyLeaves)
</script>

<style scoped>
.leave-move-view {
  --color-primary: #16a34a;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-warning-text: #b45309;
  --color-border: #e5e7eb;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-md: 10px;
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
  font-family: -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR', sans-serif;
}

.lmv-hd {
  height: 56px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
}
.lmv-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.lmv-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
}
.lmv-hd__spacer {
  width: 44px;
  height: 44px;
}

.lmv-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.lmv-state {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}
.lmv-state--err {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}
.lmv-retry {
  height: 40px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}

.lmv-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.lmv-section__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.lmv-select,
.lmv-date,
.lmv-textarea {
  width: 100%;
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-family: inherit;
  font-size: 14px;
  background: var(--color-surface);
  color: var(--color-text-primary);
}
.lmv-textarea {
  resize: vertical;
}

.lmv-hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-warning-text);
}

/* 연차취소개방-02: 요청 유형 세그먼트(이동/취소) */
.lmv-seg {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-xs);
  padding: var(--space-xs);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.lmv-seg__btn {
  height: 36px;
  border: 0;
  border-radius: calc(var(--radius-md) - 4px);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
}
.lmv-seg__btn--on {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

/* 연차이동확장-06: 이동 위치 선택(반차 파트 라디오/시간차 시각 지정) */
.lmv-radios {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.lmv-radio {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
}
.lmv-radio input {
  margin: 0;
  accent-color: var(--color-primary);
}
.lmv-help {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.lmv-summary {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}

/* 연차취소개방-02: 취소 모드 안내(차감 복원 고지) */
.lmv-notice {
  margin: 0;
  padding: var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.lmv-submit {
  height: 48px;
  /* lmv-body(flex-column) 내용 증가 시 고정 height 버튼이 압축되는 것 방지(실기기 실증) */
  flex-shrink: 0;
  margin-top: var(--space-sm);
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}
.lmv-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.icon {
  display: block;
}
</style>
