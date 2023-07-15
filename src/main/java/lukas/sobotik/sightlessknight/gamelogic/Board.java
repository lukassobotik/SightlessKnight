package lukas.sobotik.sightlessknight.gamelogic;

import java.util.HashMap;

public class Board {
    public Piece[] pieces;
    int size;
    int squareSize;
    HashMap<String, Integer> spriteIndexMap;
    BoardLocation whiteKingLocation;
    BoardLocation blackKingLocation;
    BoardLocation lastFromLocation;
    BoardLocation lastToLocation;
    Piece lastRemovedPiece;
    BoardLocation lastDoublePawnMoveWithWhitePieces;
    BoardLocation lastDoublePawnMoveWithBlackPieces;
    FenUtils fenUtils;

    public static final String STARTING_FEN_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public Board(int size, Piece[] pieces, FenUtils fenUtils) {
        this.size = size;
        this.pieces = pieces;
        this.fenUtils = fenUtils;
        squareSize = size / 8;

        whiteKingLocation = getPointFromArrayIndex(fenUtils.getWhiteKingIndex());
        blackKingLocation = getPointFromArrayIndex(fenUtils.getBlackKingIndex());
        fenUtils.whiteKingPosition = whiteKingLocation;
        fenUtils.blackKingPosition = blackKingLocation;

        System.out.println(fenUtils.generateFenFromCurrentPosition());
        printBoardInConsole();

    }
    public void resetBoardPosition() {
        pieces = fenUtils.generatePositionFromFEN(STARTING_FEN_POSITION);

        System.out.println(fenUtils.generateFenFromCurrentPosition());
        printBoardInConsole();
    }
    public void printBoardInConsole() {
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece piece = pieces[index];

                if (piece == null) {
                    System.out.print(". "); // Empty square
                } else {
                    System.out.print(fenUtils.getSymbolFromPieceType(piece.type, piece.team) + " "); // Piece character
                }
            }
            System.out.println(); // Move to the next line for the next rank
        }
    }

    BoardLocation getKing(Team team) {
        return (team == Team.WHITE) ? whiteKingLocation : blackKingLocation;
    }

    public Piece getPiece(BoardLocation boardLocation) {
        if (!isInBounds(boardLocation)) {
            return null;
        }
        return pieces[boardLocation.getX() + boardLocation.getY() * 8];
    }

    public void movePiece(BoardLocation from, BoardLocation to) {
        if (from == null || to == null) return;
        movePieceWithoutSpecialMoves(from, to);

        Piece movedPiece = pieces[to.getX() + to.getY() * 8];
        if (movedPiece == null) return;

        // Move the rook when the king castles
        if (movedPiece.type == PieceType.KING && Math.abs(from.getX() - to.getX()) == 2) {
            // Queenside Castling
            if (from.getX() > to.getX()) {
                movePieceWithoutSpecialMovesAndSave(
                        movedPiece.team == Team.WHITE
                            ? new BoardLocation(0, 0)
                            : new BoardLocation(0, 7),
                        movedPiece.team == Team.WHITE
                            ? new BoardLocation(3, 0)
                            : new BoardLocation(3, 7));
            } // Kingside Castling
            else {
                movePieceWithoutSpecialMovesAndSave(
                        movedPiece.team == Team.WHITE
                            ? new BoardLocation(7, 0)
                            : new BoardLocation(7, 7),
                        movedPiece.team == Team.WHITE
                            ? new BoardLocation(5, 0)
                            : new BoardLocation(5, 7));
            }
        }

        // Save the last move that was a double pawn move
        if (movedPiece.type == PieceType.PAWN && Math.abs(from.getY() - to.getY()) == 2) {
            if (movedPiece.team == Team.WHITE) lastDoublePawnMoveWithWhitePieces = to;
            if (movedPiece.team == Team.BLACK) lastDoublePawnMoveWithBlackPieces = to;
            movedPiece.doublePawnMoveOnMoveNumber = GameState.moveNumber;
        }

        // Handle en passant capture
        BoardLocation enPassantCapture = new BoardLocation(to.getX(), from.getY());
        if (getPiece(enPassantCapture) != null && from.getX() != to.getX() && getPiece(enPassantCapture).doublePawnMoveOnMoveNumber == GameState.moveNumber - 1) {
            removePiece(enPassantCapture);
        }

        // Check for promotion moves
        if ((to.getY() == 7 || to.getY() == 0) && movedPiece.type.equals(PieceType.PAWN)) {
            GameState.promotionLocation = to;
            GameState.isPawnPromotionPending = true;
        }

        movedPiece.hasMoved = true;

        lastFromLocation = from;
        lastToLocation = to;
    }
    public void movePieceWithoutSpecialMovesAndSave(BoardLocation from, BoardLocation to) {
        movePieceWithoutSpecialMoves(from, to);

        lastFromLocation = from;
        lastToLocation = to;
    }
    public void movePieceWithoutSpecialMoves(BoardLocation from, BoardLocation to) {
        if (from == null || to == null) return;
        if (from.equals(whiteKingLocation)) {
            whiteKingLocation = to;
        } else if (from.equals(blackKingLocation)) {
            blackKingLocation = to;
        }

        lastRemovedPiece = pieces[to.getX() + to.getY() * 8];

        pieces[to.getX() + to.getY() * 8] = pieces[from.getX() + from.getY() * 8];
        pieces[from.getX() + from.getY() * 8] = null;
    }
    public void removePiece(BoardLocation boardLocation) {
        if (!isInBounds(boardLocation)) {
            return;
        }
        pieces[boardLocation.getX() + boardLocation.getY() * 8] = null;
    }

    public void undoMove() {
        if (lastFromLocation == null || lastToLocation == null) return;
        Piece temp = lastRemovedPiece;

        movePieceWithoutSpecialMovesAndSave(lastToLocation, lastFromLocation);

        pieces[lastFromLocation.getX() + lastFromLocation.getY() * 8] = temp;
    }

    public BoardLocation getPointFromArrayIndex(int index) {
        int x = index % 8;
        int y = index / 8;
        return new BoardLocation(x, y);
    }

    public void promotePawn(BoardLocation pawnLocation, PieceType selectedPiece) {
        Team team;
        if (pawnLocation.getY() == 7) team = Team.WHITE;
        else team = Team.BLACK;
        Piece piece = new Piece(team, selectedPiece);
        pieces[pawnLocation.getX() + pawnLocation.getY() * 8] = piece;
    }

    public BoardLocation getPoint(int x, int y) {
        return new BoardLocation(x / squareSize, 7 - y / squareSize);
    }

    public Rectangle getRectangle(BoardLocation point) {
        return new Rectangle(point.getX() * squareSize, (point.getY()) * squareSize, squareSize, squareSize);
    }

    public boolean isInBounds(BoardLocation boardLocation) {
        return boardLocation.getX() < 8 && boardLocation.getX() >= 0 && boardLocation.getY() < 8 && boardLocation.getY() >= 0;
    }
}
