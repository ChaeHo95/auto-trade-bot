package com.example.autotradebot.mapper.analysis;

import com.example.autotradebot.dto.analysis.AiAnalysisFinalHistoryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiAnalysisFinalHistoryMapper {

    // ✅ AI 분석 결과 저장
    void insertAiAnalysisFinalHistory(AiAnalysisFinalHistoryDTO aiAnalysisFinalHistory);

    // ✅ 특정 심볼의 최신 AI 분석 결과 조회
    AiAnalysisFinalHistoryDTO getLatestAiAnalysisFinalHistory(@Param("symbol") String symbol);

    // ✅ 특정 심볼의 전체 AI 분석 결과 조회
    List<AiAnalysisFinalHistoryDTO> getAllAiAnalysisFinalHistory(@Param("symbol") String symbol);

    // ✅ 특정 심볼의 AI 분석 데이터 삭제
    void deleteAiAnalysisFinalHistory(@Param("symbol") String symbol);
}
