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

enum CheckState {
    NONE,
    CHECK,
    MATE
}

public class Rules {
    static final Team playerTeam = GameState.playerTeam;
    private Rules() {

    }

    static void getValidMoves(ArrayList<IntPoint2D> list, IntPoint2D selection, PieceInfo piece, Board board) {
        getValidMoves(list, selection, piece, board, true);
    }
    static void getValidMoves(ArrayList<IntPoint2D> list, IntPoint2D selection, PieceInfo piece, Board board, boolean checkForChecks) {
        switch (piece.type) {
            case PAWN:
                getValidMovesPawn(list, selection, piece.team, board);
                break;
            case BISHOP:
                getValidMovesBishop(list, selection, piece.team, board);
                break;
            case KNIGHT:
                getValidMovesKnight(list, selection, piece.team, board);
                break;
            case ROOK:
                getValidMovesRook(list, selection, piece.team, board);
                break;
            case KING:
                getValidMovesKing(list, selection, piece.team, board);
                break;
            case QUEEN:
                getValidMovesQueen(list, selection, piece.team, board);
                break;
        }
        if (checkForChecks) {
            checkForChecks(list, selection, piece.team, board);
        }
    }
    private static void checkForChecks(ArrayList<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
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
            getValidMoves(list, king, info, board, false);
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
    private static void getValidMovesQueen(ArrayList<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int yDir = -1; yDir <= 1; yDir++) {
                if (xDir == 0 && yDir == 0) {
                    continue;
                }
                pieceDirections(list, selection, team, board, xDir, yDir);
            }
        }
    }

    private static void pieceDirections(ArrayList<IntPoint2D> list, IntPoint2D selection, Team team, Board board, int xDir, int yDir) {
        IntPoint2D move = selection;
        do {
            move = move.transpose(xDir, yDir);
        } while (checkIfInBounds(list, team, board, move));
    }
    private static void getValidMovesKnight(ArrayList<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
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
    }
    private static void getValidMovesKing(ArrayList<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
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
            }
        }
    }
    private static void getValidMovesRook(ArrayList<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
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
    }
    private static boolean checkIfInBounds(ArrayList<IntPoint2D> list, Team team, Board board, IntPoint2D move) {
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


    private static void getValidMovesBishop(ArrayList<IntPoint2D> list, IntPoint2D selection, Team team, Board board) {
        for (int xDir = -1; xDir <= 1; xDir += 2) {
            for (int yDir = -1; yDir <= 1; yDir += 2) {
                pieceDirections(list, selection, team, board, xDir, yDir);
            }
        }
    }


    public static void getValidMovesPawn(ArrayList<IntPoint2D> list, IntPoint2D point, Team team, Board board) {
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
                return;
            }

            if (leftCapturePiece != null && leftCapturePiece.team != team) {
                list.add(leftCapture);
            }
            checkEnPassant(list, team, board, leftCapture, leftCapturePiece, point);
        }

        if (board.isInBounds(rightCapture)) {
            PieceInfo rightCapturePiece = board.getPiece(rightCapture);
            if (board.lastMovedDoubleWhitePawn == null || board.lastMovedDoubleBlackPawn == null) {
                return;
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
    }

    private static void checkEnPassant(ArrayList<IntPoint2D> list, Team team, Board board, IntPoint2D capture, PieceInfo capturePiece, IntPoint2D point) {
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
    }
}
