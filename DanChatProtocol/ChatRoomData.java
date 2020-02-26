import java.util.ArrayList;
import java.util.Hashtable;
import java.io.DataOutputStream;

public class ChatRoomData {
    private Hashtable<String, DataOutputStream> participants;
    private ArrayList<DataFrame> messages;

    public ChatRoomData() {
        this.participants = new Hashtable<>();
        this.messages = new ArrayList<>();
    }

    public Hashtable<String, DataOutputStream> getParticipants() {
        return this.participants;
    }

    public synchronized void addParticipant(String displayName, DataOutputStream out) {
        this.participants.put(displayName, out);
    }

    public void removeParticipant(String displayName) {
        this.participants.remove(displayName);
    }

    public ArrayList<DataOutputStream> getChatmateOutputStreams(String participantName) {

        ArrayList<DataOutputStream> results = new ArrayList<>();
        for (String key : participants.keySet()) {
            // Skip the participant
            if (key.equals(participantName)) {
                continue;
            }
            results.add(participants.get(key));
        }
        return results;

    }

    public ArrayList<DataFrame> getMessages() {
        return this.messages;
    }

    public synchronized void addMessage(DataFrame dataFrame) {
        this.messages.add(dataFrame);
    }

}