package com.example.autotradebot.dto.bot;

import lombok.Data;

import java.util.List;

@Data
public class OpenAiResponseDTO {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    private String serviceTier;
    private String systemFingerprint;

    // getters and setters
    @Data
    public static class Choice {
        private int index;
        private Message message;
        private String refusal;

        // getters and setters
    }

    @Data
    public static class Message {
        private String role;
        private String content;

        // getters and setters
    }

    @Data
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;

        // getters and setters
    }
}
