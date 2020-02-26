import java.util.TimerTask;

public class ClientTimeout extends TimerTask {
    private ChatServer.Handler clientHandler;

    public ClientTimeout(ChatServer.Handler clientHandler) {
        this.clientHandler = clientHandler;
    }

    @Override
    public void run() {
        System.out.println("Client timeout. Closing the socket...");
        this.clientHandler.stopClientTimeoutTask();
        this.clientHandler.closeSocket();
    }

}