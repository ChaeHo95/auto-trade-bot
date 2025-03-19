package com.example.autotradebot.mapper;

import com.example.autotradebot.dto.UserPositionHistoryDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserPositionHistoryMapper {
    int insertUserPositionHistory(UserPositionHistoryDto history);

    int updateUserPositionHistory(UserPositionHistoryDto history);

    int deleteUserPositionHistoryById(long id);

    UserPositionHistoryDto selectUserPositionHistoryById(long id);

    List<UserPositionHistoryDto> selectUserPositionHistoriesByEmailPk(String emailPk);

    List<UserPositionHistoryDto> selectAllUserPositionHistories();
}
