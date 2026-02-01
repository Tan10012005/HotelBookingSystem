package com.hotelbookingsystem.service;

import tools.jackson.databind.JsonNode;
import com.hotelbookingsystem.entity.Booking;
import com.hotelbookingsystem.entity.Room;
import com.hotelbookingsystem.entity.RoomStatus;
import com.hotelbookingsystem.entity.User;
import com.hotelbookingsystem.repository.BookingRepository;
import com.hotelbookingsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${openrouter.model:openai/gpt-4o-mini}")
    private String model;

    @Value("${openrouter.site-url:http://localhost:8080}")
    private String siteUrl;

    @Value("${openrouter.app-name:HotelBookingSystem}")
    private String appName;

    public ChatbotService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    public String ask(User user, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Bạn chưa cấu hình OpenRouter API key. Vui lòng đặt openrouter.api.key trong application.properties.";
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(user)));
        messages.add(Map.of("role", "user", "content", message));
        payload.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        if (siteUrl != null && !siteUrl.isBlank()) {
            headers.add("HTTP-Referer", siteUrl);
        }
        if (appName != null && !appName.isBlank()) {
            headers.add("X-Title", appName);
        }

        String url = baseUrl;
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = url + "/chat/completions";

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.postForEntity(url, entity, JsonNode.class);
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.error("OpenRouter error: status={} body={}", e.getStatusCode(), body);
            throw new RuntimeException("OpenRouter error: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("OpenRouter call failed", e);
            throw new RuntimeException("OpenRouter call failed");
        }

        JsonNode body = response.getBody();
        if (body == null) {
            return "Không nhận được phản hồi từ AI.";
        }

        JsonNode contentNode = body.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode()) {
            return "AI không trả lời hợp lệ.";
        }
        return contentNode.asText();
    }

    private String buildSystemPrompt(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý đặt phòng khách sạn cho Hotel Booking System.\n");
        sb.append("Luôn trả lời tiếng Việt, lịch sự, ngắn gọn.\n");
        sb.append("Chỉ dùng dữ liệu trong CONTEXT; nếu thiếu hãy nói 'chưa có dữ liệu'.\n");
        sb.append("Không tự ý xác nhận đặt phòng hay thanh toán; hãy hướng dẫn người dùng thao tác trên website.\n");
        sb.append("CONTEXT:\n");

        List<Room> availableRooms = roomRepository.findByStatus(RoomStatus.AVAILABLE);
        if (availableRooms.isEmpty()) {
            sb.append("Available rooms: none.\n");
        } else {
            sb.append("Available rooms:\n");
            int count = 0;
            for (Room room : availableRooms) {
                if (count >= 6) break;
                String type = room.getRoomType() != null ? room.getRoomType().getName() : "STANDARD";
                String capacity = room.getRoomType() != null ? String.valueOf(room.getRoomType().getCapacity()) : "2";
                sb.append("- Room ").append(room.getRoomNumber())
                        .append(" | type=").append(type)
                        .append(" | capacity=").append(capacity)
                        .append(" | pricePerNight=").append(room.getPricePerNight())
                        .append("\n");
                count++;
            }
        }

        if (user == null) {
            sb.append("User: not logged in.\n");
            return sb.toString();
        }

        sb.append("User: ").append(user.getEmail());
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            sb.append(" (").append(user.getFullName()).append(")");
        }
        sb.append("\n");

        List<Booking> bookings = bookingRepository.findByUserId(user.getId());
        if (bookings.isEmpty()) {
            sb.append("User bookings: none.\n");
        } else {
            sb.append("User bookings:\n");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            int count = 0;
            for (Booking b : bookings) {
                if (count >= 5) break;
                String roomNumber = b.getRoom() != null ? b.getRoom().getRoomNumber() : "N/A";
                sb.append("- Booking #").append(b.getId())
                        .append(" | room=").append(roomNumber)
                        .append(" | checkIn=").append(b.getCheckIn() != null ? b.getCheckIn().format(fmt) : "N/A")
                        .append(" | checkOut=").append(b.getCheckOut() != null ? b.getCheckOut().format(fmt) : "N/A")
                        .append(" | status=").append(b.getStatus())
                        .append(" | refund=").append(b.getRefundStatus())
                        .append("\n");
                count++;
            }
        }

        return sb.toString();
    }
}
