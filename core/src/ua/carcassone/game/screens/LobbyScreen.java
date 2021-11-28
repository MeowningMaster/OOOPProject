package ua.carcassone.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ua.carcassone.game.CarcassoneGame;
import ua.carcassone.game.Utils;
import ua.carcassone.game.game.PCLPlayers;
import ua.carcassone.game.game.Player;
import ua.carcassone.game.networking.GameWebSocketClient;
import ua.carcassone.game.networking.IncorrectClientActionException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;

import static ua.carcassone.game.Utils.*;

public class LobbyScreen implements Screen {
    private final CarcassoneGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Stage stage;
    private String tableId;

    private Skin mySkin;
    private PCLPlayers players;
    private PlayersObserver playersObserver;


    public LobbyScreen(final CarcassoneGame game, String tableId) {
        this.game = game;
        this.tableId = tableId;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getDisplayMode().width, Gdx.graphics.getDisplayMode().height);
        viewport = new FitViewport(Gdx.graphics.getDisplayMode().width, Gdx.graphics.getDisplayMode().height, camera);
        stage = new Stage(viewport, game.batch);
        Gdx.input.setInputProcessor(stage);

        mySkin = new Skin(Gdx.files.internal("skin/comic-ui.json"));

        this.playersObserver = new PlayersObserver();
        this.players = new PCLPlayers();
        this.players.addPCLListener(playersObserver);
        this.players.setPlayers(new ArrayList<>(Arrays.asList(
                new Player("You", "CLIENT", Color.WHITE)
        )));
        game.socketClient.setPCLPlayers(this.players);

        GameWebSocketClient.stateSingleObserver changeObserver = new GameWebSocketClient.stateSingleObserver(
                (stateChange)->{
                    if ( stateChange.newState == GameWebSocketClient.ClientStateEnum.IN_GAME) {
                        Gdx.app.postRunnable(() -> {
                            System.out.println("CHANGING TO A GAME SCREEN "+this.getClass().getSimpleName());
                            players.removePCLListener(playersObserver);
                            game.setScreen(new GameScreen(game, this.tableId, (int) stateChange.additionalInfo, players));
                        });
                    }
                }
        );
        game.socketClient.addStateObserver(changeObserver);


        updateStage();
    }

    private Button makeStartGameButton(String name){
        Button createButton = new TextButton(name, mySkin);
        createButton.setSize(ELEMENT_WIDTH_UNIT * 3, ELEMENT_HEIGHT_UNIT);
        createButton.setPosition(ELEMENT_WIDTH_UNIT, Utils.fromTop(ELEMENT_HEIGHT_UNIT * 6));
        createButton.addListener(new InputListener(){
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }


            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                // TODO table names concept
                try {
                    game.socketClient.startGame();
                } catch (IncorrectClientActionException e) {
                    e.printStackTrace();
                }

            }
        });

        return createButton;
    }

    private Button makeLeaveButton(String name){
        Button backButton = new TextButton(name, mySkin);
        backButton.setSize(ELEMENT_WIDTH_UNIT * 3,ELEMENT_HEIGHT_UNIT);
        backButton.setPosition(ELEMENT_WIDTH_UNIT, Utils.fromTop(ELEMENT_HEIGHT_UNIT * 8));
        backButton.addListener(new InputListener(){
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                try {
                    game.socketClient.leaveTable();
                } catch (IncorrectClientActionException e) {
                    e.printStackTrace();
                }
                dispose();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        return backButton;
    }

    @Override
    public void render(float delta) {

        ScreenUtils.clear(250f/255, 224f/255, 145f/255, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    private void updateStage(){
        stage.clear();

        Label carcassoneLabel = new Label("Lobby", mySkin, "title");
        carcassoneLabel.setSize(ELEMENT_WIDTH_UNIT, ELEMENT_HEIGHT_UNIT);
        carcassoneLabel.setPosition(ELEMENT_WIDTH_UNIT, Utils.fromTop(ELEMENT_HEIGHT_UNIT * 2));
        stage.addActor(carcassoneLabel);

        Label codeLabel = new Label("Join Code:", mySkin, "big");
        codeLabel.setSize(ELEMENT_WIDTH_UNIT, ELEMENT_HEIGHT_UNIT);
        codeLabel.setPosition(carcassoneLabel.getX(), carcassoneLabel.getY()-ELEMENT_HEIGHT_UNIT);
        stage.addActor(codeLabel);

        Label code = new Label(this.tableId, mySkin, "half-tone");
        code.setSize(ELEMENT_WIDTH_UNIT, ELEMENT_HEIGHT_UNIT);
        code.setPosition(codeLabel.getX()+codeLabel.getWidth()+20, codeLabel.getY());
        stage.addActor(code);

        Button startGameButton = makeStartGameButton("Start game");
        stage.addActor(startGameButton);

        Button leaveButton = makeLeaveButton("Leave table");
        stage.addActor(leaveButton);

        drawPlayers();
    }

    private void drawPlayers(){
        float size = playersObserver.players.size();
        float heightSpacing = 20;
        float top = Utils.fromTop(ELEMENT_HEIGHT_UNIT * 2);
        float left = fromRight(ELEMENT_WIDTH_UNIT * 2);

        Label playersLabel = new Label("Players:", mySkin, "big");
        playersLabel.setSize(100, 20);
        playersLabel.setPosition(left, top);
        stage.addActor(playersLabel);

        for (int i = 0; i < size; ++i) {
            Player player = playersObserver.players.get(i);

            Label pName = new Label(player.getName(), mySkin, "narration");
            pName.setSize(150, 40);
            pName.setPosition(left, top-(40+heightSpacing)*(i+1));
            stage.addActor(pName);
        }
    }

    private class PlayersObserver implements PropertyChangeListener {
        private ArrayList<Player> players = new ArrayList<>();

        public void propertyChange(PropertyChangeEvent evt){
            this.players = (ArrayList<Player>) evt.getNewValue();
            updateStage();
        }
    }

}