package msp05.chatbot.repository;

import msp05.chatbot.domain.ChatMessage;
import msp05.chatbot.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderBySentAtAsc(ChatRoom chatRoom);

    void deleteByChatRoom(ChatRoom chatRoom);
}
