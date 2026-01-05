package msp05.chatbot.dto;

public class AiChatRequest {
    private String message;
    private Long room_id;

    public AiChatRequest(String message, Long room_id) {
        this.message = message;
        this.room_id = room_id;
    }

    public String getMessage() {
        return message;
    }

    public Long getRoom_id() {
        return room_id;
    }
}
