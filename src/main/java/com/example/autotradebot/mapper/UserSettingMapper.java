package com.example.autotradebot.mapper;

import com.example.autotradebot.dto.UserSettingDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface UserSettingMapper {
    int insertUserSetting(UserSettingDto userSetting);

    int updateUserSetting(UserSettingDto userSetting);

    int deleteUserSettingByIdx(int idx);

    UserSettingDto selectUserSettingByIdx(int idx);

    UserSettingDto selectUserSettingByEmailPk(String emailPk);

    int updateUserAmountSetting(@Param("emailPk") String emailPk, @Param("symbol") String symbol, @Param("amount") BigDecimal amount);

    List<UserSettingDto> selectAllUserSettingsBySymbol(String symbol);
}
