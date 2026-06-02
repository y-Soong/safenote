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
