<!--
  AdminSelfJoinApproveSheet.vue — 셀프가입 승인 입력 시트
  - 작업 ID: A7
  - 트리거: 대기 카드 [승인] → 부모가 approve-options 조회 후 open=true.
  - 백엔드(부모가 호출): POST /appApi/admin/self-join/approve
      { userCd, hireDate(yyyyMMdd), rankCd|null, stdWorkType('FULL'|'DIRECT'),
        stdWorkWeekMinutes|null, stdWorkReasonCd|null }
      ★고용형태는 아무도 보내지 않는다 — 서버가 REGULAR 로 채운다(앱 요청 DTO 에 필드 자체가 없다).
  - 입력 항목은 웹 SelfJoinApprovePop.vue 와 동일하다. 사업장·부서·권한 입력란은 없다(찾기 시트 불필요).
  - 직급/사유는 native select 대신 가로 스크롤 칩이다(결정 O — 시트 안 중첩 시트 회피).
    주 소정근로 시·분만 native number 다(결정 P — TimeStepperField 는 '시각'용이라 의미가 다르다).
  - 참조 패턴: views/admin/approval/components/AdminApprovalRejectSheet.vue (시트 마크업 + 토큰 자급).
  - ★토큰 자급: position:fixed 라 부모 상속이 끊길 수 있다. dimmer 에 직접 선언한다.
  - 시트는 입력값만 emit 한다. API 호출·payload 규격 변환·성공 후 재조회는 부모(셸) 책임이다.
-->
<template>
  <transition name="sjs-fade">
    <div
      v-if="open"
      class="sjs__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="가입 승인"
      @click.self="onClose"
    >
      <div class="sjs">
        <div class="sjs__handle" aria-hidden="true"></div>

        <header class="sjs__header">
          <h2 class="sjs__title">가입 승인</h2>
          <button type="button" class="sjs__close" aria-label="닫기" @click="onClose">
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

        <div class="sjs__body">
          <!-- 대상자(읽기 전용) -->
          <div class="sjs__target">
            <p class="sjs__target-name">
              {{ target?.userNm || '-' }}
              <span class="sjs__target-id">({{ target?.userId || '-' }})</span>
            </p>
            <p class="sjs__target-org">
              {{ target?.siteNm || '-' }}<span v-if="target?.nodeNm"> · {{ target.nodeNm }}</span>
            </p>
          </div>

          <p class="sjs__guide">
            승인하면 계정이 활성화되고, 아래 입력값으로 인사정보와 소정근로시간 이력이 함께
            등록됩니다.
          </p>

          <!-- 입사일 (필수) -->
          <div class="sjs__field">
            <span class="sjs__label">입사일 *</span>
            <DateStepperField v-model="hireDate" placeholder="입사일 선택" />
          </div>

          <!-- 직급 (선택) -->
          <div class="sjs__field">
            <span class="sjs__label">직급</span>
            <div class="sjs__chips">
              <button
                type="button"
                class="sjs__chip"
                :class="{ 'is-active': rankCd === '' }"
                @click="rankCd = ''"
              >
                -
              </button>
              <button
                v-for="r in rankOptions"
                :key="r.rankCd"
                type="button"
                class="sjs__chip"
                :class="{ 'is-active': rankCd === r.rankCd }"
                @click="rankCd = r.rankCd"
              >
                {{ r.rankNm }}
              </button>
            </div>
          </div>

          <!-- 소정근로시간 유형 (필수) -->
          <div class="sjs__field">
            <span class="sjs__label">소정근로시간 *</span>
            <div class="sjs__chips">
              <button
                type="button"
                class="sjs__chip"
                :class="{ 'is-active': stdWorkType === 'FULL' }"
                @click="stdWorkType = 'FULL'"
              >
                풀타임 ({{ fullTimeLabel }})
              </button>
              <button
                type="button"
                class="sjs__chip"
                :class="{ 'is-active': stdWorkType === 'DIRECT' }"
                @click="stdWorkType = 'DIRECT'"
              >
                단시간(직접 입력)
              </button>
            </div>
          </div>

          <!-- 주 소정근로 (DIRECT 시 필수) -->
          <div v-if="isDirect" class="sjs__field">
            <span class="sjs__label">주 소정근로 *</span>
            <div class="sjs__hm">
              <input
                v-model.number="stdWorkHours"
                type="number"
                inputmode="numeric"
                min="0"
                max="168"
                class="sjs__num"
                placeholder="시간"
              />
              <span class="sjs__suffix">시간</span>
              <input
                v-model.number="stdWorkMinutes"
                type="number"
                inputmode="numeric"
                min="0"
                max="59"
                class="sjs__num"
                placeholder="분"
              />
              <span class="sjs__suffix">분</span>
            </div>
          </div>

          <!-- 소정근로 사유 (DIRECT 시 필수) -->
          <div v-if="isDirect" class="sjs__field">
            <span class="sjs__label">소정근로 사유 *</span>
            <div class="sjs__chips">
              <button
                v-for="o in reasonOptions"
                :key="o.reasonCd"
                type="button"
                class="sjs__chip"
                :class="{ 'is-active': stdWorkReasonCd === o.reasonCd }"
                @click="stdWorkReasonCd = o.reasonCd"
              >
                {{ o.reasonNm }}
              </button>
            </div>
          </div>

          <!-- 경고/안내 -->
          <p v-if="showShortWarning" class="sjs__warn">
            ⚠ 주 소정근로시간이 기준 미만입니다. 초단시간근로자는 연차·주휴 적용 대상에서 제외될 수
            있으니 계약 내용을 확인해 주세요.
          </p>
          <p v-if="isDirect" class="sjs__hint">
            ⓘ 육아기·임신기·가족돌봄 단축은 적용 기간이 필요해 승인 단계에서는 등록할 수 없습니다.
            승인 후 소정근로시간 관리에서 기간과 함께 등록해 주세요.
          </p>
        </div>

        <!-- F-10 규약: 왼쪽=진행/확정(primary), 오른쪽=이탈(취소) -->
        <footer class="sjs__footer">
          <button
            type="button"
            class="sjs__btn sjs__btn--primary"
            :disabled="!isValid || submitting"
            @click="onConfirm"
          >
            승인하기
          </button>
          <button type="button" class="sjs__btn sjs__btn--ghost" @click="onClose">취소</button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

