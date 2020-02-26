import java.util.TimerTask;

public class SendPing extends TimerTask {

    private ChatClient chatClient;

    public SendPing(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        this.chatClient.sendMessage(true, Global.FRAME_TYPE.PING, Global.ERROR_CODE.NONE, "", "");
    }

}
