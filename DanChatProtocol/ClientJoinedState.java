public class ClientJoinedState implements ClientState {

    ChatClient chatClient;

    public ClientJoinedState(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void displayInstructions() {
        System.out.println("---------------Begin of Instructions--------------");
        System.out.println("You can start chatting now. After typing your message, press Enter to send.");
        System.out.println("To retrieve historical messages of this chatroom, enter $recovery");
        System.out.println("To leave the chatroom to create/join a different room, enter $leave");
        System.out.println("To close the client, enter $close");
        System.out.println("---------------End of Instructions--------------");
        this.chatClient.setDisplayInstructions(false);
    }

    @Override
    public void handleUserInput(String input) {

        String inputLowerCase = input.toLowerCase();
        if (inputLowerCase.startsWith("$leave")) {

            // Send a close frame to the server
            this.chatClient.sendMessage(true, Global.FRAME_TYPE.LEAVE, Global.ERROR_CODE.NONE, "", "");

        } else if (inputLowerCase.startsWith("$close")) {

            this.chatClient.initClosing();

        } else if (inputLowerCase.startsWith("$recovery")) {

            this.chatClient.sendMessage(true, Global.FRAME_TYPE.RECOVERY, Global.ERROR_CODE.NONE, "", "");
        } else if (inputLowerCase.startsWith("$search ")) {

            this.chatClient.sendMessage(true, Global.FRAME_TYPE.SEARCH, Global.ERROR_CODE.NONE, input.substring(8), "");

        } else {

            if (input.equals("")) {
                return;
            }

            String message = this.chatClient.getDisplayName() + ": " + input;
            // Send the message to the server
            this.chatClient.sendMessage(true, Global.FRAME_TYPE.TEXT, Global.ERROR_CODE.NONE, message, "");

        }

    }

    @Override
    public void handleDataFrame(DataFrame frame) {

        if (frame.getType() == Global.FRAME_TYPE.LEAVE.getValue()) {

            // Remove the room and display names
            this.chatClient.setRoomName("");
            this.chatClient.setDisplayName("");
            // Go back to Open state
            this.chatClient.setCurrentState(this.chatClient.getOpenState());

        } else if (frame.getType() == Global.FRAME_TYPE.TEXT.getValue()) {

            // Receive a text message, display it
            System.out.println(frame.getMainData());

        } else if (frame.getType() == Global.FRAME_TYPE.ACK.getValue()) {

            // An acknowledgement for the sent message, there is no action to be taken

        } else if (frame.getType() == Global.FRAME_TYPE.PONG.getValue()) {

            this.chatClient.handlePongFrame();

        } else {

            // This frame is not supported
            this.chatClient.sendErrorFrame(Global.ERROR_CODE.COMMAND_NOT_ALLOWED);

        }

    }

}