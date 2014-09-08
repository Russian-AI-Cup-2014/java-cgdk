import model.Game;
import model.Hockeyist;
import model.Move;
import model.PlayerContext;

import java.io.IOException;

public final class Runner {
    private final RemoteProcessClient remoteProcessClient;
    private final String token;

    public static void main(String[] args) throws IOException {
        if (args.length == 3) {
            new Runner(args).run();
        } else {
            new Runner(new String[]{"127.0.0.1", "31001", "0000000000000000"}).run();
        }
    }

    private Runner(String[] args) throws IOException {
        remoteProcessClient = new RemoteProcessClient(args[0], Integer.parseInt(args[1]));
        token = args[2];
    }

    public void run() throws IOException {
        try {
            remoteProcessClient.writeToken(token);
            int teamSize = remoteProcessClient.readTeamSize();
            remoteProcessClient.writeProtocolVersion();
            Game game = remoteProcessClient.readGameContext();

            Strategy[] strategies = new Strategy[teamSize];

            for (int strategyIndex = 0; strategyIndex < teamSize; ++strategyIndex) {
                strategies[strategyIndex] = new MyStrategy();
            }

            PlayerContext playerContext;

            while ((playerContext = remoteProcessClient.readPlayerContext()) != null) {
                Hockeyist[] playerHockeyists = playerContext.getHockeyists();
                if (playerHockeyists == null || playerHockeyists.length != teamSize) {
                    break;
                }

                Move[] moves = new Move[teamSize];

                for (int hockeyistIndex = 0; hockeyistIndex < teamSize; ++hockeyistIndex) {
                    Hockeyist playerHockeyist = playerHockeyists[hockeyistIndex];

                    Move move = new Move();
                    moves[hockeyistIndex] = move;
                    strategies[playerHockeyist.getTeammateIndex()].move(
                            playerHockeyist, playerContext.getWorld(), game, move
                    );
                }

                remoteProcessClient.writeMoves(moves);
            }
        } finally {
            remoteProcessClient.close();
        }
    }
}
