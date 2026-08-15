<!--
  LeaveApplyMultiView.vue — 연차 기간(From-To) 신청 화면 (prafta-leavemulti, 앱)
  유형: frontend-screen (모바일 앱, 근로자)
  참조 패턴: views/leave/LeaveMoveRequestView.vue (헤더 + 본문 + 섹션 + DateStepperField)

  ★ 종일 연차 전용이다. 반차·반반차·시간차는 기존 단건 신청(/LeaveApply)을 쓴다.
  ★ 기존 단건 신청 화면(LeaveApplyView / LeaveApplyForm 1884줄)은 손대지 않는다 —
    별도 화면으로 분리해 기존 연차 기능의 회귀 위험을 0 으로 둔다.

  흐름
    1) 연차 종류 + 기간(From~To) 선택 → 미리보기 1회 조회(GET apply-multi-preview)
    2) 날짜별 체크리스트 3단 표시
         ☑ 신청 가능 · 근무계획 있음        (기본 체크)
         ☐ 신청 가능 · 스케줄 없음(주말/휴무) (기본 해제 — 체크하면 신청된다)
         ⊘ 신청 불가 + 사유                  (선택 불가)
       ※ 주말 자동 제외를 하지 않는 이유: 운영에 주말 근무자가 있고 토요일 종일 연차 승인 사례가
         실재한다. 단건 신청에도 휴일 검증이 없어, 자동 제외는 기능 후퇴가 된다.
    3) 제출(POST apply-multi) — 체크된 날짜 목록만 보낸다(범위가 아니라 목록이 서버 계약).
       신청 불가일이 섞이면 서버가 전체 거부하고 blockedDates 로 전부 알려준다(부분 성공 없음).
