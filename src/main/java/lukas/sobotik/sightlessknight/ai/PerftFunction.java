package lukas.sobotik.sightlessknight.ai;

import lukas.sobotik.sightlessknight.gamelogic.*;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;
import lukas.sobotik.sightlessknight.views.play.PlayView;

import java.util.*;

public class PerftFunction {
    Board board;
    GameState gameState;
    PlayView view;
    public PerftFunction(Board board, GameState gameState, PlayView view) {
        this.board = board;
        this.gameState = gameState;
        this.view = view;

        GameState.moveNumber = 0;
        GameState.capturedPieces = 0;
        GameState.enPassantCaptures = 0;
        GameState.enPassantCapturesReturned = 0;
    }
    public List<Move> getAllValidMoves(Team team) {
        List<Move> validMoves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            var piece = board.pieces[i];
            var location = board.getPointFromArrayIndex(i);
            if (piece == null || piece.team != team) continue;

            List<BoardLocation> moves = new ArrayList<>();
            Rules.getValidMoves(moves, board.getPointFromArrayIndex(i), piece, board, true);
            validMoves.addAll(new HashSet<>(moves).stream().map(moveLocation -> {
                Move move = new Move(location, moveLocation, piece, board.getPiece(moveLocation));
                // Pawn Promotion
                if ((moveLocation.getY() == 0 || moveLocation.getY() == 7) && piece.type == PieceType.PAWN) {
                    // Add four promotion options: bishop, knight, rook, queen
                    List<PieceType> promotionPieces = Arrays.asList(PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN);
                    for (PieceType promotionPiece : promotionPieces) {
                        Move promotionMove = new Move(location, moveLocation, piece, board.getPiece(moveLocation));
                        promotionMove.setPromotionPiece(promotionPiece);
                        validMoves.add(promotionMove);
                    }
                }
                // En Passant
                BoardLocation enPassantCapture = new BoardLocation(moveLocation.getX(), location.getY());
                if (board.getPiece(enPassantCapture) != null
                        && location.getX() != moveLocation.getX()) {
                    piece.enPassant = true;
                }
                return move;
            }).toList());
        }
        return validMoves;
    }
    public int playMoves(int depth, Team turn, boolean log) {
        if (depth == 0) return 1;

        List<Move> moves = getAllValidMoves(turn);
        int numberOfPositions = 0;
        Map<String, Integer> numberOfPositionsOnMove = new HashMap<>();

        for (Move move : moves) {
            debugPause(numberOfPositions, move);
            gameState.playTestMove(move);

            int positions = playMoves(depth - 1, turn == Team.BLACK ? Team.WHITE : Team.BLACK, log);
            numberOfPositions += positions;
            numberOfPositionsOnMove.put(move.getFrom().getAlgebraicNotationLocation() + move.getTo().getAlgebraicNotationLocation(), positions);
            debugPause(numberOfPositions, move);

            gameState.undoMove(move);
        }

        if (log) {
            System.out.println("------------------------------------------");
            List<String> sortedKeys = new ArrayList<>(numberOfPositionsOnMove.keySet());
            Collections.sort(sortedKeys);
            for (String key : sortedKeys) {
                System.out.println(key + ": " + numberOfPositionsOnMove.get(key));
            }
        }

        return numberOfPositions;
    }

    /***
     * Helpful method for debugging
     * @param numberOfPositions number of positions for the Perft Function generated so far
     * @param move what move the Perft Function is currently processing
     */
    private void debugPause(int numberOfPositions, Move move) {
        boolean pause = false;
        if (move.getFrom().getAlgebraicNotationLocation().equals("c7") && move.getTo().getAlgebraicNotationLocation().equals("c6")) pause = true;
        if (move.getMovedPiece().type.equals(PieceType.PAWN)) pause = true;

        // Manual Override
        pause = false;

        if (pause && view != null) {
            view.getUI().ifPresent(value -> value.access(() -> view.createBoard(board.pieces)));
            view.getUI().ifPresent(value -> value.access(() -> view.showTargetSquare(String.valueOf(GameState.moveNumber))));
            board.printBoardInConsole(true);
            sleep();
        } else if (pause) {
            System.out.println("-------------------");
            System.out.println(numberOfPositions);
            if (move.getFrom() == move.getTo()) System.out.println("EXPECTED ERROR");
            board.printBoardInConsole(true);
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
