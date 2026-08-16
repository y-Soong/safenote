<!--
  AdminApprovalGroupCard.vue — 연차 기간(From-To) 신청 묶음 카드
  - 작업 ID: PRAFTA-APP-LM-4 (분해: .claude/requests/app_requests/작업지시서_앱-승인관리-연차묶음일괄승인.plan.md §3 / §4)
  - 태그: prafta-leavemulti (웹 Attd_10.vue 묶음 구간과 같은 grep 키)
  - web 단일 출처: views/attd/Attd_10.vue 의 묶음 1행 + ▸ 펼침 + 일괄 처리
  - 정책: request-approval/07-interactions.md §7.7(일괄 처리) / §7.4(마감 차단)
  - props.item: 서버가 groupLeave=Y 로 접어 내린 묶음 PendingItem
      { reqId(대표=첫날), group:'LEAVE', reqTypeNm, requesterUserNm, nodeNm, reqDateDisplay,
        leaveGroupId, groupCount, groupDays, groupFromYmdDisplay, groupToYmdDisplay,
        selfYn, borrowDays,
        groupItems:[{ reqId, approvalStep, targetYmdDisplay, leaveDays, unitNm, selfYn, borrowDays }] }
  - ★단건 카드(AdminApprovalCard.vue)는 건드리지 않는다 — 단일일 신청 무회귀를 구조로 보장.
  - ★본인 신청(selfYn='Y') 은 배지로만 표시한다. 2026-08-16 사용자 확정으로 "관리자 본인 결재 차단"이
    전 유형에서 해제되어(백엔드 게이트 제거 완료) 일괄 전송에서도 제외하지 않는다.
    (분해 문서 §2-4 / §5-1 의 "본인 건 일괄 제외·전건 본인 시 버튼 비활성" 명세는 폐기)
  - 디자인 토큰: 부모(.admin-approval-view)에서 선언한 var(--...) 상속(자체 오버레이·고정 요소 없음).
-->
<template>
  <article class="apg" :class="{ 'apg--busy': submitting }">
    <!-- 상단: 유형 + 배지 -->
    <header class="apg__top">
      <span class="apg__type">{{ item.reqTypeNm || '연차' }}</span>
      <span class="apg__badges">
        <span class="apg__chip apg__chip--group">기간 {{ item.groupCount || 0 }}건</span>
        <span v-if="item.selfYn === 'Y'" class="apg__chip apg__chip--self">본인</span>
        <span v-if="Number(item.borrowDays) > 0" class="apg__chip apg__chip--borrow">가불</span>
      </span>
    </header>

    <!-- 본문: 토글 + 요청자 + 기간 요약 -->
    <div class="apg__body">
      <button
        type="button"
        class="apg__toggle"
        :aria-expanded="expanded"
        :aria-label="expanded ? '접기' : '펼쳐서 개별 처리'"
        @click="$emit('toggle', item.leaveGroupId)"
      >
        <svg class="icon" width="16" height="16" aria-hidden="true">
          <use :href="expanded ? '#i-apg-chev-down' : '#i-apg-chev-right'" />
        </svg>
      </button>
      <div class="apg__who">
        <p class="apg__requester">
          <span class="apg__requester-name">{{ item.requesterUserNm || '-' }}</span>
          <span v-if="item.nodeNm" class="apg__requester-dept">{{ item.nodeNm }}</span>
        </p>
        <p class="apg__range">{{ rangeText }}</p>
      </div>
    </div>

    <!-- 펼침: 날짜별 개별 건(탭 → 기존 단건 상세 경로) -->
    <ul v-if="expanded" class="apg__items">
      <li
        v-for="child in childItems"
        :key="child.reqId"
        class="apg__item"
        role="button"
        tabindex="0"
        @click="$emit('select-child', child)"
      >
        <span class="apg__item-date">{{ child.targetYmdDisplay }}</span>
        <span class="apg__item-unit">{{ child.unitNm || '종일' }}</span>
        <span class="apg__item-days">{{ formatLeaveDaysOnly(child.leaveDays) }}</span>
        <span v-if="child.selfYn === 'Y'" class="apg__chip apg__chip--self">본인</span>
        <svg class="icon apg__item-chev" width="16" height="16" aria-hidden="true">
          <use href="#i-apg-chev-right" />
        </svg>
      </li>
    </ul>

    <!-- 상한 초과 안내(서버 400 을 그대로 노출하지 않는다) -->
    <!-- ★:disabled 만 두고 안내를 생략하면 "눌러도 아무 일이 없다"로 오인된다 -->
    <p v-if="overLimit" class="apg__note apg__note--block">
      일괄 처리는 한 번에 최대 {{ MAX_BULK_ITEMS }}건까지 가능합니다. 펼쳐서 날짜별로 처리해 주세요.
    </p>

    <!-- 하단: 요청일시 + 일괄 액션 -->
    <footer class="apg__meta">
      <span class="apg__meta-date">{{ item.reqDateDisplay || '' }}</span>
      <span class="apg__actions">
        <button
          type="button"
          class="apg__btn apg__btn--primary"
          :disabled="!bulkEnabled"
          @click="$emit('bulk', { decision: 'APPROVE_ASIS' })"
        >
          일괄 승인
        </button>
        <button
          type="button"
          class="apg__btn apg__btn--danger-ghost"
          :disabled="!bulkEnabled"
          @click="$emit('bulk', { decision: 'REJECT' })"
        >
          일괄 반려
        </button>
      </span>
    </footer>

    <!-- 아이콘 스프라이트(카드 내부 자급 — 리스트가 여러 장 렌더해도 id 중복은 무해) -->
    <svg width="0" height="0" class="apg__sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-apg-chev-right"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol
          id="i-apg-chev-down"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="6 9 12 15 18 9" />
        </symbol>
      </defs>
    </svg>
  </article>
