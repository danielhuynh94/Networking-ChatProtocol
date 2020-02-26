public class ClientClosedState implements ClientState {

    ChatClient chatClient;

    public ClientClosedState(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void displayInstructions() {
        // Closed, there is no instruction to display
    }

    @Override
    public void handleUserInput(String input) {
        // Closed, so user input is ignored
    }

    @Override
    public void handleDataFrame(DataFrame frame) {
        if (frame.getType() == Global.FRAME_TYPE.OPEN.getValue()) {

            // Change to open state
            this.chatClient.setCurrentState(this.chatClient.getOpenState());

        } else {

            // Reply with an error frame
            this.chatClient.sendErrorFrame(Global.ERROR_CODE.COMMAND_NOT_ALLOWED);
            // Close the socket immediately
            this.chatClient.closeSocket();

        }

    }

}