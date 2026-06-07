<!--
  AdminTbmMaterialList.vue — 탭3 TBM 교육자료 관리 리스트 (자료 묶음)
  - 작업 ID: 001-P5-T-F12 (분해: 001-phase5-admin-tbm-plan.md §2-6, §3-J T-A10)
  - 백엔드: GET /appApi/admin/tbm/edu-materials?mtrlType=&title=&useYn=&page=&pageSize= (T-A10)
      식별자(회사)·스코프(회사공통 OR 접근가능 사업장)는 서버(토큰+resolveScope). 프론트는 필터 값만 전송.
  - 표시: 제목 / 타입(MTRL_TYPE 코드명) / 항목수(itemCnt) / 스코프(공통/사업장) / 등록일.
      카드 클릭 → 부모(AdminTbmView)로 select emit({ mtrlCd }). "자료 등록" 버튼 → create emit.
  - 상태별 동작: loading / error(재시도) / empty / success.
  - 디자인 토큰은 부모(.admin-tbm-view)에서 상속(셸 내부에서만 사용 — 루트 재선언 없음).
  - 참조 패턴: AdminTbmManageList.vue(필터칩/loading/error/empty/리스트) + AdminTbmSessionCard.vue(카드/배지).
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격 + UI 필터 토글만(조회 로직은 developer).
      ⚠️ API 호출/라우팅은 developer(R5) — TODO(developer) 참조.
-->
<template>
  <div class="admin-tbm-mtrl">
    <!-- 상단: 검색 + 등록 -->
    <div class="admin-tbm-mtrl__top">
      <input
        v-model.trim="title"
        class="admin-tbm-mtrl__search"
        type="text"
        maxlength="200"
        placeholder="자료 제목 검색"
        @keyup.enter="onSearch"
      />
      <button type="button" class="admin-tbm-mtrl__create" @click="$emit('create')">
        자료 등록
      </button>
    </div>

    <!-- 타입 필터 칩(MTRL_TYPE, UI 토글 — 재조회는 developer) -->
    <div class="admin-tbm-mtrl__filter" role="tablist" aria-label="자료 타입 필터">
      <button
        v-for="f in typeFilters"
        :key="f.value"
        type="button"
        class="filter-chip"
        :class="{ 'is-active': mtrlType === f.value }"
        @click="onChangeType(f.value)"
      >
        {{ f.label }}
      </button>
    </div>

    <!-- loading -->
    <p v-if="isLoading" class="admin-tbm-mtrl__state">불러오는 중…</p>

    <!-- error -->
    <div v-else-if="loadError" class="admin-tbm-mtrl__state">
      <p class="admin-tbm-mtrl__state-msg">목록을 불러오지 못했어요.</p>
      <button type="button" class="admin-tbm-mtrl__retry" @click="onRetry">다시 시도</button>
    </div>

    <!-- empty -->
    <p v-else-if="!materials.length" class="admin-tbm-mtrl__state admin-tbm-mtrl__state--empty">
      등록된 교육자료가 없어요
    </p>

    <!-- success -->
    <template v-else>
      <button
        v-for="m in materials"
        :key="m.mtrlCd"
        type="button"
        class="mtrl-card"
        @click="$emit('select', { mtrlCd: m.mtrlCd })"
      >
        <div class="mtrl-card__head">
          <span class="mtrl-card__scope" :class="scopeToneClass(m)">{{ scopeLabel(m) }}</span>
          <p class="mtrl-card__title">{{ m.title || '(제목 없음)' }}</p>
        </div>

        <div class="mtrl-card__metaline">
          <span class="mtrl-card__type">{{ m.mtrlTypeNm || m.mtrlType || '-' }}</span>
          <span class="mtrl-card__dot" aria-hidden="true">·</span>
          <span class="mtrl-card__count">항목 {{ num(m.itemCnt) }}</span>
          <span v-if="m.useYn === 'N'" class="mtrl-card__off">미사용</span>
        </div>

        <p v-if="metaText(m)" class="mtrl-card__sub">{{ metaText(m) }}</p>
      </button>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

// select: 자료 상세로({ mtrlCd }) / create: 자료 등록 폼 진입
defineEmits(['select', 'create'])

// 타입 필터(MTRL_TYPE = COM003 "TBM교육타입"). 서버 옵션 endpoint(options:[{code,name}])로 동적 구성.
//   기본은 '전체'만 두고, 마운트 시 코드 목록을 뒤에 붙인다.
const typeFilters = ref([{ value: '', label: '전체' }])