-->
<template>
  <div class="lam-view">
    <header class="lam-hd">
      <!-- ★아이콘은 인라인 SVG 로 직접 그린다. <use href="#..."> 스프라이트 참조는 그 <symbol> 을
           정의한 화면(LeaveMoveRequestView)에서만 유효해 여기서는 빈 버튼으로 렌더됐다. -->
      <button type="button" class="lam-hd__back" aria-label="뒤로" @click="onBack">
        <svg
          width="22"
          height="22"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <h1 class="lam-hd__title">기간 연차 신청</h1>
      <span class="lam-hd__spacer" aria-hidden="true"></span>
    </header>

    <main class="lam-body">
      <p v-if="metaLoading" class="lam-state">불러오는 중...</p>

      <template v-else>
        <!-- 연차 종류 — 종일 사용이 가능한 종류만 노출 -->
        <section class="lam-section">
          <h2 class="lam-section__title">연차 종류</h2>
          <select v-model="leaveCd" class="lam-select" @change="resetPreview">
            <option value="" disabled>연차 종류를 선택하세요</option>
            <!-- 잔여를 함께 보여준다 — 종류마다 잔여가 달라 선택 판단에 필요하다
                 (앱 홈의 '잔여 6.3일'은 전 종류 합계라 종류별 가용량과 다르다). -->
            <option v-for="t in fullDayLeaveTypes" :key="t.leaveCd" :value="t.leaveCd">
              {{ t.leaveNm }} (잔여 {{ fmtDays(t.balanceDays) }}일)
            </option>
          </select>
          <p class="lam-hint">기간 신청은 종일 연차만 가능합니다. 반차·시간차는 단건 신청을 이용해 주세요.</p>
        </section>

        <!-- 기간 -->
        <section class="lam-section">
          <h2 class="lam-section__title">기간</h2>
          <div class="lam-range">
            <DateStepperField v-model="fromDate" placeholder="시작일" @update:modelValue="resetPreview" />
            <span class="lam-range__tilde">~</span>
            <DateStepperField v-model="toDate" placeholder="종료일" @update:modelValue="resetPreview" />
          </div>
          <!-- From > To 역전 안내 — 서버도 거부하지만(COMMON_400_001) 조회 버튼을 누르기 전에 알린다. -->
          <p v-if="rangeReversed" class="lam-warn">
            종료일이 시작일보다 빠릅니다. 기간을 다시 선택해 주세요.
          </p>
          <button
            type="button"
            class="lam-btn lam-btn--sub"
            :disabled="!canPreview || previewLoading"
            @click="loadPreview"
          >
            {{ previewLoading ? '조회 중...' : '대상일 조회' }}
          </button>
        </section>

        <!-- 날짜별 체크리스트 -->
        <section v-if="days.length" class="lam-section">
          <h2 class="lam-section__title">신청 대상일</h2>

          <div class="lam-quick">
            <button type="button" class="lam-quick__btn" @click="checkAll(true)">전체 선택</button>
            <button type="button" class="lam-quick__btn" @click="checkAll(false)">전체 해제</button>
            <button type="button" class="lam-quick__btn" @click="checkDefaults">기본값</button>
          </div>

          <ul class="lam-days">
            <li
              v-for="d in days"
              :key="d.ymd"
              class="lam-day"
              :class="{
                'is-blocked': !d.selectable,
                'is-weekend': d.weekend || d.holiday,
                'is-checked': checked[d.ymd],
              }"
            >
              <label class="lam-day__label">
                <input
                  type="checkbox"
                  :disabled="!d.selectable"
                  :checked="!!checked[d.ymd]"
                  @change="toggle(d)"
                />
                <span class="lam-day__date">{{ fmt(d.ymd) }}({{ d.dow }})</span>
              </label>
              <span v-if="!d.selectable" class="lam-day__reason">
                {{ d.blockedReason || '신청 불가' }}
              </span>
              <span v-else-if="!d.hasSchedule" class="lam-day__note">
                {{ d.holiday ? '휴일' : '스케줄 없음' }}
              </span>
            </li>
          </ul>

          <div class="lam-summary">
            <span>선택 <b>{{ checkedCount }}</b>일</span>
            <span v-if="blockedCount > 0" class="lam-summary__blocked">
              · 신청 불가 {{ blockedCount }}일
            </span>
          </div>
          <p v-if="Number(shortageDays) > 0" class="lam-warn">
            기본 선택 기준 잔여가 {{ fmtShortage(shortageDays) }}일 부족합니다. 기간을 줄이거나 잔여를
            확인해 주세요.
          </p>
        </section>

        <!-- 사유 -->
        <section v-if="days.length" class="lam-section">
          <h2 class="lam-section__title">사유</h2>
          <textarea v-model="reason" class="lam-textarea" rows="3" placeholder="사유를 입력하세요"></textarea>
        </section>

        <!-- 결재선 — 결재가 필요한 종류일 때만.
             ★단건 화면과 동일한 구성(프리셋 전개 + 직접 추가). 서버는 approverUserCds 를 SSOT 로 받는다
               (배열 순서 = STEP_NO 이므로 재인덱싱 금지). 비우면 COMMON_400_001 로 거부된다. -->
        <section v-if="days.length && aprvRequired" class="lam-section">
          <h2 class="lam-section__title">결재선</h2>

          <div v-if="presets.length" class="lam-presets">
            <button
              v-for="p in presets"
              :key="p.presetId"
              type="button"
              class="lam-preset"
              :class="{ 'is-on': selectedPresetId === p.presetId }"
              @click="onSelectPreset(p)"
            >
              {{ p.presetNm }}
            </button>
          </div>

          <ul v-if="approverList.length" class="lam-aprv">
            <li v-for="(a, i) in approverList" :key="a.approverUserCd" class="lam-aprv__item">
              <span class="lam-aprv__step">{{ i + 1 }}</span>
              <span class="lam-aprv__name">{{ a.userNm }}</span>
              <span class="lam-aprv__meta">{{ a.rankNm || a.nodeNm || '' }}</span>
              <button type="button" class="lam-aprv__del" @click="onRemoveApprover(a)">✕</button>
            </li>
          </ul>
          <p v-else class="lam-hint">결재자를 지정해 주세요. 프리셋을 고르거나 직접 추가할 수 있어요.</p>

          <button type="button" class="lam-btn lam-btn--sub" @click="approverPickerOpen = true">
            결재자 추가
          </button>
        </section>
      </template>
    </main>

    <footer v-if="days.length" class="lam-foot">
      <button type="button" class="lam-btn" :disabled="!canSubmit || submitting" @click="onSubmit">
        {{ submitting ? '신청 중...' : `${checkedCount}일 신청하기` }}
      </button>
    </footer>

    <!-- 결재자 추가 시트 — 단건 화면과 동일한 공용 컴포넌트 재사용 -->
    <ApproverPickerSheet
      v-if="aprvRequired"
      v-model="approverPickerOpen"
      :excluded-user-cds="approverUserCds"
      @add="onAddApprovers"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'
import DateStepperField from '@/components/common/DateStepperField.vue'
import ApproverPickerSheet from '@/components/common/ApproverPickerSheet.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 ────────────────────────────────────────────────────────────────
const metaLoading = ref(true)
const leaveTypes = ref([])
const leaveCd = ref('')
const fromDate = ref('')
const toDate = ref('')
const reason = ref('')

