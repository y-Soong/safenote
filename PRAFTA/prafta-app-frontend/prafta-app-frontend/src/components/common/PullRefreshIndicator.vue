<!--
  PullRefreshIndicator.vue — 당겨서 새로고침 인디케이터(표시 전용).
  usePullToRefresh 컴포저블이 내려주는 indicatorProps 를 그대로 v-bind 한다.
    <PullRefreshIndicator v-bind="ptr.indicatorProps" />
  스크롤 컨테이너의 "맨 위 첫 자식"으로 배치해야 당긴 만큼 높이가 늘어나며 본문을 아래로 민다.
-->
<template>
  <div
    class="pull-refresh"
    :class="{ 'pull-refresh--animating': !isDragging }"
    :style="{ height: pullIndicatorHeight + 'px' }"
    aria-live="polite"
  >
    <span v-if="isRefreshing" class="pull-refresh__text">새로고침 중...</span>
    <span v-else-if="pullReady" class="pull-refresh__text">놓으면 새로고침</span>
    <span v-else-if="pullDistance > 0" class="pull-refresh__text">당겨서 새로고침</span>
  </div>
</template>

<script setup>
defineProps({
  isRefreshing: { type: Boolean, default: false },
  isDragging: { type: Boolean, default: false },
  pullReady: { type: Boolean, default: false },
  pullDistance: { type: Number, default: 0 },
  pullIndicatorHeight: { type: Number, default: 0 },
})
</script>

<style scoped>
/* 당겨서 새로고침 인디케이터 — 당김 거리에 따라 높이가 늘어났다 줄어든다 */
.pull-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  height: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}
/* 손가락을 뗀 뒤(또는 새로고침 중)에는 부드럽게 높이 전환, 당기는 중에는 즉시 반응 */
.pull-refresh--animating {
  transition: height 0.2s ease;
}
.pull-refresh__text {
  padding: 8px 0;
}
</style>
