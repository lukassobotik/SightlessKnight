package lukas.sobotik.sightlessknight.endpoints;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;
import lukas.sobotik.sightlessknight.gamelogic.AlgebraicNotationUtils;
import lukas.sobotik.sightlessknight.gamelogic.Board;
import lukas.sobotik.sightlessknight.gamelogic.BoardLocation;
import lukas.sobotik.sightlessknight.gamelogic.FenUtils;
import lukas.sobotik.sightlessknight.gamelogic.GameState;
import lukas.sobotik.sightlessknight.gamelogic.Move;
import lukas.sobotik.sightlessknight.gamelogic.Piece;
import lukas.sobotik.sightlessknight.gamelogic.Rules;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.List;
import java.util.Random;

@Endpoint
@AnonymousAllowed
public class PlayGameEndpoint {
    public static String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    FenUtils fenUtils;
    AlgebraicNotationUtils algebraicNotationUtils;
    public GameState gameState;
    Board board;
    List<Move> validMovesForPosition;

    // Piece Game
    BoardLocation targetSquare = null;
    BoardLocation startSquare = null;
    public boolean isTrainingMode = false;
    Piece trainingPiece = null;
    Piece pieceForKinglessGames = null;

    /**
     * Initializes the game board and pieces.
     * This method is called internally when starting a new game.
     */
    public void initialize() {
        Piece[] pieces = new Piece[64];
        fenUtils = new FenUtils(pieces);
        pieces = fenUtils.generatePositionFromFEN(STARTING_POSITION);
        board = new Board(64, pieces, fenUtils);

        gameState = new GameState(board, fenUtils.getStartingTeam(), false); // TODO: Implement kingless games
        algebraicNotationUtils = new AlgebraicNotationUtils(fenUtils, gameState, board);
        algebraicNotationUtils.setKinglessGame(false); // TODO: Implement kingless games
        validMovesForPosition = Rules.getAllValidMovesForTeam(GameState.currentTurn, board, true);
    }

    /**
     * Plays a move in the game.
     * @param move the move to be played
     */
    public void playMove(Move move) {
        updateGameStateAndBoard(move);
        checkIfGameEnded();

        if (GameState.isPawnPromotionPending) {
//            handlePawnPromotion(move);
        } else {
//            getAlgebraicNotation();
        }

//        managePieceMoveDrills();
        updateValidMovesList();
    }

    public void playMoveFromText(String text) {
        algebraicNotationUtils.updateVariables(fenUtils, gameState, board);
        Move move = algebraicNotationUtils.getMoveFromParsedMove(text);
        if (move == null) {
            Notification.show("Invalid Move");
            return;
        }
        playMove(move);
    }

    /**
     * Updates the game state and the game board after a move is played.
     * @param move the Move object representing the move that was played
     */
    private void updateGameStateAndBoard(Move move) {
        if (isMoveValid(move)) {
            gameState.playMove(move, false);
            board = gameState.getBoard();
            printBoard(board.pieces);
//            createBoard(board.pieces);
        }
    }

