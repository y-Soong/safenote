package com.prafta.common.cmm.login.application.command;

import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 셀프가입 INSERT / 거부행 재활용 UPDATE 커맨드.
 *
 * <p>★★[security C-1] <b>권한(authCd)을 담지 않는다.</b> 매퍼가 리터럴 '99999'(일반사원)를 쓰며,
 * 커맨드에 필드를 두면 다시 바인딩될 여지가 생긴다. 권한 부여는 관리자 화면(User_01)의 전용 경로만
 * 담당한다(그쪽은 요청자 권한레벨 이중 검증이 있다).
 */
public record UserJoinCommand(
	String cmpnyCd
	, String userCd
	, String userId
	, String userPw
	, String userNm
	, String siteCd
	, String nodeCd
	, String mblNoEnc
	, String mblNoHmac
	, String mblNoLast4
	, String emailEnc
	, String emailHmac
	, String emailDomain
	, String birthDtEnc
	, String gender
	, String useYn
) {
	public static UserJoinCommand from(
			UserJoinParam param
			, String userCd
			, String userPw
			, String phoneEnc
			, String phoneHmac
			, String phoneLast4
			, String emailEnc
			, String emailHmac
			, String emailDomain
			, String birthEnc
	) {
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(userCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(userPw == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new UserJoinCommand(
			param.cmpnyCd()
			, userCd
			, param.userId()
			, userPw
			, param.userNm()
			, param.siteCd()
			, param.nodeCd()
			, phoneEnc
			, phoneHmac
			, phoneLast4
			, emailEnc
			, emailHmac
			, emailDomain
			, birthEnc
			, param.gender()
			, param.useYn()
        );
    }
}
