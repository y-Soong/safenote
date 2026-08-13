package com.prafta.common.cmm.login.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.cmm.login.dto.request.AgrTermsRequest;
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
	/**
	 * 화면에서 동의한 약관 ID 목록(구버전 앱은 미전송 → 빈 목록).
	 *
	 * <p>termsId 만 운반한다 — 약관 버전은 클라이언트 값을 믿지 않고 서버 조회값을 저장한다.
	 */
	, List<String> agrTermsIdList
) {
	public static UserJoinParam from(UserJoinRequest request) {

		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 구버전 앱은 agrTermsList 를 보내지 않는다 — null 을 빈 목록으로 정규화해
		//   이후 로직이 null 분기 없이 "동의 응답 없음"으로 다루게 한다.
		List<String> agrTermsIdList = new ArrayList<>();
		if (request.getAgrTermsList() != null) {
			for (AgrTermsRequest agrTerms : request.getAgrTermsList()) {
				if (agrTerms != null && agrTerms.getTermsId() != null && !agrTerms.getTermsId().isBlank()) {
					agrTermsIdList.add(agrTerms.getTermsId().trim());
				}
			}
		}

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
    		, agrTermsIdList
        );
    }
}
