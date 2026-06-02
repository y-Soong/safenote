<!--
  NoticeListCard.vue — 공지사항 카드 (리스트형, 최대 3행)
  - 상세 요청서 §3.6, §4.6
  - 상세 §10 결정사항 11 (카드형 → 리스트형, 최대 3행)
  - 정렬·잘라내기는 부모/백엔드 책임. 본 컴포넌트는 받은 items 를 그대로 렌더링
-->
<template>
  <div class="card notice-card">
    <!-- 헤더 -->
    <div class="notice-head">
      <div class="notice-head-left">
        <p class="card-title">공지사항</p>
        <span v-if="unreadCount > 0" class="notice-count">{{ unreadCount }}</span>
      </div>
      <button
        type="button"
        class="head-chev"
        aria-label="전체보기"
        @click="$emit('click:more')"
      >
        <svg class="icon" width="18" height="18" aria-hidden="true">
          <use href="#i-chev" />
        </svg>
      </button>
    </div>

    <!-- 행 -->
    <div
      v-for="row in items"
      :key="row.noticeId"
      class="notice-row"
      role="button"
      tabindex="0"
      @click="$emit('click:row', row.noticeId)"
      @keydown.enter="$emit('click:row', row.noticeId)"
      @keydown.space.prevent="$emit('click:row', row.noticeId)"
    >
      <span v-if="row.isImportant" class="notice-row__imp">중요</span>
      <span class="notice-row__title" :class="{ 'notice-row__title--read': row.isRead }">
        {{ row.title }}
      </span>
      <span class="notice-row__meta">{{ row.displayTime }}</span>
      <span v-if="!row.isRead" class="notice-row__unread-dot" aria-label="미열람"></span>
    </div>

    <!-- 빈 상태 -->
    <div v-if="!items || items.length === 0" class="notice-empty">
      등록된 공지사항이 없습니다
    </div>
  </div>
</template>

<script setup>
defineProps({
  // 공지 행 배열 (최대 3행으로 잘라서 옴)
  // 각 행: { noticeId, isImportant, title, displayTime, isRead }
  items: {
    type: Array,
    default: () => [],
  },
  // 미열람 공지 카운트 (0 이면 배지 숨김)
  unreadCount: {
    type: Number,
    default: 0,
  },
})

defineEmits(['click:more', 'click:row'])

// TODO(developer): 정책 7.9 (정렬 규칙) / 7.10 (표시 개수) 확정 후 부모에서 정렬·슬라이스 처리
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--color-border);
  margin-bottom: 12px;
}

/* 공지 카드만 좌우 패딩 0, 상단 16px, 하단 8px — 행 구분선이 끝까지 닿도록 */
.notice-card {
  padding: 16px 0 8px;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 0 16px;
}

.notice-head-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-danger);
  color: #ffffff;
  font-size: 11px;
  font-weight: 500;
  line-height: 18px;
  text-align: center;
  border-radius: var(--radius-full);
}

.head-chev {
  background: transparent;
  border: 0;
  padding: 4px;
  min-width: 44px;
  min-height: 44px;
  margin: -10px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-family: inherit;
}

.notice-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-top: 0.5px solid var(--color-border-light);
  cursor: pointer;
}
.notice-row:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

.notice-row__title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-row__title--read {
  font-weight: 400;
  color: var(--color-text-secondary);
}

.notice-row__meta {
  font-size: 11px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.notice-row__unread-dot {
  width: 6px;
  height: 6px;
  background: var(--color-danger);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.notice-row__imp {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  background: var(--color-warning-tint);
  color: var(--color-warning-text-deep);
  font-size: 11px;
  font-weight: 500;
  border-radius: 4px;
  flex-shrink: 0;
}

.notice-empty {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
  border-top: 0.5px solid var(--color-border-light);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
