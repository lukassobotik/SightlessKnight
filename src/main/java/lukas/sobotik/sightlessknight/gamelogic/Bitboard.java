package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.List;

public class Bitboard {
    public long[] pieces = new long[12];
    public long[] attackedSquares = new long[12];
    // TODO: This might be unnecessary, when needed, you can AND the attackedSquares with the pieces bitboard (check if it speeds up the program)
    long[] attackedSquaresWithFriendlyPieces = new long[12];
    long blockerSquares = 0L;

    public Bitboard() {
        for (int i = 0; i < 12; i++) {
            pieces[i] = 0L;
            attackedSquares[i] = 0L;
            attackedSquaresWithFriendlyPieces[i] = 0L;
        }
    }

    public Bitboard(Bitboard bitboard) {
        this.pieces = bitboard.pieces.clone();
        this.attackedSquares = bitboard.attackedSquares.clone();
        this.attackedSquaresWithFriendlyPieces = bitboard.attackedSquaresWithFriendlyPieces.clone();
        this.blockerSquares = bitboard.blockerSquares;
    }

    public long getAttackedSquares(PieceType type, Team team) {
        int index = getIndex(type, team);
        return attackedSquares[index];
    }

    public long getTeamAttackedSquares(Team team) {
        long squares = 0L;
        for (PieceType type : PieceType.values()) {
            squares |= getAttackedSquares(type, team);
        }
        return squares;
    }

    public void updateAttackedSquaresAfterPromotion(Board board, PieceType promotionPiece, Team team) {
        var pieceIndex = getIndex(promotionPiece, team);
        var pawnIndex = getIndex(PieceType.PAWN, team);

        long squares = 0L;
        squares = calculateAttackedSquaresForIndex(team, board, pieceIndex, squares);
        attackedSquares[pieceIndex] = squares;

        squares = 0L;
        squares = calculateAttackedSquaresForIndex(team, board, pawnIndex, squares);
        attackedSquares[pawnIndex] = squares;
    }

    public void updateAttackedSquares(PieceType type, Team team, Board board, Move move) {
        int index = getIndex(type, team);

        long squares = 0L;
        // TODO: Maybe this could be moved down after the indexes containing the square are calculated (or removed entirely because it might be unnecessary)
        squares = calculateAttackedSquaresForIndex(team, board, index, squares);

        // Update the attacked squares for the moved piece
        attackedSquares[index] = squares;

        var indexes = getIndexesContainingSquare(move.getFrom(), board);
        indexes.addAll(getIndexesContainingSquare(move.getTo(), board));
        if (move.getCapturedPiece() != null) {
            if (move.getMoveFlag().equals(MoveFlag.enPassant)) {
                indexes.add(getIndex(PieceType.PAWN, Team.WHITE));
                indexes.add(getIndex(PieceType.PAWN, Team.BLACK));
            } else {
                indexes.add(
                        getIndex(move.getCapturedPiece().type, move.getCapturedPiece().team));
            }
        }
        for (int indexContainingSquare : indexes) {
            attackedSquares[indexContainingSquare] = calculateAttackedSquaresForIndex(team, board, indexContainingSquare, 0L);
        }

//        // Update the attacked squares for the opposing team
//        int enemyIndex = getIndex(type, team == Team.WHITE ? Team.BLACK : Team.WHITE);
//        attackedSquares[enemyIndex] = calculateAttackedSquaresForIndex(team == Team.WHITE ? Team.BLACK : Team.WHITE, board, enemyIndex, 0L);

    }

    public long calculateAttackedSquaresForIndex(final Team team, final Board board, final int index, long squares) {
        return calculateAttackedSquaresForIndex(team, board, index, squares, null);
    }