const previewLoading = ref(false)
const days = ref([])
const checked = reactive({})
const shortageDays = ref(0)
const submitting = ref(false)

// ── 결재선 ───────────────────────────────────────────────────────────────
//   ★연차·월차는 aprvRequired=true 다(운영 실측). 결재자를 안 보내면 서버가
//     COMMON_400_001('요청 파라미터가 누락되었습니다')로 거부한다.
//   단건 화면과 동일하게 approverUserCds 를 SSOT 로 보낸다(배열 순서 = STEP_NO, 재인덱싱 금지).
const presets = ref([])
const selectedPresetId = ref('')
const approverList = ref([])
const approverPickerOpen = ref(false)

/** 선택한 종류가 결재 대상인지 — 메타의 aprvRequired 를 그대로 따른다. */
const aprvRequired = computed(() => {
  const t = (leaveTypes.value || []).find((x) => x.leaveCd === leaveCd.value)
  return !!t?.aprvRequired
})
const approverUserCds = computed(() => approverList.value.map((a) => a.approverUserCd))

/** 프리셋 선택 → steps 를 결재선으로 전개(순서 보존). 같은 프리셋 재선택 시 해제. */
const onSelectPreset = (preset) => {
  if (selectedPresetId.value === preset.presetId) {
    selectedPresetId.value = ''
    approverList.value = []
    return
  }
  selectedPresetId.value = preset.presetId
  approverList.value = (preset.steps || []).map((s) => ({
    approverUserCd: s.approverUserCd,
    userNm: s.userNm,
    rankNm: s.rankNm,
    nodeNm: s.nodeNm,
  }))
}

/** 시트에서 추가 — userCd 기준 중복 제거. */
const onAddApprovers = (picked) => {
  const exists = new Set(approverUserCds.value)
  for (const p of picked || []) {
    if (!p?.userCd || exists.has(p.userCd)) continue
    exists.add(p.userCd)
    approverList.value.push({
      approverUserCd: p.userCd,
      userNm: p.userNm,
      rankNm: p.rankNm,
      nodeNm: p.nodeNm,
    })
  }
  // 직접 추가하면 더 이상 프리셋과 동일하지 않으므로 선택 표시를 해제한다.
  selectedPresetId.value = ''
}

const onRemoveApprover = (a) => {
  approverList.value = approverList.value.filter((x) => x.approverUserCd !== a.approverUserCd)
  selectedPresetId.value = ''
}

// 종일 사용이 가능한 종류만 — 기간 신청은 종일 전용이다.
//   ★응답 필드는 allowedUnits(List<String>) 다. 종전에 useUnitType 을 보다가 전 종류가 걸러져
//     select 가 비어 있었다(apply-meta 의 LeaveTypeItem 에 useUnitType 필드 자체가 없다).
//   applicable=false(신청 불가 종류)도 제외한다. 최종 판정은 서버(ATTD_400_102).
const fullDayLeaveTypes = computed(() =>
  (leaveTypes.value || []).filter((t) => {
    if (t.applicable === false) return false
    const units = Array.isArray(t.allowedUnits) ? t.allowedUnits : []
    // allowedUnits 가 비어 오면(구버전 응답 등) 막지 않는다 — 서버가 최종 판정한다.
    return units.length === 0 || units.includes('00')
  }),
)

// From > To 역전 여부. DateStepperField 가 상호 제약을 걸지 않으므로 화면에서 판정한다.
const rangeReversed = computed(
  () => !!fromDate.value && !!toDate.value && toYmd(fromDate.value) > toYmd(toDate.value),
)

// 역전 상태면 조회 자체를 막는다(서버 왕복 없이 즉시 안내).
const canPreview = computed(
  () => !!leaveCd.value && !!fromDate.value && !!toDate.value && !rangeReversed.value,
)

const checkedDates = computed(() =>
  days.value.filter((d) => d.selectable && checked[d.ymd]).map((d) => d.ymd),
)
const checkedCount = computed(() => checkedDates.value.length)
const blockedCount = computed(() => days.value.filter((d) => !d.selectable).length)
// 결재 대상 종류인데 결재자가 없으면 제출을 막는다(서버 COMMON_400_001 을 미리 차단).
const canSubmit = computed(
  () =>
    checkedCount.value > 0 &&
    !!reason.value.trim() &&
    (!aprvRequired.value || approverUserCds.value.length > 0),
)

