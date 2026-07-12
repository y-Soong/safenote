<template>
  <div class="side-menu">
    <!-- LNB 브랜드 바: 로고 + 서비스명 (고정) -->
    <a class="lnb-brand" href="#" @click.prevent="goHome">
      <img class="lnb-logo" :src="praftaLogo" alt="PRAFTA" />
      <span class="lnb-service-name">SAFENOTE</span>
    </a>

    <!-- 메뉴 영역 (스크롤) -->
    <div class="lnb-scroll">
      <!-- 즐겨찾기 그룹: 전역 즐겨찾기 전체 노출. 비어 있으면 숨김. 항상 최상단·기본 펼침 -->
      <section v-if="favoriteItems.length" class="lnb-group">
        <button
          type="button"
          class="lnb-group-header"
          :aria-expanded="isFavoriteExpanded"
          @click="toggleFavoriteGroup"
        >
          <span
            class="lnb-group-caret"
            :class="{ collapsed: !isFavoriteExpanded }"
            >▾</span
          >
          <span class="lnb-group-title">즐겨찾기</span>
        </button>
        <ul v-show="isFavoriteExpanded" class="lnb-menu-list">
          <li
            v-for="menu in favoriteItems"
            :key="`fav-${menu.id}`"
            :class="{ active: isActive(menu) }"
            @click="$emit('navigate', menu)"
          >
            <span class="lnb-item-text">{{ menu.label }}</span>
            <button
              type="button"
              class="icon-button"
              @click.stop="$emit('toggle-favorite', menu)"
            >
              <img :src="getImg(menu.isFavorite)" alt="즐겨찾기" />
            </button>
          </li>
        </ul>
      </section>

      <!-- 중분류 accordion 그룹 -->
      <section
        v-for="(group, gIdx) in menus"
        :key="groupKey(group, gIdx)"
        class="lnb-group"
      >
        <button
          type="button"
          class="lnb-group-header"
          :aria-expanded="isGroupExpanded(group, gIdx)"
          @click="toggleGroup(group, gIdx)"
        >
          <span
            class="lnb-group-caret"
            :class="{ collapsed: !isGroupExpanded(group, gIdx) }"
            >▾</span
          >
          <span class="lnb-group-title">{{ group.subGroupNm }}</span>
        </button>
        <ul v-show="isGroupExpanded(group, gIdx)" class="lnb-menu-list">
          <li
            v-for="menu in group.items"
            :key="menu.id"
            :class="{ active: isActive(menu) }"
            @click="$emit('navigate', menu)"
          >
            <span class="lnb-item-text">{{ menu.label }}</span>
            <button
              type="button"
              class="icon-button"
              @click.stop="$emit('toggle-favorite', menu)"
            >
              <img :src="getImg(menu.isFavorite)" alt="즐겨찾기" />
            </button>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref, watch } from "vue";
import { useRouter } from "vue-router";
import favoriteImgSrc from "@/assets/img/favorite.png";
import favoriteImgBlankSrc from "@/assets/img/favorite_blank.png";
import praftaLogo from "@/assets/img/prafta_logo.png";

const props = defineProps({
  // 중분류 그룹 배열: [{ subGroupNm, subGroupIdx, items:[{id,label,route,buttons,isFavorite}] }]
  menus: { type: Array, default: () => [] },
  // 전역 즐겨찾기 메뉴 배열(탭 무관): [{id,label,route,buttons,isFavorite:true}]
  favoriteItems: { type: Array, default: () => [] },
  activeRoute: { type: String, default: "" },
});

defineEmits(["navigate", "toggle-favorite"]);

const router = useRouter();

// localStorage 키(다른 상태와 충돌하지 않도록 네임스페이스 부여)
const STORAGE_KEY = "lnb.accordion.expanded.v1";
const FAV_STORAGE_KEY = "lnb.accordion.favorite.v1";

// 그룹명 -> 펼침 여부 맵
const expandedMap = ref(loadExpandedMap());
const isFavoriteExpanded = ref(loadFavoriteExpanded());

function loadExpandedMap() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : {};
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function loadFavoriteExpanded() {
  try {
    const raw = localStorage.getItem(FAV_STORAGE_KEY);
    // 기본 펼침
    return raw === null ? true : raw === "true";
  } catch {
    return true;
  }
}

function persistExpandedMap() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(expandedMap.value));
  } catch {
    // 저장 실패는 UX 보조 기능이므로 무시
  }
}

