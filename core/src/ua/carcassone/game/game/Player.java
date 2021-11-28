package ua.carcassone.game.game;

import com.badlogic.gdx.graphics.Color;

public class Player {
    private final String name;
    private final String code;
    private Color color;
    private int meepleCount = 0;
    public boolean left = false;

    public Player(String name, String code, Color color) {
        this.name = name;
        this.code = code;
        this.color = color;
    }


    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
