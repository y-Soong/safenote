package com.prafta.web.baim.baim06.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.baim.BaimErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim06.application.command.CopySiteNodeCommand;
import com.prafta.web.baim.baim06.application.command.SiteNodeCommand;
import com.prafta.web.baim.baim06.application.command.SiteNodeInfoCommand;
import com.prafta.web.baim.baim06.application.model.SiteNodeModel;
import com.prafta.web.baim.baim06.application.param.CopySiteNodeParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeAdminParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeInfoParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeListParam;
import com.prafta.web.baim.baim06.application.param.SiteNodeParam;
import com.prafta.web.baim.baim06.application.query.SiteNodeAdminQuery;
import com.prafta.web.baim.baim06.application.query.SiteNodeCountQuery;
import com.prafta.web.baim.baim06.application.query.SiteNodeListQuery;
import com.prafta.web.baim.baim06.application.query.SiteNodeUserQuery;
import com.prafta.web.baim.baim06.application.query.UserNodeInfoQuery;
import com.prafta.web.baim.baim06.dto.SiteNodeAdminCommand;
import com.prafta.web.baim.baim06.dto.request.SiteNodeAdminRequest;
import com.prafta.web.baim.baim06.dto.response.SiteNodeListResponse;
import com.prafta.web.baim.baim06.mapper.Baim06Mapper;
import com.prafta.web.baim.baim06.result.SiteNodeResult;
import com.prafta.web.baim.baim06.service.Baim06Service;
import com.prafta.web.baim.baim06.vo.UserNodeInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Baim06ServiceImpl implements Baim06Service{
	private final Baim06Mapper baim06Mapper;

	public SiteNodeListResponse selectSiteNodeList(SiteNodeListParam param) {
		
		SiteNodeListResponse response = null;
		
		List<SiteNodeResult> siteNodeList = baim06Mapper.selectSiteNodeList(SiteNodeListQuery.from(param));
		
		if(siteNodeList != null && siteNodeList.size() > 0) {
			response = SiteNodeListResponse.builder()
											.siteNodeList(siteNodeList)
											.build();
		}
		
		return response;
	}
	
	public void saveSiteNode(SiteNodeInfoParam param) {
		
		for(SiteNodeModel model : param.siteNodeModelList()) {
			
			baim06Mapper.saveSiteNode(SiteNodeInfoCommand.from(model));
		}
	}
	
	@Transactional
	public void deleteSiteNode(SiteNodeParam param) {
		
		int nodeCnt = baim06Mapper.selectNodeCnt(SiteNodeCountQuery.from(param));
		
		if(nodeCnt > 1) {
			throw new ApiException(BaimErrorCode.BAIM_400_001);
		} 
		else if(nodeCnt == 1) {
			baim06Mapper.deleteSiteNodeInUser(SiteNodeCommand.from(param));
			
			baim06Mapper.deleteSiteNode(SiteNodeCommand.from(param));
		}
		else {
			throw new ApiException(BaimErrorCode.BAIM_500_002);
		}
	}
	
	@Transactional
	public void deleteSiteAllNode(SiteNodeParam param) {
		
		baim06Mapper.deleteSiteAllNode(SiteNodeCommand.from(param));
		
		// 사업장에 속한 모든 사용자의 소속 일괄 초기화
		baim06Mapper.deleteSiteNodeInUser(SiteNodeCommand.from(param));
	}
	
	@Transactional
	public void copySiteNode(CopySiteNodeParam param) {

		baim06Mapper.deleteSiteAllNode(SiteNodeCommand.from(param));
		
		// 사업장에 속한 모든 사용자의 소속 일괄 초기화
		baim06Mapper.deleteSiteNodeInUser(SiteNodeCommand.from(param));
		
		baim06Mapper.copySiteNode(CopySiteNodeCommand.from(param));
		
	}
	
	@Transactional
	public void saveSiteNodeMainAdmin(SiteNodeAdminParam param) {
		
		int siteNodeInAdminCnt = baim06Mapper.selectSiteNodeInAdmin(SiteNodeAdminQuery.from(param));
		
		/* 부서 관리자로 지정한 사용자의 기존 부서에 남은 관리자가 있는지 체크 */
		if(siteNodeInAdminCnt == 0) {
			
			UserNodeInfo siteNodeAdmin = baim06Mapper.selectUserNodeInfo(UserNodeInfoQuery.from(param));
			
			if(siteNodeAdmin != null) {
				String nodeCd = siteNodeAdmin.getNodeCd();
				
				if (nodeCd != null && !nodeCd.isBlank()) {

					int siteNodeInUserCnt = baim06Mapper.selectSiteNodeInUser(SiteNodeUserQuery.from(param, nodeCd));
					
					/* 부서 관리자로 지정한 사용자의 기존 부서에 남은 근로자가 있는지 체크 */
					if(siteNodeInUserCnt > 0) {
						throw new ApiException(CommonErrorCode.COMMON_400_001, "[" + param.userNm() + "] 님의 기존 부서에 소속 근로자가 있어\n담당 정/부를 비울 수 없습니다.\n담당 정/부를 추가하거나 소속 근로자를 모두\n이동시킨 후 다시 시도해주세요.");
					}
				}
			} else {
				throw new ApiException(CommonErrorCode.COMMON_500_001, "[" + param.userNm() + "] 님의 기존 부서정보를 찾지 못했습니다.\n관리자에게 문의해주세요.");
			}
		}
		
		SiteNodeAdminCommand siteNodeAdminCommand = SiteNodeAdminCommand.from(param);
		
		baim06Mapper.deleteSiteNodeMainAdmin(siteNodeAdminCommand);
		baim06Mapper.deleteSiteNodeSubAdmin(siteNodeAdminCommand);
		baim06Mapper.saveSiteNodeMainAdmin(siteNodeAdminCommand);
		baim06Mapper.updateUserNode(siteNodeAdminCommand);
	}
	
	@Transactional
	public void saveSiteNodeSubAdmin(SiteNodeAdminParam param) {
		
		int siteNodeInAdminCnt = baim06Mapper.selectSiteNodeInAdmin(SiteNodeAdminQuery.from(param));
		
		/* 부서 관리자로 지정한 사용자의 기존 부서에 남은 관리자가 있는지 체크 */
		if(siteNodeInAdminCnt == 0) {
					
			UserNodeInfo siteNodeAdmin = baim06Mapper.selectUserNodeInfo(UserNodeInfoQuery.from(param));
			
			if(siteNodeAdmin != null) {
				String nodeCd = siteNodeAdmin.getNodeCd();
				
				if (nodeCd != null && !nodeCd.isBlank()) {
					
					int siteNodeInUserCnt = baim06Mapper.selectSiteNodeInUser(SiteNodeUserQuery.from(param, nodeCd));
					
					/* 부서 관리자로 지정한 사용자의 기존 부서에 남은 근로자가 있는지 체크 */
					if(siteNodeInUserCnt > 0) {
						throw new ApiException(CommonErrorCode.COMMON_400_001, "[" + param.userNm() + "] 님의 기존 부서에 소속 근로자가 있어\n담당 정/부를 비울 수 없습니다.\n담당 정/부를 추가하거나 소속 근로자를 모두\n이동시킨 후 다시 시도해주세요.");
					}
				}
			} else {
				throw new ApiException(CommonErrorCode.COMMON_500_001, "[" + param.userNm() + "] 님의 기존 부서정보를 찾지 못했습니다.\n관리자에게 문의해주세요.");
			}
		}
		
		SiteNodeAdminCommand siteNodeAdminCommand = SiteNodeAdminCommand.from(param);
		
		baim06Mapper.deleteSiteNodeMainAdmin(siteNodeAdminCommand);
		baim06Mapper.deleteSiteNodeSubAdmin(siteNodeAdminCommand);
		baim06Mapper.saveSiteNodeSubAdmin(siteNodeAdminCommand);
		baim06Mapper.updateUserNode(siteNodeAdminCommand);
	}
	
	@Transactional
	public void deleteSiteNodeAdmin(SiteNodeAdminParam param) {
		
		int siteNodeInAdminCnt = baim06Mapper.selectSiteNodeInAdmin(SiteNodeAdminQuery.from(param));
		
		/* 부서 관리자로 지정한 사용자의 기존 부서에 남은 관리자가 있는지 체크 */
		if(siteNodeInAdminCnt == 0) {
			
			UserNodeInfo siteNodeAdmin = baim06Mapper.selectUserNodeInfo(UserNodeInfoQuery.from(param));
			
			if(siteNodeAdmin != null) {
				String nodeCd = siteNodeAdmin.getNodeCd();
				
				if (nodeCd != null && !nodeCd.isBlank()) {
				
					int siteNodeInUserCnt = baim06Mapper.selectSiteNodeInUser(SiteNodeUserQuery.from(param, nodeCd));
					
					/* 부서 관리자로 지정한 사용자의 기존 부서에 남은 근로자가 있는지 체크 */
					if(siteNodeInUserCnt > 0) {
						throw new ApiException(CommonErrorCode.COMMON_400_001, "[" + param.userNm() + "] 님의 기존 부서에 소속 근로자가 있어\n담당 정/부를 비울 수 없습니다.\n담당 정/부를 추가하거나 소속 근로자를 모두\n이동시킨 후 다시 시도해주세요.");
					}
				}
			} else {
				throw new ApiException(CommonErrorCode.COMMON_500_001, "[" + param.userNm() + "] 님의 기존 부서정보를 찾지 못했습니다.\n관리자에게 문의해주세요.");
			}
		}
		
		SiteNodeAdminCommand siteNodeAdminCommand = SiteNodeAdminCommand.from(param);
		
		baim06Mapper.deleteSiteNodeMainAdmin(siteNodeAdminCommand);
		baim06Mapper.deleteSiteNodeSubAdmin(siteNodeAdminCommand);
	}
}
