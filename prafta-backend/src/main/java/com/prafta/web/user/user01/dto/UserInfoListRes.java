package com.prafta.web.user.user01.dto;

import java.util.List;

import com.prafta.web.user.user01.vo.UserInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserInfoListRes{
	List<UserInfo> userInfoList;
}
