<template>
  <!--
    웹/앱 화면 목업 프레임 (intro 전용)
    - 주요 기능 옆에 배치하는 자동재생 영상 영역.
    - videoSrc / appVideoSrc 를 넘기면 <video autoplay muted loop> 로 재생,
      없으면 "자동재생 영상 영역" placeholder 를 표시(영역만 확보).
  -->
  <div class="mock" :class="{ 'mock--overlap': overlap }">
    <!-- 웹(가로) -->
    <div class="mock__web">
      <div class="mock__bar"><i></i><i></i><i></i></div>
      <div class="mock__screen">
        <!-- 우선순위: #web 슬롯(라이브 컴포넌트) > videoSrc > webSrc(이미지) > placeholder -->
        <slot name="web">
          <video
            v-if="videoSrc"
            class="mock__video"
            :poster="poster || undefined"
            autoplay
            muted
            loop
            playsinline
          >
            <source :src="videoSrc" type="video/mp4" />
          </video>
          <img v-else-if="webSrc" class="mock__img" :src="webSrc" :alt="label" />
          <div v-else class="mock__ph">
            <span class="mock__play">▶</span>
            <span class="mock__phtxt">{{ label }} · 자동재생 영상 영역</span>
            <span class="mock__phsub">웹 화면 (가로)</span>
          </div>
        </slot>
      </div>
    </div>

    <!-- 앱(세로) -->
    <div v-if="showApp" class="mock__app">
      <div class="mock__notch"></div>
      <div class="mock__app-screen">
        <!-- 우선순위: #app 슬롯 > appVideoSrc > appSrc(이미지) > placeholder -->
        <slot name="app">
          <video
            v-if="appVideoSrc"
            class="mock__video"
            autoplay
            muted
            loop
            playsinline
          >
            <source :src="appVideoSrc" type="video/mp4" />
          </video>
          <img v-else-if="appSrc" class="mock__img" :src="appSrc" alt="앱 화면" />
          <div v-else class="mock__ph mock__ph--app">
            <span class="mock__play">▶</span>
            <span class="mock__phsub">앱 화면 (세로)</span>
          </div>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: "" },
  videoSrc: { type: String, default: "" }, // 예: "/downloads/feature-web.mp4"
  appVideoSrc: { type: String, default: "" },
  webSrc: { type: String, default: "" }, // 정적 이미지(웹). 예: "/intro/dashboard.png"
  appSrc: { type: String, default: "" }, // 정적 이미지(앱). 예: "/intro/risk-mobile.png"
  poster: { type: String, default: "" },
  showApp: { type: Boolean, default: true },
  overlap: { type: Boolean, default: false }, // 웹 크게 + 폰을 우측 하단에 겹쳐 배치
});
</script>

<style scoped>
.mock { display: flex; align-items: flex-end; justify-content: center; gap: 20px; }
.mock__web {
  flex: 1 1 auto;
  max-width: 720px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: var(--color-surface);
  box-shadow: var(--card-shadow);
  overflow: hidden;
}
.mock__bar { height: 26px; background: #f1f3f5; display: flex; align-items: center; gap: 6px; padding-left: 12px; }
.mock__bar i { width: 9px; height: 9px; border-radius: 50%; background: #cdd3da; display: inline-block; }
.mock__screen { aspect-ratio: 16 / 10; background: linear-gradient(160deg, #f0fdf4, #ffffff 62%); }
.mock__app {
  flex: 0 0 170px;
  width: 170px;
  border: 1px solid var(--color-border);
  border-radius: 22px;
  background: var(--color-surface);
  box-shadow: var(--card-shadow);
  overflow: hidden;
  position: relative;
  padding-top: 14px;
}
.mock__notch { position: absolute; top: 6px; left: 50%; transform: translateX(-50%); width: 40px; height: 5px; border-radius: 3px; background: var(--color-border-strong); }
.mock__app-screen { aspect-ratio: 9 / 17; background: linear-gradient(160deg, #f0fdf4, #ffffff 62%); }
.mock__video { width: 100%; height: 100%; object-fit: cover; display: block; }
.mock__img { width: 100%; height: 100%; object-fit: cover; object-position: top center; display: block; }
.mock__ph {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  color: var(--color-text-muted); text-align: center; padding: 12px;
}
.mock__play {
  width: 52px; height: 52px; border-radius: 50%;
  background: var(--color-primary); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 19px;
}
.mock__phtxt { font-size: 15px; font-weight: 700; color: var(--color-text); }
.mock__phsub { font-size: 12.5px; color: var(--color-text-muted); }
.mock__ph--app { padding: 8px; }

/* 겹침 모드: 웹을 크게, 폰을 웹 우측 하단에 살짝 걸치게 */
.mock--overlap { position: relative; display: block; padding: 0 18px 26px 0; }
.mock--overlap .mock__web { max-width: none; width: 100%; }
.mock--overlap .mock__app {
  position: absolute; right: -2px; bottom: 0; z-index: 3;
  flex: none; width: 156px;
  box-shadow: 0 16px 34px rgba(16, 24, 40, 0.20);
}

@media (max-width: 860px) {
  .mock__app { flex-basis: 140px; width: 140px; }
  .mock--overlap .mock__app { width: 120px; }
}
</style>
