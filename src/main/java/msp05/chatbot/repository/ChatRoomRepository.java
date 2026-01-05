package msp05.chatbot.repository;

import msp05.chatbot.domain.ChatRoom;
import msp05.chatbot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByUserOrderByLastModifiedAtDesc(User user);
}
