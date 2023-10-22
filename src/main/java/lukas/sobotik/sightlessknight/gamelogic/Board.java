package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

public class Board {
    public Piece[] pieces;
    int size;
    int squareSize;
    BoardLocation whiteKingLocation;
    BoardLocation blackKingLocation;
    FenUtils fenUtils;

    Piece lastRemovedPiece, lastMovedPiece;
    BoardLocation lastFromLocation, lastToLocation;
    BoardLocation lastDoublePawnMoveWithWhitePieces, lastDoublePawnMoveWithBlackPieces;
    public Board(int size, Piece[] pieces, FenUtils fenUtils) {
        this.size = size;
        this.pieces = pieces;
        this.fenUtils = fenUtils;
        squareSize = size / 8;

        whiteKingLocation = getPointFromArrayIndex(fenUtils.getWhiteKingIndex());
        blackKingLocation = getPointFromArrayIndex(fenUtils.getBlackKingIndex());
        fenUtils.whiteKingPosition = whiteKingLocation;
        fenUtils.blackKingPosition = blackKingLocation;
    }
    public void resetBoardPosition(String startPosition) {
        pieces = fenUtils.generatePositionFromFEN(startPosition);
        GameState.currentTurn = fenUtils.getStartingTeam();

        System.out.println(fenUtils.generateFenFromPosition(fenUtils.pieces));
        printBoardInConsole(false);
    }
    public void printBoardInConsole(boolean specialCharacters) {
        printBoardInConsole(specialCharacters, pieces);
    }
    public void printBoardInConsole(boolean specialCharacters, Piece[] pieces) {
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece piece = pieces[index];

                if (piece == null) {
                    System.out.print(". "); // Empty square
                } else {
                    System.out.print((specialCharacters ? getPieceSymbol(piece.type, piece.team) : fenUtils.getSymbolFromPieceType(piece.type, piece.team)) + " "); // Piece character
                }
            }
            System.out.println(); // Move to the next line for the next rank
        }
    }
    private String getPieceSymbol(PieceType type, Team team) {
        String s = "";
        if (team == Team.WHITE) {
            switch (type) {
                case PAWN -> s = "♟";
                case KNIGHT-> s = "♞";
                case BISHOP -> s = "♝";
                case ROOK -> s = "♜";
                case QUEEN -> s = "♛";
                case KING -> s = "♚";
            }
        } else {
            switch (type) {
                case PAWN -> s = "♙";
                case KNIGHT-> s = "♘";
                case BISHOP -> s = "♗";
                case ROOK -> s = "♖";
                case QUEEN -> s = "♕";
                case KING -> s = "♔";
            }
        }

        return s;
    }

    public BoardLocation getKing(Team team) {
        return (team == Team.WHITE) ? whiteKingLocation : blackKingLocation;
    }

    public Piece getPiece(BoardLocation boardLocation) {
        if (!isInBounds(boardLocation)) {
            return null;
        }
        return pieces[boardLocation.getX() + boardLocation.getY() * 8];
    }

    public void movePiece(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        if (from == null || to == null) return;
        movePieceWithoutSpecialMoves(from, to);

        Piece movedPiece;
        if (move.getMovedPiece() != null) {
            movedPiece = move.getMovedPiece();
        } else {
            movedPiece = pieces[to.getX() + to.getY() * 8];
        }
        if (movedPiece == null) return;

        lastMovedPiece = movedPiece.toBuilder()
                .build();

//        // Move the rook when the king castles
//        if (movedPiece.type == PieceType.KING && Math.abs(from.getX() - to.getX()) == 2) {
//            // Queenside Castling
//            if (from.getX() > to.getX()) {
//                movePieceWithoutSpecialMovesAndSave(
//                        movedPiece.team == Team.WHITE
//                            ? new BoardLocation(0, 0) // White Rook From
//                            : new BoardLocation(0, 7), // Black Rook From
//                        movedPiece.team == Team.WHITE
//                            ? new BoardLocation(3, 0) // White Rook To
//                            : new BoardLocation(3, 7)); // Black Rook To
//                movedPiece.setCastling("O-O-O");
//            } // Kingside Castling
//            else {
//                movePieceWithoutSpecialMovesAndSave(
//                        movedPiece.team == Team.WHITE
//                            ? new BoardLocation(7, 0) // White Rook From
//                            : new BoardLocation(7, 7), // Black Rook From
//                        movedPiece.team == Team.WHITE
//                            ? new BoardLocation(5, 0) // White Rook To
//                            : new BoardLocation(5, 7)); //Black Rook To
//                movedPiece.setCastling("O-O");
//            }
//        }

        var isCastling = movedPiece.type == PieceType.KING && Math.abs(from.getX() - to.getX()) == 2;

        // Queenside Castling
        if (move.getMoveFlag().equals(MoveFlag.queensideCastling)) {
            movePieceWithoutSpecialMovesAndSave(
                    movedPiece.team == Team.WHITE
                            ? new BoardLocation(0, 0) // White Rook From
                            : new BoardLocation(0, 7), // Black Rook From
                    movedPiece.team == Team.WHITE
                            ? new BoardLocation(3, 0) // White Rook To
                            : new BoardLocation(3, 7)); // Black Rook To
        } // Kingside Castling
        else if (move.getMoveFlag().equals(MoveFlag.kingsideCastling)) {
            movePieceWithoutSpecialMovesAndSave(
                    movedPiece.team == Team.WHITE
                            ? new BoardLocation(7, 0) // White Rook From
                            : new BoardLocation(7, 7), // Black Rook From
                    movedPiece.team == Team.WHITE
                            ? new BoardLocation(5, 0) // White Rook To
                            : new BoardLocation(5, 7)); //Black Rook To
        }

        // Save the last move that was a double pawn move
        saveDoublePawnMove(from, to, movedPiece);

        // Handle en passant capture
        BoardLocation enPassantCapture = new BoardLocation(to.getX(), from.getY());
        if (getPiece(enPassantCapture) != null
                && getPiece(enPassantCapture).type == PieceType.PAWN
                && getPiece(enPassantCapture).team != movedPiece.team
                && movedPiece.type == PieceType.PAWN
                && from.getX() != to.getX()
                && (getPiece(enPassantCapture).doublePawnMoveOnMoveNumber == GameState.moveNumber - 1)) {
            movedPiece.enPassant = true;
            removePiece(enPassantCapture);
            GameState.enPassantCaptures++;
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
    private void saveDoublePawnMove(BoardLocation from, BoardLocation to, Piece movedPiece) {
        if (movedPiece == null) return;
        if (movedPiece.type == PieceType.PAWN && Math.abs(from.getY() - to.getY()) == 2) {
            if (movedPiece.team == Team.WHITE) lastDoublePawnMoveWithWhitePieces = to;
            if (movedPiece.team == Team.BLACK) lastDoublePawnMoveWithBlackPieces = to;
            movedPiece.doublePawnMoveOnMoveNumber = GameState.moveNumber;
        }
    }
    public void movePieceWithoutSpecialMovesAndSave(BoardLocation from, BoardLocation to) {
        var movedPiece = getPiece(from);
        movePieceWithoutSpecialMoves(from, to);

        // Save the last move that was a double pawn move
        saveDoublePawnMove(from, to, movedPiece);

        lastFromLocation = from;
        lastToLocation = to;
    }
    public void movePieceWithoutSpecialMoves(BoardLocation from, BoardLocation to) {
        if (from == null || to == null || from.equals(to) || getPiece(from) == null) return;
        if (from.equals(whiteKingLocation)) {
            whiteKingLocation = to;
        } else if (from.equals(blackKingLocation)) {
            blackKingLocation = to;
        }

        lastRemovedPiece = pieces[to.getX() + to.getY() * 8];

        pieces[getArrayIndexFromLocation(to)] = pieces[getArrayIndexFromLocation(from)];
        removePiece(from);
    }
    public void removePiece(BoardLocation boardLocation) {
        if (!isInBounds(boardLocation)) {
            return;
        }
        int index = getArrayIndexFromLocation(boardLocation);
        pieces[index] = null;
    }

    public void undoLastMove() {
        if (lastFromLocation == null || lastToLocation == null) return;
        Piece temp = lastRemovedPiece;

        movePieceWithoutSpecialMovesAndSave(lastToLocation, lastFromLocation);

        pieces[lastFromLocation.getX() + lastFromLocation.getY() * 8] = temp;
    }

    public void undoMove(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        var movedPiece = move.getMovedPiece();
        var capturedPiece = move.getCapturedPiece();
        var moveFlag = move.getMoveFlag();

//        if (move.getMovedPiece().type == PieceType.KING
//                && Math.abs(move.getFrom().getX() - move.getTo().getX()) == 2
//                && move.getTo().getX() > move.getFrom().getX()) {
//            printBoardInConsole(true);
//        }
        movePieceWithoutSpecialMovesAndSave(to, from);
//        if (move.getMovedPiece().type == PieceType.KING
//                && Math.abs(move.getFrom().getX() - move.getTo().getX()) == 2
//                && move.getTo().getX() > move.getFrom().getX()) {
//            printBoardInConsole(true);
//        }
        if (pieces[getArrayIndexFromLocation(from)] != null) {
            pieces[getArrayIndexFromLocation(from)].hasMoved = lastMovedPiece.hasMoved;
        }

        if (capturedPiece != null) {
            GameState.capturedPieces++;
            int pieceIndex = getArrayIndexFromLocation(to);
            // En Passant
            if (moveFlag.equals(MoveFlag.enPassant)) {
                pieceIndex = getArrayIndexFromLocation(to.transpose(0, (movedPiece.team == Team.WHITE ? -1 : 1)));
                if (!isInBounds(to.transpose(0, (movedPiece.team == Team.WHITE ? -1 : 1)))) return;
                GameState.enPassantCapturesReturned++;
            }
            pieces[pieceIndex] = capturedPiece;
        }

        // Undo a promotion
        if (move.getPromotionPiece() != null) {
            pieces[getArrayIndexFromLocation(from)] = movedPiece;
        }

        // Move the rooks back to the original position when castled
        if (
                move.getMovedPiece().type == PieceType.KING
                && Math.abs(move.getFrom().getX() - move.getTo().getX()) == 2) {
            var team = move.getMovedPiece().team;
            var kingLocation = team == Team.WHITE ? new BoardLocation(4, 0) : new BoardLocation(4, 7);
            // Kingside Castling
            if (move.getTo().getX() > move.getFrom().getX()) {
                var rookLocation = team == Team.WHITE ? new BoardLocation(5, 0) : new BoardLocation(5, 7);
                var originalRookLocation = team == Team.WHITE ? new BoardLocation(7, 0) : new BoardLocation(7, 7);
                var castleMove = new Move(rookLocation, originalRookLocation, new Piece(team, PieceType.ROOK));
//                printBoardInConsole(true);
                pieces[getArrayIndexFromLocation(kingLocation)].castling = null;
                movePiece(castleMove);
                pieces[getArrayIndexFromLocation(kingLocation)].hasMoved = false;
                pieces[getArrayIndexFromLocation(originalRookLocation)].hasMoved = false;
            }
            // Queenside castling
            else if (move.getTo().getX() < move.getFrom().getX()) {
                var rookLocation = team == Team.WHITE ? new BoardLocation(3, 0) : new BoardLocation(3, 7);
                var originalRookLocation = team == Team.WHITE ? new BoardLocation(0, 0) : new BoardLocation(3, 7);
                var castleMove = new Move(rookLocation, originalRookLocation, new Piece(team, PieceType.ROOK));
                if (getPiece(kingLocation) != null) pieces[getArrayIndexFromLocation(kingLocation)].castling = null;
                movePiece(castleMove);
                if (getPiece(kingLocation) != null) pieces[getArrayIndexFromLocation(kingLocation)].hasMoved = false;
                if (getPiece(originalRookLocation) != null) pieces[getArrayIndexFromLocation(originalRookLocation)].hasMoved = false;
            }
        }
    }

    public void playEnPassant(Move move) {
        var from = move.getFrom();
        var to = move.getTo();

        movePieceWithoutSpecialMoves(from, to);
        removePiece(new BoardLocation(to.getX(), from.getY()));
    }

    public void undoEnPassant(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        var enPassantCapture = new BoardLocation(to.getX(), from.getY());

        movePieceWithoutSpecialMoves(to, from);
        pieces[getArrayIndexFromLocation(enPassantCapture)] = move.getCapturedPiece();
    }

    public BoardLocation getPointFromArrayIndex(int index) {
        int x = index % 8;
        int y = index / 8;
        return new BoardLocation(x, y);
    }

    public int getArrayIndexFromLocation(BoardLocation location) {
        return location.getX() + location.getY() * 8;
    }

    public void promotePawn(BoardLocation pawnLocation, PieceType selectedPiece) {
        Team team;
        if (pawnLocation.getY() == 7) team = Team.WHITE;
        else team = Team.BLACK;
        Piece piece = new Piece(team, selectedPiece);
        pieces[pawnLocation.getX() + pawnLocation.getY() * 8] = piece;
    }

    public boolean isInBounds(BoardLocation boardLocation) {
        return boardLocation.getX() < 8 && boardLocation.getX() >= 0 && boardLocation.getY() < 8 && boardLocation.getY() >= 0;
    }
}