// MTRL_TYPE 코드 목록 조회 — GET /appApi/admin/tbm/material-type-options (options:[{code,name}]).
const loadTypeFilters = async () => {
  try {
    const { data } = await api.get('/appApi/admin/tbm/material-type-options')
    const options = Array.isArray(data?.options) ? data.options : []
    typeFilters.value = [
      { value: '', label: '전체' },
      ...options.map((o) => ({ value: o.code, label: o.name })),
    ]
  } catch (e) {
    console.error('[AdminTbmMaterialList] 타입 옵션 조회 실패:', e?.message)
  }
}

// ── 반응형 상태 ───────────────────────────────────────────────────
const isLoading = ref(false)
const loadError = ref(false)
const mtrlType = ref('') // 선택된 타입 필터
const title = ref('') // 제목 검색어
// 자료 묶음: [{ mtrlCd, title, mtrlType, mtrlTypeNm, siteCd, isCommonContent, contents, itemCnt, useYn, insertNm, insertDate }]
const materials = ref([])

// 카운트 표시(없으면 0)
const num = (v) => (v == null ? 0 : v)

// 스코프 라벨/톤(SITE_CD NULL = 회사공통 / 값 = 사업장 전용. 서버 isCommonContent 우선)
const isCommon = (m) => m?.isCommonContent === true || m?.isCommonContent === 'Y' || !m?.siteCd
const scopeLabel = (m) => (isCommon(m) ? '공통' : '사업장')
const scopeToneClass = (m) =>
  isCommon(m) ? 'mtrl-card__scope--common' : 'mtrl-card__scope--site'

// 보조 메타(등록자 + 등록일)
const metaText = (m) => {
  const parts = []
  if (m?.insertNm) parts.push(m.insertNm)
  if (m?.insertDate) parts.push(m.insertDate)
  return parts.join(' · ')
}

// 교육자료 묶음 조회 — GET /appApi/admin/tbm/edu-materials (T-A10).
//   식별자/스코프는 서버(토큰+resolveScope)가 산출하므로 클라이언트는 타입/제목 필터만 전달한다(C1).
const loadMaterials = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const params = {}
    if (mtrlType.value) params.mtrlType = mtrlType.value
    if (title.value) params.title = title.value
    const { data } = await api.get('/appApi/admin/tbm/edu-materials', { params })
    // 응답 키 'materials' 통일(백엔드 AdminEduMaterialListResponse).
    materials.value = Array.isArray(data?.materials) ? data.materials : []
  } catch (e) {
    console.error('[AdminTbmMaterialList] 목록 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// 타입 필터 변경(UI 토글) → 재조회
const onChangeType = (value) => {
  if (mtrlType.value === value) return
  mtrlType.value = value
  loadMaterials()
}

// 제목 검색(엔터/돋보기) → 재조회
const onSearch = () => {
  loadMaterials()
}

const onRetry = () => {
  loadMaterials()
}

onMounted(() => {
  loadTypeFilters()
  loadMaterials()
})
</script>

<style scoped>
.admin-tbm-mtrl {
  display: flex;
  flex-direction: column;
}

/* 상단: 검색 + 등록 */
.admin-tbm-mtrl__top {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}
.admin-tbm-mtrl__search {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.admin-tbm-mtrl__search:focus {
  outline: none;
  border-color: var(--color-primary);
}
.admin-tbm-mtrl__create {
  flex-shrink: 0;
  height: 40px;
  padding: 0 var(--space-md);
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}

/* 타입 필터 칩 */
.admin-tbm-mtrl__filter {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
}
.filter-chip {
  height: 32px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.filter-chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary-tint-border);
  color: var(--color-primary);
  font-weight: 700;
}

/* 자료 카드 */
.mtrl-card {
  width: 100%;
  text-align: left;
  display: block;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-md);
  cursor: pointer;
  font-family: inherit;
}
.mtrl-card:active {
  background: var(--color-bg);
}
.mtrl-card__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.mtrl-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 스코프 배지 */
.mtrl-card__scope {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.mtrl-card__scope--common {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.mtrl-card__scope--site {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

/* 타입/항목수 메타 라인 */
.mtrl-card__metaline {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  margin-top: var(--space-sm);
  font-size: 13px;
  color: var(--color-text-secondary);
}
.mtrl-card__dot {
  color: var(--color-text-tertiary);
}
.mtrl-card__off {
  margin-left: var(--space-sm);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  font-size: 11px;
  color: var(--color-text-tertiary);
}
.mtrl-card__sub {
  margin: var(--space-sm) 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 상태 메시지 */
.admin-tbm-mtrl__state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.admin-tbm-mtrl__state--empty {
  padding: var(--space-lg) 0;
}
.admin-tbm-mtrl__state-msg {
  margin: 0 0 var(--space-sm);
}
.admin-tbm-mtrl__retry {
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
</style>
