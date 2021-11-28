package ua.carcassone.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import ua.carcassone.game.game.TileType;
import ua.carcassone.game.game.TileTypes;

public class Utils {

    private static final int SCALING_COEFFICIENT = 12;

    public enum SpacialRelation{
        LEFT, ABOVE, RIGHT, BELOW
    }

    /**
     * standard unit for interactive elements on the screen (buttons, labels, etc.).
     * */
    public static final int ELEMENT_HEIGHT_UNIT = Gdx.graphics.getHeight() / SCALING_COEFFICIENT;
    public static final int ELEMENT_WIDTH_UNIT = Gdx.graphics.getWidth() / SCALING_COEFFICIENT;

    /**
     * Returns an amount of pixels from the bottom of the screen to match some amount of pixels from the top.
     * @param y pixels from the top
     * @return pixels from the bottom
     */
    public static int fromTop(int y){
        return Gdx.graphics.getDisplayMode().height-y;
    }

    public static float fromTop(float y){
        return (Gdx.graphics.getDisplayMode().height-y);
    }

    public static int fromRight(int x){
        return Gdx.graphics.getDisplayMode().width-x;
    }

    public static float fromRight(float x){
        return (Gdx.graphics.getDisplayMode().width-x);
    }

    public static int getTileTypeId(TileType tileType){
        return TileTypes.indexOf(tileType);
    }

    public static boolean numberInRange(int number, int min, int upperBound){
        return (min <= number) && (number < upperBound);
    }

    public static float min(Vector2 vec){
        return Math.min(vec.x, vec.y);
    }

}
