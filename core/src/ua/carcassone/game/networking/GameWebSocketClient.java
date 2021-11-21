package ua.carcassone.game.networking;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import ua.carcassone.game.Settings;
import ua.carcassone.game.networking.ServerQueries.*;

public class GameWebSocketClient extends WebSocketClient {

    static class ClientState extends Observable{
        private ClientStateEnum state = ClientStateEnum.NOT_CONNECTED;

        public void set(ClientStateEnum state){
            this.state = state;
            System.out.println("State changing to "+state);
            setChanged();
            notifyObservers(state);
        }

        public boolean is(ClientStateEnum state){
            return this.state == state;
        }

        public String string(){
            return this.state.name();
        }

        public ClientStateEnum state(){
            return this.state;
        }

    }

    public enum ClientStateEnum {
        NOT_CONNECTED,
        CONNECTING_TO_SERVER,
        CONNECTED_TO_SERVER,
        CONNECTING_TO_TABLE,
        CONNECTED_TO_TABLE,
        FAILED_TO_CONNECT_TO_TABLE,
        CREATING_TABLE,

    }

    private final ClientState state = new ClientState();
    private final Json jsonConverter = new Json();

    public GameWebSocketClient() {
        super(Settings.getServerURI());
        jsonConverter.setOutputType(JsonWriter.OutputType.json);
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        //if (!this.state.is(ClientStateEnum.CONNECTING_TO_SERVER))
        //    throw new IncorrectClientActionException("connection opened but the client is in state "+this.state.string());
        System.out.println("OPENED");
        this.state.set(ClientStateEnum.CONNECTED_TO_SERVER);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("received message: " + message);

        String action;
        try{
            JsonValue fromJson = new JsonReader().parse(message);
            action = fromJson.getString("action");
            System.out.println("action is "+action);
        } catch (IllegalArgumentException exception){
            return;
        }

        if (Objects.equals(action, JOIN_TABLE_SUCCESS.class.getSimpleName())){
            if(!this.state.is(ClientStateEnum.CONNECTING_TO_TABLE))
                System.out.println("! Server sent wrong response: \n\tstate is "+this.state.string()+"\n\tserver sent: "+message);

            JOIN_TABLE_SUCCESS response = jsonConverter.fromJson(JOIN_TABLE_SUCCESS.class, message);
            this.state.set(ClientStateEnum.CONNECTED_TO_TABLE);
        }

        else if (Objects.equals(action, JOIN_TABLE_FAILURE.class.getSimpleName())){
            if(!this.state.is(ClientStateEnum.CONNECTING_TO_TABLE))
                System.out.println("! Server sent wrong response: \n\tstate is "+this.state.string()+"\n\tserver sent: "+message);

            // TODO: uncomment when JOIN_TABLE_FAILURE is created
            // JOIN_TABLE_FAILURE response = jsonConverter.fromJson(JOIN_TABLE_FAILURE.class, message);
            this.state.set(ClientStateEnum.FAILED_TO_CONNECT_TO_TABLE);
        }

        else if (Objects.equals(action, CREATE_TABLE_SUCCESS.class.getSimpleName())){
            if(!this.state.is(ClientStateEnum.CREATING_TABLE))
                System.out.println("! Server sent wrong response: \n\tstate is "+this.state.string()+"\n\tserver sent: "+message);

            CREATE_TABLE_SUCCESS response = jsonConverter.fromJson(CREATE_TABLE_SUCCESS.class, message);
            this.state.set(ClientStateEnum.CONNECTED_TO_TABLE);
        }


    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    public void connectToServer() throws IncorrectClientActionException {
        if (!this.state.is(ClientStateEnum.NOT_CONNECTED))
            throw new IncorrectClientActionException("client is already connected to a server");

        this.state.set(ClientStateEnum.CONNECTING_TO_SERVER);
        this.connect();
    }

    public void connectToTable(String table_id) throws IncorrectClientActionException {
        if (!(this.state.is(ClientStateEnum.CONNECTED_TO_SERVER) || this.state.is(ClientStateEnum.CREATING_TABLE)))
            throw new IncorrectClientActionException("can not connect to a table as client state is " + this.state.string());

        this.send(jsonConverter.toJson(new ClientQueries.JOIN_TABLE(table_id)));
        this.state.set(ClientStateEnum.CONNECTING_TO_TABLE);
    }

    public void createTable(String tableName) throws IncorrectClientActionException {
        if (!this.state.is(ClientStateEnum.CONNECTED_TO_SERVER))
            throw new IncorrectClientActionException("can not connect to a table as client state is " + this.state.string());

        this.send(jsonConverter.toJson(new ClientQueries.CREATE_TABLE(tableName)));

        this.state.set(ClientStateEnum.CREATING_TABLE);
    }

    public void restoreServerConnection() throws IncorrectClientActionException {
        System.out.println("Restoring connection");
        switch (this.state.state()){
            case NOT_CONNECTED:
            case CONNECTING_TO_SERVER:
                throw new IncorrectClientActionException("can not restore as client state is " + this.state.string());
            case CONNECTED_TO_SERVER:
                return;
            case CONNECTED_TO_TABLE:
                // TODO disconnectFromTable()
            case CONNECTING_TO_TABLE:
                this.state.set(ClientStateEnum.CONNECTED_TO_SERVER);
            case FAILED_TO_CONNECT_TO_TABLE:
                this.state.set(ClientStateEnum.CONNECTED_TO_SERVER);
        }

    }

    public void addStateObserver(Observer observer){
        state.addObserver(observer);
    }

    public static class stateMultipleObserver implements Observer {
        Consumer<ClientStateEnum> consumer;
        ClientStateEnum stoppingState = null;

        @Override
        public void update(Observable o, Object state) {
            consumer.accept((ClientStateEnum) state);
            if (state == stoppingState)
                o.deleteObserver(this);
        }

        public stateMultipleObserver(Consumer<ClientStateEnum> consumer){
            this.consumer = consumer;
        }

        public stateMultipleObserver(ClientStateEnum stoppingState, Consumer<ClientStateEnum> consumer){
            this.consumer = consumer;
            this.stoppingState = stoppingState;
        }
    }

    public static class stateSingleObserver implements Observer {
        Consumer<ClientStateEnum> consumer;

        @Override
        public void update(Observable o, Object state) {
            consumer.accept((ClientStateEnum) state);
            o.deleteObserver(this);
        }

        public stateSingleObserver(Consumer<ClientStateEnum> consumer){
            this.consumer = consumer;
        }

    }

    public static class stateAcceptableObserver implements Observer {
        Consumer<ClientStateEnum> consumer;
        List<ClientStateEnum> acceptable;
        ClientStateEnum stoppingState;

        @Override
        public void update(Observable o, Object state) {
            if (acceptable.contains((ClientStateEnum) state)){
                consumer.accept((ClientStateEnum) state);
                if (state == stoppingState)
                    o.deleteObserver(this);
            } else {
                o.deleteObserver(this);
            }

        }

        public stateAcceptableObserver(ClientStateEnum stoppingState, List<ClientStateEnum> acceptable,
                                       Consumer<ClientStateEnum> consumer){
            this.stoppingState = stoppingState;
            this.acceptable = acceptable;

            if(!this.acceptable.contains(this.stoppingState))
                this.acceptable.add(this.stoppingState);

            this.consumer = consumer;
        }

    }

}
