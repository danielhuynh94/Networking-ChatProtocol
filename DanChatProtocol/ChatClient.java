import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple Swing-based client for the chat server. Graphically it is a frame
 * with a text field for entering messages and a textarea to see the whole
 * dialog.
 *
 * The client follows the following Chat Protocol. When the server sends
 * "SUBMITNAME" the client replies with the desired screen name. The server will
 * keep sending "SUBMITNAME" requests as long as the client submits screen names
 * that are already in use. When the server sends a line beginning with
 * "NAMEACCEPTED" the client is now allowed to start sending the server
 * arbitrary strings to be broadcast to all chatters connected to the server.
 * When the server sends a line beginning with "MESSAGE" then all characters
 * following this string should be displayed in its message area.
 */
public class ChatClient {

    private final String secret = "DanChatSecret";
    private final int protocolVersion = 1;

    String serverAddress;
    DataInputStream in;
    DataOutputStream out;
    boolean running = true;

    Socket socket;

    private ClientState closedState;

    public ClientState getClosedState() {
        return this.closedState;
    }

    private ClientState openState;

    public ClientState getOpenState() {
        return this.openState;
    }

    private ClientState joinedState;

    public ClientState getJoinedState() {
        return this.joinedState;
    }

    private ClientState closingState;

    public ClientState getClosingState() {
        return this.closingState;
    }

    private ClientState currentState;

    public ClientState getCurrentState() {
        return this.currentState;
    }

    public void setCurrentState(ClientState state) {
        this.currentState = state;
        this.displayInstructions = true;

        this.stopSendPingTask();
        this.stopServerTimeoutTask();

        String str = "";
        if (state.equals(this.closedState)) {
            str = "CLOSED";
        } else if (state.equals(this.openState)) {
            str = "OPEN";
            // Send a heartbeat to the server every 3 seconds
            this.setSendPingTask();
        } else if (state.equals(this.joinedState)) {
            str = "JOINED";
            // Send a heartbeat to the server every 3 seconds
            this.setSendPingTask();
        } else if (state.equals(this.closingState)) {
            str = "CLOSING";
            // Don't get a close frame response after 3 seconds, close the connection
            this.setServerTimeoutTask();
        }

        System.out.println("Client is in " + str + " state. Room: " + this.roomName + " - Name: " + this.displayName);
    }

    private Timer timer;
    private TimerTask sendPing;

    public void setSendPingTask() {
        this.sendPing = new SendPing(this);
        this.timer.schedule(this.sendPing, 3000, 3000);
    }

    public void stopSendPingTask() {
        if (this.sendPing != null) {
            this.sendPing.cancel();
        }
    }

    private TimerTask serverTimeout;

    public void setServerTimeoutTask() {
        this.serverTimeout = new ServerTimeout(this);
        this.timer.schedule(this.serverTimeout, 10000);
    }

    public void stopServerTimeoutTask() {
        if (this.serverTimeout != null) {
            this.serverTimeout.cancel();
        }
    }

    private String roomName;

    public String getRoomName() {
        return this.roomName;
    }

    public void setRoomName(String name) {
        this.roomName = name;
    }

    private String displayName;

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    private boolean openHandShakeSent = false;
    private boolean displayInstructions = true;

    public boolean getDisplayInstructions() {
        return this.displayInstructions;
    }

    public void setDisplayInstructions(boolean display) {
        this.displayInstructions = display;
    }

    /**
     * Constructs the client by laying out the GUI and registering a listener with
     * the textfield so that pressing Return in the listener sends the textfield
     * contents to the server. Note however that the textfield is initially NOT
     * editable, and only becomes editable AFTER the client receives the
     * NAMEACCEPTED message from the server.
     */
    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        this.closedState = new ClientClosedState(this);
        this.openState = new ClientOpenState(this);
        this.joinedState = new ClientJoinedState(this);
        this.closingState = new ClientClosingState(this);
        this.currentState = this.closedState;
        this.timer = new Timer();
    }

    private void run() throws IOException {
        this.socket = new Socket(serverAddress, 59001);
        Scanner inputScanner = new Scanner(System.in);
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            System.out.println("Chat client started...");

            while (this.running) {

                // If we are in closed state, and an open handshake has not been sent, send
                if (this.currentState.equals(this.closedState) && !openHandShakeSent) {
                    // base64-encode the secret
                    String base64encodedSecret = Base64.getEncoder().encodeToString(this.secret.getBytes("utf-8"));
                    // Create an open frame
                    this.sendMessage(true, Global.FRAME_TYPE.OPEN, Global.ERROR_CODE.NONE, base64encodedSecret, "");
                    this.openHandShakeSent = true;
                    // Close the socket if don't get a response after 3 seconds
                    this.setServerTimeoutTask();
                }

                if (this.displayInstructions) {
                    this.currentState.displayInstructions();
                }

                if (System.in.available() > 0 && inputScanner.hasNextLine()) {
                    this.currentState.handleUserInput(inputScanner.nextLine());
                }

                // Handle incoming messages
                if (in.available() > 0) {
                    int messageLength = in.readInt();
                    // Get the byte stream
                    byte[] message = new byte[messageLength];
                    in.readFully(message, 0, messageLength);
                    // Parse the byte array to get a frame
                    DataFrame frame = new DataFrame();
                    boolean success = frame.parseFromByteArray(message);
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

                        // Handle the frame
                        this.currentState.handleDataFrame(frame);

                    } else {

                        // Send an error frame
                        Global.sendErrorFrame(out, Global.ERROR_CODE.CORRUPT_FRAME);
                        // Close the socket immedialte
                        this.closeSocket();

                    }
                }
            }

        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            inputScanner.close();
            this.closeSocket();
        }
    }

    public void sendMessage(boolean end, Global.FRAME_TYPE type, Global.ERROR_CODE errorCode, String mainData,
            String extendedData) {
        Global.sendMessage(out, end, this.protocolVersion, type, errorCode, mainData, extendedData);
    }

    public void sendErrorFrame(Global.ERROR_CODE errorCode) {
        Global.sendErrorFrame(out, errorCode);
    }

    public void initClosing() {
        System.out.println("Closing the program...");
        this.stopSendPingTask();
        this.stopServerTimeoutTask();
        // Send the close frame
        this.sendMessage(true, Global.FRAME_TYPE.CLOSE, Global.ERROR_CODE.NONE, "", "");
        // Move to the closing state
        this.setCurrentState(this.getClosingState());
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
            this.setCurrentState(this.closedState);
            this.closeSocket();
            return;
        }

        // Send a response back
        this.sendMessage(true, Global.FRAME_TYPE.CLOSE, Global.ERROR_CODE.NONE, "", "");

        this.setCurrentState(this.closedState);

        // Close the socket immediately
        this.closeSocket();

    }

    public void handlePongFrame() {
        this.stopServerTimeoutTask();
        this.setServerTimeoutTask();
        // System.out.println("Received a pong from the server.");
    }

    public void closeSocket() {
        try {
            this.running = false;
            this.timer.cancel();
            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void main(String[] args) throws Exception {
        // if (args.length != 1) {
        // System.err.println("Pass the server IP as the sole command line argument");
        // return;
        // }
        var client = new ChatClient("localhost");
        client.run();
    }

}