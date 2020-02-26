import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.Executors;

public class ChatServer {

    // This secret is used to verify whether the client can use this server's
    // service
    public static final String secret = "DanChatSecret";

    // The protocol version of the server
    public static final int serverVersion = 1;

    // A hashtable used to keep track of the chatrooms and each chatroom's
    // data including participants and historical messages
    private static Hashtable<String, ChatRoomData> chatRooms = new Hashtable<>();

    public static Hashtable<String, ChatRoomData> getChatrooms() {
        return ChatServer.chatRooms;
    }

    /**
     * Create a new chatroom on the server
     * 
     * @param chatRoomName
     * @return
     */
    public static synchronized Global.ERROR_CODE createChatRoom(String chatRoomName) {

        // Check whether the room name is taken
        if (chatRooms.containsKey(chatRoomName)) {
            return Global.ERROR_CODE.INVALID_ROOM_NAME;
        }

        // Add a new room
        ChatRoomData data = new ChatRoomData();
        chatRooms.put(chatRoomName, data);

        return Global.ERROR_CODE.NONE;
    }

    /**
     * Add a participant to a chatroom
     * 
     * @param chatRoomName
     * @param displayName
     * @param out
     * @return
     */
    public static synchronized Global.ERROR_CODE addChatRoomParticipant(String chatRoomName, String displayName,
            DataOutputStream out) {

        // Check whether the chat room exists
        if (!ChatServer.chatRooms.containsKey(chatRoomName)) {
            return Global.ERROR_CODE.INVALID_ROOM_NAME;
        }

        // Check whether the display name is unique
        ChatRoomData roomData = ChatServer.chatRooms.get(chatRoomName);
        if (roomData.getParticipants().containsKey(displayName)) {
            return Global.ERROR_CODE.INVALID_DISPLAY_NAME;
        }

        // Join the room
        roomData.addParticipant(displayName, out);

        return Global.ERROR_CODE.NONE;
    }

    /**
     * The client handler task.
     */
    public static class Handler implements Runnable {

        // Flag for whether the receiving message loop should run
        private boolean running = true;

        // Socket object
        private Socket socket;

        // Input stream
        private DataInputStream in;

        // Output stream
        private DataOutputStream out;

        public DataOutputStream getOutputStream() {
            return this.out;
        }

        // The client's room name
        private String roomName;

        public String getRoomName() {
            return this.roomName;
        }

        public void setRoomName(String name) {
            this.roomName = name;
        }

        // The client's display name
        private String displayName;

        public String getDisplayName() {
            return this.displayName;
        }

        public void setDisplayName(String name) {
            this.displayName = name;
        }

        private ServerState closedState;

        public ServerState getClosedState() {
            return this.closedState;
        }

        private ServerState openState;

        public ServerState getOpenState() {
            return this.openState;
        }

        private ServerState joinedState;

        public ServerState getJoinedState() {
            return this.joinedState;
        }

        private ServerState currentState;

        public ServerState getCurrentState() {
            return this.currentState;
        }

        private ServerState closingState;

        public ServerState getClosingState() {
            return this.closingState;
        }

        private Timer timeoutTimer;

        private TimerTask clientTimeoutTask;

        public void stopClientTimeoutTask() {
            if (this.clientTimeoutTask != null) {
                this.clientTimeoutTask.cancel();
            }
        }

        public void setClientTimeoutTask() {
            this.clientTimeoutTask = new ClientTimeout(this);
            this.timeoutTimer.schedule(this.clientTimeoutTask, 10000);
        }

        public void setCurrentState(ServerState state) {
            this.currentState = state;

            this.stopClientTimeoutTask();

            String str = "";
            if (state.equals(this.closedState)) {
                str = "CLOSED";
            } else if (state.equals(this.openState)) {
                str = "OPEN";
                // Close the socket if there is no ping from the client
                this.setClientTimeoutTask();
            } else if (state.equals(this.joinedState)) {
                str = "JOINED";
                // Close the socket if there is no ping from the client
                this.setClientTimeoutTask();
            } else if (state.equals(this.closingState)) {
                str = "CLOSING";
            }

            System.out
                    .println("Client is in " + str + " state. Room: " + this.roomName + " - Name: " + this.displayName);

        }

        /**
         * Constructs a handler thread, squirreling away the socket. All the interesting
         * work is done in the run method. Remember the constructor is called from the
         * server's main method, so this has to be as short as possible.
         */
        public Handler(Socket socket) {
            this.socket = socket;
            this.closedState = new ServerClosedState(this);
            this.openState = new ServerOpenState(this);
            this.joinedState = new ServerJoinedState(this);
            this.closingState = new ServerClosingState(this);
            // On startup, the current state is closed
            this.currentState = this.closedState;
            this.timeoutTimer = new Timer();
        }

