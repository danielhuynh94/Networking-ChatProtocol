import java.util.Hashtable;

public class ServerOpenState implements ServerState {
    protected ChatServer.Handler clientHandler;

    public ServerOpenState(ChatServer.Handler clientHandler) {
        this.clientHandler = clientHandler;
    }

    @Override
    public void handleDataFrame(DataFrame dataFrame) {

        if (dataFrame.getType() == Global.FRAME_TYPE.NEW.getValue()
                || dataFrame.getType() == Global.FRAME_TYPE.JOIN.getValue()) {

            String mainData = dataFrame.getMainData();
            int semicolonIndex = mainData.indexOf(";");
            String roomName = mainData.substring(0, semicolonIndex);
            String displayName = mainData.substring(semicolonIndex + 1);
            Global.ERROR_CODE error;
            Global.FRAME_TYPE responseFrameType;

            // Validate room name and display name
            if (!Global.isAlphanumeric(roomName) || !Global.isAlphanumeric(displayName)) {
                this.clientHandler.sendErrorFrame(Global.ERROR_CODE.INVALID_DATA_CONTENT);
            }

            if (dataFrame.getType() == Global.FRAME_TYPE.NEW.getValue()) {
                // Create a new room
                error = ChatServer.createChatRoom(roomName);
                if (error.getValue() != Global.ERROR_CODE.NONE.getValue()) {
                    this.clientHandler.sendErrorFrame(error);
                    return;
                }
                responseFrameType = Global.FRAME_TYPE.NEW;
            } else {
                responseFrameType = Global.FRAME_TYPE.JOIN;
            }

            // Add the user to the room
            error = ChatServer.addChatRoomParticipant(roomName, displayName, this.clientHandler.getOutputStream());
            if (error.getValue() != Global.ERROR_CODE.NONE.getValue()) {
                this.clientHandler.sendErrorFrame(error);
                return;
            }

            // Reply
            this.clientHandler.sendMessage(true, responseFrameType, Global.ERROR_CODE.NONE, mainData, "");

            // Change state to JOINED
            this.clientHandler.setRoomName(roomName);
            this.clientHandler.setDisplayName(displayName);
            this.clientHandler.setCurrentState(this.clientHandler.getJoinedState());

        } else if (dataFrame.getType() == Global.FRAME_TYPE.ROOMLIST.getValue()) {

            String roomList = "";

            // Get a list of all rooms
            Hashtable<String, ChatRoomData> chatRooms = ChatServer.getChatrooms();

            for (String key : chatRooms.keySet()) {
                if (!roomList.equals("")) {
                    roomList = roomList.concat("; ");
                }
                roomList = roomList.concat(key);
            }

            // Reply with a ROOMLIST frame
            this.clientHandler.sendMessage(true, Global.FRAME_TYPE.ROOMLIST, Global.ERROR_CODE.NONE, roomList, "");

        } else if (dataFrame.getType() == Global.FRAME_TYPE.PING.getValue()) {

            this.clientHandler.handlePingFrame();

        } else {

            // Reply with an error frame
            this.clientHandler.sendErrorFrame(Global.ERROR_CODE.COMMAND_NOT_ALLOWED);

            // Close the socket immediately
            this.clientHandler.closeSocket();

        }

    }

}