package lukas.sobotik.sightlessknight.ai;

import lukas.sobotik.sightlessknight.gamelogic.*;
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
                if ((moveLocation.getY() == 0 || moveLocation.getY() == 7) && piece.type == PieceType.PAWN) {
                    // Add four promotion options: bishop, knight, rook, queen
                    List<PieceType> promotionPieces = Arrays.asList(PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN);
                    for (PieceType promotionPiece : promotionPieces) {
                        Move promotionMove = new Move(location, moveLocation, piece, board.getPiece(moveLocation));
                        promotionMove.setPromotionPiece(promotionPiece);
                        validMoves.add(promotionMove);
                    }
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
            gameState.testsPlayMove(move);

            int positions = playMoves(depth - 1, turn == Team.BLACK ? Team.WHITE : Team.BLACK, log);
            numberOfPositions += positions;
            numberOfPositionsOnMove.put(move.getFrom().getAlgebraicNotationLocation() + move.getTo().getAlgebraicNotationLocation(), positions);

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
}
