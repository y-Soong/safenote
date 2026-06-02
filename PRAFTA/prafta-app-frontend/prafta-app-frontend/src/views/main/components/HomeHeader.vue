<!--
  HomeHeader.vue — 사업장 정적 표시 + 알림 + 아바타
  - 상세 요청서 §3.1, §4.1
  - 사업장 셀렉터/드롭다운 없음 (소속 사업장만 사용)
-->
<template>
  <div class="home-header">
    <!-- 좌측: 사업장 정적 표시 + "소속" 배지 -->
    <div class="site">
      <svg class="icon site__pin" width="18" height="18" aria-hidden="true">
        <use href="#i-mappin" />
      </svg>
      <span class="site__name">{{ siteName }}</span>
      <span class="site__tag">소속</span>
    </div>

    <!-- 우측: 알림 벨 + 아바타 -->
    <div class="h-actions">
      <button
        type="button"
        class="h-bell"
        :aria-label="`알림 ${notificationCount}건`"
        @click="$emit('click:bell')"
      >
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-bell" />
        </svg>
        <span v-if="notificationCount > 0" class="h-bell__count">{{ notificationCount }}</span>
      </button>

      <button
        type="button"
        class="avatar"
        :aria-label="`${displayInitial} 프로필 메뉴`"
        @click="$emit('click:avatar')"
      >
        {{ displayInitial }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 소속 사업장명 (정적 표시)
  siteName: {
    type: String,
    default: '',
  },
  // 미확인 알림 카운트 (0 이면 배지 숨김)
  notificationCount: {
    type: Number,
    default: 0,
  },
  // 사용자 이니셜 2자 (예: "김민")
  userInitial: {
    type: String,
    default: '',
  },
})

defineEmits(['click:bell', 'click:avatar'])

// 사용자 이니셜이 빈 문자열이면 '?' 폴백
const displayInitial = computed(() => props.userInitial?.trim() || '?')
</script>

<style scoped>
.home-header {
  height: 56px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.site {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;
}
.site__pin {
  color: var(--color-primary);
  flex-shrink: 0;
}
.site__name {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.site__tag {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 500;
  border-radius: 4px;
}

.h-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.h-bell {
  position: relative;
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
.h-bell__count {
  position: absolute;
  top: 8px;
  right: 8px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: var(--color-danger);
  color: #ffffff;
  font-size: 11px;
  font-weight: 500;
  line-height: 18px;
  text-align: center;
  border-radius: var(--radius-full);
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 500;
  border: 0.5px solid var(--color-primary-tint-border);
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
