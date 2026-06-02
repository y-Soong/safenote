<template>
  <div class="side-menu">
    <!-- LNB 브랜드 바: 로고 + 서비스명 (고정) -->
    <a class="lnb-brand" href="#" @click.prevent="goHome">
      <img class="lnb-logo" :src="praftaLogo" alt="PRAFTA" />
      <span class="lnb-service-name">SAFETY NOTE</span>
    </a>

    <!-- 메뉴 리스트 (스크롤) -->
    <ul class="lnb-menu-list">
      <li
        v-for="menu in menus"
        :key="menu.id"
        :class="{ active: isActive(menu) }"
        @click="$emit('navigate', menu)"
      >
        <span class="lnb-item-text">{{ menu.label }}</span>
        <button type="button" class="icon-button" @click.stop="toggleImg(menu)">
          <img :src="getImg(menu.id)" alt="icon" />
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { defineProps, ref, watch } from "vue";
import { useRouter } from "vue-router";
import favoriteImgSrc from "@/assets/img/favorite.png";
import favoriteImgBlankSrc from "@/assets/img/favorite_blank.png";
import praftaLogo from "@/assets/img/prafta_logo.png";

const props = defineProps({
  menus: { type: Array, default: () => [] },
  activeRoute: { type: String, default: "" },
});

const router = useRouter();
const favoriteMap = ref({});

watch(
  () => props.menus,
  (newMenus) => {
    (newMenus || []).forEach((menu) => {
      if (menu && !(menu.id in favoriteMap.value)) {
        favoriteMap.value[menu.id] = false;
      }
    });
  },
  { immediate: true }
);

function isActive(menu) {
  if (!props.activeRoute) return false;
  const segment = menu.route && String(menu.route).trim();
  const full = segment ? `/main/${segment}` : "/main";
  return full === props.activeRoute;
}

function goHome() {
  router.push("/main");
}

function toggleImg(menu) {
  favoriteMap.value[menu.id] = !favoriteMap.value[menu.id];
}

function getImg(menuId) {
  return favoriteMap.value[menuId] ? favoriteImgSrc : favoriteImgBlankSrc;
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

/* 메뉴 리스트만 스크롤 */
.lnb-menu-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

/* 메뉴 아이템: 40px, 12px padding, 13px/500 #374151 */
.lnb-menu-list li {
  height: 38px;
  min-height: 38px;
  padding: 0 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
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
  width: 24px;
  height: 24px;
  min-width: 24px;
  min-height: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: inherit;
}

.icon-button img {
  width: 16px;
  height: 16px;
}
</style>
