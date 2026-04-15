package com.prafta.web.user.user02.dto.response;

import java.util.List;

import com.prafta.web.user.user02.result.AuthMenuResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthMenuListResponse{
	List<AuthMenuResult> authMenuList;
}
