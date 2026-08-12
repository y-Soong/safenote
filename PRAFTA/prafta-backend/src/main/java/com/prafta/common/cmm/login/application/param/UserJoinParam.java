package com.prafta.common.cmm.login.application.param;

import com.prafta.common.cmm.login.dto.request.UserJoinRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 셀프가입(회원가입) 파라미터.
 *
 * <p>★★[security C-1] <b>권한(authCd)을 운반하지 않는다.</b> 비로그인 공개 EP 의 바디로 권한을
 * 받으면 권한 자가부여가 된다. 셀프가입 계정의 권한은 서버가 일반사원('99999')으로 고정한다.
 * 필드를 되살리면 그 즉시 취약점이 복원되므로 param/command/매퍼 어디에도 두지 않는다.
 */
public record UserJoinParam(
	String cmpnyCd
	, String userId
	, String userPw
	, String userNm
	, String siteCd
	, String mblNo
	, String birthDt
	, String nodeCd
	, String email
	, String gender
	, String useYn
	// [security H-1] SMS 본인인증 번호(선택). 서버는 인증 완료 기록으로 검증하고, 값이 오면 대조까지 한다.
	, String certNo
) {
	public static UserJoinParam from(UserJoinRequest request) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UserJoinParam(
    		request.getCmpnyCd()
    		, request.getUserId()
    		, request.getUserPw()
    		, request.getUserNm()
    		, request.getSiteCd()
    		, request.getMblNo()
    		, request.getBirthDt()
    		, request.getNodeCd()
    		, request.getEmail()
    		, request.getGender()
    		, request.getUseYn()
    		, request.getCertNo()
        );
    }
}
