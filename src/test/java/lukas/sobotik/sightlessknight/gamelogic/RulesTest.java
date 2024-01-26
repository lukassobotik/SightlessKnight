package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.Team;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RulesTest {

    @ParameterizedTest
    @MethodSource("provideTestCasesForIsSquareAttackedByEnemy")
    void isSquareAttackedByEnemy(String fen, BoardLocation square, Team friendlyTeam, boolean expectedResult) {
        // Create a board from the FEN string
        Piece[] pieces = new Piece[64];
        FenUtils fenUtils = new FenUtils(pieces);
        pieces = fenUtils.generatePositionFromFEN(fen);
        Board board = new Board(8, pieces, fenUtils);

        boolean result = Rules.isSquareAttackedByEnemy(square, friendlyTeam, board);
        assertEquals(expectedResult, result);
    }

    private static Stream<Arguments> provideTestCasesForIsSquareAttackedByEnemy() {
        return Stream.of(
                Arguments.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new BoardLocation(4, 4), Team.WHITE, false),
                Arguments.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new BoardLocation(4, 4), Team.BLACK, false),
                Arguments.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new BoardLocation(0, 0), Team.WHITE, false),
                Arguments.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", new BoardLocation(7, 7), Team.BLACK, false),
                Arguments.of("3r1rk1/4qpp1/4p2p/p7/PpBnn1b1/1P3N2/5PPP/R1NQR1K1 w - - 0 21", new BoardLocation(1, 1), Team.BLACK, false),
                Arguments.of("3r1rk1/4qpp1/4p2p/p7/PpBnn1b1/1P3N2/5PPP/R1NQR1K1 w - - 0 21", new BoardLocation(1, 2), Team.BLACK, true),
                Arguments.of("3r1rk1/4qpp1/4p2p/p7/PpBnn1b1/1P3N2/5PPP/R1NQR1K1 w - - 0 21", new BoardLocation(3, 3), Team.WHITE, false),
                Arguments.of("3r1rk1/4qpp1/4p2p/p7/PpBnn1b1/1P3N2/5PPP/R1NQR1K1 w - - 0 21", new BoardLocation(2, 2), Team.WHITE, true),
                Arguments.of("3r1rk1/4qpp1/4p2p/p7/PpBnn1b1/1P3N2/5PPP/R1NQR1K1 w - - 0 21", new BoardLocation(5, 5), Team.WHITE, true),
                Arguments.of("3r1rk1/4qpp1/4p2p/p7/PpBnn1b1/1P3N2/5PPP/R1NQR1K1 w - - 0 21", new BoardLocation(1, 1), Team.WHITE, false)
        );
    }
}