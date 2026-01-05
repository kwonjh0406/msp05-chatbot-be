package msp05.chatbot.service;

import msp05.chatbot.domain.ChatMessage;
import msp05.chatbot.domain.ChatRoom;
import msp05.chatbot.domain.User;
import msp05.chatbot.dto.AiChatRequest;
import msp05.chatbot.dto.AiChatResponse;
import msp05.chatbot.repository.ChatMessageRepository;
import msp05.chatbot.repository.ChatRoomRepository;
import msp05.chatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    public ChatService(ChatRoomRepository chatRoomRepository, ChatMessageRepository chatMessageRepository,
            UserRepository userRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    public List<ChatRoom> getChatRooms(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return chatRoomRepository.findByUserOrderByLastModifiedAtDesc(user);
    }

    @Transactional
    public ChatRoom createChatRoom(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatRoom chatRoom = new ChatRoom(user, "New Chat");
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void deleteChatRoom(Long roomId, String username) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!chatRoom.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to this chat room");
        }

        // 채팅방 삭제 (Cascade 설정으로 인해 메시지도 함께 삭제됨)
        chatRoomRepository.delete(chatRoom);
    }

    public List<ChatMessage> getChatMessages(Long roomId, String username) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!chatRoom.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to this chat room");
        }

        return chatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom);
    }

    @Transactional
    public ChatMessage sendMessage(Long roomId, String username, String content) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!chatRoom.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to this chat room");
        }

        // 1. Save User Message
        ChatMessage userMessage = new ChatMessage(chatRoom, ChatMessage.SenderType.USER, content);
        chatMessageRepository.save(userMessage);

        // 2. Update Room Title (Last Chat) and Time
        String title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
        chatRoom.setTitle(title);
        chatRoom.setLastModifiedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // 3. Call FastAPI AI Service
        String aiResponseContent = callAiService(content, roomId);
        ChatMessage aiMessage = new ChatMessage(chatRoom, ChatMessage.SenderType.AI, aiResponseContent);

        return chatMessageRepository.save(aiMessage);
    }

    private String callAiService(String message, Long roomId) {
        try {
            AiChatRequest request = new AiChatRequest(message, roomId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiChatRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<AiChatResponse> response = restTemplate.postForEntity(aiServiceUrl, requestEntity,
                    AiChatResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getResponse();
            } else {
                logger.error("AI Service returned status: {}", response.getStatusCode());
                return "AI 응답 오류: " + response.getStatusCode();
            }

        } catch (Exception e) {
            logger.error("Failed to call AI service", e);
            return "죄송합니다. AI 서비스 연결에 실패했습니다.";
        }
    }
}
