package ch.quack.man;

public class GameState {

    public enum State {
        IDLE,
        RUNNING,
        GAME_OVER,
        GAME_WON
    }

    private volatile int points = 0;
    private volatile boolean allCheckpointsCollected = false;
    private volatile boolean checkpointTimeout = false;
    private volatile boolean quackManDetected = false;
    private State state = State.IDLE;

    public synchronized void setPoints(int points) {
        this.points = points;
    }

    public synchronized int getPoints() {
        return points;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isQuackManDetected() {
        return quackManDetected;
    }

    public void setCheckpointTimeout(boolean checkpointTimeout) {
        this.checkpointTimeout = checkpointTimeout;
    }

    public boolean isCheckpointTimeout() {
        return checkpointTimeout;
    }

    public void setQuackManDetected(boolean quackManDetected) {
        this.quackManDetected = quackManDetected;
    }

    public boolean areAllCheckpointsCollected() {
        return allCheckpointsCollected;
    }

    public void setAllCheckpointsCollected(boolean allCheckpointsCollected) {
        this.allCheckpointsCollected = allCheckpointsCollected;
    }

}
