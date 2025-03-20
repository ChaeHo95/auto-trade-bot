package com.example.autotradebot.mapper;

import com.example.autotradebot.dto.UserTradeProcessDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTradeProcessMapper {

    int insertUserTradeProcess(UserTradeProcessDto dto);

    int updateUserTradeProcess(@Param("id") Integer id, @Param("isProcess") Integer isProcess);

    int deleteUserTradeProcessById(int id);

    UserTradeProcessDto selectUserTradeProcessByEmailPkWithSymbol(@Param("emailPk") String emailPk, @Param("symbol") String symbol);

    List<UserTradeProcessDto> selectAllUserTradeProcesses(@Param("symbol") String symbol);
}
