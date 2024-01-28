package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AlgebraicNotationUtils {
    Move move;
    FenUtils fenUtils;
    Board board;
    GameState gameState;
    static boolean kinglessGame = false;

    /**
     * Initializes the AlgebraicNotationUtils with the given parameters.
     *
     * @param move The Move object representing the current move.
     * @param fenUtils The FenUtils object for handling FEN notation.
     * @param gameState The GameState object representing the current state of the game.
     * @param board The Board object representing the current state of the chess board.
     */
    public AlgebraicNotationUtils(Move move, FenUtils fenUtils, GameState gameState, Board board) {
        this.move = move;
        this.fenUtils = fenUtils;
        this.gameState = gameState;
        this.board = board;
    }

    /**
     * Creates an instance of AlgebraicNotationUtils with the specified dependencies.
     *
     * @param fenUtils the FenUtils object to use for converting between FEN strings and game state
     * @param gameState the GameState object representing the current state of the game
     * @param board the Board object representing the chess board
     */
    public AlgebraicNotationUtils(FenUtils fenUtils, GameState gameState, Board board) {
        this.fenUtils = fenUtils;
        this.gameState = gameState;
        this.board = board;
    }

    /**
     * Update the variables fenUtils, gameState, and board with the given values.
     *
     * @param fenUtils the new FenUtils object to be assigned
     * @param gameState the new GameState object to be assigned
     * @param board the new Board object to be assigned
     */
    public void updateVariables(FenUtils fenUtils, GameState gameState, Board board) {
        this.fenUtils = fenUtils;
        this.gameState = gameState;
        this.board = board;
    }

    /**
     * Set the kinglessGame variable to the specified value.
     *
     * @param kinglessGame the new value to be assigned to kinglessGame
     */
    public void setKinglessGame(boolean kinglessGame) {
        AlgebraicNotationUtils.kinglessGame = kinglessGame;
    }

    /**
     * Parses a chess move into algebraic notation.
     *
     * @param move The move to parse.
     * @return The move parsed into algebraic notation.
     */
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
                    System.out.println(to.getAlgebraicNotationLocation());
                    board.getPiece(to).enPassant = false;
                    break;
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

        if (!kinglessGame) {
            System.out.println();
        }

        if (Rules.isCheckmate(opponentTeam, board) && !kinglessGame) {
            algebraicNotationMove += "#";
            return algebraicNotationMove;
        }

        if (Rules.isKingInCheck(opponentTeam, board) && !kinglessGame) {
            algebraicNotationMove += "+";
        }

        return algebraicNotationMove;
    }

    /**
     * Disambiguates the moves of a piece.
     *
     * @param move The move object containing the from and to locations, as well as the moved and captured pieces.
     * @param pieceType The type of the piece.
     * @return The disambiguated move as a string.
     */
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

        List<Move> otherPiecePseudoLegalMoves = Rules.getPseudoLegalMoves(board.getPointFromArrayIndex(otherPieceIndex), otherPiece, board);

        Board oldBoard = new Board(board);
        oldBoard.movePiece(new Move(to, from, movedPiece));
        List<Move> otherPieceLegalMoves = Rules.getValidMoves(oldBoard.getPointFromArrayIndex(otherPieceIndex), otherPiece, oldBoard, true);
        boolean isDisambiguationNeeded = otherPieceLegalMoves.stream().anyMatch(streamMove -> streamMove.getTo().equals(to));

        if (!isDisambiguationNeeded) {
            return normalMove;
        }

        if (otherPiecePseudoLegalMoves.stream()
                .anyMatch(streamMove -> streamMove.getTo().equals(to))) {
            var otherPiecePoint = board.getPointFromArrayIndex(otherPieceIndex);
            var fromAlgebraicNotation = from.getAlgebraicNotationLocation();
            var toAlgebraicNotation = to.getAlgebraicNotationLocation();
            var moveBuilder = new StringBuilder();

            if (otherPiecePoint.isOnSameDiagonalAs(from)) {
                moveBuilder.append(fromAlgebraicNotation);
            } else if (otherPiecePoint.isOnSameFileAs(from)) {
                moveBuilder.append(fromAlgebraicNotation.charAt(1));
            } else if (otherPiecePoint.isOnSameRankAs(from)) {
                moveBuilder.append(fromAlgebraicNotation.charAt(0));
            } else if (otherPiecePoint.isOnSameDiagonalAs(to)) {
                moveBuilder.append(fromAlgebraicNotation);
            } else if (otherPiecePoint.isOnSameFileAs(to)) {
                moveBuilder.append(fromAlgebraicNotation.charAt(1));
            } else if (otherPiecePoint.isOnSameRankAs(to)) {
                moveBuilder.append(fromAlgebraicNotation.charAt(0));
            }

            if (capturedPiece != null) {
                moveBuilder.append("x");
            }

            moveBuilder.append(toAlgebraicNotation);
            normalMove = pieceSymbol + moveBuilder;
        }
        return normalMove;
    }

    /**
     * Returns a Move object based on a chess move string from the SAN notation.
     *
     * @param parsedMove The parsed chess move string.
     * @return The Move object representing the parsed move if it is a valid move, null otherwise.
     */
    public Move getMoveFromParsedMove(String parsedMove) {
        var playerTeam = GameState.currentTurn;
        if (parsedMove.equals("O-O") || parsedMove.equals("0-0")) {
            var from = new BoardLocation(4, playerTeam == Team.WHITE ? 0 : 7);
            var to = new BoardLocation(6, playerTeam == Team.WHITE ? 0 : 7);
            var piece = board.getPiece(from);
            var move = new Move(from, to, piece, null);
            move.setMoveFlag(MoveFlag.kingsideCastling);
            return move;
        } else if (parsedMove.equals("O-O-O") || parsedMove.equals("0-0-0")) {
            var from = new BoardLocation(4, playerTeam == Team.WHITE ? 0 : 7);
            var to = new BoardLocation(2, playerTeam == Team.WHITE ? 0 : 7);
            var piece = board.getPiece(from);
            var move = new Move(from, to, piece, null);
            move.setMoveFlag(MoveFlag.queensideCastling);
            return move;
        }

        if (parsedMove.charAt(parsedMove.length() - 1) == '#' || parsedMove.charAt(parsedMove.length() - 1) == '+') {
            parsedMove = parsedMove.substring(0, parsedMove.length() - 1);
        }

        PieceType promotionPiece = null;
        if (parsedMove.contains("=")) {
            promotionPiece = new FenUtils(board.pieces)
                    .getPieceTypeFromSymbol()
                    .get(Character.toLowerCase(
                            parsedMove.charAt(parsedMove.length() - 1)));
            parsedMove = parsedMove.substring(0, parsedMove.length() - 2);
        }

        boolean isEnPassant = false;
        if (parsedMove.contains("e.p.") || parsedMove.contains("e.p")) {
            parsedMove = parsedMove.substring(0, parsedMove.length() - 4);
            parsedMove = parsedMove.trim();
            isEnPassant = true;
        }

        var to = parseSquare(parsedMove.substring(parsedMove.length() - 2));

        var beforeTakes = "";
        if (parsedMove.contains("x")) {
            for (int i = 0; i < parsedMove.length(); i++) {
                if (parsedMove.charAt(i) == 'x') {
                    beforeTakes = parsedMove.substring(0, i);
                    break;
                }
            }
        }

        PieceType movedPieceType;
        if (Character.isUpperCase(parsedMove.charAt(0))) {
            movedPieceType = new FenUtils(board.pieces)
                    .getPieceTypeFromSymbol()
                    .get(Character.toLowerCase(parsedMove.charAt(0)));
            var piece = new Piece(playerTeam, movedPieceType);

            var allMoves = Rules.getPseudoLegalMoves(to, piece, board);
            if (!allMoves.isEmpty()) {
                List<Move> possibleMoves = new ArrayList<>();
                for (Move move : allMoves) {
                    if (move.getFrom().equals(to) && move.getCapturedPiece() != null && move.getCapturedPiece().type == movedPieceType && move.getCapturedPiece().team == playerTeam) {
                        possibleMoves.add(new Move(move.getTo(), move.getFrom(), piece, board.getPiece(to)));
                    }
                }
                if (possibleMoves.size() == 1) {
                    return possibleMoves.get(0);
                } else {
                    Move moveDisambiguation = parsingMoveDisambiguation(beforeTakes, possibleMoves);
                    if (moveDisambiguation != null) {
                        return moveDisambiguation;
                    }
                }
            }

        } else {
            movedPieceType = PieceType.PAWN;
        }

        // TODO: Break this into smaller methods
        if (parsedMove.length() == 2 || Objects.equals(movedPieceType, PieceType.PAWN)) {
            BoardLocation from = null;
            Piece piece = null;
            if (!parsedMove.contains("x")) {
                try {
                    var loc = board.getPiece(to.transpose(0, playerTeam == Team.WHITE ? -1 : 1));
                    if (loc != null && loc.type.equals(PieceType.PAWN) && loc.team.equals(playerTeam)) {
                        piece = loc;
                        from = to.transpose(0, playerTeam == Team.WHITE ? -1 : 1);
                    }
                } catch (Exception ignored) {}
                try {
                    var loc = board.getPiece(to.transpose(0, playerTeam == Team.WHITE ? -2 : 2));
                    if (loc != null && loc.type.equals(PieceType.PAWN) && loc.team.equals(playerTeam)) {
                        piece = loc;
                        from = to.transpose(0, playerTeam == Team.WHITE ? -2 : 2);
                    }
                } catch (Exception ignored) {}

            } else {
                if (isEnPassant) {
                    BoardLocation pawnLocation = null;
                    try {
                        var loc = to.transpose(-1, playerTeam == Team.WHITE ? -1 : 1);
                        if (board.getPiece(loc).team.equals(playerTeam) && board.getPiece(loc).type.equals(PieceType.PAWN)) {
                            pawnLocation = loc;
                        }
                    } catch (Exception ignored) {}
                    try {
                        var loc = to.transpose(1, playerTeam == Team.WHITE ? -1 : 1);
                        if (board.getPiece(loc).team.equals(playerTeam) && board.getPiece(loc).type.equals(PieceType.PAWN)) {
                            pawnLocation = loc;
                        }
                    } catch (Exception ignored) {}

                    var moves = Rules.addLegalEnPassant(playerTeam, board, to, pawnLocation, new BoardLocation(to.getX(), to.getY() - 1), true);
                    if (!moves.isEmpty()) {
                        return moves.get(0);
                    }
                }

                try {
                    var loc = board.getPiece(to.transpose(-1, playerTeam == Team.WHITE ? -1 : 1));
                    if (loc != null && loc.type.equals(PieceType.PAWN) && loc.team.equals(playerTeam)) {
                        piece = loc;
                        from = to.transpose(-1, playerTeam == Team.WHITE ? -1 : 1);
                    }
                } catch (Exception ignored) {}
                try {
                    var loc = board.getPiece(to.transpose(1, playerTeam == Team.WHITE ? -1 : 1));
                    if (loc != null && loc.type.equals(PieceType.PAWN) && loc.team.equals(playerTeam)) {
                        piece = loc;
                        from = to.transpose(1, playerTeam == Team.WHITE ? -1 : 1);
                    }
                } catch (Exception ignored) {}

            }
            if (piece != null && from != null) {
                var move = new Move(from, to, piece, board.getPiece(to));
                if (promotionPiece != null) {
                    move.setPromotionPiece(promotionPiece);
                }
                return move;
            }
        }

        System.out.println("to: " + to.getAlgebraicNotationLocation());
        return null;
    }

    /**
     * Parses move disambiguation based on the given parameters.
     *
     * @param beforeTakes The string representing the part of the move before the pawn takes notation. It can be empty, one character representing a file, or two characters representing a rank or file and a rank.
     * @param possibleMoves The list of possible moves to be considered for disambiguation.
     * @return The move that matches the disambiguation criteria, or null if no match is found.
     */
    private static Move parsingMoveDisambiguation(final String beforeTakes, final List<Move> possibleMoves) {
        if (beforeTakes.isEmpty()) {
            // Gets rid of duplicate moves
            return possibleMoves.get(0);
        } else if (beforeTakes.length() == 1) {
            var file = beforeTakes.charAt(0);
            for (Move move : possibleMoves) {
                if (move.getFrom().getAlgebraicNotationLocation().charAt(0) == file) {
                    return move;
                }
            }
        } else if (beforeTakes.length() == 2) {
            var rank = beforeTakes.charAt(1);
            for (Move move : possibleMoves) {
                if (move.getFrom().getAlgebraicNotationLocation().charAt(1) == rank) {
                    return move;
                }
                if (move.getFrom().getAlgebraicNotationLocation().charAt(0) == rank) {
                    return move;
                }
            }
        }
        return null;
    }

    /**
     * Parses a string representation of a chess square and returns the corresponding BoardLocation object.
     *
     * @param square the algebraic notation of the chess square (e.g. "e4")
     * @return the BoardLocation object representing the given chess square
     * @throws IllegalArgumentException if the given square is invalid
     */
    public BoardLocation parseSquare(String square) {
        char file = square.charAt(0);
        int rank = Character.getNumericValue(square.charAt(1)) - 1;

        if (file < 'a' || file > 'h' || rank < 0 || rank > 7) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + square);
        }

        return new BoardLocation(file - 'a', rank);
    }
}
