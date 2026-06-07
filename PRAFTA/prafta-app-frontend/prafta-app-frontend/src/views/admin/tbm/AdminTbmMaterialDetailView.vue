<!--
  AdminTbmMaterialDetailView.vue — 관리자 TBM 교육자료 상세/미리보기
  - 작업 ID: 001-P5-T-F12-D (분해: 001-phase5-admin-tbm-plan.md §2-6, §3-J T-A10 상세)
  - 진입: 자료 리스트(AdminTbmMaterialList) 카드 선택 → /AdminTbmMaterialDetail?mtrlCd=...
          (라우트 등록은 developer).
  - 백엔드: GET /appApi/admin/tbm/edu-materials/{mtrlCd} (T-A10 상세) — 묶음 메타 + 항목(items).
  - 구성: 메타(제목/타입/설명/스코프) + 항목 미리보기(TbmMaterialSlider 재사용) + 수정/삭제 진입.
  - 디자인 토큰: AdminLauncherView/TbmHubView 세트를 .admin-tbm-material-detail-view 루트에 1회 선언
      (별도 화면이므로 자급 — 자식 TbmMaterialSlider scoped 가 상속).
  - C1: 스코프/권한은 서버만 신뢰. 클라이언트 역할 분기 없음.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + 라우팅/삭제는 TODO(developer).
      ⚠️ API 호출/라우팅/삭제 확정은 developer(R5) — TODO(developer) 참조.
-->
<template>
  <div class="admin-tbm-material-detail-view">
    <!-- 헤더 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-mtrld-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">교육자료 상세</h1>
      <span class="admin-tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main class="admin-tbm-material-detail-body">
      <!-- loading -->
      <p v-if="isLoading" class="admin-tbm-state">불러오는 중…</p>

      <!-- error -->
      <div v-else-if="loadError" class="admin-tbm-state">
        <p class="admin-tbm-state__msg">자료를 불러오지 못했어요.</p>
        <button type="button" class="admin-tbm-retry" @click="onRetry">다시 시도</button>
      </div>

      <template v-else-if="material">
        <!-- 제목 + 스코프 -->
        <div class="admin-tbm-material-detail__head">
          <span class="admin-tbm-material-detail__scope" :class="scopeToneClass">
            {{ scopeLabel }}
          </span>
          <h2 class="admin-tbm-material-detail__title">{{ material.title || '(제목 없음)' }}</h2>
        </div>

        <!-- 메타 -->
        <section class="card">
          <p class="card__label">기본 정보</p>
          <dl class="meta">
            <div class="meta__row">
              <dt>타입</dt>
              <dd>{{ material.mtrlTypeNm || material.mtrlType || '-' }}</dd>
            </div>
            <div class="meta__row">
              <dt>사용여부</dt>
              <dd>{{ material.useYn === 'N' ? '미사용' : '사용' }}</dd>
            </div>
            <div v-if="material.insertNm" class="meta__row">
              <dt>등록자</dt>
              <dd>{{ material.insertNm }}</dd>
            </div>
            <div v-if="material.insertDate" class="meta__row">
              <dt>등록일</dt>
              <dd>{{ material.insertDate }}</dd>
            </div>
          </dl>
        </section>

        <!-- 설명(CONTENTS, plain text) -->
        <section class="card">
          <p class="card__label">설명</p>
          <p class="admin-tbm-material-detail__desc">
            {{ material.contents || '설명이 없어요' }}
          </p>
        </section>

        <!-- 항목 미리보기(TbmMaterialSlider 재사용) -->
        <section class="card">
          <p class="card__label">자료 항목 ({{ items.length }})</p>
          <TbmMaterialSlider v-if="sliderMaterials.length" :materials="sliderMaterials" />
          <p v-else class="admin-tbm-state admin-tbm-state--sm">등록된 항목이 없어요</p>
        </section>

        <!-- 액션: 수정 / 삭제 -->
        <div class="admin-tbm-material-detail__actions">
          <button type="button" class="btn btn--ghost" :disabled="busy" @click="onEdit">
            수정
          </button>
          <button type="button" class="btn btn--danger" :disabled="busy" @click="onDelete">
            삭제
          </button>
        </div>
      </template>
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-mtrld-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import TbmMaterialSlider from '@/views/tbm/components/TbmMaterialSlider.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 우선) — TbmHubView/SessionDetail 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ── 상태 ──────────────────────────────────────────────────────────
const mtrlCd = computed(() => route.query.mtrlCd || '')
const isLoading = ref(false)
const loadError = ref(false)
const busy = ref(false) // 삭제 진행 가드

