<!--
  TbmMaterialSlider.vue — TBM 교육자료 슬라이드(자료1/자료2 영역 분리, 자료당 항목 ≤3)
  - 작업 ID: PRAFTA-TBM-MTRL-SLIDER (분해: prafta-app-tbm-user-detail-plan.md §4 F6)
  - 백엔드: GET /appApi/tbm/sessions/{sessionCd}/content (A6) 의 materials 배열을 props 로 받음.
    materials: [{ mtrlCd, title, items:[{ type, url, desc, sortIdx }] }]  (자료 ≤3 묶음)
    type = MTRL_ITEM_TYPE(SYS018): '01' 이미지 / '02' 동영상 / '03' 유튜브URL / '04' PDF
  - 자료(묶음)별 영역을 분리해 표시하고, 각 묶음 내 항목(≤3)을 좌우 슬라이드.
  - 디자인 토큰은 부모(.tbm-inprog-view)에서 상속.
  - planner 라운드 스코프: template + style 완성. script 는 props + UI 슬라이드 토글만(데이터 가공 없음).
-->
<template>
  <div class="mtrl-slider">
    <section
      v-for="(m, mIdx) in materials"
      :key="m.mtrlCd || mIdx"
      class="mtrl-group"
    >
      <p class="mtrl-group__title">{{ m.title || `자료 ${mIdx + 1}` }}</p>

      <!-- 항목 슬라이드(≤3) -->
      <div class="mtrl-stage">
        <div
          v-for="(item, iIdx) in (m.items || [])"
          v-show="iIdx === currentIndex(mIdx)"
          :key="item.sortIdx ?? iIdx"
          class="mtrl-slide"
        >
          <!-- 이미지형 항목(SYS018 01). url 있으면 인라인 렌더, 로드 실패 시 '이미지 열기' 링크로 폴백. -->
          <img
            v-if="isImage(item) && item.url && !isImgFailed(mIdx, iIdx)"
            class="mtrl-slide__img"
            :src="item.url"
            :alt="item.desc || m.title"
            @error="onImgError(mIdx, iIdx)"
          />
          <!-- 유튜브 링크(SYS018 03 중 유튜브 URL): 썸네일 + 재생 오버레이. 누르면 외부 브라우저.
               인라인 iframe 재생은 셸이 화이트리스트 밖 서브프레임을 차단(H-1②)해 불가하다. -->
          <button
            v-else-if="youtubeIdOf(item) && !isThumbFailed(mIdx, iIdx)"
            type="button"
            class="mtrl-slide__yt"
            :aria-label="`유튜브 열기: ${item.desc || m.title || ''}`"
            @click="openItem(item)"
          >
            <img
              class="mtrl-slide__yt-thumb"
              :src="thumbUrlOf(item)"
              :alt="item.desc || m.title || '유튜브 썸네일'"
              @error="onThumbError(mIdx, iIdx)"
            />
            <span class="mtrl-slide__yt-play" aria-hidden="true">▶</span>
          </button>
          <!-- url 보유 항목 폴백(이미지/썸네일 로드실패·동영상·외부링크·PDF·미지원):
               webview 제약 고려 외부 열기 버튼. `<a target="_blank">` 는 웹뷰에서 무반응이라
               쓰지 않는다(externalLink.js 주석 참조 — 셸에 onCreateWindow 핸들러가 없다). -->
          <button
            v-else-if="item.url"
            type="button"
            class="mtrl-slide__link"
            @click="openItem(item)"
          >{{ openLabel(item) }}</button>
          <!-- url 자체가 없는 항목(서버 서명 URL 미발급 = 원본 파일/경로 누락): 안내 문구로 graceful 처리. -->
          <p v-else class="mtrl-slide__placeholder">자료를 앱에서 표시할 수 없어요</p>

          <!-- 설명만 있는 항목 -->
          <p v-if="item.desc" class="mtrl-slide__desc">{{ item.desc }}</p>
        </div>

        <p v-if="!(m.items && m.items.length)" class="mtrl-empty">표시할 자료가 없어요</p>
      </div>

      <!-- 좌우 네비 + 인디케이터 -->
      <div v-if="(m.items || []).length > 1" class="mtrl-nav">
        <button
          type="button"
          class="mtrl-nav__btn"
          aria-label="이전"
          :disabled="currentIndex(mIdx) === 0"
          @click="prev(mIdx)"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>

        <div class="mtrl-dots">
          <span
            v-for="(d, dIdx) in (m.items || [])"
            :key="dIdx"
            class="mtrl-dots__dot"
            :class="{ 'is-active': dIdx === currentIndex(mIdx) }"
          ></span>
        </div>

        <button
          type="button"
          class="mtrl-nav__btn"
          aria-label="다음"
          :disabled="currentIndex(mIdx) >= (m.items || []).length - 1"
          @click="next(mIdx)"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
      </div>
    </section>

    <p v-if="!materials.length" class="mtrl-empty">교육자료가 없어요</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { extractYoutubeId, youtubeThumbUrl, openExternalUrl } from '@/utils/externalLink'

defineProps({
  // [{ mtrlCd, title, items:[{ type, url, desc, sortIdx }] }]  (자료 묶음 ≤3)
  //  type = MTRL_ITEM_TYPE(SYS018): '01' 이미지 / '02' 동영상 / '03' 유튜브URL(외부링크) / '04' PDF
  materials: { type: Array, default: () => [] },
})