// 그룹 키: subGroupNm 우선, 없으면 인덱스
function groupKey(group, idx) {
  return (group && group.subGroupNm) || `group-${idx}`;
}

/* 메뉴 목록이 바뀔 때, 아직 펼침 상태가 결정되지 않은 그룹의 기본값을 설정한다.
   기본값: 모든 중분류 그룹을 펼침(open). 사용자가 직접 접은 그룹은 localStorage 값이 유지된다. */
watch(
  () => props.menus,
  (newMenus) => {
    let changed = false;
    (newMenus || []).forEach((group, idx) => {
      const key = groupKey(group, idx);
      if (!(key in expandedMap.value)) {
        expandedMap.value[key] = true;
        changed = true;
      }
    });
    if (changed) persistExpandedMap();
  },
  { immediate: true, deep: true }
);

function isGroupExpanded(group, idx) {
  const key = groupKey(group, idx);
  return expandedMap.value[key] !== false;
}

function toggleGroup(group, idx) {
  const key = groupKey(group, idx);
  expandedMap.value[key] = !isGroupExpanded(group, idx);
  persistExpandedMap();
}

function toggleFavoriteGroup() {
  isFavoriteExpanded.value = !isFavoriteExpanded.value;
  try {
    localStorage.setItem(FAV_STORAGE_KEY, String(isFavoriteExpanded.value));
  } catch {
    // 무시
  }
}

function isActive(menu) {
  if (!props.activeRoute) return false;
  const segment = menu && menu.route && String(menu.route).trim();
  const full = segment ? `/safenote/main/${segment}` : "/safenote/main";
  return full === props.activeRoute;
}

function goHome() {
  router.push("/safenote/main");
}

function getImg(isFavorite) {
  return isFavorite ? favoriteImgSrc : favoriteImgBlankSrc;
}
</script>

<style scoped>
/* LNB: 배경 패널 톤, 180px */
.side-menu {
  width: 180px;
  min-width: 180px;
  background: rgba(22, 163, 74, 0.06);
  border-right: 1px solid #e5e7eb;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: "Pretendard", sans-serif;
}

/* 브랜드 바: 48px, 고정, 로고 + 서비스명 */
.lnb-brand {
  height: 48px;
  min-height: 48px;
  padding: 0 16px;
  background: rgba(22, 163, 74, 0.06);
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  text-decoration: none;
  color: inherit;
  box-sizing: border-box;
}

.lnb-logo {
  width: 25px;
  height: 25px;
  object-fit: contain;
  flex-shrink: 0;
}

.lnb-service-name {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  letter-spacing: 0;
  white-space: nowrap;
}

/* 스크롤 영역(그룹 누적) */
.lnb-scroll {
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

/* 중분류 그룹 */
.lnb-group {
  border-bottom: 1px solid rgba(229, 231, 235, 0.6);
}

/* 그룹 헤더(클릭 시 펼침/접힘) */
.lnb-group-header {
  width: 100%;
  height: 40px;
  min-height: 40px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-family: "Pretendard", sans-serif;
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  box-sizing: border-box;
  text-align: left;
}

.lnb-group-header:hover {
  background: rgba(22, 163, 74, 0.06);
}

.lnb-group-caret {
  display: inline-flex;
  width: 12px;
  font-size: 11px;
  color: #6b7280;
  transition: transform 0.15s ease;
  flex-shrink: 0;
}

.lnb-group-caret.collapsed {
  transform: rotate(-90deg);
}

.lnb-group-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 메뉴 리스트 */
.lnb-menu-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

/* 메뉴 아이템: 38px, 13px/500 #374151 (하위 항목은 들여쓰기) */
.lnb-menu-list li {
  height: 38px;
  min-height: 38px;
  padding: 0 6px 0 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  background: transparent;
  box-sizing: border-box;
  border-left: 3px solid transparent;
}

.lnb-menu-list li:hover {
  background: rgba(22, 163, 74, 0.08);
}

.lnb-menu-list li.active {
  background: rgba(22, 163, 74, 0.14);
  color: #16a34a;
  border-left-color: #16a34a;
}

.lnb-item-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-button {
  border: none;
  background: transparent;
  padding: 0;
  width: 16px;
  height: 16px;
  min-width: 16px;
  min-height: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: inherit;
}

.icon-button img {
  width: 14px;
  height: 14px;
}
</style>
