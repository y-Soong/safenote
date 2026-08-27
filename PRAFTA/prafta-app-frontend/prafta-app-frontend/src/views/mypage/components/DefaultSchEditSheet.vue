<!--
  DefaultSchEditSheet.vue — 기본 근무타입 변경 바텀시트 (F-8-3 → 관리자 승인제 전환)
  - 시트 인프라: BaseBottomSheet 재사용 (★신규 시트 인프라 금지 — F-6 원칙 승계)
  - props: modelValue(v-model), currentSchCd, currentLabel
  - emit : requested(reqId) — 기존 saved(newSchCd, newLabel) 대체. 승인 전까지 현재 근무타입
    라벨은 갱신하지 않는다("승인 전 미반영" 정책 — 작업지시서_기본근무타입-변경-관리자승인제.md §3).
  - 대기중 요청 존재 시 선택 UI 대신 배너만 노출(GET /appApi/req06/my?reqTypes=14&reqStatuses=01 로 조회).
-->
<template>
  <BaseBottomSheet v-model="open" title="기본 근무타입 변경" :show-footer="!hasPending">
    <div class="sch-edit">
      <p class="sch-edit__current">현재: {{ currentLabel || "미설정" }}</p>

      <!-- 대기중 요청 배너 -->
      <div v-if="hasPending" class="sch-edit__pending">
        <p class="sch-edit__pending-title">
          <span class="sch-edit__pending-icon" aria-hidden="true">⏳</span>
          승인 대기 중
        </p>
        <p class="sch-edit__pending-target">{{ pendingLabel || "요청 정보를 불러오는 중" }}</p>
        <p class="sch-edit__pending-date">{{ pendingReqDateDisplay }} 신청</p>
        <p class="sch-edit__pending-desc">관리자 승인 후 반영됩니다.</p>
        <button type="button" class="sch-edit__pending-link" @click="onGoToMyRequests">
          내 요청 확인하기
        </button>
      </div>

      <!-- 선택 UI (대기중 요청이 없을 때만) -->
      <template v-else>
        <p v-if="loading" class="sch-edit__state">불러오는 중...</p>
        <p v-else-if="error" class="sch-edit__state sch-edit__state--err">
          선택 가능한 근무타입을 불러오지 못했어요. 다시 시도해 주세요.
        </p>
        <p v-else-if="filteredOptions.length === 0" class="sch-edit__state">
          선택 가능한 근무타입이 없어요. 관리자에게 문의해 주세요.
        </p>

        <ul v-else class="sch-edit__list">
          <li v-for="opt in filteredOptions" :key="opt.schCd" class="sch-edit__item">
            <label class="sch-edit__radio-label">
              <input
                type="radio"
                name="defaultSchCd"
                :value="opt.schCd"
                v-model="selectedSchCd"
              />
              <span>{{ opt.schNo }} ({{ fmtTime(opt.fstSchStrTime) }}~{{ fmtTime(opt.fstSchEndTime) }})</span>
            </label>
          </li>
        </ul>

        <div class="sch-edit__reason">
          <label class="sch-edit__reason-label" for="sch-edit-reason">신청 사유</label>
          <textarea
            id="sch-edit-reason"
            v-model="reqReason"
            class="sch-edit__reason-input"
            rows="3"
            maxlength="500"
            placeholder="예: 개인 사정으로 근무시간 변경을 요청합니다."
          ></textarea>
        </div>

        <p class="sch-edit__hint">
          ⓘ 승인 시 명일부터 연말까지 근무계획이 자동 생성·갱신됩니다
          (빈 날·자동생성분만, 휴일·연차·교대팀 구간 제외). 신청 후 관리자 승인이 필요합니다.
        </p>
        <p v-if="saveError" class="sch-edit__state sch-edit__state--err">{{ saveError }}</p>
      </template>
    </div>

    <template v-if="!hasPending" #footer>
      <button
        type="button"
        class="sch-edit__save"
        :disabled="!selectedSchCd || selectedSchCd === currentSchCd || !reqReason.trim() || saving"
        @click="onSubmit"
      >
        신청하기
      </button>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  currentSchCd: { type: String, default: '' },
  currentLabel: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'requested'])

const router = useRouter()

// v-model 프록시 (BaseBottomSheet 는 modelValue 로만 열림/닫힘 제어)
const open = ref(props.modelValue)
watch(() => props.modelValue, (v) => { open.value = v })
watch(open, (v) => emit('update:modelValue', v))

// 반응형 상태 — 선택 리스트
const loading = ref(false)
const error = ref(false)
const options = ref([])
const selectedSchCd = ref(props.currentSchCd)
const reqReason = ref('')
const saving = ref(false)
const saveError = ref('')

// 반응형 상태 — 대기중 요청
const hasPending = ref(false)
const pendingLabel = ref('')
const pendingReqDateDisplay = ref('')
const pendingCheckLoading = ref(false)

