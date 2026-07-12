import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

export async function fnGetMenuList() {
  let menuInfo = {};

  try {
    // const response = await axios.post("/comApi/baseinfo/getMenuList", {
    const response = await axios.get("/comApi/baseinfo/menu-list", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        userCd: sessionStorage.getItem("gv_userCd"),
        menuSrc: "001",
      },
    });

    if (response.status === 200) {
      console.log("response.data :: ");
      console.log(response.data);
      menuInfo = response.data;
    }
  } catch (err) {
    alert(resolveApiErrorMessage(err, "메뉴 조회 중 오류가 발생했습니다."));
  }

  return menuInfo;
}

/**
 * 내 즐겨찾기 메뉴 식별자(menuDId) 목록 조회.
 * USER_CD는 서버에서 JWT로 도출하므로 클라이언트는 파라미터를 보내지 않는다(IDOR 방지).
 * 응답 계약: { favoriteMenuDIds: [menuDId, ...] }.
 * 하위호환 위해 배열 / { favoriteMenuDIds: [...] } / { favorites: [...] } 모두 수용.
 */
export async function fnGetFavorites() {
  let favorites = [];

  try {
    const response = await axios.get("/comApi/menu/favorites");

    if (response.status === 200) {
      const data = response.data;
      // 응답이 배열이거나 { favoriteMenuDIds } / { favorites } 형태 모두 허용
      if (Array.isArray(data)) {
        favorites = data;
      } else if (Array.isArray(data?.favoriteMenuDIds)) {
        favorites = data.favoriteMenuDIds;
      } else if (Array.isArray(data?.favorites)) {
        favorites = data.favorites;
      }
    }
  } catch (err) {
    // 즐겨찾기 조회 실패는 메뉴 자체 표시를 막지 않도록 비치명적으로 처리
    console.error(
      resolveApiErrorMessage(err, "즐겨찾기 조회 중 오류가 발생했습니다.")
    );
  }

  return favorites;
}

/**
 * 즐겨찾기 토글(추가/삭제). USER_CD는 서버 JWT 도출만 신뢰한다.
 * @param {string} menuDId 메뉴 식별자(MENU_D_ID)
 * @returns {Promise<boolean>} 성공 여부
 */
export async function fnToggleFavorite(menuDId) {
  const response = await axios.post("/comApi/menu/favorite-toggle", {
    menuDId,
  });

  return response.status === 200;
}
