<!--
  AdminHeader.vue — 관리자 모드 상단 헤더 (신규)
  - 작업 ID: 001-P1-F7 (분해: .claude/requests/app_requests/001-phase1-admin-ui-redesign.md)
  - 사용자 모드 HomeHeader 패턴 차용(좌:복귀+사업장 / 우:액션 버튼).
      · 좌측: "사용자 모드" 복귀 버튼(좌측 상단) + 현재 사업장명 + (siteSwitchEnabled 시) "현장 전환" 트리거.
      · 우측: [설정] 버튼 1개(알림벨 없음 — 사용자 모드 아바타→마이페이지 대칭).
  - 표시/토글 전용 컴포넌트: props 렌더 + emit 만. 진입판정/라우팅은 부모(AdminLauncherView)·developer.
  - ⚠️ C1: 역할(AUTH_CD) 분기 없음. siteSwitchEnabled 는 서버 산출값을 그대로 받는다.
  - 디자인 토큰: 부모(.admin-launcher-view)에서 선언한 var(--...) 상속(MainView↔HomeHeader 패턴 동일).
-->
<template>
  <header class="admin-header">
    <!-- 좌측 묶음: 사용자 모드 복귀 버튼 + 사업장명/현장 전환 -->
    <div class="admin-header__left">
      <!-- 사용자 모드 복귀(좌측 상단) — 부모가 router.replace('/MainView') 처리 -->
      <button
        type="button"
        class="admin-header__back"
        aria-label="사용자 모드로 돌아가기"
        @click="$emit('click:back')"
      >
        <svg class="icon" width="20" height="20" aria-hidden="true">
          <use href="#i-admin-chev-left" />
        </svg>
        <span class="admin-header__back-label">사용자 모드</span>
      </button>

      <!-- 사업장명 + (다중 사업장 시) 현장 전환 트리거 -->
      <div class="admin-header__site">
        <svg class="icon admin-header__pin" width="18" height="18" aria-hidden="true">
          <use href="#i-admin-mappin" />
        </svg>
        <span class="admin-header__site-name">{{ siteName || '-' }}</span>

        <button
          v-if="siteSwitchEnabled"
          type="button"
          class="admin-header__switch"
          @click="$emit('click:site-switch')"
        >
          현장 전환
          <svg class="icon" width="16" height="16" aria-hidden="true">
            <use href="#i-admin-chev-down" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 우측: 설정 버튼(알림벨 없음) -->
    <button
      type="button"
      class="admin-header__settings"
      aria-label="설정"
      @click="$emit('click:settings')"
    >
      <svg class="icon" width="22" height="22" aria-hidden="true">
        <use href="#i-admin-settings" />
      </svg>
    </button>
  </header>
</template>

<script setup>
defineProps({
  // 현재 선택 사업장명(정적 표시)
  siteName: { type: String, default: '' },
  // 현장 전환 트리거 노출 여부(access-context.siteSwitchEnabled — 서버 산출, C1)
  siteSwitchEnabled: { type: Boolean, default: false },
})

// click:back → 사용자 모드 복귀(부모 router.replace), click:site-switch → 현장 전환 시트 오픈(부모),
//   click:settings → 설정 진입(developer)
defineEmits(['click:back', 'click:site-switch', 'click:settings'])
</script>

<style scoped>
.admin-header {
  height: 56px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
}

/* 좌측 묶음(복귀 버튼 + 사업장) */
.admin-header__left {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  min-width: 0;
}

/* 사용자 모드 복귀 버튼(좌측 상단) */
.admin-header__back {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  height: 36px;
  margin-left: -8px;
  padding: 0 var(--space-sm) 0 var(--space-xs);
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-family: inherit;
  font-size: 13px;
}
.admin-header__back-label {
  font-weight: 500;
  white-space: nowrap;
}

/* 좌측 사업장 영역 */
.admin-header__site {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  min-width: 0;
  padding: 0 var(--space-xs);
}
.admin-header__pin {
  color: var(--color-primary);
  flex-shrink: 0;
}
.admin-header__site-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.admin-header__switch {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  background: var(--color-primary-tint);
  border: 1px solid var(--color-primary-tint-border);
  border-radius: var(--radius-full);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}

/* 우측 설정 버튼 — HomeHeader 아바타와 동일한 hit area(44×44) */
.admin-header__settings {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
  padding: 0;
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