// 반영 시점은 항상 명일(오늘+1, applyDefaultSchChange 규칙) — 적용일이 명일보다 미래인
//   근무타입은 노출하지 않는다(최종 판정은 서버 isValidDefaultSch).
const tomorrowYmd = (() => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}${m}${day}`
})()
const filteredOptions = computed(() =>
  options.value.filter((o) => !o.earliestApplyDate || o.earliestApplyDate <= tomorrowYmd),
)

// 'HHmm' → 'HH:mm'
const fmtTime = (t) => {
  if (!t || t.length < 4) return t || ''
  return `${t.substring(0, 2)}:${t.substring(2, 4)}`
}

// 옵션 조회 — 대상 사업장은 세션 토큰 식별 사용자의 SITE_CD 로만 도출(파라미터 없음, IDOR 방지).
const loadOptions = async () => {
  loading.value = true
  error.value = false
  try {
    const res = await api.get('/appApi/mypage/default-sch-options')
    options.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) {
    options.value = []
    error.value = true
  } finally {
    loading.value = false
  }
}

// 대기중 요청 조회 — GET /appApi/req06/my?reqTypes=14&reqStatuses=01&limit=1 (PRAFTA-004 확장분 재사용).
const loadPendingStatus = async () => {
  pendingCheckLoading.value = true
  try {
    const res = await api.get('/appApi/req06/my', {
      params: { reqTypes: '14', reqStatuses: '01', limit: 1 },
    })
    const items = Array.isArray(res?.data?.items) ? res.data.items : []
    if (items.length > 0) {
      const item = items[0]
      hasPending.value = true
      pendingLabel.value = (item.summaryLines && item.summaryLines[0]) || ''
      pendingReqDateDisplay.value = item.reqDateDisplay || ''
    } else {
      hasPending.value = false
      pendingLabel.value = ''
      pendingReqDateDisplay.value = ''
    }
  } catch (e) {
    // 실패는 비차단 — 선택 UI 그대로 노출(과다 차단 방지). 서버가 최종 중복 검증(ATTD_400_090)한다.
    hasPending.value = false
  } finally {
    pendingCheckLoading.value = false
  }
}

// 시트가 열릴 때마다 현재값으로 선택 초기화 + 옵션/대기여부 재조회.
watch(
  () => props.modelValue,
  (isOpen) => {
    if (!isOpen) return
    selectedSchCd.value = props.currentSchCd
    reqReason.value = ''
    saveError.value = ''
    loadOptions()
    loadPendingStatus()
  },
)

// 신청 — 성공 시 부모(MyPageView)에게 reqId 를 emit 하고 시트를 닫는다.
//   ★ 승인 전이므로 currentLabel/currentSchCd 를 갱신하지 않는다("승인 전 미반영").
const onSubmit = async () => {
  if (!selectedSchCd.value || selectedSchCd.value === props.currentSchCd) return
  if (!reqReason.value.trim() || saving.value) return

  saving.value = true
  saveError.value = ''
  try {
    const res = await api.post('/appApi/mypage/update-default-sch', {
      defaultSchCd: selectedSchCd.value,
      reqReason: reqReason.value.trim(),
    })
    const reqId = res?.data?.reqId || null
    emit('requested', reqId)
    open.value = false
  } catch (e) {
    saveError.value = resolveApiErrorMessage(e, '기본 근무타입 변경 신청 중 오류가 발생했어요.')
  } finally {
    saving.value = false
  }
}

// "내 요청 확인하기" — MyRequestsView 로 이동.
const onGoToMyRequests = () => {
  open.value = false
  router.push('/MyRequestsView')
}
</script>

<style scoped>
/* ⚠️ BaseBottomSheet 의 footer 슬롯은 default 슬롯의 DOM 형제(sibling)이지 하위(descendant)가
   아니다 — 다른 앱 화면과 동일하게 var(--x, 폴백값) 인라인 폴백을 전체 선택자에 사용한다. */
.sch-edit {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.sch-edit__current {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-primary, #374151);
}
.sch-edit__state {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-secondary, #6b7280);
  text-align: center;
  padding: 1rem 0;
}
.sch-edit__state--err {
  color: var(--color-danger, #ef4444);
}
.sch-edit__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.sch-edit__item {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  padding: 0.6rem 0.8rem;
}
.sch-edit__radio-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: var(--color-text-primary, #374151);
}
.sch-edit__reason {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.sch-edit__reason-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-primary, #374151);
}
.sch-edit__reason-input {
  width: 100%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  padding: 0.6rem 0.7rem;
  font-size: 0.85rem;
  color: var(--color-text-primary, #374151);
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
}
.sch-edit__hint {
  margin: 0;
  font-size: 0.78rem;
  line-height: 1.5;
  color: var(--color-text-secondary, #6b7280);
}
.sch-edit__save {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 8px;
  background: var(--color-primary, #16a34a);
  color: #ffffff;
  font-weight: 600;
  font-size: 0.95rem;
}
.sch-edit__save:disabled {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-secondary, #6b7280);
}

/* 대기중 요청 배너 */
.sch-edit__pending {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding: 0.9rem 1rem;
  border-radius: 10px;
  background: var(--color-warning-tint, #fffbeb);
  border: 1px solid var(--color-warning, #f59e0b);
}
.sch-edit__pending-title {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-warning, #f59e0b);
}
.sch-edit__pending-icon {
  font-size: 0.9rem;
}
.sch-edit__pending-target {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-primary, #374151);
}
.sch-edit__pending-date {
  margin: 0;
  font-size: 0.78rem;
  color: var(--color-text-secondary, #6b7280);
}
.sch-edit__pending-desc {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-secondary, #6b7280);
}
.sch-edit__pending-link {
  align-self: flex-start;
  margin-top: 0.3rem;
  border: none;
  background: transparent;
  padding: 0;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  text-decoration: underline;
}
</style>
