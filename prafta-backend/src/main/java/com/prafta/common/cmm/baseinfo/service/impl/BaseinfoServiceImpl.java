package com.prafta.common.cmm.baseinfo.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.baseinfo.dto.BaseInfoListQuery;
import com.prafta.common.cmm.baseinfo.dto.BaseInfoListReq;
import com.prafta.common.cmm.baseinfo.dto.BaseInfoListRes;
import com.prafta.common.cmm.baseinfo.dto.BaseinfoReq;
import com.prafta.common.cmm.baseinfo.dto.SiteNodeListQry;
import com.prafta.common.cmm.baseinfo.dto.SiteNodeListReq;
import com.prafta.common.cmm.baseinfo.dto.SiteNodeListRes;
import com.prafta.common.cmm.baseinfo.dto.SystInfoListQuery;
import com.prafta.common.cmm.baseinfo.dto.SystInfoListReq;
import com.prafta.common.cmm.baseinfo.dto.SystInfoListRes;
import com.prafta.common.cmm.baseinfo.dto.SystInfoQry;
import com.prafta.common.cmm.baseinfo.dto.SystInfoReq;
import com.prafta.common.cmm.baseinfo.dto.SystInfoRes;
import com.prafta.common.cmm.baseinfo.mapper.BaseinfoMapper;
import com.prafta.common.cmm.baseinfo.service.BaseinfoService;
import com.prafta.common.cmm.baseinfo.vo.BaseInfo;
import com.prafta.common.cmm.baseinfo.vo.SiteNodeInfo;
import com.prafta.common.cmm.baseinfo.vo.SystInfo;
import com.prafta.common.exception.CmmApiException;
import com.prafta.common.util.PasswordHashing;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BaseinfoServiceImpl implements BaseinfoService{
	private final BaseinfoMapper baseinfoMapper;
	
	public BaseinfoServiceImpl(BaseinfoMapper baseinfoMapper) {
		this.baseinfoMapper = baseinfoMapper;
	}
	
	public SystInfoListRes selectSystinfoList(SystInfoListReq dto) {
		
		SystInfoListQuery reqDto = SystInfoListQuery.builder()
				.systCodeList(dto.getSystCodeList())
				.build();

		SystInfoListRes retDto = null;
		
		List<SystInfo> systInfoList = baseinfoMapper.selectSystinfoList(reqDto); 
		
		if(systInfoList.size() > 0) {
			retDto = SystInfoListRes.builder()
					.systInfoList(systInfoList)
					.build();
		}
		
		return retDto;
	}
	
	public SystInfoRes selectSystinfo(SystInfoReq dto) {
		SystInfoQry reqDto = SystInfoQry.builder()
			.codeD(dto.getCodeD())
			.nameD(dto.getNameD())
			.code(dto.getCode())
			.build();
		
		SystInfoRes retDto = null;
		
		List<SystInfo> systInfoList = baseinfoMapper.selectSystinfo(reqDto); 
		
		if(systInfoList.size() > 0) {
			retDto = SystInfoRes.builder()
					.systInfoList(systInfoList)
					.build();
		}
		
		return retDto;
	}
	
	public BaseInfoListRes selectBaseinfoList(BaseInfoListReq dto) {
		
		BaseInfoListQuery reqDto = BaseInfoListQuery.builder()
									.baseCodeList(dto.getBaseCodeList())
									.cmpnyCd(dto.getCmpnyCd())
									.build();
		
		BaseInfoListRes retDto = null;
		
		List<BaseInfo> baseInfoList = baseinfoMapper.selectBaseinfoList(reqDto); 
		
		if(baseInfoList.size() > 0) {
			retDto = BaseInfoListRes.builder()
					.baseInfoList(baseInfoList)
					.build();
		}
		
		return retDto;
	}
	
	public Map<String, Object> selectCmpnyInfo(BaseinfoReq dto) {
		return baseinfoMapper.selectCmpnyInfo(dto);
	}
	
	public Map<String, Object> selectUserIdDupleChk(BaseinfoReq dto) {
		return baseinfoMapper.selectUserIdDupleChk(dto);
	}
	
	public void insertSmsAuthReq(BaseinfoReq dto) {
		if(
			dto.getDupChkYn() != null && dto.getDupChkYn() != "" && dto.getDupChkYn().equals("Y")
		) {
			int mblCnt = baseinfoMapper.selectMblUniqChk(dto);
			
			if(mblCnt > 0) {
				throw new CmmApiException("이미 등록된 휴대폰번호입니다.\n 확인 후 다시 시도해주세요.");
			}
		}
		
		baseinfoMapper.insertSmsAuthReq(dto);
	}
	
	public int updateSmsAuthReq(BaseinfoReq dto) {
		return baseinfoMapper.updateSmsAuthReq(dto);
	}
	
	public Map<String, Object> selectCertNoSmsId(BaseinfoReq dto) {
		return baseinfoMapper.selectCertNoSmsId(dto);
	}
	
	public List<Map<String, Object>> selectSiteInfoList(BaseinfoReq dto) {
		return baseinfoMapper.selectSiteInfoList(dto);
	}
	
	public SiteNodeListRes selectSiteNodeList(SiteNodeListReq dto, Map<String, Object> tokenInfo) {
		String cmpnyCd;
		
		if(tokenInfo != null && tokenInfo.get("gv_cmpnyCd") != null && (String) tokenInfo.get("gv_cmpnyCd") != "") {
			cmpnyCd = (String) tokenInfo.get("gv_cmpnyCd");
		} else {
			cmpnyCd = dto.getCmpnyCd();
		}
		
		SiteNodeListQry reqDto = SiteNodeListQry.builder()
									.cmpnyCd(cmpnyCd)
									.siteCd(dto.getSiteCd())
									.nodeCd(dto.getNodeCd())
									.nodeType(dto.getNodeType())
									.nodeNm(dto.getNodeNm())
									.parentNodeNm(dto.getParentNodeNm())
									.build();
		
		SiteNodeListRes retDto = null;
		
		List<SiteNodeInfo> siteNodeInfoList = baseinfoMapper.selectSiteNodeList(reqDto, tokenInfo);
		
		if(siteNodeInfoList != null && siteNodeInfoList.size() > 0) {
			retDto = SiteNodeListRes.builder()
									.siteNodeInfoList(siteNodeInfoList)
									.build();
			
		}
		
		return retDto;
	}
	
	public List<Map<String, Object>> selectWebMenuList(BaseinfoReq dto) {
		return baseinfoMapper.selectWebMenuList(dto);
	}
	
	public List<Map<String, Object>> selectAppMenuList(BaseinfoReq dto) {
		return baseinfoMapper.selectAppMenuList(dto);
	}
	
	public List<Map<String, Object>> selectMenuList(BaseinfoReq dto) {
		return baseinfoMapper.selectMenuList(dto);
	}
	
	public Map<String, Object> selectUserIdInfo(BaseinfoReq dto) {
		Map<String, Object> retMap = baseinfoMapper.selectUserIdInfo(dto);
		
		if(retMap == null) {
			throw new CmmApiException("가입된 계정이 없습니다.\n확인 후 다시 시도해주세요.");
		}
		return retMap;
	}
	
	public void updateUserPw(BaseinfoReq dto) {
		/* 암호화처리 */
		if(dto.getUserPw() != null) { dto.setUserPw(PasswordHashing.hashPassword(dto.getUserPw())); }
		
		baseinfoMapper.updateUserPw(dto);
	}
	
	public Map<String, Object> selectTermsDInfo(BaseinfoReq dto) {
		Map<String, Object> retMap = baseinfoMapper.selectTermsDInfo(dto);
		
		if(retMap == null) {
			throw new CmmApiException("조회된 약관 내용이 없습니다.\n확인 후 다시 시도해주세요.");
		}
		return retMap;
	}
}
