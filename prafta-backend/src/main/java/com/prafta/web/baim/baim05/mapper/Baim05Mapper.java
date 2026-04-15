package com.prafta.web.baim.baim05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.baim.baim05.application.query.DailyUserSlotListQuery;
import com.prafta.web.baim.baim05.result.DailyUserSlotListResult;

@Mapper
public interface Baim05Mapper {
	List<DailyUserSlotListResult> selectDailyUserSlotList(DailyUserSlotListQuery query);
}
