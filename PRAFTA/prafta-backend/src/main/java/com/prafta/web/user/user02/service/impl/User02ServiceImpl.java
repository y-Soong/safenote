package com.prafta.web.user.user02.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.util.MenuLockPolicy;
import com.prafta.web.user.user02.application.command.AuthMenuInfoCommand;
import com.prafta.web.user.user02.application.model.AuthMenuInfoModel;
import com.prafta.web.user.user02.application.param.AuthMenuInfoParam;
import com.prafta.web.user.user02.application.param.AuthMenuListParam;
import com.prafta.web.user.user02.application.query.AuthMenuListQuery;
import com.prafta.web.user.user02.dto.response.AuthMenuListResponse;
import com.prafta.web.user.user02.mapper.User02Mapper;
import com.prafta.web.user.user02.result.AuthMenuResult;
import com.prafta.web.user.user02.service.User02Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class User02ServiceImpl implements User02Service{
	private final User02Mapper user02Mapper;

	private static final String LOCKED_VALUE = "Y";

	public User02ServiceImpl(User02Mapper user02Mapper) {
		this.user02Mapper = user02Mapper;
	}


	public AuthMenuListResponse selectAuthMenuList(AuthMenuListParam param) {

		AuthMenuListResponse retDto = null;

		List<AuthMenuResult> authMenuList = user02Mapper.selectAuthMenuList(AuthMenuListQuery.from(param));

		if(authMenuList.size() > 0) {
			retDto = AuthMenuListResponse.builder()
					.authMenuList(authMenuList)
					.build();
		}

		return retDto;
	}

	// PRAFTA-042-2: 화면권한 저장 시 역할별 잠금(해제 불가) 화면은 USE_YN/BTN_* 를
	//   모두 'Y' 로 서버가 강제 보정한 뒤 저장한다(D5). FE 우회(devtools/직접 API) 방어.
	//   거부가 아니라 보정이며, 입력이 잠금과 달랐으면 변조 흔적을 warn 로그로 남긴다(PII 미포함).
	//
	// LNB 재편(lnb-restructure): 잠금 기준을 MENU_M_ID → MENU_D_ID 로 전환했다.
	//   한 대분류 탭(attdHr/safety/system/dailyAcct)에 여러 도메인이 섞이게 되어 MENU_M_ID 단위
	//   판정이 원래 잠금 의도를 정확히 표현하지 못하기 때문이며, MENU_D_ID 는 LNB 재편 후에도
	//   불변이라 잠금 의도를 1:1 보존한다. 따라서 menuDId -> menuMId 매핑 조회
	//   (selectMenuMIdsByMenuDIds)는 더 이상 필요하지 않다.
	@Override
	@Transactional
	public void updateAuthMenuInfo(AuthMenuInfoParam param) {

		List<AuthMenuInfoModel> models = param.authMenuInfoModelList();
		if (models == null || models.isEmpty()) {
			return;
		}

		for (AuthMenuInfoModel model : models) {

			// 잠금 판정: MENU_D_ID 기준(master 는 menuDId 무관 전체 잠금).
			boolean locked = MenuLockPolicy.isLockedMenuByMenuDId(
					model.authCd(), model.menuDId());

			if (locked) {
				AuthMenuInfoCommand corrected = lockedCommand(model);

				// 입력이 잠금과 달랐으면(어느 컬럼이든 'Y' 가 아님) 변조 흔적 warn 로그.
				if (isAnyNotLocked(model)) {
					log.warn("화면권한 잠금 위반 입력 보정 - authCd={}, menuDId={}, 입력(useYn={},srch={},new={},del={},save={},excl={})",
							model.authCd(), model.menuDId(),
							model.useYn(), model.btnSrch(), model.btnNew(),
							model.btnDel(), model.btnSave(), model.btnExcl());
				}

				user02Mapper.mergeAuthMenuInfo(corrected);
			} else {
				user02Mapper.mergeAuthMenuInfo(AuthMenuInfoCommand.from(model));
			}
		}
	}

	/** 잠금 대상의 USE_YN/BTN_* 를 모두 'Y' 로 강제한 Command 를 생성한다(불변 record라 새 인스턴스). */
	private AuthMenuInfoCommand lockedCommand(AuthMenuInfoModel model) {
		return new AuthMenuInfoCommand(
				model.authCd()
				, model.menuDId()
				, LOCKED_VALUE
				, LOCKED_VALUE
				, LOCKED_VALUE
				, LOCKED_VALUE
				, LOCKED_VALUE
				, LOCKED_VALUE
				, model.gvCmpnyCd()
				, model.gvUserCd()
		);
	}

	/** 입력값 중 하나라도 'Y' 가 아니면 true(잠금과 불일치 = 변조 흔적). */
	private boolean isAnyNotLocked(AuthMenuInfoModel model) {
		return !LOCKED_VALUE.equals(model.useYn())
				|| !LOCKED_VALUE.equals(model.btnSrch())
				|| !LOCKED_VALUE.equals(model.btnNew())
				|| !LOCKED_VALUE.equals(model.btnDel())
				|| !LOCKED_VALUE.equals(model.btnSave())
				|| !LOCKED_VALUE.equals(model.btnExcl());
	}

}