// SYS018 항목 타입 코드 분기(렌더 방식 결정). 백엔드 A6 응답 item.type 사용.
const ITEM_TYPE_IMAGE = '01'
const ITEM_TYPE_VIDEO = '02'
const ITEM_TYPE_LINK = '03'
const isImage = (item) => item.type === ITEM_TYPE_IMAGE
const isVideo = (item) => item.type === ITEM_TYPE_VIDEO
const isLink = (item) => item.type === ITEM_TYPE_LINK

// 이미지 로드 실패 추적(서명 URL/CSP 등으로 인라인 렌더 실패 시 외부 열기 링크로 폴백).
const imgFailed = ref({})
const keyOf = (mIdx, iIdx) => `${mIdx}-${iIdx}`
const isImgFailed = (mIdx, iIdx) => !!imgFailed.value[keyOf(mIdx, iIdx)]
const onImgError = (mIdx, iIdx) => {
  imgFailed.value = { ...imgFailed.value, [keyOf(mIdx, iIdx)]: true }
}

// 유튜브 항목(2026-09-05): videoId 를 뽑아 썸네일을 보여주고, 누르면 외부 브라우저로 연다.
//   타입 '03'(외부링크)에는 유튜브가 아닌 URL 도 들어오므로(운영에 github 링크 실재)
//   extractYoutubeId 가 null 이면 종전 '링크 열기' 버튼으로 자연 폴백된다.
const youtubeIdOf = (item) => (item.url ? extractYoutubeId(item.url) : null)
const thumbUrlOf = (item) => {
  const id = youtubeIdOf(item)
  return id ? youtubeThumbUrl(id) : ''
}

// 썸네일 로드 실패 추적(네트워크/삭제된 영상 등) — 실패 시 '링크 열기' 버튼으로 폴백.
const thumbFailed = ref({})
const isThumbFailed = (mIdx, iIdx) => !!thumbFailed.value[keyOf(mIdx, iIdx)]
const onThumbError = (mIdx, iIdx) => {
  thumbFailed.value = { ...thumbFailed.value, [keyOf(mIdx, iIdx)]: true }
}

// 외부 열기 공통 핸들러(웹뷰=셸 위임 / 브라우저=새 탭).
const openItem = (item) => {
  openExternalUrl(item.url)
}

// url 보유 항목의 외부 열기 링크 라벨(타입별 문구).
const openLabel = (item) => {
  if (isImage(item)) return '이미지 열기'
  if (isVideo(item)) return '동영상 보기'
  if (isLink(item)) return '링크 열기'
  return '자료 열기'
}

// 자료(묶음)별 현재 슬라이드 인덱스 맵. UI 토글 전용(데이터 가공 아님).
const indexMap = ref({})
const currentIndex = (mIdx) => indexMap.value[mIdx] || 0
const prev = (mIdx) => {
  const cur = currentIndex(mIdx)
  if (cur > 0) indexMap.value = { ...indexMap.value, [mIdx]: cur - 1 }
}
const next = (mIdx) => {
  indexMap.value = { ...indexMap.value, [mIdx]: currentIndex(mIdx) + 1 }
}
</script>

<style scoped>
.mtrl-slider {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.mtrl-group {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
}
.mtrl-group__title {
  margin: 0 0 var(--space-sm);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.mtrl-stage {
  position: relative;
  width: 100%;
  min-height: 160px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
  overflow: hidden;
}
.mtrl-slide {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.mtrl-slide__img {
  width: 100%;
  height: auto;
  display: block;
  border-radius: var(--radius-md);
}
.mtrl-slide__desc {
  margin: 0;
  padding: var(--space-md);
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}
/* 동영상/외부링크 항목: webview 외부 열기 버튼
   (2026-09-05: <a target="_blank"> → <button>. 웹뷰에서 새 창 요청은 무반응이라
    클릭 핸들러로 같은 창 내비게이션을 태워 셸이 외부 브라우저로 위임하게 한다.
    button 기본 배경/폰트를 초기화해 종전 링크와 동일한 외형을 유지한다.) */
.mtrl-slide__link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-primary);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
}

/* 유튜브 항목: 썸네일 + 중앙 재생 오버레이(누르면 외부 브라우저) */
.mtrl-slide__yt {
  position: relative;
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: #000;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
}
.mtrl-slide__yt-thumb {
  width: 100%;
  height: auto;
  display: block;
  /* hqdefault 는 4:3 캔버스에 16:9 영상을 넣어 위아래 검은 띠가 생긴다.
     16:9 로 잘라 띠를 감춘다(배경이 검정이라 잘림이 티나지 않는다). */
  aspect-ratio: 16 / 9;
  object-fit: cover;
}
.mtrl-slide__yt-play {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #ffffff;
  font-size: 18px;
  line-height: 1;
  padding-left: 3px; /* ▶ 글리프 좌우 여백 보정 */
}
/* url 부재 파일형/미지원 타입 안내 */
.mtrl-slide__placeholder {
  margin: 0;
  padding: var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.mtrl-empty {
  margin: 0;
  padding: var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.mtrl-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-sm);
}
.mtrl-nav__btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.mtrl-nav__btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.mtrl-dots {
  display: flex;
  gap: var(--space-xs);
}
.mtrl-dots__dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-border);
}
.mtrl-dots__dot.is-active {
  background: var(--color-primary);
}
</style>