    public long calculateAttackedSquaresForIndex(final Team team, final Board board, final int index, long squares, Bitboard bitboard) {
        var piecesArray = bitboard == null ? pieces[index] : bitboard.pieces[index];
        var locations = bitboardToLocations(piecesArray);
        for (BoardLocation location : locations) {
            List<Move> moves = Rules.getPseudoLegalMoves(location, getPieceFromIndex(index), board, false, bitboard);
//            // TODO: This might be unnecessary because we do the same thing in the getPseudoLegalMoves method (testPos4 depth 2 fix)
//            if (board.getPiece(location) != null && board.getPiece(location).type == PieceType.PAWN) {
//                // For pawns, consider the squares they can capture, not the squares they can move to
//                moves.addAll(Rules.getPawnCaptureMoves(location, team, board, true, true));
//            }
            for (Move m : moves) {
                squares |= (1L << board.getArrayIndexFromLocation(m.getTo()));
            }
        }
        return squares;
    }

    // TODO: Make this method more efficient
    public long getBlockerSquares() {
        long squares = 0L;
        for (int i = 0; i < 12; i++) {
            squares |= pieces[i];
        }
        return squares;
    }

    public List<BoardLocation> bitboardToLocations(long bitboard) {
        List<BoardLocation> locations = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if ((bitboard & (1L << i)) != 0) {
                int x = i % 8;
                int y = i / 8;
                locations.add(new BoardLocation(x, y));
            }
        }
        return locations;
    }

    // TODO: Add an option to do only one of the two teams (for performance reasons) (used in calculating whether a king is in check)
    public List<Integer> getIndexesContainingSquare(BoardLocation squareLocation, Board board) {
        List<Integer> indexes = new ArrayList<>();
        int squareIndex = board.getArrayIndexFromLocation(squareLocation);

        for (int i = 0; i < attackedSquares.length; i++) {
            if ((attackedSquares[i] & (1L << squareIndex)) != 0) {
                indexes.add(i);
            }
        }

        for (int i = 0; i < attackedSquaresWithFriendlyPieces.length; i++) {
            if ((attackedSquaresWithFriendlyPieces[i] & (1L << squareIndex)) != 0) {
                indexes.add(i);
            }
        }

        return indexes;
    }

    public void setPiece(PieceType type, Team team, int square) {
        int index = getIndex(type, team);
        pieces[index] |= (1L << square);
    }

    public void removePiece(PieceType type, Team team, int square) {
        int index = getIndex(type, team);
        pieces[index] &= ~(1L << square);
    }

    public boolean getPiece(PieceType type, Team team, int square) {
        int index = getIndex(type, team);
        return (pieces[index] & (1L << square)) != 0;
    }

    public long getBitboard(PieceType type, Team team) {
        int index = getIndex(type, team);
        return pieces[index];
    }

    public long getTeamBitboard(Team team) {
        return team == Team.WHITE
               ? (pieces[0] | pieces[1] | pieces[2] | pieces[3] | pieces[4] | pieces[5])
               : pieces[6] | pieces[7] | pieces[8] | pieces[9] | pieces[10] | pieces[11];
    }

    public int getIndex(PieceType type, Team team) {
        PieceType[] order = {PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN, PieceType.KING};

        int typeIndex = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == type) {
                typeIndex = i;
                break;
            }
        }

        return typeIndex + (team == Team.WHITE ? 0 : order.length);
    }

    public Piece getPieceFromIndex(int index) {
        PieceType[] order = {PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN, PieceType.KING};
        Team team = (index < 6) ? Team.WHITE : Team.BLACK;

        if (index >= 0 && index < 12) {
            return new Piece(team, order[index % 6]);
        }

        return null;
    }

    public void printBitboard() {
        printBitboard(0L);
    }

    public void printBitboard(long bitboard) {
        PieceType[] order = {PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN, PieceType.KING};
        String[] symbols = {"P", "N", "B", "R", "Q", "K"};

        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                String symbol = ".";
                for (int i = 0; i < order.length; i++) {
                    if ((bitboard & (1L << square)) != 0) {
                        symbol = "X";
                        break;
                    }
                    if (bitboard != 0L) {
                        break;
                    }
                    if ((getBitboard(order[i], Team.WHITE) & (1L << square)) != 0) {
                        symbol = symbols[i].toUpperCase();
                        break;
                    } else if ((getBitboard(order[i], Team.BLACK) & (1L << square)) != 0) {
                        symbol = symbols[i].toLowerCase();
                        break;
                    }
                }
                System.out.print(symbol + " ");
            }
            System.out.println();
        }
    }
}