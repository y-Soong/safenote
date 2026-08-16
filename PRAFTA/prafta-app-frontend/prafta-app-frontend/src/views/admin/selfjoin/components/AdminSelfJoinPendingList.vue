<!--
  AdminSelfJoinPendingList.vue — 셀프가입 승인 대기 리스트(카드 인라인)
  - 작업 ID: A6
  - 백엔드: GET /appApi/admin/self-join/pending?siteCd=&nodeCd=&incSubNodeYn=Y&userKeyword=
      · 응답: { selfJoinList: Row[] }  ※ 페이징 없음(결정 M — 서버가 전건 반환)
      · accountStatus 는 서버가 '06' 고정. 클라이언트가 보내지 않는다(앱 요청 DTO 에 필드 자체가 없다).
  - 항목은 카드 인라인(별도 컴포넌트 신설 안 함 — AdminApprovalHistoryList 관례).
  - ★PII 는 웹 User_09 대기 목록과 동일 집합(이름/아이디/부서/마스킹 휴대폰/신청일시)만 표시한다.
      이메일·생년월일·권한·고용형태를 추가하지 않는다. 휴대폰은 서버 마스킹값을 그대로 쓴다(앱 재가공 금지).
  - 디자인 토큰: 부모(.admin-selfjoin-view) 상속. 하드코딩 색/픽셀 금지.
-->
<template>
  <div class="sjp">
    <!-- 로딩 -->
    <p v-if="isLoading && items.length === 0" class="sjp-state" aria-live="polite">
      불러오는 중...
    </p>

    <!-- 빈 상태: 전체 0 -->
    <p
      v-else-if="!isLoading && items.length === 0 && !hasFilter"
      class="sjp-state"
      aria-live="polite"
    >
      승인 대기 중인 가입 신청이 없습니다.
    </p>

    <!-- 빈 상태: 필터 0 -->
    <div
      v-else-if="!isLoading && items.length === 0 && hasFilter"
      class="sjp-state sjp-state--filtered"
      aria-live="polite"
    >
      <p class="sjp-state__text">조건에 맞는 신청이 없습니다.</p>
      <button type="button" class="sjp-state__reset" @click="$emit('reset-filters')">
        필터 해제
      </button>
    </div>

    <!-- 카드 리스트 -->
    <template v-else>
      <article v-for="item in items" :key="item.userCd" class="sjc">
        <header class="sjc__top">
          <span class="sjc__name">{{ item.userNm || '-' }}</span>
          <span v-if="item.nodeNm" class="sjc__dept">{{ item.nodeNm }}</span>
        </header>

        <div class="sjc__body">
          <p class="sjc__line">{{ item.userId || '-' }}</p>
          <!-- 휴대폰은 서버에서 마스킹되어 내려온다(평문/암호문 미수신) -->
          <p class="sjc__line">{{ item.mblNo || '-' }}</p>
        </div>

        <footer class="sjc__meta">
          <span class="sjc__date">신청 {{ item.applyDtime || '-' }}</span>
          <span class="sjc__actions">
            <button
              type="button"
              class="sjc__btn sjc__btn--ghost"
              :disabled="submitting"
              @click="$emit('reject', item)"
            >
              거부
            </button>
            <button
              type="button"
              class="sjc__btn sjc__btn--primary"
              :disabled="submitting"
              @click="$emit('approve', item)"
            >
              승인
            </button>
          </span>
        </footer>
      </article>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveApiErrorMessage } from '@/utils/apiError'

const props = defineProps({
  // 조회 스코프(부모 소유). nodeCd 가 null 이면 전사역할 = 사업장 전체.
  siteCd: { type: String, default: '' },
  nodeCd: { type: String, default: null },
  keyword: { type: String, default: '' },
  // 부모가 승인/거부 API 호출 중일 때 카드 버튼 잠금(중복 제출 차단)
  submitting: { type: Boolean, default: false },
})

// update:total → 셸 배지 / approve|reject → 시트 오픈 / reset-filters → 셸 필터 초기화
const emit = defineEmits(['update:total', 'approve', 'reject', 'reset-filters'])

const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — AdminApprovalPendingList 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 데이터 상태(서버 응답으로 채움) ───────────────────────────────────
const items = ref([])
const isLoading = ref(false)

// 동시 호출 가드(부서 칩/검색 빠른 연속 변경) — 가장 최신 요청만 반영
let inflightSeq = 0

// 필터가 걸려 있는지(빈 상태 문구 분기용) — 검색어 유무만 본다.
//   부서 칩은 노드관리자에게 항상 걸려 있어 필터로 세면 문구가 늘 "조건에 맞는..."이 된다.
const hasFilter = computed(() => !!props.keyword)

// 조회 파라미터. 식별자(회사/요청자)는 axios 인터셉터의 토큰 클레임 경유 — 바디/쿼리로 보내지 않는다.
//   nodeCd 는 값이 있을 때만 싣는다(전사역할은 미전송 = 사업장 전체).
const buildParams = () => {
  const params = { incSubNodeYn: 'Y' }
  if (props.siteCd) params.siteCd = props.siteCd
  if (props.nodeCd) params.nodeCd = props.nodeCd
  if (props.keyword) params.userKeyword = props.keyword
  return params
}

const load = async () => {
  const mySeq = ++inflightSeq
  isLoading.value = true

  try {
    const res = await api.get('/appApi/admin/self-join/pending', { params: buildParams() })
    if (mySeq !== inflightSeq) return

    const data = res?.data || {}
    items.value = Array.isArray(data.selfJoinList) ? data.selfJoinList : []
    // 대기 탭은 페이징이 없어 목록 길이가 곧 건수다(결정 M).
    emit('update:total', items.value.length)
  } catch (e) {
    if (mySeq !== inflightSeq) return
    // 401(토큰) 은 axios 인터셉터가 처리. 그 외(403 권한 포함)는 반드시 사유를 알린다 —
    //   403 을 조용한 빈 목록으로 바꾸면 권한 문제가 "데이터 없음"으로 오인된다.
    console.error('[AdminSelfJoinPendingList] 대기 목록 조회 실패')
    showAlert(
      resolveApiErrorMessage(e, '가입 신청 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'),
    )
    items.value = []
    emit('update:total', 0)
  } finally {
    if (mySeq === inflightSeq) isLoading.value = false
  }
}

// 부모(셸)가 승인/거부 성공 후 재조회를 호출한다.
const reload = () => load()

watch(
  () => [props.siteCd, props.nodeCd, props.keyword],
  () => load(),
)

onMounted(load)

defineExpose({ reload })
</script>

<style scoped>
.sjp {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  min-height: 0;
}

.sjp-state {
  margin: 0;
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.sjp-state--filtered {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}
.sjp-state__text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.sjp-state__reset {
  height: 36px;
  padding: 0 var(--space-lg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 대기 카드 */
.sjc {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}
.sjc__top {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.sjc__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.sjc__dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.sjc__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sjc__line {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
.sjc__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding-top: 8px;
  border-top: 0.5px solid var(--color-border-light);
}
.sjc__date {
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
.sjc__actions {
  display: inline-flex;
  gap: var(--space-sm);
}
.sjc__btn {
  height: 34px;
  padding: 0 var(--space-md);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.sjc__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.sjc__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.sjc__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
