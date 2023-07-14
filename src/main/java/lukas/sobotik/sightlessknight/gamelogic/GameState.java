package lukas.sobotik.sightlessknight.gamelogic;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    Board board;
    public Team currentTurn;
    public boolean hasGameEnded = false;
    final List<BoardLocation> validMoves;
    BoardLocation selectedPieceLocation;
    static int moveNumber = 0;
    static final Team playerTeam = Team.WHITE;
    static boolean isPawnPromotionPending = false;
    static BoardLocation promotionLocation;
    static PieceType selectedPromotionPieceType;

    public GameState(Board board) {
        validMoves = new ArrayList<>();
        currentTurn = Team.WHITE;
        this.board = board;
    }

    public void onSquareClick(BoardLocation location) {
        System.out.println("clikc");
        Piece piece = board.getPiece(location);
        if (piece == null) return;
        if (piece.team == currentTurn) {
            selectedPieceLocation = location;
            Rules.getValidMoves(validMoves, selectedPieceLocation, piece, board);
        }
        validMoves.forEach(loc -> System.out.println("valid: " + loc.getStringLocation()));
    }

    public void play(BoardLocation from, BoardLocation to) {
        if (hasGameEnded) {
            if (Rules.isStalemate(currentTurn, board)) {
            }
            if (Rules.isCheckmate(currentTurn, board)) {
            }
            return;
        }
        if (isPawnPromotionPending) {
            return;
        }

        Piece piece = board.getPiece(from);
        if (piece == null) return;
        if (piece.team == currentTurn) {
            selectedPieceLocation = from;
            validMoves.addAll(Rules.getValidMoves(validMoves, selectedPieceLocation, piece, board));
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
            System.out.println(fenUtils.generateFenFromCurrentPosition());
        }
    }
    private void movePieceAndEndTurn(BoardLocation destination) {
        if (destination != null) {
            board.movePiece(selectedPieceLocation, destination);
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }
    private void showPawnPromotionDialog() {

    }
    private void promotePawn(PieceType selectedPieceType) {
        board.promotePawn(promotionLocation, selectedPieceType);
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
        isPawnPromotionPending = false;
        promotionLocation = null;
        selectedPromotionPieceType = null;
        movePieceAndEndTurn(null);
    }
    public boolean touchUp(int x, int y, int pointer, int button) {
        if (isPawnPromotionPending && promotionLocation != null) {
            showPawnPromotionDialog();
        }
        return false;
    }

    public boolean touchDragged(int x, int y, int pointer) {
        return false;
    }

    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}