        /**
         * Services this thread's client by repeatedly requesting a screen name until a
         * unique one has been submitted, then acknowledges the name and registers the
         * output stream for the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
                this.in = new DataInputStream(socket.getInputStream());
                this.out = new DataOutputStream(socket.getOutputStream());

                // Accept messages from this client and broadcast them.
                while (this.running) {
                    if (in.available() > 0) {
                        int messageLength = in.readInt();
                        // Create a byte array
                        byte[] message = new byte[messageLength];
                        // Read the byte stream
                        in.readFully(message, 0, messageLength);
                        // Parse the byte array into a frame
                        DataFrame frame = new DataFrame();
                        boolean success = frame.parseFromByteArray(message);
                        // The frame is received successfully, process the frame
                        if (success) {

                            // For any error frame, display the error message and close the socket
                            // immediately
                            if (frame.getType() == Global.FRAME_TYPE.ERROR.getValue()) {
                                this.handleErrorFrame(frame);
                                continue;
                            }

                            // Handle the close frames
                            if (frame.getType() == Global.FRAME_TYPE.CLOSE.getValue()) {
                                this.handleCloseFrame(frame);
                                continue;
                            }

                            // Handle the frame depending on the protocol's current state
                            this.currentState.handleDataFrame(frame);

                        } else {
                            // Send an error frame saying the received frame could not be parsed
                            Global.sendErrorFrame(out, Global.ERROR_CODE.CORRUPT_FRAME);

                            // Received a corrupt frame, close the socket immediately
                            this.closeSocket();
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println(e);
            } finally {
                this.closeSocket();
            }
        }

        public void leaveCurrentRoom() {
            // Get room data
            ChatRoomData roomData = ChatServer.chatRooms.get(this.roomName);
            // If there is no room data, return
            if (roomData == null) {
                return;
            }
            // Remove the participant from the room
            roomData.removeParticipant(this.displayName);
            this.roomName = "";
            this.displayName = "";
        }

        public void sendMessageToAllChatmates(boolean end, Global.FRAME_TYPE type, int version,
                Global.ERROR_CODE errorCode, String mainData, String extendedData) {

            // Get all chatmates
            ArrayList<DataOutputStream> outputs = ChatServer.chatRooms.get(this.roomName)
                    .getChatmateOutputStreams(this.displayName);

            // Send the message
            for (DataOutputStream outStream : outputs) {
                Global.sendMessage(outStream, end, version, type, errorCode, mainData, extendedData);
            }

        }

        public void sendMessage(boolean end, Global.FRAME_TYPE type, Global.ERROR_CODE errorCode, String mainData,
                String extendedData) {
            Global.sendMessage(this.out, end, ChatServer.serverVersion, type, errorCode, mainData, extendedData);
        }

        public void sendErrorFrame(Global.ERROR_CODE errorCode) {
            Global.sendErrorFrame(out, errorCode);
        }

        public void handleErrorFrame(DataFrame frame) {
            // Display the appropriate error message
            Global.displayErrorMessage(Global.ERROR_CODE.values()[frame.getErrorCode()]);
            // Close the socket
            this.closeSocket();
        }

        public void handleCloseFrame(DataFrame frame) {

            // If we receive a response, close the socket
            if (this.currentState.equals(this.closingState)) {
                this.closeSocket();
                return;
            }

            // Send a response back
            this.sendMessage(true, Global.FRAME_TYPE.CLOSE, Global.ERROR_CODE.NONE, "", "");

            // Close
            this.setCurrentState(this.closedState);

            // Stop the timeout task
            this.stopClientTimeoutTask();

            // Close the socket immediately
            this.closeSocket();

        }

        public void handlePingFrame() {
            // Reply a pong back to the client
            this.sendMessage(true, Global.FRAME_TYPE.PONG, Global.ERROR_CODE.NONE, "", "");
            // Reset the current timeout timer
            this.stopClientTimeoutTask();
            this.setClientTimeoutTask();
            // System.out.println("Reset the timeout timer...");
        }

        public void closeSocket() {
            try {
                this.running = false;
                this.timeoutTimer.cancel();
                this.in.close();
                this.out.close();
                this.socket.close();
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }

    }

    public static void main(String[] args) throws Exception {

        System.out.println("The chat server is running...");
        var pool = Executors.newFixedThreadPool(500);
        try (var listener = new ServerSocket(59001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }

    }

}
