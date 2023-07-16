package lukas.sobotik.sightlessknight.gamelogic;

import java.util.ArrayList;
import java.util.List;

public class AlgebraicNotationUtils {
    Move move;
    FenUtils fenUtils;
    Board board;
    GameState gameState;
    public AlgebraicNotationUtils(Move move, FenUtils fenUtils, GameState gameState, Board board) {
        this.move = move;
        this.fenUtils = fenUtils;
        this.gameState = gameState;
        this.board = board;
    }
    public AlgebraicNotationUtils(FenUtils fenUtils, GameState gameState, Board board) {
        this.fenUtils = fenUtils;
        this.gameState = gameState;
        this.board = board;
    }

    public String getParsedMove(Move move) {
        BoardLocation from = move.getFrom(),
                      to = move.getTo();
        Piece movedPiece = move.getMovedPiece(),
              capturedPiece = move.getCapturedPiece();

        String algebraicNotationMove = "";
        Team opponentTeam = movedPiece.team == Team.WHITE ? Team.BLACK : Team.WHITE;

        switch (movedPiece.type) {
            case PAWN -> {
                if (movedPiece.enPassant) {
                    algebraicNotationMove = from.getAlgebraicNotationLocation().charAt(0) + "x" + to.getAlgebraicNotationLocation() + " e.p.";
                    board.getPiece(to).enPassant = false;
                } else if (capturedPiece == null) {
                    algebraicNotationMove = to.getAlgebraicNotationLocation();
                } else {
                    algebraicNotationMove = from.getAlgebraicNotationLocation().charAt(0) + "x" + to.getAlgebraicNotationLocation();
                }

                if (movedPiece.promotion != null) {
                    algebraicNotationMove += "=" + fenUtils.getSymbolFromPieceType(movedPiece.promotion, Team.WHITE);
                }
            }
            case KNIGHT -> algebraicNotationMove = disambiguatePieceMoves(move, PieceType.KNIGHT);
            case BISHOP -> algebraicNotationMove = disambiguatePieceMoves(move, PieceType.BISHOP);
            case ROOK -> algebraicNotationMove = disambiguatePieceMoves(move, PieceType.ROOK);
            case QUEEN -> algebraicNotationMove = disambiguatePieceMoves(move, PieceType.QUEEN);
            case KING -> {
                String pieceSymbol = String.valueOf(fenUtils.getSymbolFromPieceType(PieceType.KING, Team.WHITE));

                if (movedPiece.castling != null) {
                    algebraicNotationMove = movedPiece.castling;
                    board.getPiece(to).castling = null;
                    break;
                }
                if (capturedPiece == null) {
                    algebraicNotationMove = pieceSymbol + to.getAlgebraicNotationLocation();
                } else {
                    algebraicNotationMove = pieceSymbol + "x" + to.getAlgebraicNotationLocation();
                }
            }
        }

        if (Rules.isCheckmate(opponentTeam, board)) {
            algebraicNotationMove += "#";
            return algebraicNotationMove;
        }

        if (Rules.isKingInCheck(opponentTeam, board)) {
            algebraicNotationMove += "+";
        }

        return algebraicNotationMove;
    }

    private String disambiguatePieceMoves(Move move, PieceType pieceType) {
        BoardLocation from = move.getFrom(),
                to = move.getTo();
        Piece movedPiece = move.getMovedPiece(),
                capturedPiece = move.getCapturedPiece();

        String pieceSymbol = String.valueOf(fenUtils.getSymbolFromPieceType(pieceType, Team.WHITE));

        String normalMove;
        if (capturedPiece == null) {
            normalMove = pieceSymbol + to.getAlgebraicNotationLocation();
        } else {
            normalMove = pieceSymbol + "x" + to.getAlgebraicNotationLocation();
        }

        if (movedPiece.type != pieceType) return normalMove;

        int otherPieceIndex = -1;
        for (int i = 0; i < board.pieces.length; i++) {
            Piece piece = board.pieces[i];
            if (piece == null) continue;
            if (piece.type == pieceType && piece.team == movedPiece.team && i != board.getArrayIndexFromLocation(to)) {
                otherPieceIndex = i;
                break;
            }
        }
        if (otherPieceIndex < 0) return normalMove;
        Piece otherPiece = board.pieces[otherPieceIndex];

        List<BoardLocation> otherPieceMoves = new ArrayList<>();
        Rules.getAllMoves(otherPieceMoves, board.getPointFromArrayIndex(otherPieceIndex), otherPiece, board);
        if (otherPieceMoves.contains(to)) {
            if (board.getPointFromArrayIndex(otherPieceIndex).isOnSameDiagonalAs(from)) {
                return pieceSymbol + from.getAlgebraicNotationLocation() + to.getAlgebraicNotationLocation();
            } else if (board.getPointFromArrayIndex(otherPieceIndex).isOnSameFileAs(from)) {
                return pieceSymbol + from.getAlgebraicNotationLocation().charAt(1) + to.getAlgebraicNotationLocation();
            } else if (board.getPointFromArrayIndex(otherPieceIndex).isOnSameRankAs(from)) {
                return pieceSymbol + from.getAlgebraicNotationLocation().charAt(0) + to.getAlgebraicNotationLocation();
            }
        }
        return normalMove;
    }
}
