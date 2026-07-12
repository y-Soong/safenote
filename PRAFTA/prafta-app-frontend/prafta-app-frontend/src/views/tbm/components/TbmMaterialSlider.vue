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
          <!-- url 보유 항목 폴백(이미지 로드실패/동영상/외부링크/PDF/미지원): webview 제약 고려 외부 열기 링크. -->
          <a
            v-else-if="item.url"
            class="mtrl-slide__link"
            :href="item.url"
            target="_blank"
            rel="noopener noreferrer"
          >{{ openLabel(item) }}</a>
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
/* 동영상/외부링크 항목: webview 외부 열기 링크/버튼 */
.mtrl-slide__link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
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
