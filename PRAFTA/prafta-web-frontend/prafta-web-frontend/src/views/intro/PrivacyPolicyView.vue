<template>
  <!--
    개인정보 처리방침 공개 페이지 (비로그인, '/privacy')
    - 플레이스토어/앱스토어 제출용 공개 URL. 로그인 없이 접근 가능해야 한다.
    - 문안 원본 = 약관 002(개인정보 처리방침) 정식 문안
      (prafta-backend/src/main/resources/sql/migration/prafta-terms-real-content-1.sql).
      TB_TERMS 002 문안을 개정하면 이 페이지도 함께 갱신할 것.
    - 백엔드 의존 없이 정적으로 내장한다(스토어 심사 중 서버 장애에도 접근 보장).
  -->
  <div class="privacy">
    <header class="privacy-nav">
      <div class="privacy-nav__inner">
        <router-link class="privacy-brand" to="/">
          <span class="privacy-brand__mark">PRAFTA</span>
        </router-link>
        <router-link class="privacy-nav__home" to="/">홈으로</router-link>
      </div>
    </header>

    <main class="privacy-body">
      <article class="privacy-doc">
        <h1 class="privacy-doc__title">개인정보 처리방침</h1>
        <p class="privacy-doc__meta">시행일: 2026년 7월 19일</p>

        <p>
          프라프타(PRAFTA)(이하 "회사"라 합니다)는 개인정보 보호법 등 관련
          법령을 준수하며, PRAFTA 서비스 이용자의 개인정보를 다음과 같이
          처리합니다.
        </p>

        <section>
          <h2>1. 수집하는 개인정보 항목 및 방법</h2>
          <h3>계정 정보</h3>
          <ul>
            <li>성명, 아이디, 비밀번호, 휴대폰번호, 이메일, 생년월일, 성별</li>
            <li>소속(회사, 사업장, 부서), 직급, 입사일, 고용형태</li>
          </ul>
          <h3>근태 및 안전활동 정보</h3>
          <ul>
            <li>출퇴근 일시, 출퇴근 및 외근 시 위치정보(GPS 좌표)</li>
            <li>근무계획, 연차 등 휴가 사용 내역</li>
            <li>
              TBM 참석 및 안전점검 기록, 서비스 이용 과정에서 등록하는 사진
            </li>
          </ul>
          <h3>일용근로자</h3>
          <ul>
            <li>성명, 휴대폰번호, 전자서명(입장 계약서)</li>
          </ul>
          <h3>기기 정보</h3>
          <ul>
            <li>기기 식별자, 기기 모델 및 OS 정보, 푸시 알림 토큰</li>
          </ul>
          <h3>수집 방법</h3>
          <ul>
            <li>
              고객사 관리자의 등록, 이용자의 직접 입력, 서비스 이용 과정에서의
              자동 수집
            </li>
          </ul>
        </section>

        <section>
          <h2>2. 개인정보의 처리 목적</h2>
          <ul>
            <li>계정 관리 및 본인 확인</li>
            <li>출퇴근 판정 등 근태관리, 연차 등 휴가관리</li>
            <li>TBM, 위험성평가, 순회점검 등 산업안전보건 업무 지원</li>
            <li>푸시 알림 등 서비스 제공 및 운영</li>
            <li>부정 이용 방지 및 서비스 개선</li>
          </ul>
        </section>

        <section>
          <h2>3. 보유 및 이용 기간</h2>
          <ul>
            <li>
              계정 정보: 계정 삭제(퇴직 처리) 시까지. 다만 관련 법령에 따라
              보존이 필요한 경우 해당 기간 동안 보존합니다.
            </li>
            <li>근로 관련 기록(근태, 연차 등): 근로기준법에 따라 3년</li>
            <li>산업안전보건 관련 기록: 산업안전보건법이 정한 기간</li>
          </ul>
        </section>

        <section>
          <h2>4. 개인정보의 제3자 제공</h2>
          <p>
            회사는 이용자의 동의 없이 개인정보를 제3자에게 제공하지 않습니다.
            다만 이용자가 별도 동의한 경우(소속 회사 및 연동회사 제공 등) 또는
            법령에 근거한 경우에 한하여 제공합니다.
          </p>
        </section>

        <section>
          <h2>5. 개인정보 처리의 위탁</h2>
          <p>
            회사는 서비스 제공을 위하여 다음과 같이 개인정보 처리를 위탁합니다.
          </p>
          <ul>
            <li>Amazon Web Services: 클라우드 서버 운영 및 데이터 보관</li>
            <li>Google(Firebase): 모바일 푸시 알림 발송</li>
          </ul>
          <p>
            회사는 위탁계약 시 개인정보 보호 관련 법령 준수를 명시하고 수탁자를
            관리, 감독합니다.
          </p>
        </section>

        <section>
          <h2>6. 개인정보의 파기</h2>
          <p>
            보유기간이 경과하거나 처리 목적이 달성된 개인정보는 지체 없이 복구
            불가능한 방법으로 파기합니다.
          </p>
        </section>

        <section>
          <h2>7. 정보주체의 권리</h2>
          <p>
            이용자는 언제든지 자신의 개인정보에 대한 열람, 정정, 삭제,
            처리정지를 요구할 수 있습니다. 권리 행사는 서비스 내 기능 또는 아래
            연락처를 통하여 할 수 있습니다.
          </p>
        </section>

        <section>
          <h2>8. 안전성 확보 조치</h2>
          <ul>
            <li>
              휴대폰번호, 이메일, 생년월일 등 주요 개인정보의 암호화
              저장(AES-256-GCM)
            </li>
            <li>비밀번호의 일방향 암호화 저장</li>
            <li>접근 권한 관리 및 위치정보 열람 기록 보관</li>
          </ul>
        </section>

        <section>
          <h2>9. 개인정보 보호책임자</h2>
          <ul>
            <li>성명: 김여진</li>
            <li>연락처: 010-7763-5257, aqswgg@gmail.com</li>
          </ul>
        </section>

        <section>
          <h2>10. 처리방침의 변경</h2>
          <p>이 방침이 변경되는 경우 시행 7일 전부터 서비스 내 공지합니다.</p>
        </section>
      </article>
    </main>

    <footer class="privacy-footer">
      <div class="privacy-footer__inner">
        <span>© {{ year }} PRAFTA. All rights reserved.</span>
        <span class="privacy-footer__links">
          <router-link to="/account-deletion">계정 삭제 안내</router-link>
          <router-link to="/">회사소개</router-link>
        </span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from "vue";

