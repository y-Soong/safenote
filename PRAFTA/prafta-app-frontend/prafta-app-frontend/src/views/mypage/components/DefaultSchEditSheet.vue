<!--
  DefaultSchEditSheet.vue — 기본 근무타입 변경 바텀시트 (F-8-3)
  - 시트 인프라: BaseBottomSheet 재사용 (★신규 시트 인프라 금지 — F-6 원칙 승계)
  - props: modelValue(v-model), currentSchCd, currentLabel
  - emit : saved(newSchCd, newLabel)
  - 부작용 고지("명일부터 연말까지 근무계획이 자동 생성·갱신됩니다")는 confirm 팝업 대신
    시트 내 상시 노출 안내 문구(.sch-edit__hint)로 대체한다(시트 컨텍스트 유지, planner 판단 승계).
-->
<template>
  <BaseBottomSheet v-model="open" title="기본 근무타입 변경" :show-footer="true">
    <div class="sch-edit">
      <p class="sch-edit__current">현재: {{ currentLabel || "미설정" }}</p>

      <p v-if="loading" class="sch-edit__state">불러오는 중...</p>
      <p v-else-if="error" class="sch-edit__state sch-edit__state--err">
        선택 가능한 근무타입을 불러오지 못했어요. 다시 시도해 주세요.
      </p>
      <p v-else-if="options.length === 0" class="sch-edit__state">
        선택 가능한 근무타입이 없어요. 관리자에게 문의해 주세요.
      </p>

      <ul v-else class="sch-edit__list">
        <li v-for="opt in options" :key="opt.schCd" class="sch-edit__item">
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

      <p class="sch-edit__hint">
        ⓘ 변경 시 명일부터 연말까지 근무계획이 자동 생성·갱신됩니다
        (빈 날·자동생성분만, 휴일·연차·교대팀 구간 제외).
      </p>
      <p v-if="saveError" class="sch-edit__state sch-edit__state--err">{{ saveError }}</p>
    </div>

    <template #footer>
      <button
        type="button"
        class="sch-edit__save"
        :disabled="!selectedSchCd || selectedSchCd === currentSchCd || saving"
        @click="onSave"
      >
        저장
      </button>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'
import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  currentSchCd: { type: String, default: '' },
  currentLabel: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'saved'])

// v-model 프록시 (BaseBottomSheet 는 modelValue 로만 열림/닫힘 제어)
const open = ref(props.modelValue)
watch(() => props.modelValue, (v) => { open.value = v })
watch(open, (v) => emit('update:modelValue', v))

// 반응형 상태
const loading = ref(false)
const error = ref(false)
const options = ref([])
const selectedSchCd = ref(props.currentSchCd)
const saving = ref(false)
const saveError = ref('')

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
    const { data } = await api.get('/appApi/mypage/default-sch-options')
    options.value = Array.isArray(data) ? data : []
  } catch (e) {
    options.value = []
    error.value = true
    console.warn('[DefaultSchEditSheet] 옵션 조회 실패:', e?.message)
  } finally {
    loading.value = false
  }
}

// 시트가 열릴 때마다 현재값으로 선택 초기화 + 옵션 재조회.
watch(
  () => props.modelValue,
  (isOpen) => {
    if (!isOpen) return
    selectedSchCd.value = props.currentSchCd
    saveError.value = ''
    loadOptions()
  },
)

// 저장 — 성공 시 부모(MyPageView)에게 새 코드/라벨을 emit 하고 시트를 닫는다.
const onSave = async () => {
  if (!selectedSchCd.value || selectedSchCd.value === props.currentSchCd || saving.value) return

  saving.value = true
  saveError.value = ''
  try {
    await api.post('/appApi/mypage/update-default-sch', {
      defaultSchCd: selectedSchCd.value,
    })
    const selected = options.value.find((o) => o.schCd === selectedSchCd.value)
    const newLabel = selected
      ? `${selected.schNo} (${fmtTime(selected.fstSchStrTime)}~${fmtTime(selected.fstSchEndTime)})`
      : ''
    emit('saved', selectedSchCd.value, newLabel)
    open.value = false
  } catch (e) {
    saveError.value = resolveApiErrorMessage(e, '기본 근무타입 변경 중 오류가 발생했어요.')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
/* ⚠️ BaseBottomSheet 의 footer 슬롯(.sch-edit__save 가 렌더링되는 위치)은 default 슬롯(.sch-edit)의
   DOM 형제(sibling)이지 하위(descendant)가 아니다 — .sch-edit 루트에 로컬 토큰을 선언해도
   footer 슬롯 콘텐츠까지는 상속되지 않는다(BaseBottomSheet.vue 구조 확인). 따라서 이 파일은
   :root/루트 클래스 재선언 대신, 다른 앱 화면과 동일하게 var(--x, 폴백값) 인라인 폴백을 전체
   선택자에 사용한다(전역 base.css 에 --color-primary/--color-danger 미정의 — MyPageView 등
   조상이 토큰을 선언한 상태로 렌더링되면 그 값을, 아니면 폴백값을 그대로 사용). */
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
</style>