</template>

<script setup>
import { computed } from 'vue'

// 일 단위 수량 표기 단일 출처(2026-08-09 규약) — 인라인 포맷 금지
import { formatLeaveDaysOnly } from '@/utils/leaveFormat'

const props = defineProps({
  // 서버가 접어 내린 묶음 아이템(groupItems 포함)
  item: { type: Object, required: true },
  // 펼침 여부(부모 리스트가 보유)
  expanded: { type: Boolean, default: false },
  // 부모가 bulk-process 호출 중일 때 버튼 잠금
  submitting: { type: Boolean, default: false },
})

// toggle: 펼침 토글 / select-child: 개별 건 상세 진입 / bulk: 일괄 처리 요청
defineEmits(['toggle', 'select-child', 'bulk'])

/**
 * 일괄 전송 items 상한.
 * 백엔드 AppAdminApprovalBulkServiceImpl.MAX_ITEMS 와 같은 값이며, 초과 시 서버가 400 을 던진다.
 * 서버 400 을 그대로 노출하지 않도록 화면이 먼저 안내한다(표시 제어일 뿐 인가가 아니다).
 * ★기간신청 상한(62일)보다 커야 한다 — 작으면 최대 묶음이 일괄에서 통째로 거부된다.
 */
const MAX_BULK_ITEMS = 70

// 묶음 내 개별 건(서버가 날짜 오름차순으로 내려준다)
const childItems = computed(() =>
  Array.isArray(props.item?.groupItems) ? props.item.groupItems : [],
)

// 기간 요약 표시("2026.03.02 ~ 2026.03.06 · 총 5일")
const rangeText = computed(() => {
  const from = props.item?.groupFromYmdDisplay || ''
  const to = props.item?.groupToYmdDisplay || ''
  if (!from) return ''
  return `${from} ~ ${to} · 총 ${formatLeaveDaysOnly(props.item?.groupDays)}`
})

// 상한 초과 여부(안내 문구 + 버튼 비활성 사유)
const overLimit = computed(() => childItems.value.length > MAX_BULK_ITEMS)

// 일괄 버튼 활성 조건(표시 제어일 뿐 인가가 아니다 — 서버가 최종 판정)
const bulkEnabled = computed(
  () => !props.submitting && childItems.value.length > 0 && !overLimit.value,
)
</script>

<style scoped>
.apg {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  text-align: left;
  font-family: inherit;
}
.apg--busy {
  opacity: 0.65;
}

/* 상단 */
.apg__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-sm);
}
.apg__type {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.apg__badges {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-xs);
}
.apg__chip {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 600;
}
.apg__chip--group {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.apg__chip--self,
.apg__chip--borrow {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}

/* 본문 */
.apg__body {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
}
.apg__toggle {
  width: 28px;
  height: 28px;
  margin-left: -4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-family: inherit;
}
.apg__who {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.apg__requester {
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.apg__requester-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.apg__requester-dept {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.apg__range {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 펼침 목록 */
.apg__items {
  list-style: none;
  margin: 0;
  padding: var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}
.apg__item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  min-height: 36px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
.apg__item-date {
  font-weight: 600;
  color: var(--color-text-primary);
}
.apg__item-days {
  margin-left: auto;
}
.apg__item-chev {
  color: var(--color-text-tertiary);
}

/* 안내 문구 */
.apg__note {
  margin: 0;
  font-size: 12px;
  line-height: 17px;
  color: var(--color-text-tertiary);
}
.apg__note--block {
  color: var(--color-warning-text);
}

/* 하단 */
.apg__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
  padding-top: var(--space-sm);
  border-top: 0.5px solid var(--color-border-light);
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}
.apg__actions {
  display: inline-flex;
  gap: var(--space-xs);
}
.apg__btn {
  height: 34px;
  padding: 0 var(--space-md);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.apg__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.apg__btn--danger-ghost {
  background: var(--color-surface);
  color: var(--color-danger-text);
  border: 1px solid var(--color-border);
}
.apg__btn:disabled {
  background: var(--color-disabled-bg);
  color: var(--color-disabled-text);
  border-color: var(--color-border);
  cursor: not-allowed;
}

.apg__sprite {
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
