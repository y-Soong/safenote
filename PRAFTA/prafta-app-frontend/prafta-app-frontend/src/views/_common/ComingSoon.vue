<template>
  <div class="coming-soon">
    <h1>준비 중입니다</h1>
    <p>{{ moduleLabel }} 화면은 곧 업데이트됩니다.</p>
    <button type="button" class="home-link" @click="onBack">뒤로가기</button>
  </div>
</template>

<script setup>
// 001-Phase1-F5: 관리자 모듈 빈 골격. query.module 로 전달된 모듈 키를 안내에 표기하고 뒤로가기를 제공한다.
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// query.module 이 있으면 "'MODULE'" 형태로 표기(없으면 일반 문구).
const moduleLabel = computed(() => (route.query?.module ? `'${route.query.module}'` : '이'))

// 뒤로가기: 히스토리가 있으면 back, 없으면 메인으로 복귀.
const onBack = () => {
  if (window.history.length > 1) router.back()
  else router.replace('/MainView')
}
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminLauncherView 세트와 동일 변수명) — 하드코딩 색상 금지(CLAUDE.md) */
.coming-soon {
  --color-primary: #409eff;
  --color-primary-hover: #66b1ff;
  --color-text-secondary: #666;
  --color-on-primary: #ffffff;
  --radius-sm: 6px;

  text-align: center;
  padding: 100px 20px;
}
.coming-soon h1 {
  font-size: 2.5rem;
  color: var(--color-primary);
  margin-bottom: 10px;
}
.coming-soon p {
  font-size: 1.2rem;
  color: var(--color-text-secondary);
}
.home-link {
  display: inline-block;
  margin-top: 20px;
  padding: 10px 16px;
  background-color: var(--color-primary);
  color: var(--color-on-primary);
  border-radius: var(--radius-sm);
  text-decoration: none;
}
.home-link:hover {
  background-color: var(--color-primary-hover);
}
</style>