import DateStepperField from '@/components/common/DateStepperField.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  // 승인 대상 행(대기 목록 1건) — 표시 전용
  target: { type: Object, default: null },
  // approve-options 응답 { cmpnyWeekStdMinutes, reasonOptions[], rankOptions[] }
  options: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'confirm'])

/**
 * 초단시간 경계(주 15시간 = 900분) 폴백.
 *
 * 서버 approve-options 응답에는 minWarnWeekMinutes 가 없다(백엔드 실측). 웹
 * SelfJoinApprovePop.vue 도 같은 값을 프론트에서 판정하므로 동일 기준을 쓴다.
 * 서버가 이후 임계값을 내려주면 그 값이 우선한다.
 */
const MIN_WARN_WEEK_MINUTES = 900

// ── 입력 상태 ────────────────────────────────────────────────────────
const hireDate = ref('') // 'YYYY-MM-DD' (DateStepperField 규약)
const rankCd = ref('')
const stdWorkType = ref('FULL')
const stdWorkHours = ref(null)
const stdWorkMinutes = ref(0)
const stdWorkReasonCd = ref('')

// ── 옵션 파생(서버 값 단일 출처 — 주 40시간 하드코딩 금지) ────────────
const rankOptions = computed(() => props.options?.rankOptions || [])
const reasonOptions = computed(() => props.options?.reasonOptions || [])

const isDirect = computed(() => stdWorkType.value === 'DIRECT')

const inputMinutes = computed(
  () => (Number(stdWorkHours.value) || 0) * 60 + (Number(stdWorkMinutes.value) || 0),
)

