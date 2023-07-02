package lukas.sobotik.sightlessknight;

import java.util.HashMap;
import java.util.Map;

public class FenUtils {
    PieceInfo[][] pieces;
    Team team = Team.BLACK;
    IntPoint2D whiteKing;
    IntPoint2D blackKing;
    IntPoint2D lastFrom;
    IntPoint2D lastMovedDoubleWhitePawn;
    IntPoint2D lastMovedDoubleBlackPawn;
    FenUtils(PieceInfo[][] pieces, IntPoint2D whiteKing, IntPoint2D blackKing, IntPoint2D lastFrom, IntPoint2D lastMovedDoubleWhitePawn, IntPoint2D lastMovedDoubleBlackPawn) {
        this.pieces = pieces;
        this.whiteKing = whiteKing;
        this.blackKing = blackKing;
        this.lastFrom = lastFrom;
        this.lastMovedDoubleWhitePawn = lastMovedDoubleWhitePawn;
        this.lastMovedDoubleBlackPawn = lastMovedDoubleBlackPawn;
    }

    public PieceInfo[][] generatePositionFromFEN(String fen) {
        pieces = new PieceInfo[8][8];

        HashMap<Character, PieceType> pieceTypeFromSymbol = new HashMap<>();
        pieceTypeFromSymbol.put('k', PieceType.KING);
        pieceTypeFromSymbol.put('p', PieceType.PAWN);
        pieceTypeFromSymbol.put('n', PieceType.KNIGHT);
        pieceTypeFromSymbol.put('b', PieceType.BISHOP);
        pieceTypeFromSymbol.put('r', PieceType.ROOK);
        pieceTypeFromSymbol.put('q', PieceType.QUEEN);

        String fenBoardString = fen.split(" ")[0];
        char[] fenBoard = fenBoardString.toCharArray();
        int file = 0, rank = 7;

        for (char symbol : fenBoard) {
            if (symbol == '/') {
                file = 0;
                rank--;
            } else {
                if (Character.isDigit(symbol)) {
                    file += Character.getNumericValue(symbol);
                } else {
                    Team pieceColor = (Character.isUpperCase(symbol)) ? Team.WHITE : Team.BLACK;
                    PieceType pieceType = pieceTypeFromSymbol.get(Character.toLowerCase(symbol));
                    pieces[file][rank] = new PieceInfo(pieceColor, pieceType);
                    file++;
                }
            }
        }

        return pieces;
    }
    public String generateFenFromCurrentPosition() {
        StringBuilder fenBuilder = new StringBuilder();

        // Board state in FEN format
        for (int rank = 7; rank >= 0; rank--) {
            int emptySquares = 0;

            for (int file = 0; file < 8; file++) {
                PieceInfo info = pieces[file][rank];

                if (info == null) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fenBuilder.append(emptySquares);
                        emptySquares = 0;
                    }

                    char symbol = getSymbolFromPieceType(info.type, info.team);
                    fenBuilder.append(symbol);
                }
            }

            if (emptySquares > 0) {
                fenBuilder.append(emptySquares);
            }

            if (rank > 0) {
                fenBuilder.append("/");
            }
        }

        // Active color
        Team activeColor = GameState.moveNumber % 2 == 0 ? Team.BLACK : Team.WHITE;
        fenBuilder.append(" ").append(activeColor == Team.WHITE ? "b" : "w");

        // Castling availability
        StringBuilder castlingAvailability = new StringBuilder();
        if (whiteKing.equals(new IntPoint2D(4, 0)) && pieces[4][0] != null && pieces[4][0].type == PieceType.KING && !pieces[4][0].hasMoved) {
            if (pieces[7][0] != null && pieces[7][0].type == PieceType.ROOK && !pieces[7][0].hasMoved) {
                castlingAvailability.append("K");
            }
            if (pieces[0][0] != null && pieces[0][0].type == PieceType.ROOK && !pieces[0][0].hasMoved) {
                castlingAvailability.append("Q");
            }
        }
        if (blackKing.equals(new IntPoint2D(4, 7)) && pieces[4][7] != null && pieces[4][7].type == PieceType.KING && !pieces[4][7].hasMoved) {
            if (pieces[7][7] != null && pieces[7][7].type == PieceType.ROOK && !pieces[7][7].hasMoved) {
                castlingAvailability.append("k");
            }
            if (pieces[0][7] != null && pieces[0][7].type == PieceType.ROOK && !pieces[0][7].hasMoved) {
                castlingAvailability.append("q");
            }
        }
        if (castlingAvailability.length() == 0) {
            castlingAvailability.append("-");
        }
        fenBuilder.append(" ").append(castlingAvailability);

        checkForEnPassant(fenBuilder, activeColor);

        // Halfmove clock and fullmove number
        fenBuilder.append(" 0 0");

        return fenBuilder.toString();
    }
    private void checkForEnPassant(StringBuilder fenBuilder, Team activeColor) {
        // En passant target square
        IntPoint2D enPassantTarget = (activeColor == Team.BLACK) ? lastMovedDoubleBlackPawn : lastMovedDoubleWhitePawn;
        if (enPassantTarget != null) {
            PieceInfo pieceInfo = pieces[enPassantTarget.getX()][enPassantTarget.getY()];
            if (pieceInfo == null) {
                fenBuilder.append(" -");
                return;
            }
            int lastMovedPawn = pieceInfo.doublePawnMoveOnMoveNumber;
            if (lastMovedPawn == GameState.moveNumber) {
                fenBuilder.append(" ").append(getNotationFromIntPoint(enPassantTarget));
            } else {
                fenBuilder.append(" -");
            }
        } else {
            fenBuilder.append(" -");
        }
    }
    private char getSymbolFromPieceType(PieceType type, Team team) {
        HashMap<Character, PieceType> pieceTypeFromSymbol = new HashMap<>();
        pieceTypeFromSymbol.put('k', PieceType.KING);
        pieceTypeFromSymbol.put('p', PieceType.PAWN);
        pieceTypeFromSymbol.put('n', PieceType.KNIGHT);
        pieceTypeFromSymbol.put('b', PieceType.BISHOP);
        pieceTypeFromSymbol.put('r', PieceType.ROOK);
        pieceTypeFromSymbol.put('q', PieceType.QUEEN);

        for (Map.Entry<Character, PieceType> entry : pieceTypeFromSymbol.entrySet()) {
            if (entry.getValue() == type) {
                char symbol = entry.getKey();
                return (team == Team.WHITE) ? Character.toUpperCase(symbol) : Character.toLowerCase(symbol);
            }
        }
        throw new IllegalArgumentException("Invalid piece type: " + type);
    }

    private String getNotationFromIntPoint(IntPoint2D point) {
        char file;
        char rank;

        if (team.equals(Team.WHITE)) file = (char) ('a' + point.getX());
        else file = (char) ('a' + (7 - point.getX()));

        if (team.equals(Team.WHITE)) rank = (char) ('1' + point.getY());
        else rank = (char) ('1' + (7 - point.getY()));

        return String.valueOf(file) + rank;
    }
}
