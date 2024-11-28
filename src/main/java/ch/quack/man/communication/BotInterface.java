package ch.quack.man.communication;

import ch.quack.man.GameState;
import ch.quack.man.communication.model.CheckpointMsg;
import ch.quack.man.communication.model.DetectionMsg;
import ch.quack.man.communication.model.ScoreMsg;
import ch.quack.man.communication.model.TimeoutMsg;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class BotInterface {

    private static final Logger LOGGER = Logger.getLogger(BotInterface.class.getName());

    private final Session websocketSession;
    private Consumer<ScoreMsg> scoreCallback;
    private Consumer<CheckpointMsg> checkpointCallback;
    private Consumer<TimeoutMsg> checkpointTimeoutCallback;
    private Consumer<DetectionMsg> detectionMsgConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BotInterface(Session websocketSession) {
        this.websocketSession = websocketSession;
    }


    public void send(String message) throws IOException {
        websocketSession.getBasicRemote().sendText(message);
    }

    public void sendGameState(GameState.State state) {
        try {
            String json = new ObjectMapper().writeValueAsString(Map.of("type", "GAME_STATE", "data", Map.of("state", state.toString())));
            send(json);
        } catch (JsonProcessingException e) {
            LOGGER.warning("Could not create game state msg: " + e.getMessage() + " dropping message");
        } catch (IOException e) {
            LOGGER.warning("Could not send game state msg: " + e.getMessage() + " dropping message");
        }
    }

    public void handleMsg(String message) {
        Map<String, Object> msg;
        try {
            msg = objectMapper.readValue(message, new TypeReference<HashMap<String,Object>>() {});
        } catch (JsonProcessingException e) {
            LOGGER.warning("Received message that could not be parsed: " + message);
            return;
        }
        LOGGER.info("Received message with type: " + msg.get("type"));
        if (!msg.containsKey("type")) {
            LOGGER.warning("Received message without type: " + message);
            return;
        }
        switch (((String) msg.get("type")).toLowerCase()) {
            case "score":
                if (scoreCallback != null) {
                    scoreCallback.accept(objectMapper.convertValue(msg.get("data"), ScoreMsg.class));
                }
                break;
            case "checkpoint":
                if (checkpointCallback != null) {
                    checkpointCallback.accept(objectMapper.convertValue(msg.get("data"), CheckpointMsg.class));
                }
                break;
            case "detection":
                if (detectionMsgConsumer != null) {
                    detectionMsgConsumer.accept(objectMapper.convertValue(msg.get("data"), DetectionMsg.class));
                }
                break;
            case "timeout":
                if (checkpointTimeoutCallback != null) {
                    checkpointTimeoutCallback.accept(objectMapper.convertValue(msg.get("data"), TimeoutMsg.class));
                }
                break;
            default:
                LOGGER.warning("Received message with unknown type: " + message);
        }
    }

    public void setScoreCallback(Consumer<ScoreMsg> scoreCallback) {
        this.scoreCallback = scoreCallback;
    }

    public void setCheckpointCallback(Consumer<CheckpointMsg> checkpointCallback) {
        this.checkpointCallback = checkpointCallback;
    }

    public void setCheckpointTimeoutCallback(Consumer<TimeoutMsg> checkpointTimeoutCallback) {
        this.checkpointTimeoutCallback = checkpointTimeoutCallback;
    }

    public void setDetectionMsgConsumer(Consumer<DetectionMsg> detectionMsgConsumer) {
        this.detectionMsgConsumer = detectionMsgConsumer;
    }

    public Session getSession() {
        return websocketSession;
    }

}
