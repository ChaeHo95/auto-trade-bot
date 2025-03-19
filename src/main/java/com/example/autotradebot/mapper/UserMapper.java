package com.example.autotradebot.mapper;

import com.example.autotradebot.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    int insertUser(UserDto user);

    int updateUser(UserDto user);

    int deleteUserByIdx(int idx);

    UserDto selectUserByIdx(int idx);

    UserDto selectUserByEmailPk(String emailPk);

    List<UserDto> selectAllUsers();
}
