<template>
  <!-- intro 전용 라인 아이콘 (stroke 기반, 색은 부모 color 상속) -->
  <svg
    class="iicon"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    v-html="inner"
  ></svg>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({ name: { type: String, default: "" } });

// 라인 아이콘 path 세트
const ICONS = {
  // 근태/출퇴근 — 시계
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.2v5l3.2 2"/>',
  // 일용직/인력 — 사람들
  users:
    '<circle cx="9" cy="8" r="3.1"/><path d="M3.6 19a5.4 5.4 0 0 1 10.8 0"/><path d="M16 5.4a3 3 0 0 1 0 5.8"/><path d="M17.8 19a5.2 5.2 0 0 0-2.6-4.5"/>',
  // 연차/통계 — 캘린더+막대
  calendar:
    '<rect x="3.5" y="5" width="17" height="15.5" rx="2"/><path d="M3.5 9.2h17M8 3.5v3.2M16 3.5v3.2"/><path d="M8 17v-2.5M12 17v-4M16 17v-1.8"/>',
  // 위험성평가 — 경고 삼각형
  alert:
    '<path d="M12 4.2 20.5 19H3.5z"/><path d="M12 10v4"/><path d="M12 16.6v.2"/>',
  // TBM/안전점검 — 체크 클립보드
  clipboard:
    '<rect x="5" y="4.8" width="14" height="15.7" rx="2"/><path d="M9 4.8V3.6h6v1.2"/><path d="M9 12.6l2.1 2.1 4-4.2"/>',
  // AI — 칩
  cpu:
    '<rect x="6.5" y="6.5" width="11" height="11" rx="2"/><rect x="10" y="10" width="4" height="4" rx="0.6"/><path d="M10 6.5V3.6M14 6.5V3.6M10 20.4v-2.9M14 20.4v-2.9M6.5 10H3.6M6.5 14H3.6M20.4 10h-2.9M20.4 14h-2.9"/>',
  // 제조 — 공장
  factory:
    '<path d="M3.5 20.5V10l5.5 3.6V10l5.5 3.6V6.2h5.5v14.3z"/><path d="M3.5 20.5h17"/><path d="M7 17h2M12 17h2M17 17h1.6"/>',
  // 물류 — 트럭
  truck:
    '<rect x="2.5" y="7" width="11" height="8.6" rx="1.2"/><path d="M13.5 10h3.7l3.3 3.3v2.3h-7z"/><circle cx="7" cy="18" r="1.7"/><circle cx="17.3" cy="18" r="1.7"/>',
  // 건설 — 빌딩
  building:
    '<rect x="5.5" y="3.5" width="13" height="17" rx="1"/><path d="M5.5 20.5h13"/><path d="M9 7h2M13 7h2M9 11h2M13 11h2M9 15h2M13 15h2"/>',
  // 화학 — 플라스크
  flask:
    '<path d="M9.2 3.6h5.6M10.4 3.6v5.8L5.8 17.5a1.6 1.6 0 0 0 1.4 2.4h9.6a1.6 1.6 0 0 0 1.4-2.4l-4.6-8.1V3.6"/><path d="M7.6 15.2h8.8"/>',
};

const inner = computed(() => ICONS[props.name] || "");
</script>

<style scoped>
.iicon { display: block; }
</style>