const material = ref(null) // 묶음 메타: { mtrlCd, title, mtrlType, mtrlTypeNm, contents, siteCd, isCommonContent, useYn, insertNm, insertDate }
const items = ref([]) // [{ mtrlItemCd, mtrlItemType, mtrlDesc, fileMgmtCd, thumbFileMgmtCd, durationSec, url, sortIdx, previewUrl }]

// 스코프 라벨/톤(SITE_CD NULL = 회사공통)
const isCommon = computed(
  () =>
    material.value?.isCommonContent === true ||
    material.value?.isCommonContent === 'Y' ||
    !material.value?.siteCd,
)
const scopeLabel = computed(() => (isCommon.value ? '공통' : '사업장'))
const scopeToneClass = computed(() =>
  isCommon.value
    ? 'admin-tbm-material-detail__scope--common'
    : 'admin-tbm-material-detail__scope--site',
)

// TbmMaterialSlider 계약([{ mtrlCd, title, items:[{ type, url, desc, sortIdx }] }])으로 변환.
//   단일 묶음 1건을 슬라이더 1그룹으로 매핑. type = MTRL_ITEM_TYPE(SYS018) 그대로 사용.
//   ⚠️ url: 파일형(이미지/동영상/PDF)은 서버가 미리보기 url(previewUrl)을 내려주면 사용,
//      없으면 외부 URL(url) 사용. 파일 서빙 endpoint 조립은 developer(supplement §1-2).
const sliderMaterials = computed(() => {
  if (!material.value || !items.value.length) return []
  return [
    {
      mtrlCd: material.value.mtrlCd,
      title: material.value.title,
      items: items.value.map((it) => ({
        type: it.mtrlItemType,
        url: it.previewUrl || it.url || '',
        desc: it.mtrlDesc || '',
        sortIdx: it.sortIdx,
      })),
    },
  ]
})

// ── 조회 ──────────────────────────────────────────────────────────
const loadDetail = async () => {
  if (!mtrlCd.value) {
    loadError.value = true
    return
  }
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/edu-materials/${encodeURIComponent(mtrlCd.value)}`,
    )
    // 응답 키: material + items (백엔드 AdminEduMaterialDetailResponse).
    material.value = data?.material || null
    items.value = Array.isArray(data?.items) ? data.items : []
    if (!material.value) loadError.value = true
  } catch (e) {
    console.error('[AdminTbmMaterialDetailView] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

const onRetry = () => loadDetail()

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

// 수정 진입 — /AdminTbmMaterialForm?mtrlCd=...
const onEdit = () => {
  router.push({ path: '/AdminTbmMaterialForm', query: { mtrlCd: mtrlCd.value } })
}

// 삭제 — confirm 후 DELETE /appApi/admin/tbm/edu-materials/{mtrlCd} → 성공 시 목록으로 back.
const onDelete = async () => {
  if (busy.value) return
  const ok = await askConfirm('이 교육자료를 삭제할까요? 되돌릴 수 없어요.')
  if (!ok) return
  busy.value = true
  try {
    await api.delete(`/appApi/admin/tbm/edu-materials/${encodeURIComponent(mtrlCd.value)}`)
    await showAlert('삭제되었어요.')
    router.back()
  } catch (e) {
    const msg = e?.response?.data?.message || '삭제에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    busy.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminLauncherView/TbmHubView 세트) — 자식(TbmMaterialSlider) scoped 가 상속 */
.admin-tbm-material-detail-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.admin-tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-tbm-hd__back {
  width: 36px;
  height: 36px;
  margin-left: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.admin-tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-hd__spacer {
  width: 36px;
}

/* 본문 */
.admin-tbm-material-detail-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 제목 + 스코프 */
.admin-tbm-material-detail__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.admin-tbm-material-detail__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-material-detail__scope {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.admin-tbm-material-detail__scope--common {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.admin-tbm-material-detail__scope--site {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

/* 카드 */
.card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}
.card__label {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 메타 정의리스트 */
.meta {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.meta__row {
  display: flex;
  gap: var(--space-md);
}
.meta__row dt {
  flex-shrink: 0;
  width: 72px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.meta__row dd {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-primary);
  word-break: break-all;
}

/* 설명(plain text) */
.admin-tbm-material-detail__desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 상태 메시지 */
.admin-tbm-state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.admin-tbm-state--sm {
  margin: 0;
  text-align: left;
  font-size: 13px;
}
.admin-tbm-state__msg {
  margin: 0 0 var(--space-sm);
}
.admin-tbm-retry {
  height: 36px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 액션 */
.admin-tbm-material-detail__actions {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
}
.btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.btn--danger {
  background: var(--color-surface);
  color: var(--color-danger-text);
  border: 1.5px solid var(--color-danger);
}

/* 스프라이트 */
.admin-tbm-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
