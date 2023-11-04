package lukas.sobotik.sightlessknight.gamelogic;

import lombok.Getter;
import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    @Getter
    public Board board;
    public static Team currentTurn;
    public boolean hasGameEnded = false;
    List<Move> validMoves;
    BoardLocation selectedPieceLocation;
    public static int moveNumber = 0, enPassantCaptures = 0;
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

    public void movePieceAndEndTurn(BoardLocation destination) {
        if (destination != null) {
            board.movePiece(new Move(selectedPieceLocation, destination));
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }
    public void movePieceAndEndTurn(Move move) {
        if (move.getFrom() != null && move.getTo() != null) {
            board.movePiece(move);
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }

    /**
     * Function used for playing a move in the game.
     * @param move which move should be played
     * @param isTest whether the move is being played by a test function like the Perft test or an AI.
     */
    public void playMove(Move move, boolean isTest) {
        Piece piece = move.getMovedPiece();
        if (piece == null) return;
        selectedPieceLocation = move.getFrom();
        validMoves = Rules.getValidMoves(selectedPieceLocation, piece, board, !kinglessGame);

        if (validMoves.isEmpty()) {
            return;
        }

        for (Move validMove : validMoves) {
            if (!move.getTo().equals(validMove.getTo())) {
                continue;
            }

            moveNumber++;
            updateMoveFlagsAndPieces(move, validMove);

            // Pawn Promotion
            if (isPawnPromotion(move, validMove) && (isTest || move.getPromotionPiece() != null)) {
                promotionLocation = move.getTo();
                movePieceAndEndTurn(move);
                promotePawn(move.getPromotionPiece());
                break;
            }
            if ((validMove.getTo().getY() == 0 || validMove.getTo().getY() == 7) && piece.type == PieceType.PAWN && piece.promotion == null && !isTest) {
                promotionLocation = validMove.getTo();
                isPawnPromotionPending = true;
                return;
            }

            // En Passant
            else if (isEnPassantCapture(move, validMove) && isTest) {
                move.setCapturedPiece(board.getPiece(new BoardLocation(move.getTo().getX(), move.getFrom().getY())));
                move.setMoveFlag(MoveFlag.enPassant);
            }

            movePieceAndEndTurn(move);
            if (!isTest) {
                createParsedMoveHistory(move);
                hasGameEnded = Rules.isCheckmate(currentTurn, board) || Rules.isStalemate(currentTurn, board);
            }
            break;
        }
        validMoves.clear();
        selectedPieceLocation = null;
    }
    private void updateMoveFlagsAndPieces(Move move, Move validMove) {
        if (validMove.getMoveFlag() != null && !validMove.getMoveFlag().equals(MoveFlag.none)) {
            move.setMoveFlag(validMove.getMoveFlag());
        }
        if (validMove.getCapturedPiece() != null) {
            move.setCapturedPiece(validMove.getCapturedPiece());
        }
        if (validMove.getMovedPiece() != null) {
            move.setMovedPiece(validMove.getMovedPiece());
        }
    }
    private boolean isEnPassantCapture(Move move, Move validMove) {
        BoardLocation enPassantCapture = new BoardLocation(move.getTo().getX(), move.getFrom().getY());
        return board.getPiece(enPassantCapture) != null
                && board.getPiece(enPassantCapture).type == PieceType.PAWN
                && board.getPiece(enPassantCapture).team != move.getMovedPiece().team
                && move.getMovedPiece().type == PieceType.PAWN
                && move.getFrom().getX() != move.getTo().getX()
                && (board.getPiece(enPassantCapture).doublePawnMoveOnMoveNumber == GameState.moveNumber - 1);
    }
    private boolean isPawnPromotion(Move move, Move validMove) {
        return (move.getTo().getY() == 0 || move.getTo().getY() == 7)
                && move.getMovedPiece().type == PieceType.PAWN
                && move.getPromotionPiece() != null;
    }
    public static int capturedPieces = 0, enPassantCapturesReturned = 0;
    public void undoMove(Move move) {
        if (moveNumber - 1 >= 0) moveNumber -= 1;

        board.undoMove(move);

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
        movePieceAndEndTurn((BoardLocation) null);
    }
}