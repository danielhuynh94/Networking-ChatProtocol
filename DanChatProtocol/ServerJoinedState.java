import java.util.ArrayList;

public class ServerJoinedState implements ServerState {
    protected ChatServer.Handler clientHandler;

    public ServerJoinedState(ChatServer.Handler clientHandler) {
        this.clientHandler = clientHandler;
    }

    @Override
    public void handleDataFrame(DataFrame dataFrame) {

        if (dataFrame.getType() == Global.FRAME_TYPE.LEAVE.getValue()) {

            // Notify all its chat mates
            this.clientHandler.sendMessageToAllChatmates(true, Global.FRAME_TYPE.TEXT, 0, Global.ERROR_CODE.NONE,
                    this.clientHandler.getDisplayName() + " left the room.", "");

            // Remove the client from the its current chatroom
            this.clientHandler.leaveCurrentRoom();

            // Send a LEAVE frame back to the client
            this.clientHandler.sendMessage(true, Global.FRAME_TYPE.LEAVE, Global.ERROR_CODE.NONE, "", "");

            // Change the state back to OPEN
            this.clientHandler.setCurrentState(this.clientHandler.getOpenState());

        } else if (dataFrame.getType() == Global.FRAME_TYPE.CLOSE.getValue()) {

            // Reply with a close frame
            this.clientHandler.sendMessage(true, Global.FRAME_TYPE.CLOSE, Global.ERROR_CODE.NONE, "", "");

            // Change state to closed
            this.clientHandler.setCurrentState(this.clientHandler.getClosedState());

            // Start closing the socket
            this.clientHandler.closeSocket();

        } else if (dataFrame.getType() == Global.FRAME_TYPE.TEXT.getValue()) {

            // Save the message to the chatroom's data
            ChatRoomData roomData = ChatServer.getChatrooms().get(this.clientHandler.getRoomName());

            // If there is no room data, return
            if (roomData == null) {
                return;
            }

            // Record the message
            roomData.addMessage(dataFrame);

            // Send an ACK message back to the sender
            this.clientHandler.sendMessage(true, Global.FRAME_TYPE.ACK, Global.ERROR_CODE.NONE, "", "");

            // Broadcast the message to all the chatroom's participants
            this.clientHandler.sendMessageToAllChatmates(dataFrame.getEnd(), Global.FRAME_TYPE.TEXT, 0,
                    Global.ERROR_CODE.NONE, dataFrame.getMainData(), "");

        } else if (dataFrame.getType() == Global.FRAME_TYPE.RECOVERY.getValue()) {

            ChatRoomData chatRoomData = ChatServer.getChatrooms().get(this.clientHandler.getRoomName());
            // If there is no data, return
            if (chatRoomData == null) {
                return;
            }

            // Get all the messages of the chatroom and send them to the client
            ArrayList<DataFrame> messages = chatRoomData.getMessages();
            DataFrame message = null;
            for (int i = 0; i < messages.size(); i++) {
                message = messages.get(i);
                // Mark the last message as end
                this.clientHandler.sendMessage(i == messages.size() - 1, Global.FRAME_TYPE.TEXT, Global.ERROR_CODE.NONE,
                        message.getMainData(), "");
            }

        } else if (dataFrame.getType() == Global.FRAME_TYPE.SEARCH.getValue()) {

            String searchString = dataFrame.getMainData();

            ChatRoomData chatRoomData = ChatServer.getChatrooms().get(this.clientHandler.getRoomName());
            // If there is no data, return
            if (chatRoomData == null) {
                return;
            }

            // Get all the messages of the chatroom and send them to the client
            ArrayList<DataFrame> allMessages = chatRoomData.getMessages();
            ArrayList<DataFrame> filteredMessages = new ArrayList<>();
            DataFrame message = null;

            // Filter the message by the search string
            for (int i = 0; i < allMessages.size(); i++) {
                message = allMessages.get(i);
                if (message.getMainData().contains(searchString)) {
                    filteredMessages.add(message);
                }
            }

            // Send the filtered messages
            for (int i = 0; i < filteredMessages.size(); i++) {
                message = filteredMessages.get(i);
                // Mark the last message as end
                this.clientHandler.sendMessage(i == filteredMessages.size() - 1, Global.FRAME_TYPE.TEXT,
                        Global.ERROR_CODE.NONE, message.getMainData(), "");
            }

        } else if (dataFrame.getType() == Global.FRAME_TYPE.PING.getValue()) {

            this.clientHandler.handlePingFrame();

        } else {

            // This command is not allowed
            this.clientHandler.sendErrorFrame(Global.ERROR_CODE.COMMAND_NOT_ALLOWED);

            // Close the connection immediately
            this.clientHandler.closeSocket();

        }

    }

}