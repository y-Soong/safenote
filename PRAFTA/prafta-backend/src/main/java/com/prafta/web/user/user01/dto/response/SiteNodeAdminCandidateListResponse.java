package com.prafta.web.user.user01.dto.response;

import java.util.List;

import com.prafta.web.user.user01.result.UserInfoResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SiteNodeAdminCandidateListResponse{
	
	List<UserInfoResult> userInfoList;
}
