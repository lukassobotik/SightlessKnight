/*
 * TODO:
 * Add En Peasant
 * Add Castling
 * Add promotion
 *
 */

package lukas.sobotik.sightlessknight;

import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

enum CheckState {
    NONE,
    CHECK,
    MATE
}

public class Rules {
    static final Team playerTeam = GameState.playerTeam;
    private Rules() {

    }

    static List<IntPoint2D> getValidMoves(List<IntPoint2D> list, IntPoint2D selection, PieceInfo piece, Board board) {
        return getValidMoves(list, selection, piece, board, true, true);
    }
    static List<IntPoint2D> getValidMoves(List<IntPoint2D> list, IntPoint2D selection, PieceInfo piece, Board board, boolean checkForChecks, boolean checkCastling) {
        List<IntPoint2D> validMoves = new ArrayList<>();
        switch (piece.type) {
            case PAWN:
                validMoves.addAll(Objects.requireNonNull(getValidMovesPawn(list, selection, piece.team, board)));
                break;
            case BISHOP:
                validMoves.addAll(getValidMovesBishop(list, selection, piece.team, board));
                break;
            case KNIGHT:
                validMoves.addAll(getValidMovesKnight(list, selection, piece.team, board));
                break;
            case ROOK:
                validMoves.addAll(getValidMovesRook(list, selection, piece.team, board));
                break;
            case KING:
                validMoves.addAll(getValidMovesKing(list, selection, piece.team, board, checkCastling));
                break;
            case QUEEN:
                validMoves.addAll(getValidMovesQueen(list, selection, piece.team, board));
                break;
        }
        if (checkForChecks) {
            checkForChecks(list, selection, piece.team, board);
        }
        return validMoves;
    }
    public static boolean isEnemyAttackingThisSquare(IntPoint2D square, Team friendlyTeam, Board board) {
//        Team enemyTeam = friendlyTeam == Team.WHITE ? Team.BLACK : Team.WHITE;
//        for (int index = 0; index < board.pieces.length; index++) {
//            PieceInfo info = board.pieces[index];
//            if (info == null || info.team != enemyTeam) continue;
//
//            List<IntPoint2D> validMoves = getValidMoves(list, board.getPointFromArrayIndex(index), info, board, false);
//            if (validMoves.contains(square)) return true;
//        }
//        return false;

        ArrayList<IntPoint2D> list = new ArrayList<>();

        for (PieceType type : PieceType.values()) {
            PieceInfo info = new PieceInfo(friendlyTeam, type);
            getValidMoves(list, square, info, board, false, false);
            for (IntPoint2D move : list) {
                PieceInfo target = board.getPiece(move);
                if (target != null && target.type == type) {
                    return true;
                }
            }
            list.clear();
        }
        return false;
    }
    public static String isCastlingPossible(Team team, PieceInfo[] pieces, IntPoint2D whiteKing, IntPoint2D blackKing, boolean getBothTeams) {
        StringBuilder castlingAvailability = new StringBuilder();
        if (whiteKing.equals(new IntPoint2D(4, 0)) && pieces[4] != null && pieces[4].type == PieceType.KING && !pieces[4].hasMoved) {
            if (pieces[7] != null && pieces[7].type == PieceType.ROOK && !pieces[7].hasMoved) {
                castlingAvailability.append("K");
            }
            if (pieces[0] != null && pieces[0].type == PieceType.ROOK && !pieces[0].hasMoved) {
                castlingAvailability.append("Q");
            }
        }
        if (!getBothTeams) {
            if (team == Team.WHITE) return castlingAvailability.toString();
            castlingAvailability = new StringBuilder();
        }
        if (blackKing.equals(new IntPoint2D(4, 7)) && pieces[4 + 7 * 8] != null && pieces[4 + 7 * 8].type == PieceType.KING && !pieces[4 + 7 * 8].hasMoved) {
            if (pieces[7 + 7 * 8] != null && pieces[7 + 7 * 8].type == PieceType.ROOK && !pieces[7 + 7 * 8].hasMoved) {
                castlingAvailability.append("k");
            }
            if (pieces[7 * 8] != null && pieces[7 * 8].type == PieceType.ROOK && !pieces[7 * 8].hasMoved) {
                castlingAvailability.append("q");
            }
        }
        return castlingAvailability.toString();
    }
    private static void checkForChecks(List<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
        for (int i = 0; i < list.size(); i++) {
            IntPoint2D move = list.get(i);

            board.tryMovingPieces(selection, move);

            if (checkForChecks(team, board) == CheckState.CHECK) {
                list.remove(i);
                i--;
            }

            board.undoMove();
        }
    }
    public static CheckState checkForChecks(Team team, Board board) {
        ArrayList<IntPoint2D> list = new ArrayList<>();
        IntPoint2D king = board.getKing(team);

        for (PieceType type : PieceType.values()) {
            PieceInfo info = new PieceInfo(team, type);
            getValidMoves(list, king, info, board, false, false);
            for (IntPoint2D move : list) {
                PieceInfo target = board.getPiece(move);
                if (target != null && target.type == type) {
                    return CheckState.CHECK;
                }
            }
            list.clear();
        }
        return CheckState.NONE;
    }
    private static List<IntPoint2D> getValidMovesQueen(List<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int yDir = -1; yDir <= 1; yDir++) {
                if (xDir == 0 && yDir == 0) {
                    continue;
                }
                pieceDirections(list, selection, team, board, xDir, yDir);
            }
        }
        return list;
    }

    private static List<IntPoint2D> pieceDirections(List<IntPoint2D> list, IntPoint2D selection, Team team, Board board, int xDir, int yDir) {
        IntPoint2D move = selection;
        do {
            move = move.transpose(xDir, yDir);
        } while (checkIfInBounds(list, team, board, move));
        return list;
    }
    private static List<IntPoint2D> getValidMovesKnight(List<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
        for (int direction = 0; direction < 2; direction++) {
            for (int longDir = -2; longDir <= 2; longDir += 4) {
                for (int shortDir = -1; shortDir <= 1; shortDir += 2) {
                    IntPoint2D move;
                    if (direction == 0) {
                        move = selection.transpose(longDir, shortDir);
                    } else {
                        move = selection.transpose(shortDir, longDir);
                    }
                    PieceInfo target = board.getPiece(move);
                    if (board.isInBounds(move) && (target == null || target.team != team)) {
                        list.add(move);
                    }
                }
            }
        }
        return list;
    }
    private static List<IntPoint2D> getValidMovesKing(List<IntPoint2D> list, IntPoint2D selection, Team team, Board board, boolean checkCastling) {
        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int yDir = -1; yDir <= 1; yDir++) {
                if (xDir == 0 && yDir == 0) {
                    continue;
                }
                IntPoint2D move = selection.transpose(xDir, yDir);
                PieceInfo target = board.getPiece(move);
                if (board.isInBounds(move) && (target == null || target.team != team)) {
                    list.add(move);
                }

                // Check if castling moves are valid
                if (checkCastling) {
                    list.addAll(checkCastling(selection, team, board, xDir, yDir));
                }
            }
        }
        return list;
    }
    public static List<IntPoint2D> checkCastling(IntPoint2D selection, Team team, Board board, int xDir, int yDir) {
        List<IntPoint2D> list = new ArrayList<>();
        String teamCastle = isCastlingPossible(team, board.pieces, board.whiteKing, board.blackKing, false);
        // Kingside Castling
        ArrayList<IntPoint2D> x = checkKingsideCastling(selection, team, board, xDir, yDir, list, teamCastle);
        if (x != null) return x;
        ArrayList<IntPoint2D> x1 = checkQueensideCastling(selection, team, board, xDir, yDir, list, teamCastle);
        // Queenside Castling
        if (x1 != null) return x1;
        return list;
    }

    private static ArrayList<IntPoint2D> checkQueensideCastling(IntPoint2D selection, Team team, Board board, int xDir, int yDir, List<IntPoint2D> list, String teamCastle) {
        if (xDir == -1 && yDir == 0 && selection.equals(team == Team.WHITE ? new IntPoint2D(4, 0) : new IntPoint2D(4, 7)))  {
            if (isEnemyAttackingThisSquare(selection.transpose(xDir, yDir), team, board)
                    || isEnemyAttackingThisSquare(selection.transpose(xDir - 1, yDir), team, board)
                    || isEnemyAttackingThisSquare(selection.transpose(xDir - 2, yDir), team, board))
                return new ArrayList<>();
            if (board.getPiece(selection.transpose(xDir, yDir)) != null
                    || board.getPiece(selection.transpose(xDir - 1, yDir)) != null
                    || board.getPiece(selection.transpose(xDir - 2, yDir)) != null) {
                System.out.println((board.getPiece(selection.transpose(xDir, yDir)) != null ? board.getPiece(selection.transpose(xDir, yDir)).type : "null") + " " + (board.getPiece(selection.transpose(xDir - 1, yDir)) != null ? board.getPiece(selection.transpose(xDir - 1, yDir)).type : "null") + " " + (board.getPiece(selection.transpose(xDir - 2, yDir)) != null ? board.getPiece(selection.transpose(xDir - 2, yDir)).type : "null"));
                return new ArrayList<>();
            }

            if ((teamCastle.contains("Q") && team == Team.WHITE) || (teamCastle.contains("q") && team == Team.BLACK)) {
                IntPoint2D castleMove = selection.transpose(xDir - 1, yDir);
                PieceInfo castleTarget = board.getPiece(castleMove);
                if (board.isInBounds(castleMove) && (castleTarget == null || castleTarget.team != team)) {
                    list.add(castleMove);
                }
            }
        }
        return null;
    }

    private static ArrayList<IntPoint2D> checkKingsideCastling(IntPoint2D selection, Team team, Board board, int xDir, int yDir, List<IntPoint2D> list, String teamCastle) {
        if (xDir == 1 && yDir == 0 && selection.equals(team == Team.WHITE ? board.whiteKing : board.blackKing))  {
            if (isEnemyAttackingThisSquare(selection.transpose(xDir, yDir), team, board)
                    || isEnemyAttackingThisSquare(selection.transpose(xDir + 1, yDir), team, board))
                return new ArrayList<>();
            if (board.getPiece(selection.transpose(xDir, yDir)) != null
                    || board.getPiece(selection.transpose(xDir + 1, yDir)) != null)
                return new ArrayList<>();

            if ((teamCastle.contains("K") && team == Team.WHITE) || (teamCastle.contains("k") && team == Team.BLACK)) {
                IntPoint2D castleMove = selection.transpose(xDir + 1, yDir);
                PieceInfo castleTarget = board.getPiece(castleMove);
                if (board.isInBounds(castleMove) && (castleTarget == null || castleTarget.team != team)) {
                    list.add(castleMove);
                }
            }
        }
        return null;
    }

    private static List<IntPoint2D> getValidMovesRook(List<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
        for (int direction = 0; direction < 2; direction++) {
            for (int direction2 = -1; direction2 <= 1; direction2 += 2) {
                IntPoint2D move = selection;
                do {
                    if (direction == 0) {
                        move = move.transpose(direction2, 0);
                    } else {
                        move = move.transpose(0, direction2);
                    }

                } while (checkIfInBounds(list, team, board, move));
            }
        }
        return list;
    }
    private static boolean checkIfInBounds(List<IntPoint2D> list, Team team, Board board, IntPoint2D move) {
        if (!board.isInBounds(move)) {
            return false;
        }

        PieceInfo target = board.getPiece(move);

        if (target != null) {
            if (target.team != team) {
                list.add(move);
            }
            return false;
        }

        list.add(move);
        return true;
    }


    private static List<IntPoint2D> getValidMovesBishop(List<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
        for (int xDir = -1; xDir <= 1; xDir += 2) {
            for (int yDir = -1; yDir <= 1; yDir += 2) {
                pieceDirections(list, selection, team, board, xDir, yDir);
            }
        }
        return list;
    }


    public static List<IntPoint2D> getValidMovesPawn(List<IntPoint2D> list, IntPoint2D point, Team team, Board board) {
        int forwardDirection = (team == playerTeam) ? 1 : -1;
        IntPoint2D forwardPoint = new IntPoint2D(point.getX(), (point.getY() + (forwardDirection)));
        IntPoint2D doubleForwardPoint = new IntPoint2D(point.getX(), (point.getY() + (2 * forwardDirection)));
        // Check for normal move forward
        if (board.isInBounds(forwardPoint) && board.getPiece(forwardPoint) == null) {
            list.add(forwardPoint);

            // Check for double move forward (if the pawn hasn't moved yet)
            if (team == (playerTeam == Team.WHITE ? Team.WHITE : Team.BLACK) && point.getY() == 1 && board.getPiece(doubleForwardPoint) == null) {
                list.add(doubleForwardPoint);
            } else if (team == (playerTeam == Team.BLACK ? Team.WHITE : Team.BLACK) && point.getY() == 6 && board.getPiece(doubleForwardPoint) == null) {
                list.add(doubleForwardPoint);
            }
        }

        // Check for capturing moves diagonally
        IntPoint2D leftCapture = new IntPoint2D(point.getX() - 1, point.getY() + forwardDirection);
        IntPoint2D rightCapture = new IntPoint2D(point.getX() + 1, point.getY() + forwardDirection);

        if (board.isInBounds(leftCapture)) {
            PieceInfo leftCapturePiece = board.getPiece(leftCapture);
            if (board.lastMovedDoubleWhitePawn == null || board.lastMovedDoubleBlackPawn == null) {
                return new ArrayList<>();
            }

            if (leftCapturePiece != null && leftCapturePiece.team != team) {
                list.add(leftCapture);
            }
            checkEnPassant(list, team, board, leftCapture, leftCapturePiece, point);
        }

        if (board.isInBounds(rightCapture)) {
            PieceInfo rightCapturePiece = board.getPiece(rightCapture);
            if (board.lastMovedDoubleWhitePawn == null || board.lastMovedDoubleBlackPawn == null) {
                return new ArrayList<>();
            }

            if (rightCapturePiece != null && rightCapturePiece.team != team) {
                list.add(rightCapture);
            }
            checkEnPassant(list, team, board, rightCapture, rightCapturePiece, point);
        }

        // Check for promotion moves
        if (list.size() > 0 && (forwardPoint.getY() == 7 || forwardPoint.getY() == 0)) {
            Array<IntPoint2D> promotionMoves = new Array<>();

            for (IntPoint2D move : list) {
                if (move.getY() == 0 || move.getY() == 7) {
                    promotionMoves.add(move);
                    System.out.println("Promotion move: " + move);
                }
            }
        }
        return list;
    }

    private static List<IntPoint2D> checkEnPassant(List<IntPoint2D> list, Team team, Board board, IntPoint2D capture, PieceInfo capturePiece, IntPoint2D point) {
        IntPoint2D leftPoint = new IntPoint2D(point.getX() - 1, point.getY());
        IntPoint2D rightPoint = new IntPoint2D(point.getX() + 1, point.getY());

        if (board.getPiece(rightPoint) != null
                && board.getPiece(rightPoint).type.equals(PieceType.PAWN)
                && board.getPiece(rightPoint).team != team
                && (team == playerTeam ? point.getY() == 4 : point.getY() == 3)
                && board.getPiece(rightPoint).doublePawnMoveOnMoveNumber == GameState.moveNumber
                && point.getX() < capture.getX()) {
            list.add(capture);
        }
        if (board.getPiece(leftPoint) != null
                && board.getPiece(leftPoint).type.equals(PieceType.PAWN)
                && board.getPiece(leftPoint).team != team
                && (team == playerTeam ? point.getY() == 4 : point.getY() == 3)
                && board.getPiece(leftPoint).doublePawnMoveOnMoveNumber == GameState.moveNumber
                && point.getX() > capture.getX()) {
            list.add(capture);
        }
        return list;
    }

    public static boolean isKingInCheck(Team team, Board board) {
        IntPoint2D kingPosition = team == Team.WHITE ? board.whiteKing : board.blackKing;
        return isEnemyAttackingThisSquare(kingPosition, team, board);
    }

    public static boolean isCheckmate(Team team, Board board) {
        if (!isKingInCheck(team, board)) {
            return false;
        }
        List<IntPoint2D> validMoves = new ArrayList<>();
        for (int i = 0; i < board.pieces.length; i++) {
            IntPoint2D point = board.getPointFromArrayIndex(i);
            PieceInfo piece = board.pieces[i];
            if (piece != null && piece.team == team) {
                getValidMoves(validMoves, point, piece, board);
                for (IntPoint2D move : validMoves) {
                    board.tryMovingPieces(point, move);
                    if (!isKingInCheck(team, board)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isStalemate(Team team, Board board) {
        if (isKingInCheck(team, board)) {
            return false;
        }
        List<IntPoint2D> validMoves = new ArrayList<>();
        for (int i = 0; i < board.pieces.length; i++) {
            IntPoint2D point = board.getPointFromArrayIndex(i);
            PieceInfo piece = board.pieces[i];
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
