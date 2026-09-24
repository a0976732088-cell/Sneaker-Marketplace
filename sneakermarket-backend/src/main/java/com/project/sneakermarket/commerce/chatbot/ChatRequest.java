package com.project.sneakermarket.commerce.chatbot;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {

    @NotBlank
    private String question;
}
