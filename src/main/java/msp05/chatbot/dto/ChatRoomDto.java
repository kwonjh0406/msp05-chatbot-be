package msp05.chatbot.dto;

public class ChatRoomDto {
    private Long id;
    private String title;

    public ChatRoomDto(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
