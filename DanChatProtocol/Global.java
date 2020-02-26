import java.io.DataOutputStream;

public class Global {
    public enum FRAME_TYPE {
        TEXT(1), OPEN(11), CLOSE(12), ACK(13), NEW(14), JOIN(15), ROOMLIST(16), LEAVE(17), ERROR(18), PING(19),
        PONG(20), RECOVERY(21), SEARCH(22);

        private final int value;

        private FRAME_TYPE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum ERROR_CODE {
        NONE(0), VERSION_NOT_SUPPORTED(1), INVALID_ROOM_NAME(2), INVALID_DISPLAY_NAME(3), INVALID_DATA_CONTENT(4),
        COMMAND_NOT_ALLOWED(5), INVALID_SECRET_KEY(6), CORRUPT_FRAME(7);

        private final int value;

        private ERROR_CODE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static void displayErrorMessage(ERROR_CODE errorCode) {
        String errorMessage = "";
        switch (errorCode) {
        case VERSION_NOT_SUPPORTED:
            errorMessage = "The requested version is not supported.";
            break;
        case INVALID_ROOM_NAME:
            errorMessage = "The room name is invalid. It must contain only alphanumeric characters.";
            break;
        case INVALID_DISPLAY_NAME:
            errorMessage = "The display name is invalid. It must contain only alphanumeric characters.";
            break;
        case COMMAND_NOT_ALLOWED:
            errorMessage = "The command message is invalid due to the receiver's current state or unsupported.";
            break;
        case INVALID_SECRET_KEY:
            errorMessage = "The secret key is invalid.";
            break;
        case CORRUPT_FRAME:
            errorMessage = "The recevied frame cannot be interpreted.";
            break;
        }
        System.out.println(errorMessage);
    }

    public static void sendMessage(DataOutputStream out, boolean end, int version, Global.FRAME_TYPE type,
            Global.ERROR_CODE errorCode, String mainData, String extendedData) {
        try {
            // Create a byte array from the provided data
            byte[] message = new DataFrame(end, false, false, false, version, type.getValue(), errorCode.getValue(),
                    mainData, extendedData).getByteArray();
            // Send the array length
            out.writeInt(message.length);
            // Send the array
            out.write(message, 0, message.length);
            // Flush the stream
            out.flush();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void sendErrorFrame(DataOutputStream out, Global.ERROR_CODE errorCode) {
        Global.sendMessage(out, true, ChatServer.serverVersion, Global.FRAME_TYPE.ERROR, errorCode, "", "");
    }

    public static boolean isAlphanumeric(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isLetterOrDigit(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}