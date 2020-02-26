import java.util.TimerTask;

public class ServerTimeout extends TimerTask {
    private ChatClient chatClient;

    public ServerTimeout(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        System.out.println("Timed out. Closing the socket...");
        this.chatClient.closeSocket();
    }

}