package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.List;

public class Bitboard {
    private long[] pieces = new long[12];
    private long[] controlledSquares = new long[2];

    public Bitboard() {
        for (int i = 0; i < 12; i++) {
            pieces[i] = 0L;
        }
    }

    public long getControlledSquares(Team team) {
        return controlledSquares[team == Team.WHITE ? 0 : 1];
    }

    public void updateControlledSquares(Team team, Board board) {
    long squares = 0L;
    for (PieceType type : PieceType.values()) {
        long bitboard = getBitboard(type, team);
        for (int i = 0; i < 64; i++) {
            if ((bitboard & (1L << i)) != 0) {
                Piece piece = new Piece(team, type);
                List<Move> moves = new ArrayList<>();
                if (type == PieceType.PAWN) {
                    // For pawns, consider the squares they can capture, not the squares they can move to
                    moves.addAll(Rules.getPawnCaptureMoves(new BoardLocation(i % 8, i / 8), team, board, true, true));
                } else {
                    moves.addAll(Rules.getValidMoves(new BoardLocation(i % 8, i / 8), piece, board, false, false, true));
                }
                for (Move move : moves) {
                    squares |= (1L << board.getArrayIndexFromLocation(move.getTo()));
                }
            }
        }
    }
    controlledSquares[team == Team.WHITE ? 0 : 1] = squares;
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

    private int getIndex(PieceType type, Team team) {
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

    public void printBitboard() {
        PieceType[] order = {PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN, PieceType.KING};
        String[] symbols = {"P", "N", "B", "R", "Q", "K"};

        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                String symbol = ".";
                for (int i = 0; i < order.length; i++) {
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

    public void printControlledSquares(Team team) {
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                if ((getControlledSquares(team) & (1L << square)) != 0) {
                    System.out.print("X ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
    }
}