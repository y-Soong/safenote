package com.prafta.common.cmm.dailyjoin.application.param;

import com.prafta.common.cmm.dailyjoin.dto.request.UserIdDupleCheckRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 일일사용자 회원가입 - 사용자ID 중복체크 파라미터.
 */
public record UserIdDupleCheckParam(
    String cmpnyCd
    , String userId
) {
    public static UserIdDupleCheckParam from(UserIdDupleCheckRequest request) {

        if (request == null
                || request.getCmpnyCd() == null || request.getCmpnyCd().isBlank()
                || request.getUserId() == null || request.getUserId().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 형식/길이 서버측 검증
        DailyJoinValidators.validateCmpnyCd(request.getCmpnyCd());
        DailyJoinValidators.validateUserId(request.getUserId());

        return new UserIdDupleCheckParam(
            request.getCmpnyCd()
            , request.getUserId()
        );
    }
}