    /**
     * Method that checks if a move is valid.
     * @param move the move to check
     * @return if the move is valid
     */
    public boolean isMoveValid(Move move) {
        for (Move validMove : validMovesForPosition) {
            if (move.equals(validMove)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that updates the valid moves list for the current position.
     */
    public void updateValidMovesList() {
        validMovesForPosition = Rules.getAllValidMovesForTeam(GameState.currentTurn, board, true);
    }

    /**
     * Check if the game has ended and display the appropriate message and dialog.
     */
    private void checkIfGameEnded() {
        if (gameState.hasGameEnded) {
            if (Rules.isStalemate(GameState.currentTurn, board) && !GameState.isPawnPromotionPending) {
                Notification.show("Game Over!");
                VerticalLayout dialogLayout = new VerticalLayout();
                Text dialogText = new Text("Game Drawn by Stalemate");
//                createGameOverDialog(dialogLayout, dialogText);
            }
            if (Rules.isCheckmate(GameState.currentTurn, board)) {
                Notification.show("Game Over!");
                VerticalLayout dialogLayout = new VerticalLayout();
                Text dialogText = new Text((GameState.currentTurn == Team.WHITE ? "Black" : "White") + " Won by Checkmate");
//                createGameOverDialog(dialogLayout, dialogText);
            }
        }
    }

    /**
     * Get the URL of the chess piece image based on the specified piece string.
     * @param s The piece string. Must be one of the following: "P", "N", "B", "R", "Q", "K", "p", "n", "b", "r", "q", "k".
     * @return The URL of the chess piece image. Returns an empty string if the piece string is null or invalid.
     */
    private String getChessPieceUrl(String s) {
        if (s == null) {
            return "";
        }

        var url = "";
        switch (s) {
            case "P" -> url = "images/sprites/w_pawn.svg";
            case "N" -> url = "images/sprites/w_knight.svg";
            case "B" -> url = "images/sprites/w_bishop.svg";
            case "R" -> url = "images/sprites/w_rook.svg";
            case "Q" -> url = "images/sprites/w_queen.svg";
            case "K" -> url = "images/sprites/w_king.svg";
            case "p" -> url = "images/sprites/b_pawn.svg";
            case "n" -> url = "images/sprites/b_knight.svg";
            case "b" -> url = "images/sprites/b_bishop.svg";
            case "r" -> url = "images/sprites/b_rook.svg";
            case "q" -> url = "images/sprites/b_queen.svg";
            case "k" -> url = "images/sprites/b_king.svg";
        }
        return url;
    }

    /**
     * Print the chess board.
     */
    public void printBoard() {
        printBoard(board.pieces);
    }

    /**
     * Print the chess board.
     * @param pieces An array of Piece objects representing the chess board.
     */
    private void printBoard(Piece[] pieces) {
        for (int rank = 7; rank >= 0; rank--) {
            Paragraph rankParagraph = new Paragraph();
            StringBuilder builder = new StringBuilder();
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece piece = pieces[index];

                if (piece == null) {
                    System.out.print(". "); // Empty square
                    builder.append(". ");
                } else {
                    System.out.print(fenUtils.getSymbolFromPieceType(piece.type, piece.team) + " "); // Piece character
                    builder.append(fenUtils.getSymbolFromPieceType(piece.type, piece.team)).append(" ");
                }
            }
            System.out.println();
            rankParagraph.setText(builder.toString());
            rankParagraph.setId(String.valueOf(rank));
            rankParagraph.addClassName("board");
        }
        System.out.println("-----------------------------");
    }

    /**
     * Generate a chess game with a single knight piece.
     * @return An array of Piece objects representing the chess board.
     */
    private Piece[] generatePieceTrainingGame(Piece piece) {
        pieceForKinglessGames = piece;

        Piece[] pieces = new Piece[64];
        fenUtils = new FenUtils(pieces);

        int piecePos = new Random().nextInt(64);
        pieces[piecePos] = piece;

        board = new Board(64, pieces, fenUtils);
        gameState = new GameState(board, fenUtils.getStartingTeam(), true);
        startSquare = board.getPointFromArrayIndex(piecePos);
        if (piece.type.equals(PieceType.KNIGHT)) {
            targetSquare = board.getPointFromArrayIndex(getRandomSquareWithinDistance(piecePos, 3, false));
        } else if (piece.type.equals(PieceType.BISHOP)) {
            targetSquare = board.getPointFromArrayIndex(getRandomSquareWithinDistance(piecePos, 5, true));
        } else {
            targetSquare = board.getPointFromArrayIndex(getRandomSquareWithinDistance(piecePos, 5, false));
        }
        System.out.println(fenUtils.generateFenFromPosition(pieces));
        System.out.println(targetSquare.getAlgebraicNotationLocation());
        return pieces;
    }

    /**
     * Generates a random square within a given distance from a given square.
     * The generated square will be different from the given square.
     * Used for piece move drills.
     * @param square The square for which the random square is generated.
     * @param distance The maximum distance from the given square.
     * @return A random square within the given distance from the given square.
     */
    public static int getRandomSquareWithinDistance(int square, int distance, boolean isBishop) {
        Random random = new Random();
        int targetSquare;
        var isDarkSquare = (square / 8 + square % 8) % 2 != 0;

        do {
            int file = square % 8;
            int rank = square / 8;

            int minFile = Math.max(0, file - distance);
            int maxFile = Math.min(7, file + distance);
            int minRank = Math.max(0, rank - distance);
            int maxRank = Math.min(7, rank + distance);

            int randomFile = random.nextInt(maxFile - minFile + 1) + minFile;
            int randomRank;

            do {
                randomRank = random.nextInt(maxRank - minRank + 1) + minRank;
                targetSquare = randomRank * 8 + randomFile;
            } while (isBishop && ((targetSquare / 8 + targetSquare % 8) % 2 == 0) == isDarkSquare);

        } while (targetSquare == square);

        return targetSquare;
    }
}
