import lukas.sobotik.sightlessknight.ai.PerftFunction;
import lukas.sobotik.sightlessknight.gamelogic.Board;
import lukas.sobotik.sightlessknight.gamelogic.FenUtils;
import lukas.sobotik.sightlessknight.gamelogic.GameState;
import lukas.sobotik.sightlessknight.gamelogic.Piece;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;
import lukas.sobotik.sightlessknight.views.play.PlayView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerftTest {

    private Board board;
    private GameState gameState;
    private PerftFunction perftFunction;

    @BeforeEach
    public void setUp() {

    }

    @ParameterizedTest
    @MethodSource("fenPositionsProvider")
    public void testPerft(String fen, int depth, int expectedPositions) {
        // Set up the position based on the given FEN
        Piece[] pieces = new Piece[64];
        FenUtils fenUtils = new FenUtils(pieces);
        pieces = fenUtils.generatePositionFromFEN(fen);
        board = new Board(64, pieces, fenUtils);
        gameState = new GameState(board, fenUtils.getStartingTeam(), false);
        perftFunction = new PerftFunction(board, gameState, null);

        int actualPositions = perftFunction.playMoves(depth, fenUtils.getStartingTeam(), false);
        assertEquals(expectedPositions, actualPositions, "FEN: " + fen + ", Team " + fenUtils.getStartingTeam() + ", Depth " + depth + " positions mismatch.");
    }

    private static Stream<Arguments> fenPositionsProvider() {
        var enPassantWhite = "8/2p5/8/1P6/8/8/8/K1k5 b - - 0 1";
        var enPassantBlack = "K1k5/8/8/8/2p5/8/1P6/8 w - - 0 1";
        var castling = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        var startingPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        // Add FENs and their corresponding expected positions for each depth
        return Stream.of(
                Arguments.of(enPassantWhite, 1, 5),
                Arguments.of(enPassantWhite, 2, 16),
                Arguments.of(enPassantWhite, 3, 100),
                Arguments.of(enPassantWhite, 4, 485),
                Arguments.of(enPassantWhite, 5, 3567),
//                Arguments.of(enPassantWhite, 6, 21270),

                Arguments.of(enPassantBlack, 1, 3),
                Arguments.of(enPassantBlack, 2, 14),
                Arguments.of(enPassantBlack, 3, 53),
                Arguments.of(enPassantBlack, 4, 328),
                Arguments.of(enPassantBlack, 5, 1747),
//                Arguments.of(enPassantBlack, 6, 12036),

                Arguments.of(castling, 1, 26),
//                Arguments.of(castling, 2, 568),
//                Arguments.of(castling, 3, 13744),
//                Arguments.of(castling, 4, 314346),
//                Arguments.of(castling, 5, 7594526),
//                Arguments.of(castling, 6, 179862938),

                Arguments.of(startingPosition, 1, 20),
                Arguments.of(startingPosition, 2, 400),
                Arguments.of(startingPosition, 3, 8902),
                Arguments.of(startingPosition, 4, 197281)
        );
    }
}