const year = computed(() => new Date().getFullYear());
</script>

<style scoped>
.privacy {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.7;
}

/* 상단 네비게이션 — 랜딩(CompanyIntroView)과 동일 톤 */
.privacy-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: saturate(180%) blur(8px);
  border-bottom: 1px solid var(--color-border);
}
.privacy-nav__inner {
  max-width: 860px;
  margin: 0 auto;
  height: var(--header-height);
  padding: 0 var(--header-padding-x);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.privacy-brand {
  display: flex;
  align-items: baseline;
  gap: 8px;
  text-decoration: none;
}
.privacy-brand__mark {
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--color-text-strong);
}
.privacy-brand__sub {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
}
.privacy-nav__home {
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: 14px;
}
.privacy-nav__home:hover {
  color: var(--color-text-strong);
}

/* 본문 */
.privacy-body {
  flex: 1;
  padding: 40px var(--header-padding-x) 64px;
}
.privacy-doc {
  max-width: 860px;
  margin: 0 auto;
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  padding: 40px 44px 48px;
}
.privacy-doc__title {
  color: var(--color-text-strong);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin: 0 0 6px;
}
.privacy-doc__meta {
  color: var(--color-text-muted);
  font-size: 13px;
  margin: 0 0 24px;
}
.privacy-doc section {
  margin-top: 28px;
}
.privacy-doc h2 {
  color: var(--color-text-strong);
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--color-border);
}
.privacy-doc h3 {
  color: var(--color-text-strong);
  font-size: 15px;
  font-weight: 700;
  margin: 14px 0 6px;
}
.privacy-doc p {
  margin: 0 0 10px;
}
.privacy-doc ul {
  margin: 0 0 10px;
  padding-left: 20px;
}
.privacy-doc li {
  margin: 3px 0;
}

/* 푸터 */
.privacy-footer {
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}
.privacy-footer__inner {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px var(--header-padding-x);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-text-muted);
  font-size: 13px;
}
.privacy-footer__links {
  display: inline-flex;
  gap: 18px;
}
.privacy-footer__inner a {
  color: var(--color-text-muted);
  text-decoration: none;
}
.privacy-footer__inner a:hover {
  color: var(--color-text-strong);
}

/* 반응형 */
@media (max-width: 640px) {
  .privacy-doc {
    padding: 28px 20px 32px;
  }
  .privacy-doc__title {
    font-size: 22px;
  }
}
</style>
