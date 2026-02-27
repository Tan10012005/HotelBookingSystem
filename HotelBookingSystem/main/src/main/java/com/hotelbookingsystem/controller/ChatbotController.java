package com.hotelbookingsystem.controller;

import com.hotelbookingsystem.dto.ChatbotRequest;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.service.ChatbotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatbotRequest request, HttpSession session) {
        String message = request != null ? request.getMessage() : null;
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("reply", "Vui lòng nhập câu hỏi."));
        }

        if (message.length() > 2000) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("reply", "Câu hỏi quá dài, vui lòng rút gọn."));
        }

        User user = (User) session.getAttribute("user");
        try {
            String reply = chatbotService.ask(user, message.trim());
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("reply", "Có lỗi khi gọi AI: " + e.getMessage()));
        }
    }
}
