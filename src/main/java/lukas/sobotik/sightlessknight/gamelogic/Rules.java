package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Rules {
    static final Team playerTeam = GameState.playerTeam;
    static boolean[] castlingNotAvailable = {false, false, false, false};
    private Rules() {

    }
    /**
     * Method with default settings for getting valid moves of a piece
     * @param selectedPieceLocation where the piece is located on the board
     * @param piece what piece are the moves generated for
     * @param board the board where the moves take place
     * @param removePseudoLegalMoves whether the method should check for checks and piece pins
     * @return list of valid moves for a given piece
     */
    public static List<Move> getValidMoves(BoardLocation selectedPieceLocation, Piece piece, Board board, boolean removePseudoLegalMoves) {
        return getValidMoves(selectedPieceLocation, piece, board, removePseudoLegalMoves, true, false);
    }

    /**
     * Method with manual settings for getting valid moves of a piece
     * @param selectedPieceLocation where the piece is located on the board
     * @param piece what piece are the moves generated for
     * @param board the board where the moves take place
     * @param removePseudoLegalMoves whether the method should check for checks and piece pins
     * @param addCastlingMoves whether the method should add castling moves
     * @param ignoreEnPassant whether the method should ignore en passant moves
     * @return list of valid moves for a given piece
     */
    static List<Move> getValidMoves(BoardLocation selectedPieceLocation, Piece piece, Board board, boolean removePseudoLegalMoves, boolean addCastlingMoves, boolean ignoreEnPassant) {
        List<Move> legalMoves = new ArrayList<>();
        switch (piece.type) {
            case PAWN -> legalMoves.addAll(Objects.requireNonNull(getValidPawnMoves(selectedPieceLocation, piece.team, board, ignoreEnPassant)));
            case BISHOP -> legalMoves.addAll(getValidBishopMoves(legalMoves, selectedPieceLocation, piece.team, board, false));
            case KNIGHT -> legalMoves.addAll(getValidKnightMoves(selectedPieceLocation, piece.team, board, false));
            case ROOK -> legalMoves.addAll(getValidRookMoves(selectedPieceLocation, piece.team, board, false));
            case KING -> legalMoves.addAll(getValidKingMoves(legalMoves, selectedPieceLocation, piece.team, board, addCastlingMoves, false));
            case QUEEN -> legalMoves.addAll(getValidQueenMoves(legalMoves, selectedPieceLocation, piece.team, board, false));
        }
        if (removePseudoLegalMoves) {
            removePseudoLegalMoves(legalMoves, selectedPieceLocation, piece.team, board);
        }
        return legalMoves;
    }

    /**
     * Used for disambiguating friendly moves, it will return all possible moves including friendly piece captures, so it shouldn't be used for normal applications
     * @param selectedPieceLocation location of the piece that the method will generate legal moves for
     * @param piece piece that the method will generate moves for
     * @param board the board where the moves take place
     * @return list of all pseudo-legal moves for a given piece
     */
    static List<Move> getPseudoLegalMoves(BoardLocation selectedPieceLocation, Piece piece, Board board) {
        List<Move> legalMoves = new ArrayList<>();
        switch (piece.type) {
            case KNIGHT -> legalMoves.addAll(getValidKnightMoves(selectedPieceLocation, piece.team, board, true));
            case BISHOP -> legalMoves.addAll(getValidBishopMoves(legalMoves, selectedPieceLocation, piece.team, board, true));
            case ROOK -> legalMoves.addAll(getValidRookMoves(selectedPieceLocation, piece.team, board, true));
            case QUEEN -> legalMoves.addAll(getValidQueenMoves(legalMoves, selectedPieceLocation, piece.team, board, true));
            case KING -> legalMoves.addAll(getValidKingMoves(legalMoves, selectedPieceLocation, piece.team, board, true, true));
        }
        return legalMoves;
    }

    /**
     * This method is used to get all valid moves for a specific team on the board.
     * It also handles special moves like pawn promotion and en passant.
     * @param team The team for which to generate the moves.
     * @param board The current state of the game board.
     * @param addTestMoves A boolean flag indicating whether to add test moves or not. If true, it will add all possible promotion moves for pawns and mark en passant possible moves.
     * @return A list of all valid moves for the given team.
     */
    public static List<Move> getAllValidMovesForTeam(Team team, Board board, boolean addTestMoves) {
        List<Move> validMoves = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            var piece = board.pieces[i];
            var location = board.getPointFromArrayIndex(i);
            if (piece == null || piece.team != team) continue;

            var allMoves = Rules.getValidMoves(board.getPointFromArrayIndex(i), piece, board, true);
            validMoves.addAll(new HashSet<>(allMoves).stream().map(move -> {
                if (!addTestMoves) return move;
                var moveLocation = move.getTo();
                // Pawn Promotion
                if (((moveLocation.getY() == 0 && piece.team == Team.BLACK) || (moveLocation.getY() == 7 && piece.team == Team.WHITE))
                        && piece.type == PieceType.PAWN) {
                    // Add four promotion options: bishop, knight, rook, queen
                    List<PieceType> promotionPieces = Arrays.asList(PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN);
                    for (PieceType promotionPiece : promotionPieces) {
                        Move promotionMove = new Move(location, moveLocation, piece, board.getPiece(moveLocation));
                        promotionMove.setPromotionPiece(promotionPiece);
                        if (promotionPiece == PieceType.QUEEN) return promotionMove;
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

    /**
     * Method that returns whether enemy is attacking a certain square on the board
     * @param square what square the method should search for
     * @param friendlyTeam friendly team
     * @param board board where the pieces move
     * @return true or false depending on whether enemy attacks the given square
     */
    public static boolean isSquareAttackedByEnemy(BoardLocation square, Team friendlyTeam, Board board) {
        List<Move> list;

        for (PieceType type : PieceType.values()) {
            Piece info = new Piece(friendlyTeam, type);
            list = getValidMoves(square, info, board, false, false, true);
            for (Move move : list) {
                var to = move.getTo();
                Piece target = board.getPiece(to);
                if (target != null && target.type == type) {
                    return true;
                }
            }
            list.clear();
        }
        return false;
    }

    /**
     * Method that returns whether a king can castle
     * @param team what team's king castling rights the method should return
     * @param pieces array of pieces on the board
     * @param whiteKing location of the white king
     * @param blackKing location of the black king
     * @param returnBothTeams whether the method should return both teams' castling rights
     * @return the FEN notation of castling rights ["K" for white's kingside castling rights, "Q" for white's queenside castling rights, "k" for black's kingside castling rights, "q" for black's queenside castling rights], e.g. "KQkq" or "Kkq"
     */
    public static String isCastlingPossible(Team team, Piece[] pieces, Board board, BoardLocation whiteKing, BoardLocation blackKing, boolean returnBothTeams) {
        StringBuilder castlingAvailability = new StringBuilder();
        if (whiteKing == null || blackKing == null) return "";

        boolean whiteKingMoved;
        boolean whiteKingRookMoved;
        boolean whiteQueenRookMoved;
        if (board == null) {
            whiteKingMoved = whiteKing.equals(new BoardLocation(4, 0))
                    && pieces[4] != null
                    && pieces[4].type == PieceType.KING
                    && !pieces[4].hasMoved;
            whiteKingRookMoved = pieces[7] != null
                    && pieces[7].type == PieceType.ROOK
                    && !pieces[7].hasMoved;
            whiteQueenRookMoved = pieces[0] != null
                    && pieces[0].type == PieceType.ROOK
                    && !pieces[0].hasMoved;
        } else {
            whiteKingMoved = !board.whiteKingMoves.getValue()
                    && pieces[4] != null
                    && pieces[4].type == PieceType.KING;
            whiteKingRookMoved = !board.whiteKingRookMoves.getValue()
                    && pieces[7] != null
                    && pieces[7].type == PieceType.ROOK;
            whiteQueenRookMoved = !board.whiteQueenRookMoves.getValue()
                    && pieces[0] != null
                    && pieces[0].type == PieceType.ROOK;
        }

        boolean blackKingMoved;
        boolean blackKingRookMoved;
        boolean blackQueenRookMoved;
        if (board == null) {
            blackKingMoved = blackKing.equals(new BoardLocation(4, 7))
                    && pieces[4 + 7 * 8] != null
                    && pieces[4 + 7 * 8].type == PieceType.KING
                    && !pieces[4 + 7 * 8].hasMoved;
            blackKingRookMoved = pieces[7 + 7 * 8] != null
                    && pieces[7 + 7 * 8].type == PieceType.ROOK
                    && !pieces[7 + 7 * 8].hasMoved;
            blackQueenRookMoved = pieces[7 * 8] != null
                    && pieces[7 * 8].type == PieceType.ROOK
                    && !pieces[7 * 8].hasMoved;
        } else {
            blackKingMoved = !board.blackKingMoves.getValue()
                    && pieces[4 + 7 * 8] != null
                    && pieces[4 + 7 * 8].type == PieceType.KING;
            blackKingRookMoved = !board.blackKingRookMoves.getValue()
                    && pieces[7 + 7 * 8] != null
                    && pieces[7 + 7 * 8].type == PieceType.ROOK;
            blackQueenRookMoved = !board.blackQueenRookMoves.getValue()
                    && pieces[7 * 8] != null
                    && pieces[7 * 8].type == PieceType.ROOK;
        }

        if (whiteKingMoved) {
            if (whiteKingRookMoved && !castlingNotAvailable[0]) {
                castlingAvailability.append("K");
            }
            if (whiteQueenRookMoved && !castlingNotAvailable[1]) {
                castlingAvailability.append("Q");
            }
        }
        if (!returnBothTeams) {
            if (team == Team.WHITE) return castlingAvailability.toString();
            castlingAvailability = new StringBuilder();
        }
        if (blackKingMoved) {
            if (blackKingRookMoved && !castlingNotAvailable[2]) {
                castlingAvailability.append("k");
            }
            if (blackQueenRookMoved && !castlingNotAvailable[3]) {
                castlingAvailability.append("q");
            }
        }
        return castlingAvailability.toString();
    }

    /**
     * Method that removes pseudo-legal moves (e.g. moves through pins to the king)
     * @param legalMoves piece's legal moves
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team which team the piece is
     * @param board the board where the pieces move
     */
    private static void removePseudoLegalMoves(List<Move> legalMoves, BoardLocation selectedPieceLocation, Team team, Board board) {
        for (int i = 0; i < legalMoves.size(); i++) {
            BoardLocation move = legalMoves.get(i).getTo();

            board.movePieceWithoutSpecialMovesAndSave(selectedPieceLocation, move);

            if (isKingInCheck(team, board)) {
                legalMoves.remove(i);
                i--;
            }

            board.undoLastMove();
        }
    }

    /**
     * Method that returns legal moves for a given queen
     * @param legalQueenMoves list that the method will update
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the queen is
     * @param board the board where the pieces move
     * @param ignoreFriendlyPieces whether the queen can move through and capture friendly pieces
     * @return list of legal queen moves
     */
    private static List<Move> getValidQueenMoves(List<Move> legalQueenMoves, BoardLocation selectedPieceLocation, Team team, Board board, boolean ignoreFriendlyPieces) {
        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int yDir = -1; yDir <= 1; yDir++) {
                if (xDir == 0 && yDir == 0) {
                    continue;
                }
                pieceDirections(legalQueenMoves, selectedPieceLocation, team, board, xDir, yDir, ignoreFriendlyPieces);
            }
        }
        return legalQueenMoves;
    }

    /**
     * Transposes the selected piece's location on the board along the specified direction (xDir, yDir).
     * The transposition is repeated until it encounters the boundary of the board or a blocking piece.
     *
     * @param boardLocations the list of all valid board locations
     * @param selectedPieceLocation to location of the piece on the board
     * @param team what team the piece is
     * @param board the board where the piece move
     * @param xDir The direction to move along the x-axis (horizontal direction).
     * @param yDir The direction to move along the y-axis (vertical direction).
     * @param ignoreFriendlyPieces If true, ignore friendly pieces while transposing; otherwise, consider them as blocking pieces.
     */
    private static void pieceDirections(List<Move> boardLocations, BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir, boolean ignoreFriendlyPieces) {
        BoardLocation move = selectedPieceLocation;
        do {
            move = move.transpose(xDir, yDir);
        } while (checkIfInBounds(boardLocations, team, board, selectedPieceLocation, move, ignoreFriendlyPieces));
    }

    /**
     * Returns valid moves for a given knight
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the knight is
     * @param board the board where the pieces move
     * @param ignoreFriendlyPieces whether the knight can move through and capture friendly pieces
     * @return list of legal knight moves
     */
    private static List<Move> getValidKnightMoves(BoardLocation selectedPieceLocation, Team team, Board board, boolean ignoreFriendlyPieces) {
        List<Move> legalKnightMoves = new ArrayList<>();
        for (int direction = 0; direction < 2; direction++) {
            for (int longDir = -2; longDir <= 2; longDir += 4) {
                for (int shortDir = -1; shortDir <= 1; shortDir += 2) {
                    BoardLocation to;
                    if (direction == 0) {
                        to = selectedPieceLocation.transpose(longDir, shortDir);
                    } else {
                        to = selectedPieceLocation.transpose(shortDir, longDir);
                    }
                    Piece target = board.getPiece(to);
                    if (board.isInBounds(to) && ((target == null || target.team != team) || ignoreFriendlyPieces)) {
                        Move move = new Move(selectedPieceLocation, to, board.getPiece(selectedPieceLocation), target);
                        legalKnightMoves.add(move);
                    }
                }
            }
        }
        return legalKnightMoves;
    }

    /**
     * Returns valid moves for a given king
     * @param legalKingMoves list the method will update
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the king is
     * @param board the board where the pieces move
     * @param addCastlingMoves whether it should check for castling moves
     * @return list of legal king moves
     */
    private static List<Move> getValidKingMoves(List<Move> legalKingMoves, BoardLocation selectedPieceLocation, Team team, Board board, boolean addCastlingMoves, boolean ignoreFriendlyPieces) {
        for (int xDir = -1; xDir <= 1; xDir++) {
            for (int yDir = -1; yDir <= 1; yDir++) {
                if (xDir == 0 && yDir == 0) {
                    continue;
                }
                BoardLocation move = selectedPieceLocation.transpose(xDir, yDir);
                Piece target = board.getPiece(move);
                if (board.isInBounds(move) && ((target == null || target.team != team) || ignoreFriendlyPieces)) {
                    legalKingMoves.add(new Move(selectedPieceLocation, move, board.getPiece(selectedPieceLocation), target));
                }

                // Check if castling moves are valid
                if (addCastlingMoves) {
                    legalKingMoves.addAll(getValidCastlingMoves(selectedPieceLocation, team, board, xDir, yDir));
                }
            }
        }
        return legalKingMoves;
    }

    /**
     * Method returning all valid castling moves for a team
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the method will check castling for
     * @param board the board where the pieces move
     * @param xDir The direction to move along the x-axis (horizontal direction).
     * @param yDir The direction to move along the y-axis (vertical direction).
     * @return list of valid castling moves for the given team
     */
    public static List<Move> getValidCastlingMoves(BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir) {
        List<Move> legalKingMoves = new ArrayList<>();
        String teamCastle = isCastlingPossible(team, board.pieces, board, board.whiteKingLocation, board.blackKingLocation, false);
        // Kingside Castling
        List<Move> kingsideCastling = checkKingsideCastling(selectedPieceLocation, team, board, xDir, yDir, legalKingMoves, teamCastle);
        if (kingsideCastling != null) return kingsideCastling;
        // Queenside Castling
        List<Move> queensideCastling = checkQueensideCastling(selectedPieceLocation, team, board, xDir, yDir, legalKingMoves, teamCastle);
        if (queensideCastling != null) return queensideCastling;
        return legalKingMoves;
    }

    /**
     * Method that will add queenside castling moves if it's legal
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the king is
     * @param board the board where the pieces move
     * @param xDir The direction to move along the x-axis (horizontal direction).
     * @param yDir The direction to move along the y-axis (vertical direction).
     * @param legalKingMoves list the method will add the castling moves to
     * @param fen the FEN string used to determine whether castling is legal
     * @return returns all king moves that include queenside castling if it's legal
     */
    private static List<Move> checkQueensideCastling(BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir, List<Move> legalKingMoves, String fen) {
        if (xDir == -1 && yDir == 0 && selectedPieceLocation.equals(team == Team.WHITE ? new BoardLocation(4, 0) : new BoardLocation(4, 7)))  {
            if (isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir, yDir), team, board)
                    || isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir - 1, yDir), team, board))
                return new ArrayList<>();
            if (board.getPiece(selectedPieceLocation.transpose(xDir, yDir)) != null
                    || board.getPiece(selectedPieceLocation.transpose(xDir - 1, yDir)) != null
                    || board.getPiece(selectedPieceLocation.transpose(xDir - 2, yDir)) != null) {
                return new ArrayList<>();
            }
            if (isKingInCheck(team, board)) {
                return new ArrayList<>();
            }

            if ((fen.contains("Q") && team == Team.WHITE) || (fen.contains("q") && team == Team.BLACK)) {
                BoardLocation castleMove = selectedPieceLocation.transpose(xDir - 1, yDir);
                Piece castleTarget = board.getPiece(castleMove);
                if (board.isInBounds(castleMove) && (castleTarget == null || castleTarget.team != team)) {
                    var move = new Move(selectedPieceLocation, castleMove, board.getPiece(selectedPieceLocation));
                    move.setMoveFlag(MoveFlag.queensideCastling);
                    legalKingMoves.add(move);
                }
            }
        }
        return null;
    }

    /**
     * Method that will add kingside castling moves if it's legal
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the king is
     * @param board the board where the pieces move
     * @param xDir The direction to move along the x-axis (horizontal direction).
     * @param yDir The direction to move along the y-axis (vertical direction).
     * @param legalKingMoves list the method will add the castling moves to
     * @param fen the FEN string used to determine whether castling is legal
     * @return returns all king moves that include kingside castling if it's legal
     */
    public static List<Move> checkKingsideCastling(BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir, List<Move> legalKingMoves, String fen) {
        if (xDir == 1 && yDir == 0 && selectedPieceLocation.equals(team == Team.WHITE ? board.whiteKingLocation : board.blackKingLocation))  {
            if (isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir, yDir), team, board)
                    || isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir + 1, yDir), team, board))
                return new ArrayList<>();
            if (board.getPiece(selectedPieceLocation.transpose(xDir, yDir)) != null
                    || board.getPiece(selectedPieceLocation.transpose(xDir + 1, yDir)) != null)
                return new ArrayList<>();
            if (isKingInCheck(team, board)) {
                return new ArrayList<>();
            }

            if ((fen.contains("K") && team == Team.WHITE) || (fen.contains("k") && team == Team.BLACK)) {
                BoardLocation castleMove = selectedPieceLocation.transpose(xDir + 1, yDir);
                Piece castleTarget = board.getPiece(castleMove);
                if (board.isInBounds(castleMove) && (castleTarget == null || castleTarget.team != team)) {
                    var move = new Move(selectedPieceLocation, castleMove, board.getPiece(selectedPieceLocation));
                    move.setMoveFlag(MoveFlag.kingsideCastling);
                    legalKingMoves.add(move);
                }
            }
        }
        return null;
    }

    /**
     * Method returning all legal moves for a given rook
     * @param selection where the rook is located on the board
     * @param team what team the rook is
     * @param board the board where the pieces move
     * @param ignoreFriendlyPieces whether the rook can move through and capture friendly pieces
     * @return returns list of all legal moves for a given rook
     */
    private static List<Move> getValidRookMoves(BoardLocation selection, Team team, Board board, boolean ignoreFriendlyPieces) {
        List<Move> legalRookMoves = new ArrayList<>();
        for (int direction = 0; direction < 2; direction++) {
            for (int direction2 = -1; direction2 <= 1; direction2 += 2) {
                BoardLocation move = selection;
                do {
                    if (direction == 0) {
                        move = move.transpose(direction2, 0);
                    } else {
                        move = move.transpose(0, direction2);
                    }

                } while (checkIfInBounds(legalRookMoves, team, board, selection, move, ignoreFriendlyPieces));
            }
        }
        return legalRookMoves;
    }

    /**
     * Method that checks if a move is in bounds and adds it to the list of legal moves if it is
     * @param legalMoves list of legal moves
     * @param team what team the pieces are
     * @param board the board where the pieces move
     * @param to what move the method should check whether it's in bounds
     * @param ignoreFriendlyPieces whether the method should allow going through or capturing friendly pieces
     * @return true of false whether the move is in bounds.
     */
    private static boolean checkIfInBounds(List<Move> legalMoves, Team team, Board board, BoardLocation from, BoardLocation to, boolean ignoreFriendlyPieces) {
        if (!board.isInBounds(to)) {
            return false;
        }

        Piece target = board.getPiece(to);

        if (target != null) {
            if (target.team != team || ignoreFriendlyPieces) {
                legalMoves.add(new Move(from, to, board.getPiece(from), target));
            }
            return false;
        }

        legalMoves.add(new Move(from, to, board.getPiece(from)));
        return true;
    }


    /**
     * Method returning all legal moves for a given bishop
     * @param legalBishopMoves list the method will update
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the bishop is
     * @param board the board where the pieces move
     * @return returns a list of all legal moves for a given bishop
     */
    private static List<Move> getValidBishopMoves(List<Move> legalBishopMoves, BoardLocation selectedPieceLocation, Team team, Board board, boolean ignoreFriendlyPieces) {
        for (int xDir = -1; xDir <= 1; xDir += 2) {
            for (int yDir = -1; yDir <= 1; yDir += 2) {
                pieceDirections(legalBishopMoves, selectedPieceLocation, team, board, xDir, yDir, ignoreFriendlyPieces);
            }
        }
        return legalBishopMoves;
    }

    /**
     * Method returning all legal moves for a given pawn
     * @param pawnLocation where the pawn is located on the board
     * @param team what team the pawn is
     * @param board the board where the pieces move
     * @param ignoreEnPassant whether the method should ignore en passant moves
     * @return returns a list of legal moves for a given pawn
     */
    public static List<Move> getValidPawnMoves(BoardLocation pawnLocation, Team team, Board board, boolean ignoreEnPassant) {
        List<Move> legalPawnMoves = new ArrayList<>();

        int forwardDirection = (team == playerTeam) ? 1 : -1;
        BoardLocation forwardPoint = new BoardLocation(pawnLocation.getX(), (pawnLocation.getY() + (forwardDirection)));
        BoardLocation doubleForwardPoint = new BoardLocation(pawnLocation.getX(), (pawnLocation.getY() + (2 * forwardDirection)));

        // Check for normal move forward
        if (board.isInBounds(forwardPoint) && board.getPiece(forwardPoint) == null) {
            legalPawnMoves.add(new Move(pawnLocation, forwardPoint, board.getPiece(pawnLocation)));
            // Check for double move forward (if the pawn hasn't moved yet)
            if (team == (playerTeam == Team.WHITE ? Team.WHITE : Team.BLACK) && pawnLocation.getY() == 1 && board.getPiece(doubleForwardPoint) == null) {
                legalPawnMoves.add(new Move(pawnLocation, doubleForwardPoint, board.getPiece(pawnLocation)));
            } else if (team == (playerTeam == Team.BLACK ? Team.WHITE : Team.BLACK) && pawnLocation.getY() == 6 && board.getPiece(doubleForwardPoint) == null) {
                legalPawnMoves.add(new Move(pawnLocation, doubleForwardPoint, board.getPiece(pawnLocation)));
            }
        }

        // Check for capture moves
        BoardLocation leftCapture = new BoardLocation(pawnLocation.getX() - 1, pawnLocation.getY() + forwardDirection);
        BoardLocation rightCapture = new BoardLocation(pawnLocation.getX() + 1, pawnLocation.getY() + forwardDirection);
        legalPawnMoves.addAll(addPawnCaptureMoves(pawnLocation, team, board, leftCapture, ignoreEnPassant));
        legalPawnMoves.addAll(addPawnCaptureMoves(pawnLocation, team, board, rightCapture, ignoreEnPassant));

        return legalPawnMoves;
    }

    /**
     * Method that will add capture moves for a given pawn
     * @param pawnLocation where the pawn is located on the board
     * @param team what team the pawn is
     * @param board the board where the pieces move
     * @param captureLocation where the pawn can capture a piece
     * @param ignoreEnPassant whether the method should ignore en passant moves
     * @return list of legal pawn capture moves
     */
    private static List<Move> addPawnCaptureMoves(BoardLocation pawnLocation, Team team, Board board, BoardLocation captureLocation, boolean ignoreEnPassant) {
        List<Move> legalPawnMoves = new ArrayList<>();
        if (board.isInBounds(captureLocation)) {
            Piece rightCapturePiece = board.getPiece(captureLocation);

            if (rightCapturePiece != null && rightCapturePiece.team != team) {
                legalPawnMoves.add(new Move(pawnLocation, captureLocation, board.getPiece(pawnLocation), rightCapturePiece));
            }

            if (!ignoreEnPassant) {
                legalPawnMoves.addAll(checkEnPassant(team, board, captureLocation, pawnLocation));
            }
        }
        return legalPawnMoves;
    }

    /**
     * Method that tries adding En Passant moves
     * @param team what team the pawn is
     * @param board the board where the pieces move
     * @param captureMove location where the pawn can capture a piece
     * @param pawnLocation where the pawn is located on the board
     * @return list of legal En Passant moves
     */
    private static List<Move> checkEnPassant(Team team, Board board, BoardLocation captureMove, BoardLocation pawnLocation) {
        BoardLocation leftLocation = new BoardLocation(pawnLocation.getX() - 1, pawnLocation.getY());
        BoardLocation rightLocation = new BoardLocation(pawnLocation.getX() + 1, pawnLocation.getY());

        List<Move> legalPawnMoves = new ArrayList<>();
        legalPawnMoves.addAll(addLegalEnPassant(team, board, captureMove, pawnLocation, rightLocation, pawnLocation.getX() < captureMove.getX()));
        legalPawnMoves.addAll(addLegalEnPassant(team, board, captureMove, pawnLocation, leftLocation, pawnLocation.getX() > captureMove.getX()));
        return legalPawnMoves;
    }

    /**
     * Method that will add En Passant moves if it's legal
     * @param team what team the pawn is
     * @param board the board where the pieces move
     * @param captureMove location where the pawn can capture a piece
     * @param pawnLocation where the pawn is located on the board
     * @param pieceLocationNextToPawn location of the piece next to the pawn
     * @param isDifferentFile whether the pawn location is on the same file as the location where the pawn can capture a piece
     * @return list of legal En Passant moves
     */
    public static List<Move> addLegalEnPassant(Team team, Board board, BoardLocation captureMove, BoardLocation pawnLocation, BoardLocation pieceLocationNextToPawn, boolean isDifferentFile) {
        Piece pieceNextToPawn = board.getPiece(pieceLocationNextToPawn);
        if (pieceNextToPawn != null
                && pieceNextToPawn.type.equals(PieceType.PAWN)
                && pieceNextToPawn.team != team
                && (team == playerTeam ? pawnLocation.getY() == 4 : pawnLocation.getY() == 3)
                && pieceNextToPawn.doublePawnMoveOnMoveNumber == GameState.moveNumber
                && isDifferentFile) {


            // Perform the en passant capture move and check if the king is in check
            var move = new Move(pawnLocation, captureMove, board.getPiece(pawnLocation), board.getPiece(pieceLocationNextToPawn));
            board.playEnPassant(move);
            boolean isKingInCheck = isKingInCheck(team, board);
            board.undoEnPassant(move);

            // If the king is not in check after the en passant capture, add it to the legal moves
            if (!isKingInCheck) {
                return Collections.singletonList(new Move(pawnLocation, captureMove, board.getPiece(pawnLocation), board.getPiece(pieceLocationNextToPawn)));
            }
        }
        return new ArrayList<>();
    }

    /**
     * Method that checks whether the king of a given team is in check
     * @param team what team the king is
     * @param board the board where the pieces move
     * @return true or false whether the king is in check
     */
    public static boolean isKingInCheck(Team team, Board board) {
        BoardLocation kingPosition = team == Team.WHITE ? board.getKing(Team.WHITE) : board.getKing(Team.BLACK);
        return isSquareAttackedByEnemy(kingPosition, team, board);
    }

    /**
     * Method that checks if a team is in checkmate.
     * @param team the team to check for.
     * @param board the board where the pieces move.
     * @return true or false whether the team is in checkmate.
     */
    public static boolean isCheckmate(Team team, Board board) {
        if (!isKingInCheck(team, board) || GameState.kinglessGame) {
            return false;
        }
        List<Move> validMoves;
        for (int i = 0; i < board.pieces.length; i++) {
            BoardLocation point = board.getPointFromArrayIndex(i);
            Piece piece = board.pieces[i];
            if (piece != null && piece.team == team) {
                validMoves = getValidMoves(point, piece, board, true, false, false);
                for (Move move : validMoves) {
                    var to = move.getTo();
                    board.movePieceWithoutSpecialMovesAndSave(point, to);
                    if (!isKingInCheck(team, board)) {
                        board.undoLastMove();
                        return false;
                    }
                    board.undoLastMove();
                }
            }
        }
        return true;
    }

    /**
     * Method that checks if a team is in stalemate.
     * @param team the team to check for.
     * @param board the board where the pieces move.
     * @return true or false whether the team is in stalemate.
     */
    public static boolean isStalemate(Team team, Board board) {
        if (isKingInCheck(team, board) || GameState.kinglessGame) {
            return false;
        }
        List<Move> validMoves;
        for (int i = 0; i < board.pieces.length; i++) {
            BoardLocation point = board.getPointFromArrayIndex(i);
            Piece piece = board.pieces[i];
            if (piece != null && piece.team == team) {
                validMoves = getValidMoves(point, piece, board, false);
                if (!validMoves.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Method that returns whether a piece is on a given square.
     * @param piece what piece should be on the square.
     * @param square location of the square.
     * @param board the board where the pieces move.
     * @return true or false whether a piece is on the given square.
     */
    public static boolean isPieceOnSquare(Piece piece, BoardLocation square, Board board) {
        if (board.getPiece(square) == null) return false;
        return board.getPiece(square).equals(piece);
    }
}