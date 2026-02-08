package com.prafta.web.baim.baim06.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.exception.BaimApiException;
import com.prafta.web.baim.baim06.dto.CopySiteNodeReq;
import com.prafta.web.baim.baim06.dto.CopySiteNodeSave;
import com.prafta.web.baim.baim06.dto.SiteNodeDelete;
import com.prafta.web.baim.baim06.dto.SiteNodeListQry;
import com.prafta.web.baim.baim06.dto.SiteNodeListReq;
import com.prafta.web.baim.baim06.dto.SiteNodeListRes;
import com.prafta.web.baim.baim06.dto.SiteNodeReq;
import com.prafta.web.baim.baim06.dto.SiteNodeSave;
import com.prafta.web.baim.baim06.mapper.Baim06Mapper;
import com.prafta.web.baim.baim06.service.Baim06Service;
import com.prafta.web.baim.baim06.vo.SiteNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim06ServiceImpl implements Baim06Service{
	private final Baim06Mapper baim06Mapper;
		
	public Baim06ServiceImpl(Baim06Mapper baim06Mapper) {
		this.baim06Mapper = baim06Mapper;
	}
	
//	public DailyUserSlotListRes selectDailyUserSlotList(DailyUserSlotListReq dto, Map<String, Object> tokenInfo) {
//		
//		DailyUserSlotListQry reqDto = DailyUserSlotListQry.builder()
//										.siteCd(dto.getSiteCd())
//										.slotType(dto.getSlotType())
//										.slotStatus(dto.getSlotStatus())
//										.useYn(dto.getUseYn())
//										.currUserId(dto.getCurrUserId())
//										.build();
//		
//		DailyUserSlotListRes retDto = null;
//		
//		List<DailyUserSlotList> dailyUserSlotList = baim05Mapper.selectDailyUserSlotList(reqDto, tokenInfo);
//		
//		if(dailyUserSlotList.size() > 0) {
//			retDto = DailyUserSlotListRes.builder()
//					.dailyUserSlotList(dailyUserSlotList)
//					.build();
//		}
//		
//		return retDto;
//	}
	public SiteNodeListRes selectSiteNodeList(SiteNodeListReq dto, Map<String, Object> tokenInfo) {
		SiteNodeListQry reqdDto = SiteNodeListQry.builder()
										.siteCd(dto.getSiteCd())
										.build();
		
		SiteNodeListRes retDto = null;
		
		List<SiteNode> siteNodeList = baim06Mapper.selectSiteNodeList(reqdDto, tokenInfo);
		
		if(siteNodeList != null && siteNodeList.size() > 0) {
			retDto = SiteNodeListRes.builder()
											.siteNodeList(siteNodeList)
											.build();
		}
		
		return retDto;
	}
	
	public void saveSiteNode(List<SiteNodeReq> dtoList, Map<String, Object> tokenInfo) {
		
		for(SiteNodeReq dto : dtoList) {
			
			SiteNodeSave reqDto = SiteNodeSave.builder()
										.siteCd(dto.getSiteCd())
										.nodeCd(dto.getNodeCd())
										.nodeNm(dto.getNodeNm())
										.nodeType(dto.getNodeType())
										.parentNodeCd(dto.getParentNodeCd())
										.selfAttdApprvYn(dto.getSelfAttdApprvYn())
										.build();
			
			baim06Mapper.saveSiteNode(reqDto, tokenInfo);
		}
	}
	
	public void deleteSiteNode(SiteNodeReq dto, Map<String, Object> tokenInfo) {
		SiteNodeDelete reqDto = SiteNodeDelete.builder()
										.siteCd(dto.getSiteCd())
										.nodeCd(dto.getNodeCd())
										.build();
		
		int nodeCnt = baim06Mapper.selectNodeCnt(reqDto, tokenInfo);
		
		if(nodeCnt > 1) {
			throw new BaimApiException("하위 조직이 존재하는 경우 삭제할 수 없습니다.");
		} 
		else if(nodeCnt == 1) {
			baim06Mapper.deleteSiteNode(reqDto, tokenInfo);
		}
		else {
			throw new BaimApiException("조직정보 삭제를 실패했습니다.");
		}
	}
	
	public void deleteSiteAllNode(SiteNodeReq dto, Map<String, Object> tokenInfo) {
		SiteNodeDelete reqDto = SiteNodeDelete.builder()
										.siteCd(dto.getSiteCd())
										.build();
		
		baim06Mapper.deleteSiteAllNode(reqDto, tokenInfo);
	}
	
	@Transactional
	public void copySiteNode(CopySiteNodeReq dto, Map<String, Object> tokenInfo) {
		
		SiteNodeDelete reqDto = SiteNodeDelete.builder()
				.siteCd(dto.getSiteCd())
				.build();

		baim06Mapper.deleteSiteAllNode(reqDto, tokenInfo);
		
		CopySiteNodeSave reqDto2 = CopySiteNodeSave.builder()
										.siteCd(dto.getSiteCd())
										.targetSiteCd(dto.getTargetSiteCd())
										.build();
		
		baim06Mapper.copySiteNode(reqDto2, tokenInfo);
		
	}
}