const onBack = () => router.back()

const fmt = (ymd) =>
  !ymd || ymd.length !== 8 ? ymd : `${ymd.slice(4, 6)}.${ymd.slice(6, 8)}`

// 'YYYY-MM-DD' → 'YYYYMMDD' (DateStepperField 는 하이픈 형식을 쓴다)
const toYmd = (v) => String(v || '').replace(/-/g, '')

/**
 * 연차 일수 표기 — 소수 2자리 반올림 + 의미 없는 뒤 0 제거.
 *   1.60000 → '1.6'   1.40000 → '1.4'   2.71251 → '2.71'   1.00000 → '1'
 *
 * ★1자리로 자르지 않는 이유: 연차는 시간차 환산 때문에 0.14285 같은 값이 실재한다.
 *   1자리면 부족 0.04 가 '0.0일 부족'으로 표시돼 "부족한데 0" 이라는 모순이 생긴다.
 *   2자리로 두되 뒤 0 을 없애면 대부분 1~2자리로 짧게 보이면서 그 모순도 안 생긴다.
 *   그래도 2자리에서 0 으로 떨어지는 극소량은 fmtShortage 가 따로 처리한다.
 */
const fmtDays = (v) => {
  const n = Number(v ?? 0)
  if (!isFinite(n)) return '0'
  return String(Math.round(n * 100) / 100)
}

/** 부족 일수 전용 — 2자리에서 0 으로 떨어지지만 실제로는 0 초과인 경우를 구분해 표기한다. */
const fmtShortage = (v) => {
  const n = Number(v ?? 0)
  if (n > 0 && Math.round(n * 100) / 100 === 0) return '0.01 미만'
  return fmtDays(n)
}

const resetPreview = () => {
  days.value = []
  Object.keys(checked).forEach((k) => delete checked[k])
  shortageDays.value = 0
}

// ── 미리보기 ────────────────────────────────────────────────────────────
//   범위 확정 직후 1회만 호출한다. 체크 토글은 로컬 처리(재호출 없음).
const loadPreview = async () => {
  if (!canPreview.value) return
  previewLoading.value = true
  try {
    const { data } = await api.get('/appApi/leaveflow/apply-multi-preview', {
      params: {
        leaveCd: leaveCd.value,
        fromYmd: toYmd(fromDate.value),
        toYmd: toYmd(toDate.value),
      },
    })
    days.value = Array.isArray(data?.days) ? data.days : []
    shortageDays.value = data?.shortageDays ?? 0
    Object.keys(checked).forEach((k) => delete checked[k])
    days.value.forEach((d) => {
      // 서버가 계산한 기본 체크(근무계획 기준, 무계획 구간은 달력 폴백)를 그대로 반영한다.
      checked[d.ymd] = !!d.defaultChecked
    })
  } catch (err) {
    resetPreview()
    showAlert(resolveApiErrorMessage(err, '대상일을 불러오지 못했어요.'))
  } finally {
    previewLoading.value = false
  }
}

const toggle = (d) => {
  if (!d.selectable) return
  checked[d.ymd] = !checked[d.ymd]
}
const checkAll = (v) => {
  days.value.forEach((d) => {
    if (d.selectable) checked[d.ymd] = v
  })
}
const checkDefaults = () => {
  days.value.forEach((d) => {
    checked[d.ymd] = !!d.defaultChecked
  })
}

