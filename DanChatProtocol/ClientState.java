public interface ClientState {

    public void displayInstructions();

    public void handleUserInput(String input);

    public void handleDataFrame(DataFrame frame);
}