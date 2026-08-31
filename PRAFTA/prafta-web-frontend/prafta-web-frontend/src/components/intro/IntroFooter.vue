<template>
  <footer class="intro-footer">
    <div class="intro-footer__inner">
      <div class="intro-footer__top">
        <div class="intro-footer__company">
          <p class="intro-footer__company-name">프라프타</p>
          <p class="intro-footer__company-meta">
            대표 김여진 · 사업자등록번호 111-12-46366
          </p>
        </div>

        <nav class="intro-footer__sitemap" aria-label="사이트맵">
          <router-link
            v-for="link in sitemapLinks"
            :key="link.path"
            :to="link.path"
            >{{ link.label }}</router-link
          >
          <router-link to="/privacy" class="intro-footer__privacy"
            >개인정보처리방침</router-link
          >
        </nav>
      </div>

      <div class="intro-footer__bottom">
        <p class="intro-footer__slogan">
          현장이 돌아가는 것만으로 근태와 안전이 증명되는 회사를 만듭니다.
        </p>
        <div class="intro-footer__legal">
          <span>© {{ year }} PRAFTA. All rights reserved.</span>
          <router-link to="/account-deletion" class="intro-footer__util"
            >계정 삭제 안내</router-link
          >
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from "vue";

const year = computed(() => new Date().getFullYear());

const sitemapLinks = [
  { path: "/attendance", label: "근태관리" },
  { path: "/safety", label: "안전관리" },
  { path: "/pricing", label: "이용요금" },
  { path: "/", label: "회사소개" },
  { path: "/contact", label: "도입문의" },
];
</script>

<style scoped>
.intro-footer {
  border-top: 1px solid var(--color-border);
  background: var(--color-bg);
}
.intro-footer__inner {
  max-width: 1160px;
  margin: 0 auto;
  padding: 40px var(--header-padding-x) 24px;
}
.intro-footer__top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-border);
}
.intro-footer__company-name {
  font-weight: 800;
  color: var(--color-text-strong);
  margin: 0 0 4px;
}
.intro-footer__company-meta {
  font-size: var(--intro-text-xs);
  color: var(--color-text-muted);
  margin: 0;
}
.intro-footer__sitemap {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
}
.intro-footer__sitemap a {
  color: var(--color-text-muted);
  text-decoration: none;
}
.intro-footer__sitemap a:hover {
  color: var(--color-text-strong);
}
.intro-footer__sitemap a.intro-footer__privacy {
  font-weight: 700;
  color: var(--color-text-strong);
}
.intro-footer__bottom {
  padding-top: 20px;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.intro-footer__slogan {
  font-size: var(--intro-text-xs);
  color: var(--color-text-muted);
  margin: 0;
}
.intro-footer__legal {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: var(--intro-text-xs);
  color: var(--color-text-muted);
}
.intro-footer__util {
  color: var(--color-text-muted);
  text-decoration: underline;
}

@media (max-width: 767px) {
  .intro-footer__top,
  .intro-footer__bottom {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
