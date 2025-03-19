package com.example.autotradebot.mapper;

import com.example.autotradebot.dto.UserSettingDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserSettingMapper {
    int insertUserSetting(UserSettingDto userSetting);

    int updateUserSetting(UserSettingDto userSetting);

    int deleteUserSettingByIdx(int idx);

    UserSettingDto selectUserSettingByIdx(int idx);

    UserSettingDto selectUserSettingByEmailPk(String emailPk);

    List<UserSettingDto> selectAllUserSettings();
}
