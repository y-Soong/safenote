package com.prafta.web.baim.baim05.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.baim.BaimErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.common.security.normalize.Normalizers;
import com.prafta.common.util.PasswordHasher;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotCommand;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotUpdCommand;
import com.prafta.web.baim.baim05.application.command.InsertDailyQrUserCommand;
import com.prafta.web.baim.baim05.application.command.LinkPoliciesCommand;
import com.prafta.web.baim.baim05.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.application.param.InsertDailyQrUserParam;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim05.application.query.DailyUserLinkPoliciesQuery;
import com.prafta.web.baim.baim05.application.query.DailyUserSlotListQuery;
import com.prafta.web.baim.baim05.application.query.UserSlotCountQuery;
import com.prafta.web.baim.baim05.dto.response.DailyUserLinkPoliciesResponse;
import com.prafta.web.baim.baim05.dto.response.DailyUserSlotListResponse;
import com.prafta.web.baim.baim05.dto.response.InsertDailyQrUserResponse;
import com.prafta.web.baim.baim05.mapper.Baim05Mapper;
import com.prafta.web.baim.baim05.result.DailyUserLinkPolicyResult;
import com.prafta.web.baim.baim05.result.DailyUserQrInfoResult;
import com.prafta.web.baim.baim05.result.DailyUserSlotListResult;
import com.prafta.web.baim.baim05.service.Baim05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Baim05ServiceImpl implements Baim05Service{
	private final Baim05Mapper baim05Mapper;
	private final HmacSigner hmacSigner; 
	private final AesGcmCrypto aesGcmCrypto;
	private final PasswordHasher passwordHasher;
	private final JwtUtil jwtUtil;
		
	
	public DailyUserLinkPoliciesResponse selectDailyUserLinkPolicyList(DailyUserLinkPoliciesParam param) {
		
		DailyUserLinkPoliciesResponse response = null;
		
		DailyUserLinkPolicyResult dailyUserLinkPolicy = baim05Mapper.selectDailyUserLinkPolicyList(DailyUserLinkPoliciesQuery.from(param));
		
		if(dailyUserLinkPolicy != null) {
			response = DailyUserLinkPoliciesResponse.builder()
					.dailyUserLinkPolicy(dailyUserLinkPolicy)
					.build();
		}
		
		return response;
	}
	
	public DailyUserSlotListResponse selectDailyUserSlotList(DailyUserSlotListParam param) {
		
		DailyUserSlotListResponse response = null;
		
		List<DailyUserSlotListResult> dailyUserSlotList = baim05Mapper.selectDailyUserSlotList(DailyUserSlotListQuery.from(param));
		
		if(dailyUserSlotList.size() > 0) {
			response = DailyUserSlotListResponse.builder()
					.dailyUserSlotList(dailyUserSlotList)
					.build();
		}
		
		return response;
	}
	
	public void saveDailyUserLinkPolicy(LinkPoliciesParam param) {
		
		baim05Mapper.saveDailyUserLinkPolicy(LinkPoliciesCommand.from(param));
		
		int dayLimitCnt = Integer.parseInt(String.valueOf(param.dayLimitCnt()));
		int dailyUserSlotCnt = baim05Mapper.selectDailyUserSlotCnt(UserSlotCountQuery.from(param));
		
		if(dayLimitCnt > 0 || dailyUserSlotCnt > 0) {
			int limit = 0;
			
			if(dailyUserSlotCnt > 0) {
				if(dailyUserSlotCnt > dayLimitCnt) {
					limit = dailyUserSlotCnt;
				} else {
					limit = dayLimitCnt;
				}
			} else {
				limit = dayLimitCnt;
			}
			
			for(int i = 0; i < limit; i++) {
				String useYn = "";
				
				if(i < dayLimitCnt) {
					useYn = "Y";
				} 
				else {
					useYn = "N";
				}
				
				baim05Mapper.saveDailyUserSlot(DailyUserSlotCommand.from(param, i, useYn));
			}
		}
	}

	@Override
	@Transactional
	public InsertDailyQrUserResponse insertDailyQrUser(InsertDailyQrUserParam param) {
		String userCd = baim05Mapper.selectDailyUserCd(param.gvCmpnyCd());
		// 1) 난수 해시값만 생성
		String userPw = passwordHasher.generateRandomHash();
		
		// 2) 정규화
        String phoneNorm = Normalizers.normalizePhone(param.mblNo());

        // 3) ENC (AES-GCM)
        String phoneEnc = (phoneNorm == null) ? null : aesGcmCrypto.encrypt(phoneNorm);

        // 4) HMAC 인덱스 (equals/중복/계정찾기)
        // 회사 단위 유니크면 cmpnyCd 섞는 걸 추천
        String phoneHmac = (phoneNorm == null) ? null : hmacSigner.hmacSha256Base64Url(phoneNorm);

        // 5) 파생값
        String phoneLast4 = Normalizers.last4(phoneNorm);
        
        InsertDailyQrUserCommand command = InsertDailyQrUserCommand.from(userCd, param, userPw, phoneEnc, phoneHmac, phoneLast4);
        
        // 휴대폰번호 기준 계정 중복 체크
        int userCnt = baim05Mapper.selectDailyUserDuplicateCnt(phoneHmac);
        
        if(userCnt > 0) {
        	throw new ApiException(BaimErrorCode.BAIM_400_003);
        }
		
        // 일일사용자 계정 생성
		baim05Mapper.insertDailyQrUser(command);
		
		// 일일사용자 계정 슬롯 할당
		baim05Mapper.updateDailyUserSlotCurrUserCd(DailyUserSlotUpdCommand.from(param, userCd));
		
		return InsertDailyQrUserResponse.builder().dailyUserQrInfoResult(new DailyUserQrInfoResult(param.gvCmpnyCd(), param.siteCd(), userCd)).build();
	}
}