// ── 제출 ────────────────────────────────────────────────────────────────
const onSubmit = async () => {
  if (!canSubmit.value || submitting.value) return
  const dates = checkedDates.value
  const ok = window.confirm(`${dates.length}일을 신청할까요?`)
  if (!ok) return

  submitting.value = true
  try {
    await api.post('/appApi/leaveflow/apply-multi', {
      leaveCd: leaveCd.value,
      // ★leaveType 은 apply-meta 응답(LeaveTypeItem)에 없는 필드다 — 보내지 않는다.
      //   서버가 leaveCd 로 종류를 조회해 판정하므로 불필요하다.
      dates,
      reason: reason.value,
      // 결재 대상이 아니면 undefined (단건 화면과 동일 규약).
      approverUserCds: aprvRequired.value ? approverUserCds.value : undefined,
    })
    await showAlert(`${dates.length}일 신청되었어요`)
    router.back()
  } catch (err) {
    const body = err?.response?.data
    // 잔여 부족(전체 거부) — 서버가 필요/배정/부족 일수를 함께 내려준다.
    //   잔여는 부여 유효기간 때문에 날짜마다 달라, 단순 합계로는 알 수 없는 값이다.
    if (body?.shortageDays !== undefined && Number(body.shortageDays) > 0) {
      await showAlert(
        `잔여 연차가 부족해 신청되지 않았어요.\n\n` +
          `· 필요 ${fmtDays(body.neededDays)}일\n` +
          `· 사용 가능 ${fmtDays(body.assignedDays)}일\n` +
          `· 부족 ${fmtShortage(body.shortageDays)}일\n\n` +
          `기간을 줄이거나 잔여를 확인해 주세요.`,
      )
      return
    }
    // 신청 불가일이 있으면 전체 거부하고 blockedDates 로 전부 내려준다(첫 건만 알려주지 않는다).
    const blocked = body?.blockedDates
    if (Array.isArray(blocked) && blocked.length) {
      const lines = blocked
        .slice(0, 8)
        .map((b) => `· ${fmt(b.date)} ${b.reason || ''}`)
        .join('\n')
      const more = blocked.length > 8 ? `\n외 ${blocked.length - 8}일` : ''
      await showAlert(`신청할 수 없는 날짜가 있어 신청되지 않았어요.\n${lines}${more}`)
      // 상태가 바뀐 것이므로 미리보기를 다시 받아 화면을 최신화한다.
      await loadPreview()
    } else {
      showAlert(resolveApiErrorMessage(err, '연차 신청 중 오류가 발생했습니다.'))
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    // 종류 메타 + 결재선 프리셋을 함께 받는다(단건 화면과 동일).
    const [metaRes, presetRes] = await Promise.all([
      api.get('/appApi/leaveflow/apply-meta'),
      api.get('/appApi/leaveflow/approval-presets'),
    ])
    leaveTypes.value = Array.isArray(metaRes?.data?.leaveTypes) ? metaRes.data.leaveTypes : []
    presets.value = Array.isArray(presetRes?.data?.presets) ? presetRes.data.presets : []
    // 기본 프리셋이 있으면 미리 전개해 둔다(관리자가 매번 고르지 않아도 되게).
    const def = presets.value.find((p) => p.defaultYn === 'Y' || p.defaultYn === true)
    if (def) onSelectPreset(def)
  } catch (err) {
    showAlert(resolveApiErrorMessage(err, '연차 종류를 불러오지 못했어요.'))
  } finally {
    metaLoading.value = false
  }
})
</script>

