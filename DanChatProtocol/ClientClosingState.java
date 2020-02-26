public class ClientClosingState implements ClientState {

    ChatClient chatClient;

    public ClientClosingState(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void handleUserInput(String input) {
        System.out.println("The program is closing. No input is processed.");
    }

    @Override
    public void handleDataFrame(DataFrame frame) {

        // Received CLOSE response from server.
        if (frame.getType() == Global.FRAME_TYPE.CLOSE.getValue()) {
            System.out.println("Received a CLOSE response from server. The program is closed.");
        }

        this.chatClient.stopSendPingTask();

        this.chatClient.stopServerTimeoutTask();

        // Expects a close frame from the server, whatever the server responds, close
        // the socket anyway
        this.chatClient.closeSocket();
    }

    @Override
    public void displayInstructions() {

    }

}