package com.project.sneakermarket.commerce.chatbot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;


    @PostMapping("/api/chatbot/ask")
    public ChatResponse ask(@Valid @RequestBody ChatRequest request) {
        String answer = chatbotService.ask(request.getQuestion());
        return new ChatResponse(answer);
    }
}
