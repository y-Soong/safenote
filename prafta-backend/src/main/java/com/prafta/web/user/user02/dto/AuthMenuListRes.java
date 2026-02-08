package com.prafta.web.user.user02.dto;

import java.util.List;

import com.prafta.web.user.user02.vo.AuthMenu;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthMenuListRes{
	List<AuthMenu> authMenuList;
}
