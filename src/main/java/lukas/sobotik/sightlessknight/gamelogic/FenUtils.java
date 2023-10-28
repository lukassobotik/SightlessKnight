package lukas.sobotik.sightlessknight.gamelogic;

import lombok.Getter;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.HashMap;
import java.util.Map;

public class FenUtils {
    Piece[] pieces;
    Team team = Team.BLACK;
    BoardLocation whiteKingPosition;
    BoardLocation blackKingPosition;
    @Getter
    int whiteKingIndex, blackKingIndex;
    BoardLocation lastFromMove;
    BoardLocation lastDoublePawnMoveWithWhite;
    BoardLocation lastDoublePawnMoveWithBlack;
    @Getter
    Team startingTeam;
    public FenUtils(Piece[] pieces, BoardLocation whiteKingPosition, BoardLocation blackKingPosition, BoardLocation lastFromMove, BoardLocation lastDoublePawnMoveWithWhite, BoardLocation lastDoublePawnMoveWithBlack) {
        this.pieces = pieces;
        this.whiteKingPosition = whiteKingPosition;
        this.blackKingPosition = blackKingPosition;
        this.lastFromMove = lastFromMove;
        this.lastDoublePawnMoveWithWhite = lastDoublePawnMoveWithWhite;
        this.lastDoublePawnMoveWithBlack = lastDoublePawnMoveWithBlack;
    }

    public FenUtils(Piece[] pieces) {
        this.pieces = pieces;
    }

    public Piece[] generatePositionFromFEN(String fen) {
        pieces = new Piece[8 * 8];

        String fenBoardString = fen.split(" ")[0];
        char[] fenBoard = fenBoardString.toCharArray();
        int rank = 0, file = 7;

        for (char symbol : fenBoard) {
            if (symbol == '/') {
                rank = 0;
                file--;
            } else {
                if (Character.isDigit(symbol)) {
                    rank += symbol - '0';
                } else {
                    Team pieceColor = (Character.isUpperCase(symbol)) ? Team.WHITE : Team.BLACK;
                    PieceType pieceType = getPieceTypeFromSymbol().get(Character.toLowerCase(symbol));
                    pieces[file * 8 + rank] = new Piece(pieceColor, pieceType);
                    if (pieceType.equals(PieceType.KING) && pieceColor.equals(Team.WHITE)) whiteKingIndex = file * 8 + rank;
                    if (pieceType.equals(PieceType.KING) && pieceColor.equals(Team.BLACK)) blackKingIndex = file * 8 + rank;
                    rank++;
                }
            }
        }

        String fenStartingTeam = fen.split(" ")[1];
        if (fenStartingTeam.equals("b")) {
            startingTeam = Team.BLACK;
        } else {
            startingTeam = Team.WHITE;
        }

        return pieces;
    }
    public String generateFenFromPosition(Piece[] pieces) {
        return generateFenFromPosition(pieces, null);
    }
    public String generateFenFromPosition(Piece[] pieces, Team turn) {
        StringBuilder fenBuilder = new StringBuilder();

        for (int rank = 7; rank >= 0; rank--) {
            int emptySquares = 0;
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece info = pieces[index];

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
        Team activeColor;
        if (team == null) {
            activeColor = GameState.moveNumber % 2 == 0 ? Team.BLACK : Team.WHITE;
        } else {
            activeColor = team;
        }
        fenBuilder.append(" ").append(activeColor == Team.WHITE ? "b" : "w");

        // Castling availability
        StringBuilder castlingAvailability = new StringBuilder();
        castlingAvailability.append(Rules.isCastlingPossible(team, pieces, null, whiteKingPosition, blackKingPosition, true));
        if (castlingAvailability.length() == 0) {
            castlingAvailability.append("-");
        }
        fenBuilder.append(" ").append(castlingAvailability);

        // En Passant
        checkForEnPassant(fenBuilder, activeColor);

        // TODO: Implement Halfmove clock and fullmove number
        fenBuilder.append(" 0 1");

        return fenBuilder.toString();
    }
    private void checkForEnPassant(StringBuilder fenBuilder, Team activeColor) {
        // En passant target square
        BoardLocation enPassantTarget = (activeColor == Team.BLACK) ? lastDoublePawnMoveWithBlack : lastDoublePawnMoveWithWhite;
        if (enPassantTarget != null) {
            Piece piece = pieces[enPassantTarget.getX() + enPassantTarget.getY() * 8];
            if (piece == null) {
                fenBuilder.append(" -");
                return;
            }
            int lastMovedPawn = piece.doublePawnMoveOnMoveNumber;
            if (lastMovedPawn == GameState.moveNumber) {
                fenBuilder.append(" ").append(getNotationFromIntPoint(enPassantTarget));
            } else {
                fenBuilder.append(" -");
            }
        } else {
            fenBuilder.append(" -");
        }
    }
    public char getSymbolFromPieceType(PieceType type, Team team) {
        HashMap<Character, PieceType> pieceTypeFromSymbol = getPieceTypeFromSymbol();

        for (Map.Entry<Character, PieceType> entry : pieceTypeFromSymbol.entrySet()) {
            if (entry.getValue() == type) {
                char symbol = entry.getKey();
                return (team == Team.WHITE) ? Character.toUpperCase(symbol) : Character.toLowerCase(symbol);
            }
        }
        throw new IllegalArgumentException("Invalid piece type: " + type);
    }

    private HashMap<Character, PieceType> getPieceTypeFromSymbol() {
        HashMap<Character, PieceType> pieceTypeFromSymbol = new HashMap<>();
        pieceTypeFromSymbol.put('k', PieceType.KING);
        pieceTypeFromSymbol.put('p', PieceType.PAWN);
        pieceTypeFromSymbol.put('n', PieceType.KNIGHT);
        pieceTypeFromSymbol.put('b', PieceType.BISHOP);
        pieceTypeFromSymbol.put('r', PieceType.ROOK);
        pieceTypeFromSymbol.put('q', PieceType.QUEEN);
        return pieceTypeFromSymbol;
    }

    private String getNotationFromIntPoint(BoardLocation point) {
        char file;
        char rank;

        if (team.equals(Team.WHITE)) file = (char) ('a' + point.getX());
        else file = (char) ('a' + (7 - point.getX()));

        if (team.equals(Team.WHITE)) rank = (char) ('1' + point.getY());
        else rank = (char) ('1' + (7 - point.getY()));

        return String.valueOf(file) + rank;
    }
}
