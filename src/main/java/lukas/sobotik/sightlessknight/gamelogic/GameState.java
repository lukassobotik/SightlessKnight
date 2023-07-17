package lukas.sobotik.sightlessknight.gamelogic;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
    public static BoardLocation promotionLocation;
    static PieceType selectedPromotionPieceType;
    public static boolean kinglessGame = false;
    public List<Move> moveHistory;
    public List<String> parsedMoveHistory;
    public List<String> fenMoveHistory;
    public GameState(Board board, Team startingTeam, boolean kinglessGame) {
        validMoves = new ArrayList<>();
        moveHistory = new ArrayList<>();
        parsedMoveHistory = new ArrayList<>();
        fenMoveHistory = new ArrayList<>();
        currentTurn = startingTeam;
        moveNumber = 0;
        GameState.kinglessGame = kinglessGame;
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
        Piece capturedPiece = board.getPiece(to);
        if (piece == null) return;
        if (piece.team == currentTurn || kinglessGame) {
            selectedPieceLocation = from;
            Rules.getValidMoves(validMoves, selectedPieceLocation, piece, board, !kinglessGame);
        }

        if (!validMoves.isEmpty()) {
            for (BoardLocation move : validMoves) {
                if (to.equals(move)) {
                    moveNumber++;
                    System.err.println("moveNumber: " + moveNumber);
                    movePieceAndEndTurn(to);
                    createParsedMoveHistory(new Move(from, to, board.getPiece(to), capturedPiece));
                    hasGameEnded = Rules.isCheckmate(currentTurn, board) || Rules.isStalemate(currentTurn, board);
                    break;
                }
            }
            validMoves.clear();
            selectedPieceLocation = null;
        }
    }
    private void movePieceAndEndTurn(BoardLocation destination) {
        if (destination != null) {
            board.movePiece(selectedPieceLocation, destination);
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }
    private void createParsedMoveHistory(Move move) {
        moveHistory.add(new Move(move.getFrom(), move.getTo(), board.getPiece(move.getTo()), move.getCapturedPiece()));
        String parsedMove = new AlgebraicNotationUtils(new FenUtils(board.pieces), this, board).getParsedMove(move);
        parsedMoveHistory.add(parsedMove);

        FenUtils fenUtils = new FenUtils(board.pieces, board.whiteKingLocation, board.blackKingLocation, board.lastToLocation, board.lastDoublePawnMoveWithWhitePieces, board.lastDoublePawnMoveWithBlackPieces);
        fenMoveHistory.add(fenUtils.generateFenFromPosition(fenUtils.pieces));
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