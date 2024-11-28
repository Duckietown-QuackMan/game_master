package ch.quack.man;

import ch.quack.man.communication.BotInterface;
import ch.quack.man.communication.GhostBotEndpoint;
import ch.quack.man.communication.QuackManEndpoint;
import ch.quack.man.communication.model.CheckpointMsg;
import ch.quack.man.communication.model.DetectionMsg;
import ch.quack.man.communication.model.ScoreMsg;
import ch.quack.man.communication.model.TimeoutMsg;
import org.glassfish.tyrus.server.Server;

import javax.websocket.DeploymentException;
import java.util.*;
import java.util.logging.Logger;


public class GameMaster {

    private static final Logger LOGGER = Logger.getLogger(GameMaster.class.getName());

    private static final int LOOP_FREQUENCY = 10;

    private static GameMaster instance;

    private volatile boolean gameRunning = false;
    private final GameState gameState = new GameState();

    private final int expectedGhostBots;

    private Server server;

    private final Set<BotInterface> ghostBots = Collections.synchronizedSet(new HashSet<>());
    private volatile BotInterface quackMan;

    private GameMaster(int expectedGhostBots) {
        this.expectedGhostBots = expectedGhostBots;
    }

    public void gameLoopIteration() {
        // Game loop iteration

        // 1. Check connection status
        boolean quackManConnected = quackMan != null;
        int numGhostBotsConnected = ghostBots.size();
        boolean ghostBotsConnected = numGhostBotsConnected == expectedGhostBots;

        // 2. Gather inputs
        boolean quackManDetected = gameState.isQuackManDetected();
        boolean checkpointTimeout = gameState.isCheckpointTimeout();
        boolean allCheckpointsCollected = gameState.areAllCheckpointsCollected();

        // 3. Update game state
        switch (gameState.getState()) {
            case IDLE:
                gameState.setState(handleIdleState(quackManConnected, ghostBotsConnected));
                break;
            case RUNNING:
                gameState.setState(handleRunningState(quackManDetected, allCheckpointsCollected, checkpointTimeout));
                break;
            case GAME_OVER:
                gameState.setState(handleGameOverState());
                break;
        }

        // 4. send game state to clients
        if (quackMan != null) {
            quackMan.sendGameState(gameState.getState());
        }
        ghostBots.forEach(bot -> bot.sendGameState(gameState.getState()));

        // 5. print game state
        LOGGER.info("Game state: " + gameState.getState() + " Points: " + gameState.getPoints());
    }

    private GameState.State handleIdleState(boolean quackManConnected, boolean ghostBotsConnected) {
        if (quackManConnected && ghostBotsConnected) {
            return GameState.State.RUNNING;
        } else {
            return GameState.State.IDLE;
        }
    }

    private GameState.State handleRunningState(boolean quackManDetected, boolean allCheckpointsCollected, boolean checkpointTimeout) {
        if (quackManDetected || checkpointTimeout) {
            return GameState.State.GAME_OVER;
        } else if (allCheckpointsCollected) {
            return GameState.State.GAME_WON;
        } else {
            return GameState.State.RUNNING;
        }
    }

    private GameState.State handleGameOverState() {
        return GameState.State.GAME_OVER;
    }

    public void startGame() {
        gameRunning = true;
        try {
            startServer();
            while (gameRunning) {
                // Game loop
                gameLoopIteration();
                Thread.sleep(1000 / LOOP_FREQUENCY);
            }

        } catch (Exception e) {
            LOGGER.severe("Error during game loop: " + e.getMessage());
        } finally {
            LOGGER.info("Game loop stopped.");
            server.stop();
        }
    }

    private void scoreUpdateCallback(ScoreMsg scoreMsg) {
        gameState.setPoints(scoreMsg.score());
    }

    private void allCheckpointsCollectedCallback(CheckpointMsg checkpointMsg) {
        gameState.setAllCheckpointsCollected(checkpointMsg.allCheckpointsCollected());
    }

    private void checkpointTimeoutCallback(TimeoutMsg timeoutMsg) {
        gameState.setCheckpointTimeout(timeoutMsg.checkpointTimeout());
    }

    private void detectionCallback(DetectionMsg detectionMsg) {
        gameState.setQuackManDetected(detectionMsg.detected());
    }

    public void registerQuackMan(BotInterface quackMan) {
        this.quackMan = quackMan;
        quackMan.setScoreCallback(this::scoreUpdateCallback);
        quackMan.setCheckpointCallback(this::allCheckpointsCollectedCallback);
        quackMan.setCheckpointTimeoutCallback(this::checkpointTimeoutCallback);
    }

    public void deregisterQuackMan(BotInterface quackMan) {
        this.quackMan = null;
    }

    public void registerGhostBot(BotInterface ghostBot) {
        ghostBots.add(ghostBot);
        ghostBot.setDetectionMsgConsumer(this::detectionCallback);
    }

    public void deregisterGhostBot(BotInterface ghostBot) {
        ghostBots.remove(ghostBot);
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void stopGame() {
        gameRunning = false;
    }

    public GameState getCurrentState() {
        return gameState;
    }

    private void startServer() throws DeploymentException {
        server = new Server("localhost", 8025, "/ws", GhostBotEndpoint.class, QuackManEndpoint.class);
        server.start();
    }

    public static GameMaster initialize(int expectedGhostBots) {
        instance = new GameMaster(expectedGhostBots);
        return instance;
    }

    public static GameMaster getInstance() {
        return instance;
    }

}
