package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public Board board;
    public static Team currentTurn;
    public boolean hasGameEnded = false;
    final List<BoardLocation> validMoves;
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

                    movePieceAndEndTurn(move);
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
    public void movePieceAndEndTurn(Move move) {
        if (move.getFrom() != null && move.getTo() != null) {
            board.movePiece(move.getFrom(), move.getTo());
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }

    /**
     * Function used by tests like the Perft test, generally shouldn't be used to play user moves
     * @param move which move should be played
     */
    public void playTestMove(Move move) {
        Piece piece = move.getMovedPiece();
        if (piece == null) return;
        selectedPieceLocation = move.getFrom();
        Rules.getValidMoves(validMoves, selectedPieceLocation, piece, board, true);

        if (!validMoves.isEmpty()) {
            for (BoardLocation validMove : validMoves) {
                if (move.getTo().equals(validMove)) {
                    moveNumber++;
                    // Pawn Promotion
                    if ((move.getTo().getY() == 0 || move.getTo().getY() == 7) && piece.type == PieceType.PAWN && move.getPromotionPiece() != null ) {
                        promotionLocation = move.getTo();
                        movePieceAndEndTurn(move);
                        promotePawn(move.getPromotionPiece());
                        break;
                    }
                    // En Passant
                    BoardLocation enPassantCapture = new BoardLocation(move.getTo().getX(), move.getFrom().getY());
                    if (board.getPiece(enPassantCapture) != null
                            && board.getPiece(enPassantCapture).type == PieceType.PAWN
                            && board.getPiece(enPassantCapture).team != move.getMovedPiece().team
                            && move.getMovedPiece().type == PieceType.PAWN
                            && move.getFrom().getX() != move.getTo().getX()
                            && (board.getPiece(enPassantCapture).doublePawnMoveOnMoveNumber == GameState.moveNumber - 1)) {
                        move.setCapturedPiece(board.getPiece(enPassantCapture));
                        move.setMoveFlag(MoveFlag.enPassant);
                    } else if (move.getMovedPiece().type == PieceType.KING && Math.abs(move.getFrom().getX() - move.getTo().getX()) == 2) {
                        move.setMoveFlag(MoveFlag.castling);
                    } else {
                        move.setMoveFlag(MoveFlag.none);
                    }
                    movePieceAndEndTurn(move);
                    break;
                }
            }
            validMoves.clear();
            selectedPieceLocation = null;
        }
    }
    public int capturedPieces = 0, enPassantCapturesReturned = 0;
    public void undoMove(Move move) {
        if (moveNumber - 1 >= 0) moveNumber -= 1;
        else return;

        board.movePieceWithoutSpecialMovesAndSave(move.getTo(), move.getFrom());
        if (move.getCapturedPiece() != null) {
            capturedPieces++;
            int pieceIndex = board.getArrayIndexFromLocation(move.getTo());
            if (move.getMoveFlag().equals(MoveFlag.enPassant)) {
                pieceIndex = board.getArrayIndexFromLocation(move.getTo().transpose(0, (move.getMovedPiece().team == Team.WHITE ? -1 : 1)));
                if (!board.isInBounds(move.getTo().transpose(0, (move.getMovedPiece().team == Team.WHITE ? -1 : 1)))) return;
                enPassantCapturesReturned++;
            }
            board.pieces[pieceIndex] = move.getCapturedPiece();
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
        movePieceAndEndTurn((BoardLocation) null);
    }
}