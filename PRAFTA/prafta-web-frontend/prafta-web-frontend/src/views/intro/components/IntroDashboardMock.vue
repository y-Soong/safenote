<template>
  <!--
    홈페이지 목업 전용 — 관리자 근태+안전 대시보드 화면 (가짜 데이터)
    MockupFrame 의 #web 슬롯에 넣어 "제품 화면처럼" 보여주는 용도.
    실제 제품 DashboardView 와는 무관(intro 전용). 컨테이너를 100% 채움.
  -->
  <div class="dash" aria-hidden="true">
    <!-- 좌측 내비 -->
    <aside class="dash__lnb">
      <div class="dash__brand">
        <span class="dash__brand-mark">S</span>
        <span class="dash__brand-txt">SAFENOTE</span>
      </div>
      <nav class="dash__nav">
        <span class="dash__nav-item is-active"><i class="ico ico--grid"></i>대시보드</span>
        <span class="dash__nav-item"><i class="ico ico--clock"></i>근태·인사</span>
        <span class="dash__nav-item"><i class="ico ico--shield"></i>산업안전</span>
        <span class="dash__nav-item"><i class="ico ico--check"></i>안전점검</span>
        <span class="dash__nav-item"><i class="ico ico--doc"></i>리포트</span>
      </nav>
      <span class="dash__nav-item dash__nav-item--bottom"><i class="ico ico--cog"></i>설정</span>
    </aside>

    <!-- 본문 -->
    <div class="dash__main">
      <!-- 상단바 -->
      <header class="dash__top">
        <span class="dash__site">인천 제1공장 <i class="caret"></i></span>
        <span class="dash__spacer"></span>
        <span class="dash__search">현장·작업자 검색</span>
        <span class="dash__bell">🔔<i class="dot"></i></span>
        <span class="dash__me"><span class="dash__avatar">관</span>관리자</span>
      </header>

      <div class="dash__body">
        <div class="dash__heading">
          <div>
            <h1>관리자 대시보드</h1>
            <p>2026년 6월 3일 (수) · 인천 제1공장</p>
          </div>
          <span class="dash__live"><i></i>실시간</span>
        </div>

        <!-- KPI -->
        <ul class="kpis">
          <li class="kpi">
            <span class="kpi__label">현재 근무 인원</span>
            <strong class="kpi__value">128<small>/142명</small></strong>
            <span class="kpi__trend up">출근율 92%</span>
          </li>
          <li class="kpi">
            <span class="kpi__label">금일 지각·결근</span>
            <strong class="kpi__value">6<small>건</small></strong>
            <span class="kpi__trend down">전일 대비 -2</span>
          </li>
          <li class="kpi">
            <span class="kpi__label">위험성평가 진행률</span>
            <strong class="kpi__value">76<small>%</small></strong>
            <span class="kpi__trend up">이번 분기</span>
          </li>
          <li class="kpi kpi--warn">
            <span class="kpi__label">미조치 위험요인</span>
            <strong class="kpi__value">5<small>건</small></strong>
            <span class="kpi__trend warn">조치 필요</span>
          </li>
        </ul>

        <!-- 패널 -->
        <div class="panels">
          <!-- 근태: 주간 출근 현황 -->
          <section class="panel">
            <div class="panel__head">
              <h2>주간 출근 현황</h2>
              <span class="panel__tag">근태·인사</span>
            </div>
            <div class="bars">
              <div v-for="d in week" :key="d.day" class="bars__col">
                <div class="bars__stack">
                  <span class="bars__seg seg--leave" :style="{ height: d.leave + 'px' }"></span>
                  <span class="bars__seg seg--late" :style="{ height: d.late + 'px' }"></span>
                  <span class="bars__seg seg--work" :style="{ height: d.work + 'px' }"></span>
                </div>
                <span class="bars__day">{{ d.day }}</span>
              </div>
            </div>
            <ul class="legend">
              <li><i class="lg lg--work"></i>정상 출근</li>
              <li><i class="lg lg--late"></i>지각</li>
              <li><i class="lg lg--leave"></i>연차·휴가</li>
            </ul>
          </section>

          <!-- 안전: 활동 요약 -->
          <section class="panel">
            <div class="panel__head">
              <h2>오늘의 안전 활동</h2>
              <span class="panel__tag panel__tag--green">산업안전</span>
            </div>
            <ul class="safe">
              <li><span class="safe__ic">🦺</span><span class="safe__nm">TBM 완료</span><b>18/20</b></li>
              <li><span class="safe__ic">📋</span><span class="safe__nm">안전점검</span><b>24건</b></li>
              <li><span class="safe__ic">⚠️</span><span class="safe__nm">아차사고 신고</span><b>3건</b></li>
            </ul>
            <div class="gauge">
              <div class="gauge__bar"><span style="width: 88%"></span></div>
              <span class="gauge__txt">안전 활동 이행률 <b>88%</b></span>
            </div>
          </section>
        </div>

        <!-- 최근 위험성 발굴 제보 -->
        <section class="panel panel--wide">
          <div class="panel__head">
            <h2>최근 위험성 발굴 제보</h2>
            <span class="panel__more">전체 보기 ›</span>
          </div>
          <table class="rtable">
            <thead>
              <tr><th>작업자</th><th>발굴 내용</th><th>위험등급</th><th>상태</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in reports" :key="r.id">
                <td>{{ r.name }}</td>
                <td class="rtable__desc">{{ r.desc }}</td>
                <td><span class="grade" :class="'grade--' + r.level">{{ r.grade }}</span></td>
                <td><span class="stat" :class="'stat--' + r.s">{{ r.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
// 목업 표시용 고정 데이터 (실데이터 아님)
const week = [
  { day: "월", work: 92, late: 7, leave: 6 },
  { day: "화", work: 96, late: 5, leave: 5 },
  { day: "수", work: 88, late: 9, leave: 8 },
  { day: "목", work: 99, late: 4, leave: 4 },
  { day: "금", work: 84, late: 8, leave: 13 },
];
const reports = [
  { id: 1, name: "김현수", desc: "2공정 컨베이어 비상정지 스위치 식별 표시 마모", grade: "높음", level: "high", status: "조치중", s: "doing" },
  { id: 2, name: "박지영", desc: "지게차 이동통로 바닥 단차 — 보행자 동선 겹침", grade: "보통", level: "mid", status: "접수", s: "new" },
  { id: 3, name: "이정훈", desc: "분전반 주변 가연물 적치, 소화기 비치 누락", grade: "높음", level: "high", status: "조치완료", s: "done" },
  { id: 4, name: "최민아", desc: "옥외 작업장 우천 시 미끄럼 주의 안내 부족", grade: "낮음", level: "low", status: "접수", s: "new" },
];
</script>

<style scoped>
.dash {
  display: flex;
  width: 100%;
  height: 100%;
  background: #f1f5f3;
  color: #1f2937;
  font-size: 11px;
  line-height: 1.4;
  overflow: hidden;
  text-align: left;
}

/* ===== LNB ===== */
.dash__lnb {
  flex: 0 0 19%;
  min-width: 96px;
  background: linear-gradient(180deg, #166534, #14532d);
  color: #d6f5e0;
  display: flex;
  flex-direction: column;
  padding: 11px 9px;
}
.dash__brand { display: flex; align-items: center; gap: 6px; margin-bottom: 16px; padding: 0 3px; }
.dash__brand-mark {
  width: 18px; height: 18px; border-radius: 5px; background: #22c55e; color: #06351b;
  font-weight: 900; font-size: 11px; display: flex; align-items: center; justify-content: center;
}
.dash__brand-txt { font-weight: 800; font-size: 11px; letter-spacing: 0.04em; color: #fff; }
.dash__nav { display: flex; flex-direction: column; gap: 2px; }
.dash__nav-item {
  display: flex; align-items: center; gap: 7px;
  padding: 6px 8px; border-radius: 7px; color: #b6e6c6; font-weight: 600; white-space: nowrap;
}
.dash__nav-item.is-active { background: rgba(255, 255, 255, 0.16); color: #fff; }
.dash__nav-item--bottom { margin-top: auto; }
.ico { width: 11px; height: 11px; border-radius: 3px; background: currentColor; opacity: 0.85; flex: 0 0 auto; }
.ico--grid { border-radius: 2px; }
.ico--clock, .ico--shield { border-radius: 50%; }

/* ===== MAIN ===== */
.dash__main { flex: 1 1 auto; display: flex; flex-direction: column; min-width: 0; }
.dash__top {
  height: 34px; flex: 0 0 34px; background: #fff; border-bottom: 1px solid #e5e7eb;
  display: flex; align-items: center; gap: 9px; padding: 0 12px;
}
.dash__site { font-weight: 700; color: #111827; display: inline-flex; align-items: center; gap: 5px; }
.caret { width: 0; height: 0; border-left: 3px solid transparent; border-right: 3px solid transparent; border-top: 4px solid #6b7280; }
.dash__spacer { flex: 1 1 auto; }
.dash__search {
  color: #9ca3af; background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 7px;
  padding: 4px 9px; font-size: 10px;
}
.dash__bell { position: relative; font-size: 11px; }
.dash__bell .dot { position: absolute; top: -1px; right: -1px; width: 5px; height: 5px; border-radius: 50%; background: #ef4444; }
.dash__me { display: inline-flex; align-items: center; gap: 5px; font-weight: 700; color: #374151; }
.dash__avatar {
  width: 18px; height: 18px; border-radius: 50%; background: #166534; color: #fff;
  font-size: 9px; font-weight: 800; display: flex; align-items: center; justify-content: center;
}

.dash__body { flex: 1 1 auto; padding: 12px; overflow: hidden; }
.dash__heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 11px; }
.dash__heading h1 { font-size: 15px; font-weight: 800; color: #111827; margin: 0; }
.dash__heading p { font-size: 10px; color: #6b7280; margin: 2px 0 0; }
.dash__live { display: inline-flex; align-items: center; gap: 4px; font-size: 9.5px; font-weight: 700; color: #16a34a; }
.dash__live i { width: 6px; height: 6px; border-radius: 50%; background: #22c55e; box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18); }

/* ===== KPI ===== */
.kpis { list-style: none; margin: 0 0 11px; padding: 0; display: grid; grid-template-columns: repeat(4, 1fr); gap: 9px; }
.kpi { background: #fff; border: 1px solid #e5e7eb; border-radius: 11px; padding: 9px 10px; display: flex; flex-direction: column; gap: 3px; }
.kpi--warn { border-color: #fbcfcf; background: #fff7f7; }
.kpi__label { font-size: 9.5px; color: #6b7280; font-weight: 600; }
.kpi__value { font-size: 19px; font-weight: 800; color: #111827; line-height: 1; }
.kpi__value small { font-size: 10px; font-weight: 600; color: #9ca3af; margin-left: 1px; }
.kpi__trend { font-size: 9px; font-weight: 700; }
.kpi__trend.up { color: #16a34a; }
.kpi__trend.down { color: #2563eb; }
.kpi__trend.warn { color: #dc2626; }

/* ===== PANELS ===== */
.panels { display: grid; grid-template-columns: 1.25fr 1fr; gap: 9px; margin-bottom: 9px; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 11px; padding: 10px 11px; }
.panel__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.panel__head h2 { font-size: 11.5px; font-weight: 800; color: #111827; margin: 0; }
.panel__tag { font-size: 8.5px; font-weight: 700; color: #6b7280; background: #f3f4f6; border-radius: 5px; padding: 2px 6px; }
.panel__tag--green { color: #15803d; background: #dcfce7; }
.panel__more { font-size: 9.5px; font-weight: 700; color: #16a34a; }

/* bars */
.bars { display: flex; align-items: flex-end; gap: 10px; height: 84px; padding: 0 2px; }
.bars__col { flex: 1 1 0; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.bars__stack { display: flex; flex-direction: column-reverse; justify-content: flex-start; width: 60%; max-width: 20px; }
.bars__seg { display: block; width: 100%; }
.seg--work { background: #22c55e; border-radius: 0 0 3px 3px; }
.seg--late { background: #fbbf24; }
.seg--leave { background: #cbd5e1; border-radius: 3px 3px 0 0; }
.bars__day { font-size: 9px; color: #6b7280; font-weight: 600; }
.legend { list-style: none; display: flex; gap: 11px; margin: 8px 0 0; padding: 0; }
.legend li { display: inline-flex; align-items: center; gap: 4px; font-size: 9px; color: #6b7280; }
.lg { width: 8px; height: 8px; border-radius: 2px; }
.lg--work { background: #22c55e; }
.lg--late { background: #fbbf24; }
.lg--leave { background: #cbd5e1; }

/* safe list */
.safe { list-style: none; margin: 0 0 9px; padding: 0; display: flex; flex-direction: column; gap: 6px; }
.safe li { display: flex; align-items: center; gap: 7px; font-size: 10.5px; color: #374151; }
.safe__ic { font-size: 11px; }
.safe__nm { flex: 1 1 auto; }
.safe b { font-weight: 800; color: #111827; }
.gauge { display: flex; align-items: center; gap: 8px; }
.gauge__bar { flex: 1 1 auto; height: 7px; background: #eef2f0; border-radius: 99px; overflow: hidden; }
.gauge__bar span { display: block; height: 100%; background: linear-gradient(90deg, #22c55e, #16a34a); border-radius: 99px; }
.gauge__txt { font-size: 9px; color: #6b7280; font-weight: 600; white-space: nowrap; }
.gauge__txt b { color: #16a34a; font-weight: 800; }

/* report table */
.panel--wide { padding-bottom: 8px; }
.rtable { width: 100%; border-collapse: collapse; font-size: 10px; }
.rtable th { text-align: left; font-size: 8.5px; color: #9ca3af; font-weight: 700; padding: 0 6px 6px; border-bottom: 1px solid #eef0f2; }
.rtable td { padding: 6px; border-bottom: 1px solid #f3f4f6; color: #374151; vertical-align: middle; }
.rtable tr:last-child td { border-bottom: 0; }
.rtable__desc { color: #4b5563; max-width: 0; width: 60%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.grade { font-size: 8.5px; font-weight: 800; padding: 2px 6px; border-radius: 99px; }
.grade--high { color: #b91c1c; background: #fee2e2; }
.grade--mid { color: #b45309; background: #fef3c7; }
.grade--low { color: #15803d; background: #dcfce7; }
.stat { font-size: 8.5px; font-weight: 700; padding: 2px 6px; border-radius: 5px; }
.stat--new { color: #6b7280; background: #f3f4f6; }
.stat--doing { color: #1d4ed8; background: #dbeafe; }
.stat--done { color: #15803d; background: #dcfce7; }
</style>
