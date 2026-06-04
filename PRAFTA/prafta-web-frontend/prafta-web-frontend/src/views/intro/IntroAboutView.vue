<template>
  <!--
    프라프타 홈페이지 — 회사 소개 (/about)
    출처: 홈페이지 IA 명세 §2. 확보된 사실만 확정, 미보강 영역은 placeholder.
  -->
  <div class="intro-page">
    <IntroHeader />

    <!-- 페이지 헤더 -->
    <section class="phead">
      <div class="phead__inner">
        <p class="phead__kicker">ABOUT PRAFTA</p>
        <h1 class="phead__title">회사 소개</h1>
        <p class="phead__slogan">현장 관리, 플랫폼으로 완성하다</p>
      </div>
    </section>

    <!-- 프라프타는 -->
    <section class="section">
      <div class="section__inner section__inner--narrow">
        <h2 class="section__title">프라프타는</h2>
        <p class="lead">
          프라프타는 흩어져 있던 현장 관리 업무 — <strong>근태·인사</strong>와 <strong>산업안전</strong> — 을
          하나의 플랫폼으로 완성하는 회사입니다. 우리는 제조·물류·산업현장을 위한 통합 SaaS
          <strong>세이프노트(SAFENOTE)</strong>를 만듭니다.
        </p>
      </div>
    </section>

    <!-- 미션 / 지향 -->
    <section class="section section--alt">
      <div class="section__inner">
        <ul class="cards cards--2">
          <li class="card">
            <h3 class="card__title">우리의 지향</h3>
            <p class="card__desc">
              흩어진 현장 관리 업무를 하나의 시스템으로 — 노무 및 산업재해 리스크를 동시에 줄입니다.
            </p>
          </li>
          <li class="card">
            <h3 class="card__title">우리가 푸는 문제</h3>
            <p class="card__desc">
              수기·엑셀 정산, 종이 점검표, 인사–안전 부서 데이터 단절, 일용직 이중 관리. 현장의 비효율과
              법적 리스크를 기술로 해결합니다.
            </p>
          </li>
        </ul>
      </div>
    </section>

    <!-- 우리가 만드는 것 -->
    <section class="section">
      <div class="section__inner">
        <h2 class="section__title">우리가 만드는 것 — 세이프노트</h2>
        <p class="section__lead">근태·인사 + 산업안전 통합 SaaS, 현장의 사람과 안전을 하나의 서비스로.</p>
        <ul class="cards cards--3">
          <li class="card"><h3 class="card__title">근태·인사 관리</h3><p class="card__desc">모바일 출퇴근·GPS, 근무표/교대, 일용직 전용 관리, 연차 자동관리.</p></li>
          <li class="card"><h3 class="card__title">산업안전 관리</h3><p class="card__desc">위험성평가, 안전점검, TBM, 아차사고 관리와 증빙 자동 보존.</p></li>
          <li class="card"><h3 class="card__title">AI 안전 지원</h3><p class="card__desc">위험요인·개선안, TBM 자료 초안을 AI가 제안(담당자 승인 후 확정).</p></li>
        </ul>
        <div class="section__more">
          <button type="button" class="btn btn--primary" @click="goService">서비스 자세히 보기</button>
        </div>
      </div>
    </section>

    <!-- 연혁/팀/비전 (보강 필요 placeholder) -->
    <section class="section section--alt">
      <div class="section__inner">
        <h2 class="section__title">연혁 · 팀 · 비전</h2>
        <!-- TODO: 회사 연혁/구성원/비전 콘텐츠 확정 후 교체 -->
        <div class="placeholder">
          회사 연혁, 구성원, 비전 콘텐츠가 들어갈 영역입니다. (자료 확정 후 보강)
        </div>
      </div>
    </section>

    <!-- 연락 -->
    <section class="section">
      <div class="section__inner section__inner--narrow">
        <h2 class="section__title">연락처</h2>
        <ul class="contactlist">
          <li><span>대표</span> 윤순기</li>
          <li><span>전화</span> <a href="tel:01077635257">010-7763-5257</a></li>
          <li><span>이메일</span> <a href="mailto:dudjswp@gmail.com">dudjswp@gmail.com</a></li>
          <!-- TODO: 주소/사업자등록정보 보강 -->
          <li><span>주소</span> (보강 예정)</li>
        </ul>
        <div class="section__more">
          <button type="button" class="btn btn--primary" @click="goContact">도입 문의하기</button>
        </div>
      </div>
    </section>

    <IntroFooter />
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import IntroHeader from "./components/IntroHeader.vue";
import IntroFooter from "./components/IntroFooter.vue";