<style scoped>
.lam-view {
  /* ★디자인 토큰 선언 — 이 앱 프론트는 :root 전역 토큰이 없고 화면마다 자기 루트에 선언한다
     (LeaveApplyView / LeaveMoveRequestView / MyLeaveSummaryView 등 전부 동일 패턴).
     이 블록이 없으면 공용 자식 컴포넌트(BaseBottomSheet·ApproverPickerSheet)가 쓰는
     폴백 없는 var(--color-surface) 가 값 없음으로 풀려 시트가 투명하게 렌더된다
     (2026-08-15 실기기 결함 — 종전 --color-primary 미정의로 버튼색이 틀어진 것과 같은 뿌리).
     값은 LeaveApplyView 와 동일하게 맞춘다. */
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-primary-text-deep: #15803d;
  --color-primary-text-darkest: #14532d;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning: #f59e0b;
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

  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-bg, #f7f8fa);
}
.lam-hd {
  display: flex;
  align-items: center;
  padding: 0.6rem 0.75rem;
  background: #fff;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.lam-hd__back {
  border: none;
  background: transparent;
  padding: 0.2rem;
  cursor: pointer;
}
.lam-hd__title {
  flex: 1;
  text-align: center;
  font-size: 1rem;
  font-weight: 700;
  margin: 0;
  color: var(--color-text-strong, #111827);
}
.lam-hd__spacer {
  width: 22px;
}
.lam-body {
  flex: 1;
  padding: 0.75rem;
  overflow-y: auto;
}
.lam-state {
  text-align: center;
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.9rem;
  padding: 2rem 0;
}
.lam-section {
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.6rem;
  padding: 0.75rem;
  margin-bottom: 0.6rem;
}
.lam-section__title {
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
  color: var(--color-text-strong, #111827);
}
.lam-select,
.lam-textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 0.4rem;
  font-size: 0.9rem;
  font-family: inherit;
  color: var(--color-text-strong, #111827);
  background: #fff;
}
.lam-hint {
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0.4rem 0 0;
  line-height: 1.5;
}
.lam-range {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.lam-range__tilde {
  color: var(--color-text-muted, #9ca3af);
}
.lam-quick {
  display: flex;
  gap: 0.35rem;
  margin-bottom: 0.5rem;
}
.lam-quick__btn {
  flex: 1;
  padding: 0.3rem;
  font-size: 0.75rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 0.35rem;
  background: #fff;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  font-family: inherit;
}
.lam-days {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 46vh;
  overflow-y: auto;
}
.lam-day {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.4rem;
  padding: 0.4rem 0.2rem;
  border-bottom: 1px solid var(--color-border-weak, #f3f4f6);
}
.lam-day__label {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  cursor: pointer;
}
.lam-day__date {
  font-size: 0.85rem;
  color: var(--color-text-strong, #111827);
}
/* 신청 불가(⊘) — 사유와 함께 흐리게. 선택 자체가 막힌다. */
.lam-day.is-blocked {
  opacity: 0.55;
}
.lam-day.is-blocked .lam-day__label {
  cursor: not-allowed;
}
.lam-day__reason {
  font-size: 0.72rem;
  color: var(--color-warning-text, #b45309);
  text-align: right;
  line-height: 1.4;
}
/* 기본 해제(☐) — 주말·휴무. 선택은 가능하다는 걸 흐린 안내로 알린다. */
.lam-day__note {
  font-size: 0.72rem;
  color: var(--color-text-muted, #9ca3af);
}
.lam-summary {
  margin-top: 0.5rem;
  font-size: 0.82rem;
  color: var(--color-text-strong, #111827);
}
.lam-summary__blocked {
  color: var(--color-warning-text, #b45309);
}
.lam-warn {
  margin: 0.4rem 0 0;
  font-size: 0.78rem;
  color: var(--color-danger, #ef4444);
  line-height: 1.5;
}
/* 결재선 — 프리셋 칩 + 지정된 결재자 목록 */
.lam-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  margin-bottom: 0.5rem;
}
.lam-preset {
  padding: 0.25rem 0.6rem;
  font-size: 0.78rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 1rem;
  background: #fff;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  font-family: inherit;
}
.lam-preset.is-on {
  border-color: #16a34a;
  background: #dcfce7;
  color: #16a34a;
  font-weight: 700;
}
.lam-aprv {
  list-style: none;
  margin: 0 0 0.4rem;
  padding: 0;
}
.lam-aprv__item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0;
  border-bottom: 1px solid var(--color-border-weak, #f3f4f6);
}
.lam-aprv__step {
  width: 1.15rem;
  height: 1.15rem;
  flex: none;
  border-radius: 50%;
  background: #dcfce7;
  color: #16a34a;
  font-size: 0.7rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.lam-aprv__name {
  font-size: 0.85rem;
  color: var(--color-text-strong, #111827);
}
.lam-aprv__meta {
  flex: 1;
  font-size: 0.72rem;
  color: var(--color-text-muted, #9ca3af);
}
.lam-aprv__del {
  border: none;
  background: transparent;
  color: var(--color-text-muted, #9ca3af);
  cursor: pointer;
  font-size: 0.8rem;
  padding: 0.1rem 0.3rem;
  font-family: inherit;
}

.lam-foot {
  padding: 0.6rem 0.75rem calc(0.6rem + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid var(--color-border, #e5e7eb);
}
/* ★색상은 앱 실제 팔레트(assets/css/button.css 의 .btn-primary)와 동일한 #16a34a 를 직접 쓴다.
   이 앱 프론트에는 --color-primary 토큰이 정의돼 있지 않아, var(--color-primary, …) 로 두면
   폴백값(#30796a)이 그대로 나와 다른 화면 버튼과 톤이 어긋난다(실기기에서 확인됨).
   button.css 는 전역 import 대상이 아니라 .btn-primary 클래스도 쓸 수 없다. */
.lam-btn {
  width: 100%;
  padding: 0.7rem;
  border: none;
  border-radius: 0.5rem;
  background: #16a34a;
  color: #fff;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s;
}
.lam-btn:hover:not(:disabled) {
  background: #15803d;
}
.lam-btn:disabled {
  background: var(--color-border, #d1d5db);
  cursor: not-allowed;
}
.lam-btn--sub {
  margin-top: 0.5rem;
  padding: 0.5rem;
  font-size: 0.85rem;
  background: #dcfce7;
  color: #16a34a;
}
.lam-btn--sub:hover:not(:disabled) {
  background: #bbf7d0;
}
</style>
