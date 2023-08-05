import lukas.sobotik.sightlessknight.ai.PerftFunction;
import lukas.sobotik.sightlessknight.gamelogic.Board;
import lukas.sobotik.sightlessknight.gamelogic.FenUtils;
import lukas.sobotik.sightlessknight.gamelogic.GameState;
import lukas.sobotik.sightlessknight.gamelogic.Piece;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerftTest {
    private static final HashMap<String, String> positionMap = new HashMap<>();

    @BeforeEach
    public void setUp() {

    }

    @BeforeAll
    public static void setUpBeforeAll() {
        positionMap.put("enPassantWhite", "8/2p5/8/1P6/8/8/8/K1k5 b - - 0 1");
        positionMap.put("enPassantBlack", "K1k5/8/8/8/2p5/8/1P6/8 w - - 0 1");
        positionMap.put("castling", "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        positionMap.put("startingPosition", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        positionMap.put("testPos2", "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - ");
        positionMap.put("testPos3", "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - ");
        positionMap.put("testPos4", "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
        positionMap.put("testPos5", "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
        positionMap.put("testPos6", "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10");
    }

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

        int actualPositions = perftFunction.playMoves(depth, fenUtils.getStartingTeam(), false);
        assertEquals(expectedPositions, actualPositions,
                "Name: " + getPositionNameFromFen(fen)
                        + ", FEN: " + fen
                        + ", Move " + GameState.moveNumber
                        + ", Pieces Captured: " + gameState.capturedPieces
                        + ", Depth " + depth + " positions mismatch.");
    }

    private static Stream<Arguments> fenPositionsProvider() {
        var enPassantWhite = positionMap.get("enPassantWhite");
        var enPassantBlack = positionMap.get("enPassantBlack");
        var castling = positionMap.get("castling");
        var startingPosition = positionMap.get("startingPosition");
        var testPos2 = positionMap.get("testPos2");
        var testPos3 = positionMap.get("testPos3");
        var testPos4 = positionMap.get("testPos4");
        var testPos5 = positionMap.get("testPos5");
        var testPos6 = positionMap.get("testPos6");
        // Add FENs and their corresponding expected positions for each depth
        return Stream.of(
                Arguments.of(enPassantWhite, 1, 5),
                Arguments.of(enPassantWhite, 2, 16),
                Arguments.of(enPassantWhite, 3, 100),
                Arguments.of(enPassantWhite, 4, 485),
                Arguments.of(enPassantWhite, 5, 3567),
                Arguments.of(enPassantWhite, 6, 21270),

                Arguments.of(enPassantBlack, 1, 3),
                Arguments.of(enPassantBlack, 2, 14),
                Arguments.of(enPassantBlack, 3, 53),
                Arguments.of(enPassantBlack, 4, 328),
                Arguments.of(enPassantBlack, 5, 1747),
                Arguments.of(enPassantBlack, 6, 12036),

                Arguments.of(castling, 1, 26),
                Arguments.of(castling, 2, 568),
                Arguments.of(castling, 3, 13744),
                Arguments.of(castling, 4, 314346),
//                Arguments.of(castling, 5, 7594526),
//                Arguments.of(castling, 6, 179862938),

                Arguments.of(startingPosition, 1, 20),
                Arguments.of(startingPosition, 2, 400),
                Arguments.of(startingPosition, 3, 8902),
                Arguments.of(startingPosition, 4, 197281),
//                Arguments.of(startingPosition, 5, 4865609),
//                Arguments.of(startingPosition, 6, 119060324),

                Arguments.of(testPos2, 1, 48),
                Arguments.of(testPos2, 2, 2039),
                Arguments.of(testPos2, 3, 97862),
                Arguments.of(testPos2, 4, 4085603),
//                Arguments.of(testPos2, 5, 193690690),
//                Arguments.of(testPos2, 6, 8031647685),

                Arguments.of(testPos3, 1, 14),
                Arguments.of(testPos3, 2, 191),
                Arguments.of(testPos3, 3, 2812),
                Arguments.of(testPos3, 4, 43238),
//                Arguments.of(testPos3, 5, 674624),
//                Arguments.of(testPos3, 6, 11030083),

                Arguments.of(testPos4, 1, 6),
                Arguments.of(testPos4, 2, 264),
                Arguments.of(testPos4, 3, 9467),
                Arguments.of(testPos4, 4, 422333),
//                Arguments.of(testPos4, 5, 15833292),
//                Arguments.of(testPos4, 6, 706045033),

                Arguments.of(testPos5, 1, 44),
                Arguments.of(testPos5, 2, 1486),
                Arguments.of(testPos5, 3, 62379),
                Arguments.of(testPos5, 4, 2103487),
//                Arguments.of(testPos5, 5, 89941194),

                Arguments.of(testPos6, 1, 46),
                Arguments.of(testPos6, 2, 2079),
                Arguments.of(testPos6, 3, 89890),
                Arguments.of(testPos6, 4, 3894594)
//                Arguments.of(testPos6, 5, 164075551),
//                Arguments.of(testPos6, 6, 6923051137)
        );
    }

    public String getPositionNameFromFen(String fen) {
        for (var entry : positionMap.entrySet()) {
            if (Objects.equals(entry.getValue(), fen)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Invalid FEN: " + fen);
    }
}