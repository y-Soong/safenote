<template>
  <header class="top-nav">
    <nav class="top-menu">
      <ul>
        <li
          v-for="menu in menus"
          :key="menu.id"
          @click="$emit('navigate', menu.id)"
        >
          {{ menu.label }}
        </li>
      </ul>
    </nav>

    <!-- 사용자 정보 -->
    <div class="user-info relative">
      <span class="user-badge" @click.stop="toggleUserMenu">
        {{ userId }}
      </span>

      <!-- User Menu -->
      <div v-if="userMenuOpen" class="user-dropdown">
        <button
          type="button"
          class="user-dropdown-item"
          @click.stop="goToMyInfo"
        >
          내정보
        </button>
        <button type="button" class="user-dropdown-item" @click.stop="logout">
          로그아웃
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import {
  ref,
  getCurrentInstance,
  defineProps,
  onMounted,
  onBeforeUnmount,
} from "vue";
import { useUserStore } from "@/stores/userStore";
import { useRouter } from "vue-router";
import axios from "@/api/axios";

const userStore = useUserStore();
const userId = sessionStorage.getItem("gv_userNm");
const userMenuOpen = ref(false);
const router = useRouter();

defineProps({
  menus: Array,
});

const { proxy } = getCurrentInstance();

const handleClickOutside = (event) => {
  // user-info 영역을 벗어났는지 확인
  const userInfoEl = document.querySelector(".user-info");
  if (userInfoEl && !userInfoEl.contains(event.target)) {
    userMenuOpen.value = false;
  }
};

onMounted(() => {
  window.addEventListener("click", handleClickOutside);
});

onBeforeUnmount(() => {
  window.removeEventListener("click", handleClickOutside);
});

const toggleUserMenu = () => {
  userMenuOpen.value = !userMenuOpen.value;
};

const goToMyInfo = () => {
  proxy.$alert("내정보로 이동");
  // sessionStorage.clear();
  userMenuOpen.value = false;
};

const logout = async () => {
  try {
    const refreshToken = localStorage.getItem("refreshToken");
    // || localStorage.getItem("gv_refreshToken");
    const response = await axios.post("/comApi/login/logout", {
      refreshToken,
    });
    if (response.status === 200) {
      proxy.$alert("로그아웃 처리됐습니다.");
      sessionStorage.clear();
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("gv_refreshToken");
      userStore.logout();
      userMenuOpen.value = false;
      router.push("/"); // 로그인 성공 → 메인화면 이동
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "로그아웃 처리 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
/* Header: 옅은 그린 tint, 구획만 보이게 */
.top-nav {
  display: flex;
  align-items: center;
  height: 48px;
  min-height: 48px;
  background: rgba(22, 163, 74, 0.04);
  border-bottom: 1px solid #e5e7eb;
  box-shadow: none;
  padding: 0 20px;
  gap: 12px;
  font-family: "Pretendard", sans-serif;
  z-index: 50;
}

.top-menu {
  flex: 1;
}
.top-menu ul {
  display: flex;
  gap: 2.5rem;
  list-style: none;
  margin: 0;
  padding: 0;
}
.top-menu li {
  cursor: pointer;
  font-weight: 600;
  font-size: 0.9rem;
  color: #111827;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

/* Admin 배지: 32px, BG #FFF, border #E5E7EB */
.user-badge {
  height: 32px;
  min-height: 32px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: background 0.2s, border-color 0.2s;
  box-sizing: border-box;
}
.user-badge:hover {
  background: rgba(22, 163, 74, 0.06);
}

.user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 0.5rem;
  min-width: 8rem;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 50;
  overflow: hidden;
}

.user-dropdown-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 0.5rem 1rem;
  border: none;
  background: transparent;
  font-size: 0.9rem;
  font-family: "Pretendard", sans-serif;
  color: #374151;
  cursor: pointer;
  transition: background 0.2s;
}
.user-dropdown-item:hover {
  background: #f9fafb;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.4s ease;
}

.slide-fade-enter-from {
  transform: translateX(100%);
  opacity: 0;
}
.slide-fade-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}
</style>
