<template>
  <!--
    프라프타 홈페이지 공용 상단 네비게이션 (intro 전용)
    - 모든 intro 페이지(메인/회사소개/서비스소개/도입문의)에서 공유.
    - '로그인'은 자체 인증이 아니라 SafeNote 서비스 진입(SERVICE_PATH)으로 링크 이동.
  -->
  <header class="introh">
    <div class="introh__inner">
      <RouterLink class="introh__brand" to="/">
        <span class="introh__mark">프라프타</span>
        <span class="introh__sub">SAFENOTE</span>
      </RouterLink>

      <nav class="introh__links" :class="{ 'is-open': menuOpen }">
        <RouterLink to="/about" @click="closeMenu">회사 소개</RouterLink>
        <RouterLink to="/service" @click="closeMenu">서비스 소개</RouterLink>
        <RouterLink to="/pricing" @click="closeMenu">요금</RouterLink>
        <RouterLink to="/contact" @click="closeMenu">도입 문의</RouterLink>
        <button
          type="button"
          class="introh__login introh__login--mobile"
          @click="goService"
        >
          로그인
        </button>
      </nav>

      <div class="introh__right">
        <button type="button" class="introh__login" @click="goService">
          로그인
        </button>
        <button
          type="button"
          class="introh__hamburger"
          :aria-expanded="menuOpen ? 'true' : 'false'"
          aria-label="메뉴 열기"
          @click="toggleMenu"
        >
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";

// SafeNote 서비스(로그인/관리자) 진입 경로. 라우터 base 변경 시 이 상수만 갱신한다.
const SERVICE_PATH = "/safenote";

const router = useRouter();
const menuOpen = ref(false);

const toggleMenu = () => {
  menuOpen.value = !menuOpen.value;
};
const closeMenu = () => {
  menuOpen.value = false;
};
const goService = () => {
  closeMenu();
  router.push(SERVICE_PATH);
};
</script>

<style scoped>
.introh {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--color-primary-pressed);
  word-break: keep-all;
}
.introh__inner {
  max-width: 1080px;
  margin: 0 auto;
  height: var(--header-height);
  padding: 0 var(--header-padding-x);
  display: flex;
  align-items: center;
  gap: 24px;
}
.introh__brand {
  display: flex;
  align-items: baseline;
  gap: 8px;
  text-decoration: none;
}
.introh__mark {
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #fff;
  font-size: 20px;
}
.introh__sub {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.85);
}
.introh__links {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 26px;
}
.introh__links a {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  font-size: 15.5px;
  font-weight: 600;
}
.introh__links a:hover,
.introh__links a.router-link-active {
  color: #fff;
}
.introh__right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.introh__login {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: var(--btn-radius-lg);
  background: transparent;
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
  white-space: nowrap;
}
.introh__login:hover {
  background: #fff;
  color: var(--color-primary);
}
.introh__login--mobile {
  display: none;
}
.introh__hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 40px;
  height: 40px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--btn-radius);
  cursor: pointer;
}
.introh__hamburger span {
  display: block;
  width: 18px;
  height: 2px;
  margin: 0 auto;
  background: #fff;
}

@media (max-width: 860px) {
  .introh__links {
    position: absolute;
    top: var(--header-height);
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    background: var(--color-primary-hover);
    padding: 8px 0;
    display: none;
  }
  .introh__links.is-open {
    display: flex;
  }
  .introh__links a {
    padding: 12px var(--header-padding-x);
    font-size: 15px;
  }
  .introh__login--mobile {
    display: inline-flex;
    margin: 8px var(--header-padding-x);
  }
  .introh__right .introh__login {
    display: none;
  }
  .introh__hamburger {
    display: flex;
  }
}
</style>