const fullTimeLabel = computed(() => {
  const m = Number(props.options?.cmpnyWeekStdMinutes)
  if (!m || m <= 0) return '회사 기준'
  const h = Math.floor(m / 60)
  const r = m % 60
  return r === 0 ? `주 ${h}시간` : `주 ${h}시간 ${r}분`
})

const showShortWarning = computed(() => {
  const threshold = Number(props.options?.minWarnWeekMinutes) || MIN_WARN_WEEK_MINUTES
  if (!isDirect.value) return false
  // 경고일 뿐 저장은 허용한다(서버 판정과 동일 규약).
  return inputMinutes.value > 0 && inputMinutes.value < threshold
})

// 입력 완결성만 검증한다(서버가 재검증한다).
const isValid = computed(() => {
  if (!hireDate.value) return false
  if (!isDirect.value) return true
  return inputMinutes.value > 0 && !!stdWorkReasonCd.value
})

const onClose = () => emit('close')

const onConfirm = () => {
  if (!isValid.value || props.submitting) return
  // 전송 규격 변환(하이픈 제거·null 치환)은 부모가 한다. 시트는 입력값만 넘긴다.
  emit('confirm', {
    hireDate: hireDate.value,
    rankCd: rankCd.value,
    stdWorkType: stdWorkType.value,
    stdWorkWeekMinutes: isDirect.value ? inputMinutes.value : null,
    stdWorkReasonCd: isDirect.value ? stdWorkReasonCd.value : null,
  })
}

// 오늘(YYYY-MM-DD) — 입사일 기본값. 서버/앱 단일 타임존(KST) 규약상 로컬 날짜를 그대로 쓴다.
const todayYmd = () => {
  const now = new Date()
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${mm}-${dd}`
}

// 열릴 때 초기화(기존 시트 관례) — 직전 대상자의 입력값이 남지 않게 한다.
watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return
    hireDate.value = todayYmd()
    rankCd.value = ''
    stdWorkType.value = 'FULL'
    stdWorkHours.value = null
    stdWorkMinutes.value = 0
    stdWorkReasonCd.value = reasonOptions.value[0]?.reasonCd || ''
  },
)
</script>

<style scoped>
/* ★토큰 자급(self-contained) — position:fixed 라 부모 토큰 상속이 끊길 수 있다.
   빠뜨리면 시트가 투명하게 렌더된다(3회 재발 실증). */
.sjs__dimmer {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 130;
}
.sjs {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 85vh;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}
.sjs__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.sjs__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.sjs__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.sjs__close {
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
.sjs__body {
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 대상자 */
.sjs__target {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sjs__target-name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.sjs__target-id {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-tertiary);
}
.sjs__target-org {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.sjs__guide {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-border-light);
  border-radius: var(--radius-md);
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

/* 필드 */
.sjs__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.sjs__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.sjs__chips {
  display: flex;
  gap: var(--space-sm);
  overflow-x: auto;
  padding-bottom: var(--space-xs);
  -webkit-overflow-scrolling: touch;
}
.sjs__chip {
  flex: 0 0 auto;
  height: 36px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}
.sjs__chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 700;
}

/* 시·분 입력 */
.sjs__hm {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.sjs__num {
  width: 72px;
  height: 40px;
  box-sizing: border-box;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 15px;
  font-family: inherit;
  text-align: right;
}
.sjs__num:focus {
  outline: none;
  border-color: var(--color-primary);
}
.sjs__suffix {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.sjs__warn {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
  font-size: 12px;
  line-height: 1.5;
}
.sjs__hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-tertiary);
}

/* 푸터 */
.sjs__footer {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.sjs__btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.sjs__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.sjs__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.sjs__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.sjs-fade-enter-active,
.sjs-fade-leave-active {
  transition: opacity 0.18s ease;
}
.sjs-fade-enter-from,
.sjs-fade-leave-to {
  opacity: 0;
}
</style>
