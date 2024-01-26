package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.ai.PerftFunction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DevelopmentPerftTest {
    @ParameterizedTest
    @MethodSource("fenPositionsProvider")
    public void testPerft(String fen, int depth, int expectedPositions) {
        // Set up the position based on the given FEN
        Piece[] pieces = new Piece[64];
        FenUtils fenUtils = new FenUtils(pieces);
        pieces = fenUtils.generatePositionFromFEN(fen);
        Board board = new Board(64, pieces, fenUtils);
        GameState gameState = new GameState(board, fenUtils.getStartingTeam(), false);
        PerftFunction perftFunction = new PerftFunction(board, gameState, null);

        int actualPositions = perftFunction.playMoves(depth, fenUtils.getStartingTeam(), true, false, true);
        assertEquals(expectedPositions, actualPositions,
                     ", FEN: " + fen
                             + ", Move " + GameState.moveNumber
                             + ", Pieces Captured: " + GameState.capturedPieces
                             + ", Depth " + depth + " positions mismatch.");
    }

    private static Stream<Arguments> fenPositionsProvider() {
        var promotion = "8/1P6/8/8/8/8/8/K1k5 w - - 0 1";
        var enPassantBlack = "K1k5/8/8/8/2p5/8/1P6/8 w - - 0 1";
        var testPos2 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - ";
        var testPos2toE1 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R4K1R b kq - 1 1";
        var testPos2simplified = "4k3/8/bn6/3P4/8/8/4B3/5K2 b - - 1 1";
        var testPos5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        var testPos6 = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10";

        return Stream.of();
    }
}
