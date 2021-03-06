package ua.carcassone.game.networking;

public class ClientQueries {

    public static class JOIN_TABLE {
        final String action = "JOIN_TABLE";
        public String tableId;

        public JOIN_TABLE(String tableId){
            this.tableId = tableId;
        }
    }

    public static class CREATE_TABLE {
        String action;
        String tableName;

        public CREATE_TABLE(String tableName){
            this.tableName = tableName;
            this.action = "CREATE_TABLE";
        }
    }

    public static class LEAVE_TABLE {
        final String action = "LEAVE_TABLE";
        String additionalInfo;

        public LEAVE_TABLE(String additionalInfo) {
            this.additionalInfo = additionalInfo;
        }
    }

    public static class START_GAME {
        final String action = "START_GAME";
        public String someInfo;

        // TODO узнать почему не создается норм класс если без аргументов
        public START_GAME(String someInfo){
            this.someInfo = someInfo;
        }
    }

    public static class PUT_TILE {
        final String action = "PUT_TILE";

        static class Position{
            public int x;
            public int y;

            public Position(int x, int y){
                this.x = x;
                this.y = y;
            }
        }

        public Position position;
        public int rotation;
        public int meeple;

        public PUT_TILE(int x, int y, int rotation, int meeple){
            this.position = new Position(x, y);
            this.rotation = rotation;
            this.meeple = meeple;
        }
    }

}
