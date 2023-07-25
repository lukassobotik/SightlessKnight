package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Rules {
    static final Team playerTeam = GameState.playerTeam;
    private Rules() {

    }
    /**
     * Method with default settings for getting valid moves of a piece
     * @param legalMoves list that will be updated with the legal moves
     * @param selectedPieceLocation where the piece is located on the board
     * @param piece what piece are the moves generated for
     * @param board the board where the moves take place
     * @param checkForChecks whether the method should check for checks and piece pins
     */
    public static void getValidMoves(List<BoardLocation> legalMoves, BoardLocation selectedPieceLocation, Piece piece, Board board, boolean checkForChecks) {
        getValidMoves(legalMoves, selectedPieceLocation, piece, board, checkForChecks, true);
    }

    /**
     * Method with manual settings for getting valid moves of a piece
     * @param legalMoves list that will be updated with the legal moves
     * @param selectedPieceLocation where the piece is located on the board
     * @param piece what piece are the moves generated for
     * @param board the board where the moves take place
     * @param checkForChecks whether the method should check for checks and piece pins
     * @param checkCastlingMoves whether the method should add castling moves
     */
    static void getValidMoves(List<BoardLocation> legalMoves, BoardLocation selectedPieceLocation, Piece piece, Board board, boolean checkForChecks, boolean checkCastlingMoves) {
        switch (piece.type) {
            case PAWN -> legalMoves.addAll(Objects.requireNonNull(getValidPawnMoves(legalMoves, selectedPieceLocation, piece.team, board)));
            case BISHOP -> legalMoves.addAll(getValidBishopMoves(legalMoves, selectedPieceLocation, piece.team, board));
            case KNIGHT -> legalMoves.addAll(getValidKnightMoves(legalMoves, selectedPieceLocation, piece.team, board, false));
            case ROOK -> legalMoves.addAll(getValidRookMoves(legalMoves, selectedPieceLocation, piece.team, board, false));
            case KING -> legalMoves.addAll(getValidKingMoves(legalMoves, selectedPieceLocation, piece.team, board, checkCastlingMoves));
            case QUEEN -> legalMoves.addAll(getValidQueenMoves(legalMoves, selectedPieceLocation, piece.team, board, false));
        }
        if (checkForChecks) {
            checkForChecks(legalMoves, selectedPieceLocation, piece.team, board);
        }
    }

    /**
     * Used for disambiguating friendly moves, it will return all possible moves including friendly piece captures, so it shouldn't be used for normal applications
     * @param legalMoves list of the legal moves that will be updated
     * @param selectedPieceLocation location of the piece that the method will generate legal moves for
     * @param piece piece that the method will generate moves for
     * @param board the board where the moves take place
     */
    static void getAllMoves(List<BoardLocation> legalMoves, BoardLocation selectedPieceLocation, Piece piece, Board board) {
        switch (piece.type) {
            case KNIGHT -> legalMoves.addAll(getValidKnightMoves(legalMoves, selectedPieceLocation, piece.team, board, true));
            case ROOK -> legalMoves.addAll(getValidRookMoves(legalMoves, selectedPieceLocation, piece.team, board, true));
            case QUEEN -> legalMoves.addAll(getValidQueenMoves(legalMoves, selectedPieceLocation, piece.team, board, true));
        }
    }

    /**
     * Method that returns whether enemy is attacking a certain square on the board
     * @param square what square the method should search for
     * @param friendlyTeam friendly team
     * @param board board where the pieces move
     * @return true or false depending on whether enemy attacks the given square
     */
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

    /**
     * Method that returns whether a king can castle
     * @param team what team's king castling rights the method should return
     * @param pieces array of pieces on the board
     * @param whiteKing location of the white king
     * @param blackKing location of the black king
     * @param returnBothTeams whether the method should return both teams' castling rights
     * @return the FEN notation of castling rights ["K" for white's kingside castling rights, "Q" for white's queenside castling rights, "k" for black's kingside castling rights, "q" for black's queenside castling rights], e.g. "KQkq" or "Kkq"
     */
    public static String isCastlingPossible(Team team, Piece[] pieces, BoardLocation whiteKing, BoardLocation blackKing, boolean returnBothTeams) {
        StringBuilder castlingAvailability = new StringBuilder();
        if (whiteKing == null || blackKing == null) return "";

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

    /**
     * Method that removes pseudo-legal moves (e.g. moves through pins to the king)
     * @param legalMoves piece's legal moves
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team which team the piece is
     * @param board the board where the pieces move
     */
    private static void checkForChecks(List<BoardLocation> legalMoves, BoardLocation selectedPieceLocation, Team team, Board board) {
        for (int i = 0; i < legalMoves.size(); i++) {
            BoardLocation move = legalMoves.get(i);

            board.movePieceWithoutSpecialMovesAndSave(selectedPieceLocation, move);

            if (isKingInCheck(team, board)) {
                legalMoves.remove(i);
                i--;
            }

            board.undoMove();
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
    private static List<BoardLocation> getValidQueenMoves(List<BoardLocation> legalQueenMoves, BoardLocation selectedPieceLocation, Team team, Board board, boolean ignoreFriendlyPieces) {
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
    private static void pieceDirections(List<BoardLocation> boardLocations, BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir, boolean ignoreFriendlyPieces) {
        BoardLocation move = selectedPieceLocation;
        do {
            move = move.transpose(xDir, yDir);
        } while (checkIfInBounds(boardLocations, team, board, move, ignoreFriendlyPieces));
    }

    /**
     * Returns valid moves for a given knight
     * @param legalKnightMoves list that the method will update
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the knight is
     * @param board the board where the pieces move
     * @param ignoreFriendlyPieces whether the knight can move through and capture friendly pieces
     * @return list of legal knight moves
     */
    private static List<BoardLocation> getValidKnightMoves(List<BoardLocation> legalKnightMoves, BoardLocation selectedPieceLocation, Team team, Board board, boolean ignoreFriendlyPieces) {
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
                    if (board.isInBounds(move) && ((target == null || target.team != team) || ignoreFriendlyPieces)) {
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
     * @param checkCastlingMoves whether it should check for castling moves
     * @return list of legal king moves
     */
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

    /**
     * Method returning all valid castling moves for a team
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the method will check castling for
     * @param board the board where the pieces move
     * @param xDir The direction to move along the x-axis (horizontal direction).
     * @param yDir The direction to move along the y-axis (vertical direction).
     * @return list of valid castling moves for the given team
     */
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
    private static ArrayList<BoardLocation> checkQueensideCastling(BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir, List<BoardLocation> legalKingMoves, String fen) {
        if (xDir == -1 && yDir == 0 && selectedPieceLocation.equals(team == Team.WHITE ? new BoardLocation(4, 0) : new BoardLocation(4, 7)))  {
            if (isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir, yDir), team, board)
                    || isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir - 1, yDir), team, board)
                    || isSquareAttackedByEnemy(selectedPieceLocation.transpose(xDir - 2, yDir), team, board))
                return new ArrayList<>();
            if (board.getPiece(selectedPieceLocation.transpose(xDir, yDir)) != null
                    || board.getPiece(selectedPieceLocation.transpose(xDir - 1, yDir)) != null
                    || board.getPiece(selectedPieceLocation.transpose(xDir - 2, yDir)) != null) {
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

    /**
     * Method returning all legal moves for a given rook
     * @param legalRookMoves list the method will update
     * @param selection where the rook is located on the board
     * @param team what team the rook is
     * @param board the board where the pieces move
     * @param ignoreFriendlyPieces whether the rook can move through and capture friendly pieces
     * @return returns list of all legal moves for a given rook
     */
    private static List<BoardLocation> getValidRookMoves(List<BoardLocation> legalRookMoves, BoardLocation selection, Team team, Board board, boolean ignoreFriendlyPieces) {
        for (int direction = 0; direction < 2; direction++) {
            for (int direction2 = -1; direction2 <= 1; direction2 += 2) {
                BoardLocation move = selection;
                do {
                    if (direction == 0) {
                        move = move.transpose(direction2, 0);
                    } else {
                        move = move.transpose(0, direction2);
                    }

                } while (checkIfInBounds(legalRookMoves, team, board, move, ignoreFriendlyPieces));
            }
        }
        return legalRookMoves;
    }

    /**
     * Method that checks if a move is in bounds
     * @param legalMoves list of legal moves
     * @param team what team the pieces are
     * @param board the board where the pieces move
     * @param move what move the method should check whether it's in bounds
     * @param ignoreFriendlyPieces whether the method should allow going through or capturing friendly pieces
     * @return true of false whether the move is in bounds.
     */
    private static boolean checkIfInBounds(List<BoardLocation> legalMoves, Team team, Board board, BoardLocation move, boolean ignoreFriendlyPieces) {
        if (!board.isInBounds(move)) {
            return false;
        }

        Piece target = board.getPiece(move);

        if (target != null) {
            if (target.team != team || ignoreFriendlyPieces) {
                legalMoves.add(move);
            }
            return false;
        }

        legalMoves.add(move);
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
    private static List<BoardLocation> getValidBishopMoves(List<BoardLocation> legalBishopMoves, BoardLocation selectedPieceLocation, Team team, Board board) {
        for (int xDir = -1; xDir <= 1; xDir += 2) {
            for (int yDir = -1; yDir <= 1; yDir += 2) {
                pieceDirections(legalBishopMoves, selectedPieceLocation, team, board, xDir, yDir, false);
            }
        }
        return legalBishopMoves;
    }

    /**
     * Method returning all legal moves for a given pawn
     * @param legalPawnMoves list the method will update
     * @param pawnLocation where the pawn is located on the board
     * @param team what team the pawn is
     * @param board the board where the pieces move
     * @return returns a list of legal moves for a given pawn
     */
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
        if (!legalPawnMoves.isEmpty() && (forwardPoint.getY() == 7 || forwardPoint.getY() == 0)) {
            List<BoardLocation> promotionMoves = new ArrayList<>();

            for (BoardLocation move : legalPawnMoves) {
                if (move.getY() == 0 || move.getY() == 7) {
                    promotionMoves.add(move);
                }
            }
        }
        return legalPawnMoves;
    }

    /**
     * Method that will add capture moves for a given pawn
     * @param legalPawnMoves list the method will add the moves to
     * @param pawnLocation where the pawn is located on the board
     * @param team what team the pawn is
     * @param board the board where the pieces move
     * @param captureLocation where the pawn can capture a piece
     * @return a boolean whether the parent method should return nothing
     */
    private static boolean addPawnCaptureMoves(List<BoardLocation> legalPawnMoves, BoardLocation pawnLocation, Team team, Board board, BoardLocation captureLocation) {
        if (board.isInBounds(captureLocation)) {
            Piece rightCapturePiece = board.getPiece(captureLocation);

            if (rightCapturePiece != null && rightCapturePiece.team != team) {
                legalPawnMoves.add(captureLocation);
            }
            checkEnPassant(legalPawnMoves, team, board, captureLocation, pawnLocation);
        }
        return false;
    }

    /**
     * Method that tries adding En Passant moves
     * @param legalPawnMoves list the method will add the moves to
     * @param team what team the pawn is
     * @param board the board where the pieces move
     * @param captureMove location where the pawn can capture a piece
     * @param pawnLocation where the pawn is located on the board
     */
    private static void checkEnPassant(List<BoardLocation> legalPawnMoves, Team team, Board board, BoardLocation captureMove, BoardLocation pawnLocation) {
        BoardLocation leftLocation = new BoardLocation(pawnLocation.getX() - 1, pawnLocation.getY());
        BoardLocation rightLocation = new BoardLocation(pawnLocation.getX() + 1, pawnLocation.getY());

        addLegalEnPassant(legalPawnMoves, team, board, captureMove, pawnLocation, rightLocation, pawnLocation.getX() < captureMove.getX());
        addLegalEnPassant(legalPawnMoves, team, board, captureMove, pawnLocation, leftLocation, pawnLocation.getX() > captureMove.getX());
    }

    /**
     * Method that will add En Passant moves if it's legal
     * @param legalPawnMoves list the method will add the moves to
     * @param team what team the pawn is
     * @param board the board where the pieces move
     * @param captureMove location where the pawn can capture a piece
     * @param pawnLocation where the pawn is located on the board
     * @param pieceLocationNextToPawn location of the piece next to the pawn
     * @param isDifferentFile whether the pawn location is on the same file as the location where the pawn can capture a piece
     */
    private static void addLegalEnPassant(List<BoardLocation> legalPawnMoves, Team team, Board board, BoardLocation captureMove, BoardLocation pawnLocation, BoardLocation pieceLocationNextToPawn, boolean isDifferentFile) {
        Piece pieceNextToPawn = board.getPiece(pieceLocationNextToPawn);
        if (pieceNextToPawn != null
                && pieceNextToPawn.type.equals(PieceType.PAWN)
                && pieceNextToPawn.team != team
                && (team == playerTeam ? pawnLocation.getY() == 4 : pawnLocation.getY() == 3)
                && pieceNextToPawn.doublePawnMoveOnMoveNumber == GameState.moveNumber
                && isDifferentFile) {

//            // Perform the en passant capture move and check if the king is in check
//            board.movePiece(pawnLocation, captureMove);
//            board.printBoardInConsole();
//            boolean isKingInCheck = isKingInCheck(team, board);
//            board.undoMove();
//
//            // If the king is not in check after the en passant capture, add it to the legal moves
//            if (!isKingInCheck) {
            legalPawnMoves.add(captureMove);
//            }
        }
    }

    /**
     * Method that checks whether the king of a given team is in check
     * @param team what team the king is
     * @param board the board where the pieces move
     * @return true or false whether the king is in check
     */
    public static boolean isKingInCheck(Team team, Board board) {
        BoardLocation kingPosition = team == Team.WHITE ? board.whiteKingLocation : board.blackKingLocation;
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
        List<BoardLocation> validMoves = new ArrayList<>();
        for (int i = 0; i < board.pieces.length; i++) {
            BoardLocation point = board.getPointFromArrayIndex(i);
            Piece piece = board.pieces[i];
            if (piece != null && piece.team == team) {
                getValidMoves(validMoves, point, piece, board, false);
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
