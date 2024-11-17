import ch.quack.man.GameMaster;
import ch.quack.man.GameState;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameMasterTest {

    @Test
    void testServerStartup() throws InterruptedException {
        GameMaster gameMaster = GameMaster.initialize(2);
        Thread gameMasterThread = new Thread(gameMaster::startGame);
        gameMasterThread.start();
        Thread.sleep(500);
        assertTrue(gameMaster.isGameRunning(), "Server should be running");
        gameMaster.stopGame();
        Thread.sleep(500);
        assertFalse(gameMaster.isGameRunning(), "Server should be stopped");
    }

    @Test
    void testIdleToRunningTransition() {
        GameMaster gameMaster = GameMaster.initialize(1);
        gameMaster.gameLoopIteration();

        MockBot quackman = Utils.connectQuackMan(msg -> {});
        MockBot ghostbot = Utils.connectGhostBot(msg -> {});

        gameMaster.gameLoopIteration();

        assertEquals(GameState.State.RUNNING, gameMaster.getCurrentState().getState(), "Game should transition to RUNNING state");
    }

    @Test
    void testRunningToGameOverTransition() {
        GameMaster gameMaster = GameMaster.initialize(1);
        gameMaster.gameLoopIteration();

        MockBot quackman = Utils.connectQuackMan(msg -> {});
        MockBot ghostbot = Utils.connectGhostBot(msg -> {});

        gameMaster.gameLoopIteration();

        ghostbot.handleMsg("""
                {
                    "type": "DETECTION",
                    "bot": "APRICOT",
                    "data": {
                        "detected": true
                    }
                }
                """);

        gameMaster.gameLoopIteration();

        assertEquals(GameState.State.GAME_OVER, gameMaster.getCurrentState().getState(), "Game should transition to GAME_OVER");
    }

    @Test
    void testRunningToGameWonTransition() {
        GameMaster gameMaster = GameMaster.initialize(0);
        gameMaster.gameLoopIteration();

        MockBot quackman = Utils.connectQuackMan(msg -> {});

        gameMaster.gameLoopIteration();

        quackman.handleMsg("""
                {
                    "type": "CHECKPOINT",
                    "bot": "APRICOT",
                    "data": {
                        "allCheckpointsCollected": true
                    }
                }
                """);

        gameMaster.gameLoopIteration();

        assertEquals(GameState.State.GAME_WON, gameMaster.getCurrentState().getState(), "Game should transition to GAME_WON");
    }

    @Test
    void testGameStateBroadcast() {
        GameMaster gameMaster = GameMaster.initialize(1);
        gameMaster.gameLoopIteration();

        List<String> quackmanMessages = new LinkedList<>();
        List<String> ghostbotMessages = new LinkedList<>();

        MockBot quackman = Utils.connectQuackMan(quackmanMessages::add);
        MockBot ghostbot = Utils.connectGhostBot(ghostbotMessages::add);

        gameMaster.gameLoopIteration();

        assertFalse(quackmanMessages.isEmpty(), "QuackMan should receive game state messages");
        assertFalse(ghostbotMessages.isEmpty(), "GhostBot should receive game state messages");

        assertTrue(quackmanMessages.getFirst().contains("\"type\":\"GAME_STATE\""));
        assertTrue(ghostbotMessages.getFirst().contains("\"type\":\"GAME_STATE\""));
    }

    @Test
    void testQuackManScoreUpdate() {
        GameMaster gameMaster = GameMaster.initialize(0);
        gameMaster.gameLoopIteration();

        MockBot quackman = Utils.connectQuackMan(msg -> {});

        gameMaster.gameLoopIteration();

        quackman.handleMsg("""
                {
                    "type": "SCORE",
                    "bot": "APRICOT",
                    "data": {
                        "score": 42
                    }
                }
                """);

        gameMaster.gameLoopIteration();

        assertEquals(42, gameMaster.getCurrentState().getPoints(), "Score should be updated to 42");
    }

    @Test
    void multiGhostBotTest() {
        GameMaster gameMaster = GameMaster.initialize(2);
        gameMaster.gameLoopIteration();

        MockBot quackman = Utils.connectQuackMan(msg -> {});
        MockBot ghostbot1 = Utils.connectGhostBot(msg -> {});
        MockBot ghostbot2 = Utils.connectGhostBot(msg -> {});

        gameMaster.gameLoopIteration();

        assertEquals(GameState.State.RUNNING, gameMaster.getCurrentState().getState(), "Game should transition to RUNNING state");
    }

    @Test
    void testGameProgression() {
        GameMaster gameMaster = GameMaster.initialize(1);
        gameMaster.gameLoopIteration();

        List<String> quackmanMessages = new LinkedList<>();
        List<String> ghostbotMessages = new LinkedList<>();

        MockBot quackman = Utils.connectQuackMan(quackmanMessages::add);
        MockBot ghostbot = Utils.connectGhostBot(ghostbotMessages::add);

        gameMaster.gameLoopIteration();

        assertEquals("""
                {
                    "type": "GAME_STATE",
                    "data": {
                        "state": "RUNNING"
                    }
                }
                """.replaceAll("\\s", ""), quackmanMessages.getLast().replaceAll("\\s", ""), "QuackMan should receive game state messages");

        assertEquals("""
                {
                    "type": "GAME_STATE",
                    "data": {
                        "state": "RUNNING"
                    }
                }
                """.replaceAll("\\s", ""), ghostbotMessages.getLast().replaceAll("\\s", ""), "GhostBot should receive game state messages");

        // quackman sends score update
        quackman.handleMsg("""
                {
                    "type": "SCORE",
                    "bot": "APRICOT",
                    "data": {
                        "score": 42
                    }
                }
                """);

        gameMaster.gameLoopIteration();

        assertEquals(42, gameMaster.getCurrentState().getPoints(), "Score should be updated to 42");


        // quatman sends score update
        quackman.handleMsg("""
                {
                    "type": "SCORE",
                    "bot": "APRICOT",
                    "data": {
                        "score": 100
                    }
                }
                """);

        gameMaster.gameLoopIteration();

        assertEquals(100, gameMaster.getCurrentState().getPoints(), "Score should be updated to 100");

        // ghostbot sends detection
        ghostbot.handleMsg("""
                {
                    "type": "DETECTION",
                    "bot": "APRICOT",
                    "data": {
                        "detected": true
                    }
                }
                """);

        gameMaster.gameLoopIteration();

        assertEquals(GameState.State.GAME_OVER, gameMaster.getCurrentState().getState(), "Game should transition to GAME_OVER");

        // state should be broadcasted
        assertEquals("""
                {
                    "type": "GAME_STATE",
                    "data": {
                        "state": "GAME_OVER"
                    }
                }
                """.replaceAll("\\s", ""), quackmanMessages.getLast().replaceAll("\\s", ""), "QuackMan should receive game state messages");

        assertEquals("""
                {
                    "type": "GAME_STATE",
                    "data": {
                        "state": "GAME_OVER"
                    }
                }
                """.replaceAll("\\s", ""), ghostbotMessages.getLast().replaceAll("\\s", ""), "GhostBot should receive game state messages");


    }




}
