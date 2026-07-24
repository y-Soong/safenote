<template>
  <!--
    계정 삭제 안내 공개 페이지 (비로그인, '/account-deletion')
    - 구글 플레이 정책: 앱 내 계정 생성이 있는 앱은 로그인 없이 접근되는
      계정 삭제 요청 안내 URL 을 Play Console 에 제출해야 한다.
    - 삭제 범위/법정 보존 예외는 개인정보 처리방침(약관 002)과 동일 기준 — 002 개정 시 함께 검토.
    - 스타일/레이아웃은 PrivacyPolicyView 와 동일 톤.
  -->
  <div class="deletion">
    <header class="deletion-nav">
      <div class="deletion-nav__inner">
        <router-link class="deletion-brand" to="/">
          <span class="deletion-brand__mark">PRAFTA</span>
        </router-link>
        <router-link class="deletion-nav__home" to="/">홈으로</router-link>
      </div>
    </header>

    <main class="deletion-body">
      <article class="deletion-doc">
        <h1 class="deletion-doc__title">계정 및 데이터 삭제 안내</h1>
        <p class="deletion-doc__meta">PRAFTA 앱 이용자용</p>

        <p>
          프라프타(PRAFTA)(이하 "회사"라 합니다)는 PRAFTA 서비스 이용자가
          언제든지 계정 및 관련 데이터의 삭제를 요청할 수 있도록 다음의 절차를
          제공합니다.
        </p>

        <section>
          <h2>1. 삭제 요청 방법</h2>
          <p>아래 두 가지 방법 중 편한 방법으로 요청할 수 있습니다.</p>
          <ul>
            <li>
              <strong>소속 회사 관리자를 통한 요청</strong> — 소속
              회사(고객사)의 PRAFTA 관리자에게 계정 삭제(퇴직 처리)를 요청하면
              관리자 웹에서 계정이 삭제 처리됩니다.
            </li>
            <li>
              <strong>회사에 직접 요청</strong> — 아래 연락처로 계정 삭제를
              요청할 수 있습니다. 본인 확인을 위하여 성명, 아이디, 소속 회사,
              가입 시 등록한 휴대폰번호를 함께 알려주시기 바랍니다.
              <br />이메일: aqswgg@gmail.com / 전화: 010-7763-5257
            </li>
          </ul>
          <p>
            일용근로자 계정(휴대폰번호 기반 가입)도 동일한 방법으로 요청할 수
            있습니다.
          </p>
        </section>

        <section>
          <h2>2. 처리 절차 및 기한</h2>
          <ul>
            <li>본인 확인 완료 후 지체 없이 삭제를 처리합니다.</li>
            <li>처리 결과는 요청 시 사용한 연락처로 회신합니다.</li>
          </ul>
        </section>

        <section>
          <h2>3. 삭제되는 데이터</h2>
          <ul>
            <li>
              계정 정보: 성명, 아이디, 비밀번호, 휴대폰번호, 이메일, 생년월일,
              성별, 소속 정보
            </li>
            <li>기기 정보: 기기 식별자, 푸시 알림 토큰</li>
            <li>서비스 이용 데이터 중 법정 보존 대상이 아닌 정보</li>
          </ul>
        </section>

        <section>
          <h2>4. 법령에 따라 일정 기간 보존되는 데이터</h2>
          <p>
            다음 기록은 관련 법령에 따라 보존 의무가 있어 즉시 삭제되지 않으며,
            보존기간 경과 후 복구 불가능한 방법으로 파기됩니다. 보존기간 동안
            해당 기록은 다른 목적으로 이용되지 않습니다.
          </p>
          <ul>
            <li>근로 관련 기록(근태, 연차 등): 근로기준법에 따라 3년</li>
            <li>산업안전보건 관련 기록: 산업안전보건법이 정한 기간</li>
          </ul>
        </section>

        <section>
          <h2>5. 참고</h2>
          <ul>
            <li>
              계정 삭제 후에는 동일 아이디로 로그인할 수 없으며, 서비스 이용을
              위해서는 소속 회사 관리자를 통하여 다시 등록하여야 합니다.
            </li>
            <li>
              개인정보 처리에 관한 자세한 내용은
              <router-link to="/privacy">개인정보 처리방침</router-link>을
              참고하시기 바랍니다.
            </li>
          </ul>
        </section>
      </article>
    </main>

    <footer class="deletion-footer">
      <div class="deletion-footer__inner">
        <span>© {{ year }} PRAFTA. All rights reserved.</span>
        <router-link to="/privacy">개인정보 처리방침</router-link>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from "vue";

const year = computed(() => new Date().getFullYear());
</script>

<style scoped>
.deletion {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.7;
}

/* 상단 네비게이션 — PrivacyPolicyView 와 동일 톤 */
.deletion-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: saturate(180%) blur(8px);
  border-bottom: 1px solid var(--color-border);
}
.deletion-nav__inner {
  max-width: 860px;
  margin: 0 auto;
  height: var(--header-height);
  padding: 0 var(--header-padding-x);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.deletion-brand {
  display: flex;
  align-items: baseline;
  gap: 8px;
  text-decoration: none;
}
.deletion-brand__mark {
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--color-text-strong);
}
.deletion-brand__sub {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
}
.deletion-nav__home {
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: 14px;
}
.deletion-nav__home:hover {
  color: var(--color-text-strong);
}

/* 본문 */
.deletion-body {
  flex: 1;
  padding: 40px var(--header-padding-x) 64px;
}
.deletion-doc {
  max-width: 860px;
  margin: 0 auto;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 40px 44px 48px;
}
.deletion-doc__title {
  color: var(--color-text-strong);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin: 0 0 6px;
}
.deletion-doc__meta {
  color: var(--color-text-muted);
  font-size: 13px;
  margin: 0 0 24px;
}
.deletion-doc section {
  margin-top: 28px;
}
.deletion-doc h2 {
  color: var(--color-text-strong);
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--color-border);
}
.deletion-doc p {
  margin: 0 0 10px;
}
.deletion-doc ul {
  margin: 0 0 10px;
  padding-left: 20px;
}
.deletion-doc li {
  margin: 3px 0;
}
.deletion-doc a {
  color: var(--color-primary);
}

/* 푸터 */
.deletion-footer {
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}
.deletion-footer__inner {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px var(--header-padding-x);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-text-muted);
  font-size: 13px;
}
.deletion-footer__inner a {
  color: var(--color-text-muted);
  text-decoration: none;
}
.deletion-footer__inner a:hover {
  color: var(--color-text-strong);
}

/* 반응형 */
@media (max-width: 640px) {
  .deletion-doc {
    padding: 28px 20px 32px;
  }
  .deletion-doc__title {
    font-size: 22px;
  }
}
</style>
