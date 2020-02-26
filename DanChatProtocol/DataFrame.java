import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class DataFrame {
    private boolean end;
    private boolean rsv1;
    private boolean rsv2;
    private boolean rsv3;

    private int version;
    private int type;
    private int errorCode;

    private int mainDataLength;
    private String mainData;
    private int extendedDataLength;
    private String extendedData;

    public DataFrame() {
    }

    public DataFrame(boolean end, boolean rsv1, boolean rsv2, boolean rsv3, int version, int type, int errorCode,
            String mainData, String extendedData) {
        this.end = end;
        this.rsv1 = rsv1;
        this.rsv2 = rsv2;
        this.rsv3 = rsv3;
        this.version = version;
        this.type = type;
        this.errorCode = errorCode;
        if (mainData != null) {
            this.mainDataLength = mainData.length();
        }
        this.mainData = mainData;
        if (extendedData != null) {
            this.extendedDataLength = extendedData.length();
        }
        this.extendedData = extendedData;
    }

    public boolean parseFromByteArray(byte[] byteArray) {
        try {

            // The first byte is end
            this.end = byteArray[0] == 1;

            // The second byte is rsv1
            this.rsv1 = byteArray[1] == 1;

            // The second byte is rsv2
            this.rsv2 = byteArray[2] == 1;

            // The second byte is rsv3
            this.rsv3 = byteArray[3] == 1;

            // The next 4 bytes is version
            this.version = ByteBuffer.wrap(Arrays.copyOfRange(byteArray, 4, 8)).getInt();

            // The next 4 bytes is type
            this.type = ByteBuffer.wrap(Arrays.copyOfRange(byteArray, 8, 12)).getInt();

            // The next 4 bytes is error code
            this.errorCode = ByteBuffer.wrap(Arrays.copyOfRange(byteArray, 12, 16)).getInt();

            // The next 4 bytes is mainDataLength
            this.mainDataLength = ByteBuffer.wrap(Arrays.copyOfRange(byteArray, 16, 20)).getInt();

            // the next <mainDataLength> bytes is the main data
            int toIndex = 20 + this.mainDataLength;
            this.mainData = new String(Arrays.copyOfRange(byteArray, 20, toIndex));

            // The next 4 bytes is extendedDataLength
            this.extendedDataLength = ByteBuffer.wrap(Arrays.copyOfRange(byteArray, toIndex, toIndex + 4)).getInt();

            // The next <extendedDataLength> bytes is the extended data
            int fromIndex = toIndex + 4;
            this.extendedData = new String(
                    Arrays.copyOfRange(byteArray, fromIndex, fromIndex + this.extendedDataLength));

        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public boolean getEnd() {
        return this.end;
    }

    public boolean getRsv1() {
        return this.rsv1;
    }

    public boolean getRsv2() {
        return this.rsv2;
    }

    public boolean getRsv3() {
        return this.rsv3;
    }

    public int getVersion() {
        return this.version;
    }

    public int getType() {
        return this.type;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public int getMainDataLength() {
        return this.mainDataLength;
    }

    public String getMainData() {
        return this.mainData;
    }

    public int getExtendedDataLength() {
        return this.extendedDataLength;
    }

    public String getExtendedData() {
        return this.extendedData;
    }

    public byte[] getByteArray() {
        byte[] end = { (byte) (this.end ? 1 : 0) };
        byte[] rsv1 = { (byte) (this.rsv1 ? 1 : 0) };
        byte[] rsv2 = { (byte) (this.rsv2 ? 1 : 0) };
        byte[] rsv3 = { (byte) (this.rsv3 ? 1 : 0) };

        ByteBuffer byteBuffer;

        byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        byte[] version = byteBuffer.putInt(this.version).array();

        byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        byte[] type = byteBuffer.putInt(this.type).array();

        byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        byte[] errorCode = byteBuffer.putInt(this.errorCode).array();

        byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        byte[] mainDataLength = byteBuffer.putInt(this.mainDataLength).array();

        byte[] mainData = this.mainData.getBytes();

        byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        byte[] extendedDataLength = byteBuffer.putInt(this.extendedDataLength).array();

        byte[] extendedData = this.extendedData.getBytes();

        int length = end.length + rsv1.length + rsv2.length + rsv3.length + version.length + type.length
                + errorCode.length + mainDataLength.length + mainData.length + extendedDataLength.length
                + extendedData.length;

        byte[] result = new byte[length];
        result[0] = end[0];
        result[1] = rsv1[0];
        result[2] = rsv2[0];
        result[3] = rsv3[0];
        int count = 4;

        // Copy the bytes from version
        for (int i = 0; i < version.length; i++) {
            result[count] = version[i];
            count++;
        }

        // Copy the bytes from type
        for (int i = 0; i < type.length; i++) {
            result[count] = type[i];
            count++;
        }

        // Copy the bytes from error code
        for (int i = 0; i < errorCode.length; i++) {
            result[count] = errorCode[i];
            count++;
        }

        // Copy the bytes from main data length
        for (int i = 0; i < mainDataLength.length; i++) {
            result[count] = mainDataLength[i];
            count++;
        }

        // Copy the bytes from main data
        for (int i = 0; i < mainData.length; i++) {
            result[count] = mainData[i];
            count++;
        }

        // Copy the bytes from extended data length
        for (int i = 0; i < extendedDataLength.length; i++) {
            result[count] = extendedDataLength[i];
            count++;
        }

        // Copy the bytes from extended data
        for (int i = 0; i < extendedData.length; i++) {
            result[count] = extendedData[i];
            count++;
        }

        return result;
    }

}