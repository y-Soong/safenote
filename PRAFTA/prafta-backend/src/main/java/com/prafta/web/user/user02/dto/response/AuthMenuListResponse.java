package com.prafta.web.user.user02.dto.response;

import java.util.List;
import java.util.Map;

import com.prafta.common.util.MenuAccessNotePolicy;
import com.prafta.web.user.user02.result.AuthMenuResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthMenuListResponse{
	List<AuthMenuResult> authMenuList;

	/**
	 * 화면권한 부여만으로는 이용할 수 없는 화면의 추가 조건 안내(표시 전용).
	 * 키 = 소문자 MENU_D_ID. 화면은 자기 menuDId 를 소문자로 바꿔 조회한다.
	 *
	 * <p>★{@link AuthMenuResult} 는 MyBatis 결과 record 라 필드를 늘리면 SELECT 컬럼 순서와
	 * 어긋나 매핑이 깨진다. 그래서 행에 끼우지 않고 응답에 별도 맵으로 얹는다.
	 */
	Map<String, MenuAccessNotePolicy.AccessNote> menuNotes;
}
