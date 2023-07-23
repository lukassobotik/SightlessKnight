package lukas.sobotik.sightlessknight.gamelogic;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public Board board;
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
    public void play(Move move) {
        BoardLocation from = move.getFrom(), to = move.getTo();

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
            for (BoardLocation moveLoc : validMoves) {
                if (to.equals(moveLoc)) {
                    moveNumber++;
                    System.err.println("moveNumber: " + moveNumber);

                    if ((moveLoc.getY() == 0 || moveLoc.getY() == 7) && piece.type == PieceType.PAWN && piece.promotion == null) {
                        promotionLocation = to;
                        isPawnPromotionPending = true;
                        return;
                    }

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
    public Board getBoard() {
        return board;
    }
    public void movePieceAndEndTurn(BoardLocation destination) {
        if (destination != null) {
            board.movePiece(selectedPieceLocation, destination);
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }

    /**
     * Function used by tests like the Perft test, generally shouldn't be used to play user moves
     * @param move which move should be played
     */
    public void testsPlayMove(Move move) {
        Piece piece = move.getMovedPiece();
        if (piece == null) return;
        selectedPieceLocation = move.getFrom();
        Rules.getValidMoves(validMoves, selectedPieceLocation, piece, board, true);

        if (!validMoves.isEmpty()) {
            for (BoardLocation validMove : validMoves) {
                if (move.getTo().equals(validMove)) {
                    if ((move.getTo().getY() == 0 || move.getTo().getY() == 7) && piece.type == PieceType.PAWN && move.getPromotionPiece() != null ) {
                        promotionLocation = move.getTo();
                        movePieceAndEndTurn(move.getTo());
                        promotePawn(move.getPromotionPiece());
                        return;
                    }
                    movePieceAndEndTurn(move.getTo());
                    break;
                }
            }
            validMoves.clear();
            selectedPieceLocation = null;
        }
    }
    public int capturedPieces = 0;
    public void undoMove(Move move) {
        board.movePiece(move.getTo(), move.getFrom());
        if (move.getCapturedPiece() != null) {
            capturedPieces++;
            board.pieces[board.getArrayIndexFromLocation(move.getTo())] = move.getCapturedPiece();
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }
    public void createParsedMoveHistory(Move move) {
        boolean isPromotionSquare = (move.getTo().getY() == 0 && move.getMovedPiece().team == Team.BLACK) || (move.getTo().getY() == 7 && move.getMovedPiece().team == Team.WHITE);
        if (move.getMovedPiece().type.equals(PieceType.PAWN) && isPromotionSquare && move.getMovedPiece().promotion == null) return;

        moveHistory.add(new Move(move.getFrom(), move.getTo(), board.getPiece(move.getTo()), move.getCapturedPiece()));
        String parsedMove = new AlgebraicNotationUtils(new FenUtils(board.pieces), this, board).getParsedMove(move);
        parsedMoveHistory.add(parsedMove);

        FenUtils fenUtils = new FenUtils(board.pieces, board.whiteKingLocation, board.blackKingLocation, board.lastToLocation, board.lastDoublePawnMoveWithWhitePieces, board.lastDoublePawnMoveWithBlackPieces);
        fenMoveHistory.add(fenUtils.generateFenFromPosition(fenUtils.pieces));
    }
    public void resetHistory() {
        moveHistory = new ArrayList<>();
        parsedMoveHistory = new ArrayList<>();
        fenMoveHistory = new ArrayList<>();
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