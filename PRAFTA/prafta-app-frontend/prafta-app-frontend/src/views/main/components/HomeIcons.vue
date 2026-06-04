<!--
  HomeIcons.vue — 홈 화면 전용 인라인 SVG sprite
  - 시안(prafta_home_variations.html)의 <symbol> 정의 20개를 단일 컴포넌트로 묶음
  - 호출부에서 <svg class="icon"><use href="#i-mappin"/></svg> 패턴으로 재사용
  - CDN 의존 금지(상세 요청서 §8.1) — 모든 SVG 는 인라인
  - MainView 최상단에 1회 마운트되어 페이지 전역에서 use 참조 가능
-->
<template>
  <svg width="0" height="0" class="home-icons-sprite" aria-hidden="true" focusable="false">
    <defs>
      <!-- 상태바용 (현재 미사용이나 추후 모달/위젯에서 재사용 가능) -->
      <symbol
        id="i-signal"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M3 19h2" />
        <path d="M9 19v-4" />
        <path d="M15 19V9" />
        <path d="M21 19V5" />
      </symbol>
      <symbol
        id="i-wifi"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M12 18l.01 0" />
        <path d="M9.172 15.172a4 4 0 0 1 5.656 0" />
        <path d="M6.343 12.343a8 8 0 0 1 11.314 0" />
        <path d="M3.515 9.515c4.686 -4.687 12.284 -4.687 17 0" />
      </symbol>
      <symbol
        id="i-battery"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect x="2" y="8" width="18" height="8" rx="1" />
        <path d="M22 11v2" />
        <rect x="4" y="10" width="11" height="4" fill="currentColor" stroke="none" />
      </symbol>

      <!-- 위치 -->
      <symbol
        id="i-mappin"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="12" cy="11" r="3" />
        <path d="M17.657 16.657L13.414 20.9a2 2 0 0 1-2.827 0l-4.244-4.243a8 8 0 1 1 11.314 0z" />
      </symbol>
      <symbol
        id="i-mappinoff"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M5.6 5.6c-1.6 4.1 .4 8.4 2.4 10.4l4 4.2 1.4-1.4" />
        <path d="M19.66 13.69c1.39-3.5-.32-7.18-2.66-9.51a8 8 0 0 0-10.3-.83" />
        <path d="M9 9a3 3 0 0 0 4.5 2.6" />
        <line x1="3" y1="3" x2="21" y2="21" />
      </symbol>

      <!-- 헤더 -->
      <symbol
        id="i-bell"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path
          d="M10 5a2 2 0 0 1 4 0a7 7 0 0 1 4 6v3a4 4 0 0 0 2 3h-16a4 4 0 0 0 2 -3v-3a7 7 0 0 1 4 -6"
        />
        <path d="M9 17v1a3 3 0 0 0 6 0v-1" />
      </symbol>

      <!-- 진입(chevron) -->
      <symbol
        id="i-chev"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <polyline points="9 18 15 12 9 6" />
      </symbol>

      <!-- 차단/잠금 -->
      <symbol
        id="i-lock"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect x="5" y="11" width="14" height="10" rx="2" />
        <path d="M8 11V7a4 4 0 0 1 8 0v4" />
      </symbol>

      <!-- 안전활동 -->
      <symbol
        id="i-clipboard"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
        <rect x="9" y="3" width="6" height="4" rx="1" />
        <path d="M9 14l2 2 4-4" />
      </symbol>
      <symbol
        id="i-camera"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path
          d="M5 7h2l2-3h6l2 3h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2"
        />
        <circle cx="12" cy="13" r="3" />
        <path d="M16 6h4" />
        <path d="M18 4v4" />
      </symbol>

      <!-- TBM -->
      <symbol
        id="i-clock"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="12" cy="12" r="9" />
        <polyline points="12 7 12 12 15 14" />
      </symbol>
      <symbol
        id="i-check"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <polyline points="5 12 10 17 19 8" />
      </symbol>
      <symbol
        id="i-circlecheck"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="12" cy="12" r="9" />
        <polyline points="8 12 11 15 16 10" />
      </symbol>
      <symbol
        id="i-usercheck"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="9" cy="7" r="4" />
        <path d="M3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2" />
        <polyline points="15 11 17 13 21 9" />
      </symbol>
      <symbol
        id="i-caloff"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M4 7a2 2 0 0 1 2 -2h2" />
        <path d="M12 5h5a2 2 0 0 1 2 2v10" />
        <path d="M4 11v8a2 2 0 0 0 2 2h12" />
        <path d="M16 3v4" />
        <path d="M8 3v4" />
        <path d="M4 11h12" />
        <line x1="3" y1="3" x2="21" y2="21" />
      </symbol>

      <!-- 탭바 -->
      <symbol
        id="i-home"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M5 12l-2 0l9-9l9 9l-2 0" />
        <path d="M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-7" />
        <path d="M10 21v-6a2 2 0 0 1 2-2h0a2 2 0 0 1 2 2v6" />
      </symbol>
      <symbol
        id="i-cal"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect x="4" y="5" width="16" height="16" rx="2" />
        <line x1="16" y1="3" x2="16" y2="7" />
        <line x1="8" y1="3" x2="8" y2="7" />
        <line x1="4" y1="11" x2="20" y2="11" />
      </symbol>
      <symbol
        id="i-shield"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M12 3l8 4v6c0 4.4-3.6 8-8 8s-8-3.6-8-8V7l8-4z" />
      </symbol>
      <symbol
        id="i-monitor"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect x="3" y="4" width="18" height="12" rx="1" />
        <line x1="7" y1="20" x2="17" y2="20" />
        <line x1="9" y1="16" x2="9" y2="20" />
        <line x1="15" y1="16" x2="15" y2="20" />
      </symbol>
      <symbol
        id="i-user"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="12" cy="8" r="4" />
        <path d="M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1" />
      </symbol>
    </defs>
  </svg>
</template>

<script setup>
// 본 컴포넌트는 SVG <symbol> 정의만 담는다. 로직 없음.
</script>

<style scoped>
.home-icons-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
