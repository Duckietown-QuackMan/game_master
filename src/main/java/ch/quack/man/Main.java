package ch.quack.man;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        GameMaster gameMaster = GameMaster.initialize(2);
        gameMaster.startGame();
    }
}