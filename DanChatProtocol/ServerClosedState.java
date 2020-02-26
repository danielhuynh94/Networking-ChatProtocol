import java.util.Base64;

public class ServerClosedState implements ServerState {
    protected ChatServer.Handler clientHandler;

    public ServerClosedState(ChatServer.Handler clientHandler) {
        this.clientHandler = clientHandler;
    }

    @Override
    public void handleDataFrame(DataFrame frame) {
        if (frame.getType() == Global.FRAME_TYPE.OPEN.getValue()) {

            // Validate the supported version
            if (ChatServer.serverVersion != frame.getVersion()) {
                this.clientHandler.sendErrorFrame(Global.ERROR_CODE.VERSION_NOT_SUPPORTED);
            }

            // Validate the secret key
            byte[] base64decodedBytes = Base64.getDecoder().decode(frame.getMainData());
            String decodedSecret = new String(base64decodedBytes);
            if (!ChatServer.secret.equals(decodedSecret)) {
                this.clientHandler.sendErrorFrame(Global.ERROR_CODE.INVALID_SECRET_KEY);
            }

            // Version is supported, and secret key matches, send an open frame back to
            // complete the handshake
            this.clientHandler.sendMessage(true, Global.FRAME_TYPE.OPEN, Global.ERROR_CODE.NONE, "", "");

            // Switch to OPEN state
            this.clientHandler.setCurrentState(this.clientHandler.getOpenState());

        } else {

            // If this is not an open frame, send an error back to the
            this.clientHandler.sendErrorFrame(Global.ERROR_CODE.COMMAND_NOT_ALLOWED);

            // Close the socket immediately
            this.clientHandler.closeSocket();

        }
    }

}