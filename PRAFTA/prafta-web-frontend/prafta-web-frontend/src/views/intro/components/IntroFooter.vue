<template>
  <!--
    프라프타 홈페이지 공용 푸터 (intro 전용)
    - 딥그린 배경(브랜드). 회사 정보·연락처·정책 링크 placeholder.
  -->
  <footer class="introf">
    <div class="introf__inner">
      <div class="introf__brand">
        <div class="introf__name">프라프타 <span>PRAFTA</span></div>
        <p class="introf__slogan">현장 관리, 플랫폼으로 완성하다</p>
      </div>

      <div class="introf__cols">
        <div class="introf__col">
          <h4>도입 문의</h4>
          <p>CEO 윤순기</p>
          <p><a href="tel:01077635257">010-7763-5257</a></p>
          <p><a href="mailto:dudjswp@gmail.com">dudjswp@gmail.com</a></p>
        </div>
        <div class="introf__col">
          <h4>바로가기</h4>
          <p><RouterLink to="/service">서비스 소개</RouterLink></p>
          <p><RouterLink to="/pricing">요금 안내</RouterLink></p>
          <p><RouterLink to="/contact">도입 문의</RouterLink></p>
          <p><a :href="SERVICE_PATH">서비스 로그인</a></p>
        </div>
        <div class="introf__col">
          <h4>정책</h4>
          <!-- TODO: 약관/개인정보처리방침 본문 확정 후 링크 연결 -->
          <p><a href="#" @click.prevent>개인정보처리방침</a></p>
          <p><a href="#" @click.prevent>이용약관</a></p>
        </div>
      </div>
    </div>

    <div class="introf__bottom">
      <div class="introf__bottom-inner">
        <span>© {{ year }} PRAFTA. All rights reserved.</span>
        <span class="introf__bizinfo"
          ><!-- TODO: 사업자등록정보 보강 -->세이프노트 · 근태·인사 + 산업안전
          통합 플랫폼</span
        >
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from "vue";

const SERVICE_PATH = "/safenote";
const year = computed(() => new Date().getFullYear());
</script>

<style scoped>
.introf {
  background: linear-gradient(
    160deg,
    var(--color-primary-pressed) 0%,
    #14532d 100%
  );
  color: #fff;
  word-break: keep-all;
}
.introf__inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 48px var(--header-padding-x) 32px;
  display: flex;
  flex-wrap: wrap;
  gap: 40px;
  justify-content: space-between;
}
.introf__name {
  font-size: 23px;
  font-weight: 800;
}
.introf__name span {
  font-size: 13px;
  font-weight: 700;
  opacity: 0.7;
  margin-left: 6px;
}
.introf__slogan {
  margin: 8px 0 0;
  font-size: 15.5px;
  color: rgba(255, 255, 255, 0.8);
}
.introf__cols {
  display: flex;
  flex-wrap: wrap;
  gap: 48px;
}
.introf__col h4 {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
}
.introf__col p {
  margin: 0 0 9px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.92);
}
.introf__col a {
  color: rgba(255, 255, 255, 0.92);
  text-decoration: none;
}
.introf__col a:hover {
  color: #fff;
  text-decoration: underline;
}
.introf__bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}
.introf__bottom-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 18px var(--header-padding-x);
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.6);
}

@media (max-width: 860px) {
  .introf__inner {
    flex-direction: column;
    gap: 28px;
  }
  .introf__cols {
    gap: 28px;
  }
}
</style>
