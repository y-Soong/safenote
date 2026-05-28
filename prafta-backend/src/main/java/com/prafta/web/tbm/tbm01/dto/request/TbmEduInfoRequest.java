package com.prafta.web.tbm.tbm01.dto.request;

import java.util.List;

import com.prafta.web.tbm.tbm01.application.model.TbmEduItemInfoModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TbmEduInfoRequest {

	private String mtrlCd;
	private String title;
	private String contents;
	private String mtrlType;
	private String useYn;
	private String siteCd;		// prafta-033-A: 스코프(사업장). 빈 값/NULL=회사공통
	private List<TbmEduItemInfoModel> tbmEduItemInfoModelList;
}
