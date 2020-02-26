public class ClientOpenState implements ClientState {

    ChatClient chatClient;

    public ClientOpenState(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void displayInstructions() {
        System.out.println("---------------Begin of Instructions--------------");
        System.out
                .println("Room name and display name are case-sensitive and must include only alphanumeric character.");
        System.out.println(
                "If you want to create a new room, enter $newroom <room name>;<display name>. Example: $newroom danchat1;dan");
        System.out.println("If you want to get a list of existing rooms, enter $listroom ");
        System.out.println(
                "If you want to join an existing room, enter $joinroom <room name>;<display name>. Example: $joinroom danchat1;dan");
        System.out.println("If you want to exit the chat program, enter $close");
        System.out.println("---------------End of Instructions--------------");
        this.chatClient.setDisplayInstructions(false);
    }

    @Override
    public void handleUserInput(String input) {
        String roomName = "";
        String displayName = "";

        if (input.toLowerCase().startsWith("$newroom ")) {

            // Validate the input
            int indexOfSemiColon = input.indexOf(";");
            if (indexOfSemiColon < 0) {
                this.chatClient.setDisplayInstructions(true);
                return;
            }

            roomName = input.substring(9, input.indexOf(";"));
            displayName = input.substring(input.indexOf(";") + 1);

            // If both room name and display name are alphanumeric, send the message to the
            // server
            if (Global.isAlphanumeric(roomName) && Global.isAlphanumeric(displayName)) {
                this.chatClient.sendMessage(true, Global.FRAME_TYPE.NEW, Global.ERROR_CODE.NONE, input.substring(9),
                        "");
            } else {
                // Display the instructions again
                this.chatClient.setDisplayInstructions(true);
            }

            // System.out.println("New room request" + roomName + " " + displayName);

        } else if (input.toLowerCase().startsWith("$listroom")) {

            // Send the message to the server
            this.chatClient.sendMessage(true, Global.FRAME_TYPE.ROOMLIST, Global.ERROR_CODE.NONE, "", "");

        } else if (input.toLowerCase().startsWith("$joinroom ")) {

            // Validate the input
            int indexOfSemiColon = input.indexOf(";");
            if (indexOfSemiColon < 0) {
                this.chatClient.setDisplayInstructions(true);
                return;
            }

            roomName = input.substring(10, input.indexOf(";"));
            displayName = input.substring(input.indexOf(";") + 1);

            // If both room name and display name are alphanumeric, send the message to the
            // server
            if (Global.isAlphanumeric(roomName) && Global.isAlphanumeric(displayName)) {
                this.chatClient.sendMessage(true, Global.FRAME_TYPE.JOIN, Global.ERROR_CODE.NONE, input.substring(10),
                        "");
            } else {
                this.chatClient.setDisplayInstructions(true);
            }

        } else if (input.toLowerCase().startsWith("$close")) {

            this.chatClient.initClosing();

        } else {

            // Display the instruction again
            this.chatClient.setDisplayInstructions(true);

        }
    }

    @Override
    public void handleDataFrame(DataFrame frame) {

        if (frame.getType() == Global.FRAME_TYPE.NEW.getValue()
                || frame.getType() == Global.FRAME_TYPE.JOIN.getValue()) {

            String mainData = frame.getMainData();
            int semicolonIndex = mainData.indexOf(";");

            // Create and join a new chatroom successfully.
            String roomName = mainData.substring(0, semicolonIndex);
            String displayName = mainData.substring(semicolonIndex + 1);

            // Validate the roomname and display name
            if (!Global.isAlphanumeric(roomName)) {
                System.out.println("Room name cannot be set because it does not contain only alphanumeric letters.");
                this.chatClient.sendErrorFrame(Global.ERROR_CODE.INVALID_ROOM_NAME);
                this.chatClient.closeSocket();
                return;
            }

            if (!Global.isAlphanumeric(displayName)) {
                System.out.println("Display name cannot be set because it does not contain only alphanumeric letters.");
                this.chatClient.sendErrorFrame(Global.ERROR_CODE.INVALID_DISPLAY_NAME);
                this.chatClient.closeSocket();
                return;
            }

            // Set the room name and display name
            this.chatClient.setRoomName(roomName);
            this.chatClient.setDisplayName(displayName);

            // Set the state to JOINED
            this.chatClient.setCurrentState(this.chatClient.getJoinedState());

        } else if (frame.getType() == Global.FRAME_TYPE.ROOMLIST.getValue()) {

            // Print out all the room name
            System.out.println(frame.getMainData());

        } else if (frame.getType() == Global.FRAME_TYPE.PONG.getValue()) {

            this.chatClient.handlePongFrame();

        } else {

            // Reply with an error frame
            this.chatClient.sendErrorFrame(Global.ERROR_CODE.COMMAND_NOT_ALLOWED);
            // Close the socket immediately
            this.chatClient.closeSocket();

        }

    }

}