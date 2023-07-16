package lukas.sobotik.sightlessknight.gamelogic;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    Board board;
    public static Team currentTurn;
    public boolean hasGameEnded = false;
    final List<BoardLocation> validMoves;
    BoardLocation selectedPieceLocation;
    public static int moveNumber = 0;
    static final Team playerTeam = Team.WHITE;
    public static boolean isPawnPromotionPending = false;
    static BoardLocation promotionLocation;
    static PieceType selectedPromotionPieceType;
    public static boolean kinglessGame = false;
    public GameState(Board board, Team startingTeam, boolean kinglessGame) {
        validMoves = new ArrayList<>();
        currentTurn = startingTeam;
        moveNumber = 0;
        this.kinglessGame = kinglessGame;
        this.board = board;
    }
    public void play(BoardLocation from, BoardLocation to) {
        if (hasGameEnded && !kinglessGame) {
            return;
        }
        if (isPawnPromotionPending) {
            return;
        }

        Piece piece = board.getPiece(from);
        if (piece == null) return;
        if (piece.team == currentTurn || kinglessGame) {
            selectedPieceLocation = from;
            Rules.getValidMoves(validMoves, selectedPieceLocation, piece, board, !kinglessGame);
        }
        validMoves.forEach(loc -> System.out.println("valid: " + loc.getStringLocation()));

        System.out.println("validMoves: " + validMoves.size());

        if (!validMoves.isEmpty()) {
            for (BoardLocation move : validMoves) {
                if (to.equals(move)) {
                    moveNumber++;
                    System.err.println("moveNumber: " + moveNumber);
                    movePieceAndEndTurn(to);
                    System.out.println("isCheckmate: " + Rules.isCheckmate(currentTurn, board));
                    System.out.println("isStalemate: " + Rules.isStalemate(currentTurn, board));
                    hasGameEnded = Rules.isCheckmate(currentTurn, board) || Rules.isStalemate(currentTurn, board);
                    break;
                }
            }
            validMoves.clear();
            selectedPieceLocation = null;

            FenUtils fenUtils = new FenUtils(board.pieces, board.whiteKingLocation, board.blackKingLocation, board.lastToLocation, board.lastDoublePawnMoveWithWhitePieces, board.lastDoublePawnMoveWithBlackPieces);
            System.out.println(fenUtils.generateFenFromPosition(fenUtils.pieces));
        }
    }
    private void movePieceAndEndTurn(BoardLocation destination) {
        if (destination != null) {
            board.movePiece(selectedPieceLocation, destination);
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }
    public void promotePawn(PieceType selectedPieceType) {
        board.promotePawn(promotionLocation, selectedPieceType);
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
        isPawnPromotionPending = false;
        promotionLocation = null;
        selectedPromotionPieceType = null;
        movePieceAndEndTurn(null);
    }
}