public class ServerClosingState implements ServerState {

    protected ChatServer.Handler clientHandler;

    public ServerClosingState(ChatServer.Handler clientHandler) {
        this.clientHandler = clientHandler;
    }

    @Override
    public void handleDataFrame(DataFrame frame) {
        // In closing state, cannot process any message
    }

}