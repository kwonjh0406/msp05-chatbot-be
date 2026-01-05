package msp05.chatbot.controller;

import msp05.chatbot.domain.ChatMessage;
import msp05.chatbot.domain.ChatRoom;
import msp05.chatbot.dto.ChatMessageDto;
import msp05.chatbot.dto.ChatRoomDto;
import msp05.chatbot.dto.MessageRequest;
import msp05.chatbot.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Get all chat rooms for the current user
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getRooms(Authentication authentication) {
        List<ChatRoom> rooms = chatService.getChatRooms(authentication.getName());
        List<ChatRoomDto> dtos = rooms.stream()
                .map(r -> new ChatRoomDto(r.getId(), r.getTitle())) // Simple DTO to avoid circular ref / lazy load
                                                                    // issues in JSON
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // Create a new chat room
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDto> createRoom(Authentication authentication) {
        ChatRoom room = chatService.createChatRoom(authentication.getName());
        return ResponseEntity.ok(new ChatRoomDto(room.getId(), room.getTitle()));
    }

    // Get messages for a room
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(@PathVariable Long roomId, Authentication authentication) {
        List<ChatMessage> messages = chatService.getChatMessages(roomId, authentication.getName());
        List<ChatMessageDto> dtos = messages.stream()
                .map(m -> new ChatMessageDto(m.getSender().name(), m.getContent()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // Send a message
    @PostMapping("/rooms/{roomId}/send")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Long roomId,
            @RequestBody MessageRequest request,
            Authentication authentication) {

        ChatMessage aiMessage = chatService.sendMessage(roomId, authentication.getName(), request.getContent());
        return ResponseEntity.ok(new ChatMessageDto(aiMessage.getSender().name(), aiMessage.getContent()));
    }

    // Delete a chat room
    // Deletes the room and cascades to messages
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId, Authentication authentication) {
        chatService.deleteChatRoom(roomId, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