const router = useRouter();
const goService = () => router.push("/service");
const goContact = () => router.push("/contact");
</script>

<style scoped>
.intro-page { height: 100vh; height: 100dvh; overflow-y: auto; background: var(--color-surface); color: var(--color-text); font-size: 17.5px; line-height: 1.72; word-break: keep-all; overflow-wrap: break-word; }

.btn { display: inline-flex; align-items: center; justify-content: center; height: 42px; padding: 0 20px; border: 1px solid transparent; border-radius: var(--btn-radius-lg); font-weight: 700; cursor: pointer; transition: background 0.15s, color 0.15s; }
.btn--primary { background: var(--color-primary); color: #fff; }
.btn--primary:hover { background: var(--color-primary-hover); }

.phead { background: linear-gradient(180deg, #f1faf4 0%, var(--color-surface) 100%); border-bottom: 1px solid var(--color-border); color: var(--color-text); }
.phead__inner { max-width: 1080px; margin: 0 auto; padding: 64px var(--header-padding-x); }
.phead__kicker { color: var(--color-primary); font-weight: 700; font-size: 13px; letter-spacing: 0.12em; margin: 0 0 10px; }
.phead__title { color: var(--color-text-strong); font-size: 38px; font-weight: 800; margin: 0 0 8px; }
.phead__slogan { font-size: 18px; color: var(--color-text-muted); margin: 0; }

.section { padding: 68px var(--header-padding-x); }
.section--alt { background: #f5fbf7; }
.section__inner { max-width: 1080px; margin: 0 auto; }
.section__inner--narrow { max-width: 760px; }
.section__title { color: var(--color-text-strong); font-size: 30px; font-weight: 800; letter-spacing: -0.02em; text-align: center; margin: 0 0 10px; }
.section__lead { text-align: center; color: var(--color-text-muted); margin: 0 0 32px; }
.section__more { text-align: center; margin-top: 28px; }
.lead { font-size: 19px; color: var(--color-text); text-align: center; margin: 0; line-height: 1.65; }
.lead strong { color: var(--color-primary-pressed); }

.cards { list-style: none; margin: 0; padding: 0; display: grid; gap: 18px; }
.cards--2 { grid-template-columns: 1fr 1fr; }
.cards--3 { grid-template-columns: repeat(3, 1fr); }
.card { background: var(--card-bg); border: var(--card-border); border-radius: var(--card-radius); box-shadow: var(--card-shadow); padding: var(--card-padding); }
.card__title { color: var(--color-text-strong); font-size: 19px; font-weight: 700; margin: 0 0 7px; }
.card__desc { color: var(--color-text-muted); font-size: 15.5px; margin: 0; }

.placeholder { border: 2px dashed var(--color-border-strong); border-radius: var(--card-radius); padding: 48px 20px; text-align: center; color: var(--color-text-muted); background: var(--color-surface); }

.contactlist { list-style: none; margin: 0 auto; padding: 0; max-width: 460px; }
.contactlist li { display: flex; gap: 14px; padding: 13px 0; border-top: 1px solid var(--color-border); font-size: 16.5px; color: var(--color-text); }
.contactlist li:first-child { border-top: 0; }
.contactlist li span { flex: 0 0 64px; color: var(--color-text-muted); font-weight: 700; }
.contactlist a { color: var(--color-primary); text-decoration: none; }
.contactlist a:hover { text-decoration: underline; }

@media (max-width: 860px) {
  .cards--2, .cards--3 { grid-template-columns: 1fr; }
  .phead__title { font-size: 30px; }
}
</style>
