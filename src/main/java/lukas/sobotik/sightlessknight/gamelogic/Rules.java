package lukas.sobotik.sightlessknight.gamelogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

enum CheckState {
    NONE,
    CHECK,
    CHECKMATE,
    STALEMATE
}

public class Rules {
    static final Team playerTeam = GameState.playerTeam;
    private Rules() {

    }

    static List<BoardLocation> getValidMoves(List<BoardLocation> legalMoves, BoardLocation selectedPieceLocation, Piece piece, Board board) {
        return getValidMoves(legalMoves, selectedPieceLocation, piece, board, true, true);
    }
    static List<BoardLocation> getValidMoves(List<BoardLocation> legalMoves, BoardLocation selectedPieceLocation, Piece piece, Board board, boolean checkForChecks, boolean checkCastlingMoves) {
        switch (piece.type) {
            case PAWN -> legalMoves.addAll(Objects.requireNonNull(getValidPawnMoves(legalMoves, selectedPieceLocation, piece.team, board)));
            case BISHOP -> legalMoves.addAll(getValidBishopMoves(legalMoves, selectedPieceLocation, piece.team, board));
            case KNIGHT -> legalMoves.addAll(getValidKnightMoves(legalMoves, selectedPieceLocation, piece.team, board));
            case ROOK -> legalMoves.addAll(getValidRookMoves(legalMoves, selectedPieceLocation, piece.team, board));
            case KING -> legalMoves.addAll(getValidKingMoves(legalMoves, selectedPieceLocation, piece.team, board, checkCastlingMoves));
            case QUEEN -> legalMoves.addAll(getValidQueenMoves(legalMoves, selectedPieceLocation, piece.team, board));
        }
        if (checkForChecks) {
            legalMoves = checkForChecks(legalMoves, selectedPieceLocation, piece.team, board);
        }
        return legalMoves;
    }
    public static boolean isSquareAttackedByEnemy(BoardLocation square, Team friendlyTeam, Board board) {
        ArrayList<BoardLocation> list = new ArrayList<>();

        for (PieceType type : PieceType.values()) {
            Piece info = new Piece(friendlyTeam, type);
            getValidMoves(list, square, info, board, false, false);
            for (BoardLocation move : list) {
                Piece target = board.getPiece(move);
                if (target != null && target.type == type) {
                    return true;
                }
            }
            list.clear();
        }
        return false;
    }
    public static String isCastlingPossible(Team team, Piece[] pieces, BoardLocation whiteKing, BoardLocation blackKing, boolean returnBothTeams) {
        StringBuilder castlingAvailability = new StringBuilder();
        if (whiteKing.equals(new BoardLocation(4, 0)) && pieces[4] != null && pieces[4].type == PieceType.KING && !pieces[4].hasMoved) {
            if (pieces[7] != null && pieces[7].type == PieceType.ROOK && !pieces[7].hasMoved) {
                castlingAvailability.append("K");
            }
            if (pieces[0] != null && pieces[0].type == PieceType.ROOK && !pieces[0].hasMoved) {
                castlingAvailability.append("Q");
            }
        }
        if (!returnBothTeams) {
            if (team == Team.WHITE) return castlingAvailability.toString();
            castlingAvailability = new StringBuilder();
        }
        if (blackKing.equals(new BoardLocation(4, 7)) && pieces[4 + 7 * 8] != null && pieces[4 + 7 * 8].type == PieceType.KING && !pieces[4 + 7 * 8].hasMoved) {
            if (pieces[7 + 7 * 8] != null && pieces[7 + 7 * 8].type == PieceType.ROOK && !pieces[7 + 7 * 8].hasMoved) {
                castlingAvailability.append("k");
            }
            if (pieces[7 * 8] != null && pieces[7 * 8].type == PieceType.ROOK && !pieces[7 * 8].hasMoved) {
                castlingAvailability.append("q");
            }
        }
        return castlingAvailability.toString();
    }
    private static List<BoardLocation> checkForChecks(List<BoardLocation> legalMoves, BoardLocation selectedPieceLocation, Team team, Board board) {
        for (int i = 0; i < legalMoves.size(); i++) {
            BoardLocation move = legalMoves.get(i);

            board.movePieceWithoutSpecialMovesAndSave(selectedPieceLocation, move);

            if (checkForChecks(team, board) == CheckState.CHECK) {
                legalMoves.remove(i);
                i--;
            }

            board.undoMove();
        }
        return legalMoves;
    }
    public static CheckState checkForChecks(Team team, Board board) {
        ArrayList<BoardLocation> list = new ArrayList<>();
        BoardLocation king = board.getKing(team);

        for (PieceType type : PieceType.values()) {
            Piece info = new Piece(team, type);
            getValidMoves(list, king, info, board, false, false);
            for (BoardLocation move : list) {
                Piece target = board.getPiece(move);
                if (target != null && target.type == type) {
                    return CheckState.CHECK;
                }
            }
            list.clear();
        }
        return CheckState.NONE;
    }
    private static List<BoardLocation> getValidQueenMoves(List<BoardLocation> legalQueenMoves, BoardLocation selectedPieceLocation, Team team, Board board) {
        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int yDir = -1; yDir <= 1; yDir++) {
                if (xDir == 0 && yDir == 0) {
                    continue;
                }
                pieceDirections(legalQueenMoves, selectedPieceLocation, team, board, xDir, yDir);
            }
        }
        return legalQueenMoves;
    }

    private static void pieceDirections(List<BoardLocation> boardLocations, BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir) {
        BoardLocation move = selectedPieceLocation;
        do {
            move = move.transpose(xDir, yDir);
        } while (checkIfInBounds(boardLocations, team, board, move));
    }
    private static List<BoardLocation> getValidKnightMoves(List<BoardLocation> legalKnightMoves, BoardLocation selectedPieceLocation, Team team, Board board) {
        for (int direction = 0; direction < 2; direction++) {
            for (int longDir = -2; longDir <= 2; longDir += 4) {
                for (int shortDir = -1; shortDir <= 1; shortDir += 2) {
                    BoardLocation move;
                    if (direction == 0) {
                        move = selectedPieceLocation.transpose(longDir, shortDir);
                    } else {
                        move = selectedPieceLocation.transpose(shortDir, longDir);
                    }
                    Piece target = board.getPiece(move);
                    if (board.isInBounds(move) && (target == null || target.team != team)) {
                        legalKnightMoves.add(move);
                    }
                }
            }
        }
        return legalKnightMoves;
    }
    private static List<BoardLocation> getValidKingMoves(List<BoardLocation> legalKingMoves, BoardLocation selectedPieceLocation, Team team, Board board, boolean checkCastlingMoves) {
        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int yDir = -1; yDir <= 1; yDir++) {
                if (xDir == 0 && yDir == 0) {
                    continue;
                }
                BoardLocation move = selectedPieceLocation.transpose(xDir, yDir);
                Piece target = board.getPiece(move);
                if (board.isInBounds(move) && (target == null || target.team != team)) {
                    legalKingMoves.add(move);
                }

                // Check if castling moves are valid
                if (checkCastlingMoves) {
                    legalKingMoves.addAll(getValidCastlingMoves(selectedPieceLocation, team, board, xDir, yDir));
                }
            }
        }
        return legalKingMoves;
    }
    public static List<BoardLocation> getValidCastlingMoves(BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir) {
        List<BoardLocation> legalKingMoves = new ArrayList<>();
        String teamCastle = isCastlingPossible(team, board.pieces, board.whiteKingLocation, board.blackKingLocation, false);
        // Kingside Castling
        ArrayList<BoardLocation> kingsideCastling = checkKingsideCastling(selectedPieceLocation, team, board, xDir, yDir, legalKingMoves, teamCastle);
        if (kingsideCastling != null) return kingsideCastling;
        // Queenside Castling
        ArrayList<BoardLocation> queensideCastling = checkQueensideCastling(selectedPieceLocation, team, board, xDir, yDir, legalKingMoves, teamCastle);
        if (queensideCastling != null) return queensideCastling;
        return legalKingMoves;
    }

    private static ArrayList<BoardLocation> checkQueensideCastling(BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir, List<BoardLocation> legalKingMoves, String fen) {
        if (xDir == -1 && yDir == 0 && selectedPieceLocation.equals(team == Team.WHITE ? new BoardLocation(4, 0) : new BoardLocation(4, 7)))  {
            if (isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir, yDir), team, board)
                    || isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir - 1, yDir), team, board)
                    || isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir - 2, yDir), team, board))
                return new ArrayList<>();
            if (board.getPiece(selectedPieceLocation.transpose(xDir, yDir)) != null
                    || board.getPiece(selectedPieceLocation.transpose(xDir - 1, yDir)) != null
                    || board.getPiece(selectedPieceLocation.transpose(xDir - 2, yDir)) != null) {
                System.out.println((board.getPiece(selectedPieceLocation.transpose(xDir, yDir)) != null ? board.getPiece(selectedPieceLocation.transpose(xDir, yDir)).type : "null") + " " + (board.getPiece(selectedPieceLocation.transpose(xDir - 1, yDir)) != null ? board.getPiece(selectedPieceLocation.transpose(xDir - 1, yDir)).type : "null") + " " + (board.getPiece(selectedPieceLocation.transpose(xDir - 2, yDir)) != null ? board.getPiece(selectedPieceLocation.transpose(xDir - 2, yDir)).type : "null"));
                return new ArrayList<>();
            }

            if ((fen.contains("Q") && team == Team.WHITE) || (fen.contains("q") && team == Team.BLACK)) {
                BoardLocation castleMove = selectedPieceLocation.transpose(xDir - 1, yDir);
                Piece castleTarget = board.getPiece(castleMove);
                if (board.isInBounds(castleMove) && (castleTarget == null || castleTarget.team != team)) {
                    legalKingMoves.add(castleMove);
                }
            }
        }
        return null;
    }

    private static ArrayList<BoardLocation> checkKingsideCastling(BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir, List<BoardLocation> legalKingMoves, String fen) {
        if (xDir == 1 && yDir == 0 && selectedPieceLocation.equals(team == Team.WHITE ? board.whiteKingLocation : board.blackKingLocation))  {
            if (isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir, yDir), team, board)
                    || isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir + 1, yDir), team, board))
                return new ArrayList<>();
            if (board.getPiece(selectedPieceLocation.transpose(xDir, yDir)) != null
                    || board.getPiece(selectedPieceLocation.transpose(xDir + 1, yDir)) != null)
                return new ArrayList<>();

            if ((fen.contains("K") && team == Team.WHITE) || (fen.contains("k") && team == Team.BLACK)) {
                BoardLocation castleMove = selectedPieceLocation.transpose(xDir + 1, yDir);
                Piece castleTarget = board.getPiece(castleMove);
                if (board.isInBounds(castleMove) && (castleTarget == null || castleTarget.team != team)) {
                    legalKingMoves.add(castleMove);
                }
            }
        }
        return null;
    }

    private static List<BoardLocation> getValidRookMoves(List<BoardLocation> legalRookMoves, BoardLocation selection, Team team, Board board) {
        for (int direction = 0; direction < 2; direction++) {
            for (int direction2 = -1; direction2 <= 1; direction2 += 2) {
                BoardLocation move = selection;
                do {
                    if (direction == 0) {
                        move = move.transpose(direction2, 0);
                    } else {
                        move = move.transpose(0, direction2);
                    }

                } while (checkIfInBounds(legalRookMoves, team, board, move));
            }
        }
        return legalRookMoves;
    }
    private static boolean checkIfInBounds(List<BoardLocation> list, Team team, Board board, BoardLocation move) {
        if (!board.isInBounds(move)) {
            return false;
        }

        Piece target = board.getPiece(move);

        if (target != null) {
            if (target.team != team) {
                list.add(move);
            }
            return false;
        }

        list.add(move);
        return true;
    }


    private static List<BoardLocation> getValidBishopMoves(List<BoardLocation> legalBishopMoves, BoardLocation selection, Team team, Board board) {
        for (int xDir = -1; xDir <= 1; xDir += 2) {
            for (int yDir = -1; yDir <= 1; yDir += 2) {
                pieceDirections(legalBishopMoves, selection, team, board, xDir, yDir);
            }
        }
        return legalBishopMoves;
    }


    public static List<BoardLocation> getValidPawnMoves(List<BoardLocation> legalPawnMoves, BoardLocation pawnLocation, Team team, Board board) {
        int forwardDirection = (team == playerTeam) ? 1 : -1;
        BoardLocation forwardPoint = new BoardLocation(pawnLocation.getX(), (pawnLocation.getY() + (forwardDirection)));
        BoardLocation doubleForwardPoint = new BoardLocation(pawnLocation.getX(), (pawnLocation.getY() + (2 * forwardDirection)));

        // Check for normal move forward
        if (board.isInBounds(forwardPoint) && board.getPiece(forwardPoint) == null) {
            legalPawnMoves.add(forwardPoint);
            // Check for double move forward (if the pawn hasn't moved yet)
            if (team == (playerTeam == Team.WHITE ? Team.WHITE : Team.BLACK) && pawnLocation.getY() == 1 && board.getPiece(doubleForwardPoint) == null) {
                legalPawnMoves.add(doubleForwardPoint);
            } else if (team == (playerTeam == Team.BLACK ? Team.WHITE : Team.BLACK) && pawnLocation.getY() == 6 && board.getPiece(doubleForwardPoint) == null) {
                legalPawnMoves.add(doubleForwardPoint);
            }
        }

        // Check for capture moves
        BoardLocation leftCapture = new BoardLocation(pawnLocation.getX() - 1, pawnLocation.getY() + forwardDirection);
        BoardLocation rightCapture = new BoardLocation(pawnLocation.getX() + 1, pawnLocation.getY() + forwardDirection);
        if (addPawnCaptureMoves(legalPawnMoves, pawnLocation, team, board, leftCapture)) return new ArrayList<>();
        if (addPawnCaptureMoves(legalPawnMoves, pawnLocation, team, board, rightCapture)) return new ArrayList<>();

        // Check for promotion moves
        if (legalPawnMoves.size() > 0 && (forwardPoint.getY() == 7 || forwardPoint.getY() == 0)) {
            List<BoardLocation> promotionMoves = new ArrayList<>();

            for (BoardLocation move : legalPawnMoves) {
                if (move.getY() == 0 || move.getY() == 7) {
                    promotionMoves.add(move);
                }
            }
        }
        return legalPawnMoves;
    }

    private static boolean addPawnCaptureMoves(List<BoardLocation> legalPawnMoves, BoardLocation pawnLocation, Team team, Board board, BoardLocation captureLocation) {
        if (board.isInBounds(captureLocation)) {
            Piece rightCapturePiece = board.getPiece(captureLocation);
            if (board.lastDoublePawnMoveWithWhitePieces == null || board.lastDoublePawnMoveWithBlackPieces == null) {
                return true;
            }

            if (rightCapturePiece != null && rightCapturePiece.team != team) {
                legalPawnMoves.add(captureLocation);
            }
            checkEnPassant(legalPawnMoves, team, board, captureLocation, pawnLocation);
        }
        return false;
    }

    private static void checkEnPassant(List<BoardLocation> legalPawnMoves, Team team, Board board, BoardLocation captureMove, BoardLocation pawnLocation) {
        BoardLocation leftLocation = new BoardLocation(pawnLocation.getX() - 1, pawnLocation.getY());
        BoardLocation rightLocation = new BoardLocation(pawnLocation.getX() + 1, pawnLocation.getY());

        addLegalEnPassant(legalPawnMoves, team, board, captureMove, pawnLocation, rightLocation, pawnLocation.getX() < captureMove.getX());
        addLegalEnPassant(legalPawnMoves, team, board, captureMove, pawnLocation, leftLocation, pawnLocation.getX() > captureMove.getX());
    }

    private static void addLegalEnPassant(List<BoardLocation> legalPawnMoves, Team team, Board board, BoardLocation captureMove, BoardLocation pawnLocation, BoardLocation pieceLocationNextToPawn, boolean isDifferentFile) {
        if (board.getPiece(pieceLocationNextToPawn) != null
                && board.getPiece(pieceLocationNextToPawn).type.equals(PieceType.PAWN)
                && board.getPiece(pieceLocationNextToPawn).team != team
                && (team == playerTeam ? pawnLocation.getY() == 4 : pawnLocation.getY() == 3)
                && board.getPiece(pieceLocationNextToPawn).doublePawnMoveOnMoveNumber == GameState.moveNumber
                && isDifferentFile) {
            legalPawnMoves.add(captureMove);
        }
    }

    public static boolean isKingInCheck(Team team, Board board) {
        BoardLocation kingPosition = team == Team.WHITE ? board.whiteKingLocation : board.blackKingLocation;
        return isSquareAttackedByEnemy(kingPosition, team, board);
    }

    public static boolean isCheckmate(Team team, Board board) {
        if (!isKingInCheck(team, board)) {
            return false;
        }
        List<BoardLocation> validMoves = new ArrayList<>();
        for (int i = 0; i < board.pieces.length; i++) {
            BoardLocation point = board.getPointFromArrayIndex(i);
            Piece piece = board.pieces[i];
            if (piece != null && piece.team == team) {
                getValidMoves(validMoves, point, piece, board, true, false);
                for (BoardLocation move : validMoves) {
                    board.movePieceWithoutSpecialMovesAndSave(point, move);
                    if (!isKingInCheck(team, board)) {
                        board.undoMove();
                        return false;
                    }
                    board.undoMove();
                }
            }
        }
        return true;

//        List<BoardLocation> validMoves = new ArrayList<>();
//        for (int i = 0; i < board.pieces.length; i++) {
//            BoardLocation point = board.getPointFromArrayIndex(i);
//            Piece piece = board.pieces[i];
//            if (piece != null && piece.team == team) {
//                getValidMoves(validMoves, point, piece, board, false, false);
//                for (BoardLocation move : validMoves) {
//                    board.movePieceWithoutSpecialMovesAndSave(point, move);
//                    if (!checkForChecks(team, board).equals(CheckState.CHECK)) {
//                        board.undoMove();
//                        board.printBoardInConsole();
//                        return false;
//                    }
//                    board.undoMove();
//                }
//            }
//        }
//        System.out.println("CHCHCHCHHCHHC");
//        validMoves.forEach(item -> System.err.println(item.getX() + ":" + item.getY()));
//        System.err.println(checkForChecks(team, board));
//                if (validMoves.isEmpty() && checkForChecks(team == Team.WHITE ? Team.BLACK : Team.WHITE, board).equals(CheckState.CHECK)) {
//                    return true;
//                }
//                return false;
    }

    public static boolean isStalemate(Team team, Board board) {
        if (isKingInCheck(team, board)) {
            return false;
        }
        List<BoardLocation> validMoves = new ArrayList<>();
        for (int i = 0; i < board.pieces.length; i++) {
            BoardLocation point = board.getPointFromArrayIndex(i);
            Piece piece = board.pieces[i];
            if (piece != null && piece.team == team) {
                getValidMoves(validMoves, point, piece, board);
                if (!validMoves.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
