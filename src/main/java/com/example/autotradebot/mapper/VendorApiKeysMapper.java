package com.example.autotradebot.mapper;

import com.example.autotradebot.dto.VendorApiKeyDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VendorApiKeysMapper {
    int insertVendorApiKey(VendorApiKeyDto vendorApiKey);

    int updateVendorApiKey(VendorApiKeyDto vendorApiKey);

    int deleteVendorApiKeyById(long id);

    VendorApiKeyDto selectVendorApiKeyById(long id);

    VendorApiKeyDto selectVendorApiKeyByEmailPk(String emailPk);

    List<VendorApiKeyDto> selectAllVendorApiKeys();
}
