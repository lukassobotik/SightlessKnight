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

    public MoveHistoryStack whiteKingMoves = new MoveHistoryStack(false);
    public MoveHistoryStack whiteKingRookMoves = new MoveHistoryStack(false);
    public MoveHistoryStack whiteQueenRookMoves = new MoveHistoryStack(false);
    public MoveHistoryStack blackKingMoves = new MoveHistoryStack(false);
    public MoveHistoryStack blackKingRookMoves = new MoveHistoryStack(false);
    public MoveHistoryStack blackQueenRookMoves = new MoveHistoryStack(false);

    public Bitboard bitboard;
    /**
     * Initializes a new instance of the Board class.
     * @param size The size of the board.
     * @param pieces An array of pieces on the board.
     * @param fenUtils  An instance of FenUtils class.
     */
    public Board(int size, Piece[] pieces, FenUtils fenUtils) {
        this.size = size;
        this.pieces = pieces;
        this.fenUtils = fenUtils;
        squareSize = size / 8;

        bitboard = new Bitboard();

        whiteKingLocation = getPointFromArrayIndex(fenUtils.getWhiteKingIndex());
        blackKingLocation = getPointFromArrayIndex(fenUtils.getBlackKingIndex());
        fenUtils.whiteKingPosition = whiteKingLocation;
        fenUtils.blackKingPosition = blackKingLocation;

        for (int i = 0; i < 64; i++) {
            Piece piece = pieces[i];
            if (piece != null) {
                bitboard.setPiece(piece.type, piece.team, i);
            }
        }
    }

    /**
     * Copy constructor for the Board class.
     * Creates a new Board object by copying the properties of the given Board object.
     *
     * @param copyBoard The Board object to be copied.
     */
    public Board(Board copyBoard) {
    this.size = copyBoard.size;
    this.squareSize = copyBoard.squareSize;
    this.fenUtils = copyBoard.fenUtils; // Assuming fenUtils doesn't need to be deep copied
    this.whiteKingLocation = new BoardLocation(copyBoard.whiteKingLocation);
    this.blackKingLocation = new BoardLocation(copyBoard.blackKingLocation);
    this.lastRemovedPiece = copyBoard.lastRemovedPiece != null ? new Piece(copyBoard.lastRemovedPiece) : null;
    this.lastMovedPiece = copyBoard.lastMovedPiece != null ? new Piece(copyBoard.lastMovedPiece) : null;
    this.lastFromLocation = copyBoard.lastFromLocation != null ? new BoardLocation(copyBoard.lastFromLocation) : null;
    this.lastToLocation = copyBoard.lastToLocation != null ? new BoardLocation(copyBoard.lastToLocation) : null;
    this.lastDoublePawnMoveWithWhitePieces = copyBoard.lastDoublePawnMoveWithWhitePieces != null ? new BoardLocation(copyBoard.lastDoublePawnMoveWithWhitePieces) : null;
    this.lastDoublePawnMoveWithBlackPieces = copyBoard.lastDoublePawnMoveWithBlackPieces != null ? new BoardLocation(copyBoard.lastDoublePawnMoveWithBlackPieces) : null;
    this.whiteKingMoves = new MoveHistoryStack(copyBoard.whiteKingMoves);
    this.whiteKingRookMoves = new MoveHistoryStack(copyBoard.whiteKingRookMoves);
    this.whiteQueenRookMoves = new MoveHistoryStack(copyBoard.whiteQueenRookMoves);
    this.blackKingMoves = new MoveHistoryStack(copyBoard.blackKingMoves);
    this.blackKingRookMoves = new MoveHistoryStack(copyBoard.blackKingRookMoves);
    this.blackQueenRookMoves = new MoveHistoryStack(copyBoard.blackQueenRookMoves);

    // Deep copy of pieces array
    this.pieces = new Piece[copyBoard.pieces.length];
    for (int i = 0; i < copyBoard.pieces.length; i++) {
        if (copyBoard.pieces[i] != null) {
            this.pieces[i] = new Piece(copyBoard.pieces[i]);
        }
    }
}

    /**
     * Resets the board position based on the given starting position.
     * @param startPosition The starting position in Forsyth-Edwards Notation (FEN) format.
     */
    public void resetBoardPosition(String startPosition) {
        pieces = fenUtils.generatePositionFromFEN(startPosition);
        GameState.currentTurn = fenUtils.getStartingTeam();

        System.out.println(fenUtils.generateFenFromPosition(fenUtils.pieces));
        printBoardInConsole(false);
    }

    /**
     * Prints the board in the console.
     * @param specialCharacters true to use special chess characters, false to use regular characters.
     */
    public void printBoardInConsole(boolean specialCharacters) {
        printBoardInConsole(specialCharacters, pieces);
    }

    /**
     * Prints the board in the console.
     *
     * @param specialCharacters true to use special chess characters, false to use regular characters.
     * @param pieces the array of pieces representing the chess board.
     */
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

    /**
     * Returns the symbol representation of a chess piece.
     *
     * @param type the type of the chess piece.
     * @param team the team of the chess piece.
     * @return the symbol representation of the chess piece.
     */
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

    /**
     * Returns the location of the king for the given team.
     *
     * @param team the team whose king's location is to be retrieved
     * @return the location of the king for the given team
     */
    public BoardLocation getKing(Team team) {
        return (team == Team.WHITE) ? whiteKingLocation : blackKingLocation;
    }

    /**
     * Returns the piece at the specified board location.
     *
     * @param boardLocation the location on the board to retrieve the piece from
     * @return the piece at the specified board location, or null if the location is out of bounds
     */
    public Piece getPiece(BoardLocation boardLocation) {
        if (!isInBounds(boardLocation)) {
            return null;
        }
        return pieces[boardLocation.getX() + boardLocation.getY() * 8];
    }

    /**
     * Moves a piece on the board based on the provided move.
     *
     * @param move the move object containing the from and to locations
     */
    public void movePiece(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        if (from == null || to == null) return;
        movePieceWithoutSpecialMoves(move);

        Piece movedPiece;
        if (move.getMovedPiece() != null) {
            movedPiece = move.getMovedPiece();
        } else {
            movedPiece = pieces[to.getX() + to.getY() * 8];
        }
        if (movedPiece == null) return;

        addMovedPieceListItemForCastling(move);
        lastMovedPiece = movedPiece.toBuilder()
                .build();

        handleCastling(move, movedPiece);

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

        movedPiece.index = getArrayIndexFromLocation(to);

        lastFromLocation = from;
        lastToLocation = to;
    }

    /**
     * Saves the last move that was a double pawn move.
     *
     * @param from the starting location of the move
     * @param to the destination location of the move
     * @param movedPiece the piece that was moved
     */
    private void saveDoublePawnMove(BoardLocation from, BoardLocation to, Piece movedPiece) {
        if (movedPiece == null) return;
        if (movedPiece.type == PieceType.PAWN && Math.abs(from.getY() - to.getY()) == 2) {
            if (movedPiece.team == Team.WHITE) lastDoublePawnMoveWithWhitePieces = to;
            if (movedPiece.team == Team.BLACK) lastDoublePawnMoveWithBlackPieces = to;
            movedPiece.doublePawnMoveOnMoveNumber = GameState.moveNumber;
        }
    }

    /**
     * Moves a piece on the board without executing any special moves and saves the move.
     *
     * @param from the starting location of the move
     * @param to the destination location of the move
     */
    public void movePieceWithoutSpecialMovesAndSave(BoardLocation from, BoardLocation to) {
        movePieceWithoutSpecialMovesAndSave(new Move(from, to, getPiece(from), getPiece(to)));
    }

    /**
     * Moves a piece on the board without executing any special moves and saves the move.
     *
     * @param move the move object containing the starting and destination locations of the move,
     *             as well as the piece being moved
     */
    public void movePieceWithoutSpecialMovesAndSave(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        var movedPiece = getPiece(from);
        movePieceWithoutSpecialMoves(move);

        // Save the last move that was a double pawn move
        saveDoublePawnMove(from, to, movedPiece);

        lastFromLocation = from;
        lastToLocation = to;
    }

    /**
     * Handles castling moves on the chess board.
     *
     * @param move the move object representing the castling move
     * @param movedPiece the piece that is being moved
     */
    private void handleCastling(Move move, Piece movedPiece) {
        // Queenside Castling
        if (move.getMoveFlag().equals(MoveFlag.queensideCastling)) {
            movePieceWithoutSpecialMovesAndSave(
                    movedPiece.team == Team.WHITE
                    ? new BoardLocation(0, 0) // White Rook From
                    : new BoardLocation(0, 7), // Black Rook From
                    movedPiece.team == Team.WHITE
                    ? new BoardLocation(3, 0) // White Rook To
                    : new BoardLocation(3, 7)); // Black Rook To
            movedPiece.setCastling("O-O-O");
        } // Kingside Castling
        else if (move.getMoveFlag().equals(MoveFlag.kingsideCastling)) {
            movePieceWithoutSpecialMovesAndSave(
                    movedPiece.team == Team.WHITE
                    ? new BoardLocation(7, 0) // White Rook From
                    : new BoardLocation(7, 7), // Black Rook From
                    movedPiece.team == Team.WHITE
                    ? new BoardLocation(5, 0) // White Rook To
                    : new BoardLocation(5, 7)); //Black Rook To
            movedPiece.setCastling("O-O");
        }
    }

    /**
     * Adds the moved piece to the respective list for tracking castling moves.
     *
     * @param move the move object representing the castling move
     */
    private void addMovedPieceListItemForCastling(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        var movedPiece = move.getMovedPiece();
        if (movedPiece == null) return;

        if (movedPiece.type == PieceType.KING) {
            if (movedPiece.team == Team.WHITE) {
                whiteKingMoves.setValue(movedPiece.hasMoved);
            } else {
                blackKingMoves.setValue(movedPiece.hasMoved);
            }
        }
        if (movedPiece.type == PieceType.ROOK) {
            if (movedPiece.team == Team.WHITE) {
                if (from.getX() == 0) {
                    whiteQueenRookMoves.setValue(movedPiece.hasMoved);
                } else if (from.getX() == 7) {
                    whiteKingRookMoves.setValue(movedPiece.hasMoved);
                }
            } else {
                if (from.getX() == 0) {
                    blackQueenRookMoves.setValue(movedPiece.hasMoved);
                } else if (from.getX() == 7) {
                    blackKingRookMoves.setValue(movedPiece.hasMoved);
                }
            }
        }
    }

    /**
     * Removes the moved piece from the respective list for tracking castling moves.
     *
     * @param move the move object representing the castling move
     */
    private void removeMovedPieceListItemForCastling(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        var movedPiece = move.getMovedPiece();
        if (movedPiece == null) return;

        if (movedPiece.type == PieceType.KING) {
            if (movedPiece.team == Team.WHITE) {
                whiteKingMoves.goBack();
            } else if (movedPiece.team == Team.BLACK) {
                blackKingMoves.goBack();
            }
        }
        if (movedPiece.type == PieceType.ROOK) {
            if (movedPiece.team == Team.WHITE) {
                if (from.getX() == 0) {
                    whiteQueenRookMoves.goBack();
                } else if (from.getX() == 7) {
                    whiteKingRookMoves.goBack();
                }
            } else {
                if (from.getX() == 0) {
                    blackQueenRookMoves.goBack();
                } else if (from.getX() == 7) {
                    blackKingRookMoves.goBack();
                }
            }
        }
    }

    /**
     * Moves the piece on the chessboard without considering any special moves.
     *
     * @param move the move object representing the move
     */
    public void movePieceWithoutSpecialMoves(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        if (from == null || to == null || from.equals(to) || getPiece(from) == null) return;
        if (from.equals(whiteKingLocation)) {
            whiteKingLocation = to;
        } else if (from.equals(blackKingLocation)) {
            blackKingLocation = to;
        }

        if (move.getCapturedPiece() != null) {
            lastRemovedPiece = move.getCapturedPiece();
        } else if (pieces[to.getX() + to.getY() * 8] != null) {
            lastRemovedPiece = pieces[to.getX() + to.getY() * 8];
        } else {
            lastRemovedPiece = null;
        }

        pieces[getArrayIndexFromLocation(to)] = pieces[getArrayIndexFromLocation(from)];
        removePiece(from);

        // Update the Bitboard
        bitboard.removePiece(move.getMovedPiece().type, move.getMovedPiece().team, getArrayIndexFromLocation(move.getFrom()));
        bitboard.setPiece(move.getMovedPiece().type, move.getMovedPiece().team, getArrayIndexFromLocation(move.getTo()));
        bitboard.updateControlledSquares(Team.WHITE, this);
        bitboard.updateControlledSquares(Team.BLACK, this);
    }

    /**
     * Removes a piece from the chessboard at the specified board location.
     *
     * @param boardLocation the board location of the piece to be removed
     */
    public void removePiece(BoardLocation boardLocation) {
        if (!isInBounds(boardLocation)) {
            return;
        }
        int index = getArrayIndexFromLocation(boardLocation);
        pieces[index] = null;

        Piece piece = pieces[getArrayIndexFromLocation(boardLocation)];
        if (piece != null) {
            bitboard.removePiece(piece.type, piece.team, getArrayIndexFromLocation(boardLocation));
        }
    }

    /**
     * Undoes the last move on the chessboard.
     * If there was no previous move, the method returns without doing anything.
     * The method restores the piece that was previously removed and moves it back to its original location.
     * The special move flags for the moved piece are not affected.
     */
    public void undoLastMove() {
        if (lastFromLocation == null || lastToLocation == null) return;
        Piece temp = lastRemovedPiece;

        movePieceWithoutSpecialMovesAndSave(lastToLocation, lastFromLocation);

        pieces[lastFromLocation.getX() + lastFromLocation.getY() * 8] = temp;
    }

    /**
     * Undoes a given move on the chessboard.
     * If the move is null, the method returns without doing anything.
     * The method restores the moved piece to its original location and restores any captured piece.
     * If the move was a promotion, the original piece is restored instead of the promoted piece.
     * If the move was a castling move, the rook is moved back to its original position.
     * The special move flags for the moved piece are not affected.
     *
     * @param move the move to undo
     */
    public void undoMove(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        var movedPiece = move.getMovedPiece();
        var capturedPiece = move.getCapturedPiece();
        var moveFlag = move.getMoveFlag();

        movePieceWithoutSpecialMovesAndSave(to, from);
        removeMovedPieceListItemForCastling(move);

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
        if (move.getMovedPiece().type == PieceType.KING
                && Math.abs(move.getFrom().getX() - move.getTo().getX()) == 2) {
            var team = move.getMovedPiece().team;
            var kingLocation = team == Team.WHITE ? new BoardLocation(4, 0) : new BoardLocation(4, 7);
            // Kingside Castling
            if (move.getTo().getX() > move.getFrom().getX()) {
                var rookLocation = team == Team.WHITE ? new BoardLocation(5, 0) : new BoardLocation(5, 7);
                var originalRookLocation = team == Team.WHITE ? new BoardLocation(7, 0) : new BoardLocation(7, 7);
                var castleMove = new Move(rookLocation, originalRookLocation, new Piece(team, PieceType.ROOK));
                movePiece(castleMove);
            }
            // Queenside castling
            else if (move.getTo().getX() < move.getFrom().getX()) {
                var rookLocation = team == Team.WHITE ? new BoardLocation(3, 0) : new BoardLocation(3, 7);
                var originalRookLocation = team == Team.WHITE ? new BoardLocation(0, 0) : new BoardLocation(0, 7);
                var castleMove = new Move(rookLocation, originalRookLocation, new Piece(team, PieceType.ROOK));
                movePiece(castleMove);
            }
        }
    }

    /**
     * Plays the en passant move on the chessboard.
     * If the move is null, the method returns without doing anything.
     * The method moves the moved piece without considering any special move flags.
     * It also removes the captured piece on the location adjacent to the destination square.
     *
     * @param move the en passant move to play
     */
    public void playEnPassant(Move move) {
        var from = move.getFrom();
        var to = move.getTo();

        movePieceWithoutSpecialMoves(move);
        removePiece(new BoardLocation(to.getX(), from.getY()));
    }

    /**
     * Undoes the en passant move on the chessboard.
     * If the move is null, the method returns without doing anything.
     * The method moves the moved piece back to its original position without considering any special move flags.
     * It also restores the captured piece on the location adjacent to the destination square.
     *
     * @param move the en passant move to undo
     */
    public void undoEnPassant(Move move) {
        var from = move.getFrom();
        var to = move.getTo();
        var enPassantCapture = new BoardLocation(to.getX(), from.getY());

        movePieceWithoutSpecialMoves(new Move(to, from, getPiece(to), getPiece(from)));
        pieces[getArrayIndexFromLocation(enPassantCapture)] = move.getCapturedPiece();
    }

    /**
     * Calculates the board location (x, y) from the given array index.
     *
     * @param index the array index
     * @return the board location corresponding to the given index
     */
    public BoardLocation getPointFromArrayIndex(int index) {
        int x = index % 8;
        int y = index / 8;
        return new BoardLocation(x, y);
    }

    /**
     * Calculates the array index from the given board location (x, y).
     *
     * @param location the board location
     * @return the array index calculated from the given location
     */
    public int getArrayIndexFromLocation(BoardLocation location) {
        return location.getX() + location.getY() * 8;
    }

    /**
     * Promotes a pawn at the given board location with the selected piece.
     *
     * @param pawnLocation the location of the pawn to be promoted
     * @param selectedPiece the piece to promote the pawn to
     */
    public void promotePawn(BoardLocation pawnLocation, PieceType selectedPiece) {
        Team team;
        if (pawnLocation.getY() == 7) team = Team.WHITE;
        else team = Team.BLACK;
        Piece piece = new Piece(team, selectedPiece);
        pieces[pawnLocation.getX() + pawnLocation.getY() * 8] = piece;
    }

    /**
     * Checks if the given board location is within the bounds of the board.
     *
     * @param boardLocation the board location to check
     * @return true if the board location is within the bounds, false otherwise
     */
    public boolean isInBounds(BoardLocation boardLocation) {
        int x = boardLocation.getX();
        int y = boardLocation.getY();
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }
}
