package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Rules {
    static final Team playerTeam = GameState.playerTeam;
    final static long[] rankMasks8 = { // from rank1 to rank8
            0xFFL, 0xFF00L, 0xFF0000L, 0xFF000000L, 0xFF00000000L, 0xFF0000000000L, 0xFF000000000000L, 0xFF00000000000000L
    };
    final static long[] fileMasks8 = { // from rank1 to rank8
            0x101010101010101L, 0x202020202020202L, 0x404040404040404L, 0x808080808080808L,
            0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L
    };
    final static long[] diagonalMasks8 = { // from top left to bottom right
            0x1L, 0x102L, 0x10204L, 0x1020408L, 0x102040810L, 0x10204081020L, 0x1020408102040L,
            0x102040810204080L, 0x204081020408000L, 0x408102040800000L, 0x810204080000000L,
            0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L
    };
    final static long[] antiDiagonalMasks8 = { // from top right to bottom left
            0x80L, 0x8040L, 0x804020L, 0x80402010L, 0x8040201008L, 0x804020100804L, 0x80402010080402L,
            0x8040201008040201L, 0x4020100804020100L, 0x2010080402010000L, 0x1008040201000000L,
            0x804020100000000L, 0x402010000000000L, 0x201000000000000L, 0x100000000000000L
    };

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
            case BISHOP -> legalMoves.addAll(getValidBishopMoves(legalMoves, selectedPieceLocation, piece.team, board, false, null));
            case KNIGHT -> legalMoves.addAll(getValidKnightMoves(selectedPieceLocation, piece.team, board, false));
            case ROOK -> legalMoves.addAll(getValidRookMoves(selectedPieceLocation, piece.team, board, false, null));
            case KING -> legalMoves.addAll(getValidKingMoves(legalMoves, selectedPieceLocation, piece.team, board, addCastlingMoves, false));
            case QUEEN -> legalMoves.addAll(getValidQueenMoves(legalMoves, selectedPieceLocation, piece.team, board, false, null));
        }
        if (removePseudoLegalMoves && !legalMoves.isEmpty()) {
            legalMoves = removePseudoLegalMoves(legalMoves, selectedPieceLocation, piece.team, board);
        }
        return legalMoves;
    }

    static List<Move> getPseudoLegalMoves(BoardLocation selectedPieceLocation, Piece piece, Board board) {
        return getPseudoLegalMoves(selectedPieceLocation, piece, board, true, null);
    }
    /**
     * Used for disambiguating friendly moves, it will return all possible moves including friendly piece captures, so it shouldn't be used for normal applications
     * @implNote !!! With pawns, it will return only captures, not normal moves !!!
     * @param selectedPieceLocation location of the piece that the method will generate legal moves for
     * @param piece piece that the method will generate moves for
     * @param board the board where the moves take place
     * @return list of all pseudo-legal moves for a given piece
     */
    static List<Move> getPseudoLegalMoves(BoardLocation selectedPieceLocation, Piece piece, Board board, boolean addCastlingMoves, Bitboard bitboard) {
        List<Move> legalMoves = new ArrayList<>();
        switch (piece.type) {
            case PAWN -> legalMoves.addAll(Objects.requireNonNull(getPawnCaptureMoves(selectedPieceLocation, piece.team, board, true, true)));
            case KNIGHT -> legalMoves.addAll(getValidKnightMoves(selectedPieceLocation, piece.team, board, true));
            case BISHOP -> legalMoves.addAll(getValidBishopMoves(legalMoves, selectedPieceLocation, piece.team, board, true, bitboard));
            case ROOK -> legalMoves.addAll(getValidRookMoves(selectedPieceLocation, piece.team, board, true, bitboard));
            case QUEEN -> legalMoves.addAll(getValidQueenMoves(legalMoves, selectedPieceLocation, piece.team, board, true, bitboard));
            case KING -> legalMoves.addAll(getValidKingMoves(legalMoves, selectedPieceLocation, piece.team, board, addCastlingMoves, true));
        }
        return legalMoves;
    }

    /**
     * Method that returns whether enemy is attacking a certain square on the board
     * @param square what square the method should search for
     * @param friendlyTeam friendly team
     * @param board board where the pieces move
     * @return true or false depending on whether enemy attacks the given square
     */
    public static boolean isSquareAttackedByEnemy(BoardLocation square, Team friendlyTeam, Board board) {
        Team enemyTeam = (friendlyTeam == Team.WHITE) ? Team.BLACK : Team.WHITE;
        int squareIndex = board.getArrayIndexFromLocation(square);

        long bitboard = board.bitboard.getTeamAttackedSquares(enemyTeam);
        if ((bitboard & (1L << squareIndex)) != 0) {
            return true;
        }

        return false;
//        List<Move> list;
//
//        for (PieceType type : PieceType.values()) {
//            Piece info = new Piece(friendlyTeam, type);
//            list = getValidMoves(square, info, board, false, false, true);
//            for (Move move : list) {
//                var to = move.getTo();
//                Piece target = board.getPiece(to);
//                if (target != null && target.type == type && target.team != friendlyTeam) {
//                    return true;
//                }
//            }
//            list.clear();
//        }
//        return false;
    }
/*
01110001
10000001
10000001
10000001
10000001
10000001
10111001
10101110
 */
    public static boolean isSquareAttackedByEnemy(BoardLocation square, Team friendlyTeam, Board board, Bitboard bitboard) {
        if (bitboard == null) {
            return isSquareAttackedByEnemy(square, friendlyTeam, board);
        }

        Team enemyTeam = (friendlyTeam == Team.WHITE) ? Team.BLACK : Team.WHITE;
        int squareIndex = board.getArrayIndexFromLocation(square);

        long bitboardAttackedSquares = bitboard.getTeamAttackedSquares(enemyTeam);
        if ((bitboardAttackedSquares & (1L << squareIndex)) != 0) {
            return true;
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
    private static List<Move> removePseudoLegalMoves(List<Move> legalMoves, BoardLocation selectedPieceLocation, Team team, Board board) {
        List<Move> uniqueMoves = new ArrayList<>(new HashSet<>(legalMoves));
        for (int i = 0; i < uniqueMoves.size(); i++) {
            Move uniqueMove = uniqueMoves.get(i);
            BoardLocation move = uniqueMove.getTo();

            var isEnPassant = false;
            if (uniqueMove.getMoveFlag().equals(MoveFlag.enPassant)) {
                isEnPassant = true;
                board.playEnPassant(uniqueMove);
            } else {
                board.movePieceWithoutSpecialMovesAndSave(selectedPieceLocation, move);
            }

//            System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
//            board.printBoardInConsole(true);
//            System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
//            board.bitboard.printBitboard();
//            System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
//            board.bitboard.printBitboard(board.bitboard.getTeamAttackedSquares(Team.BLACK));
//            System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");

            var bitboard = new Bitboard(board.bitboard);
            var indexesContainingSquare = bitboard.getIndexesContainingSquare(uniqueMove.getTo(), board);

            if (uniqueMove.getCapturedPiece() != null) {
                if (uniqueMove.getMoveFlag().equals(MoveFlag.enPassant)) {
                            indexesContainingSquare.addAll(
                                bitboard.getIndexesContainingSquare(
                                        board.getPointFromArrayIndex(uniqueMove.getCapturedPiece().index), board));
                } else {
                    indexesContainingSquare.add(
                            bitboard.getIndex(uniqueMove.getCapturedPiece().type, uniqueMove.getCapturedPiece().team));
                }
            }

            indexesContainingSquare.addAll(bitboard.getIndexesContainingSquare(uniqueMove.getFrom(), board));
            bitboard = board.updateBitboardAfterAMove(uniqueMove, false, isEnPassant, bitboard);


            if (bitboard == null) {
                board.undoLastMove();
                continue;
            }

//            bitboard.printBitboard();
//            System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
//            bitboard.printBitboard(bitboard.getTeamAttackedSquares(Team.WHITE));
//            System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");

            // TODO: This array sometimes loops over the same thing multiple times, which is inefficient
            for (int indexContainingSquare : indexesContainingSquare) {
                bitboard.attackedSquares[indexContainingSquare] = bitboard.calculateAttackedSquaresForIndex(team, board, indexContainingSquare, 0L, bitboard);
            }

            if (isKingInCheck(team, board, bitboard)) {
                uniqueMoves.remove(i);
                i--;
            }

            if (uniqueMove.getMoveFlag().equals(MoveFlag.enPassant)) {
                board.undoEnPassant(uniqueMove);
            } else {
                board.undoLastMove();
            }
        }
        return uniqueMoves;

//        for (int i = 0; i < uniqueMoves.size(); i++) {
//            BoardLocation move = uniqueMoves.get(i).getTo();
//
//            board.movePieceWithoutSpecialMovesAndSave(selectedPieceLocation, move);
//            var oldBitboard = board.bitboard.pieces.clone();
//            var oldAttackedSquares = board.bitboard.attackedSquares.clone();
//            var indexesContainingSquare = board.bitboard.getIndexesContainingSquare(uniqueMoves.get(i), board);
//            board.updateBitboardAfterAMove(uniqueMoves.get(i), false);
//            for (int indexContainingSquare : indexesContainingSquare) {
//                board.bitboard.attackedSquares[indexContainingSquare] = board.bitboard.calculateAttackedSquaresForIndex(team, board, indexContainingSquare, 0L);
//            }
//
//            if (isKingInCheck(team, board)) {
//                uniqueMoves.remove(i);
//                i--;
//            }
//
//            board.undoLastMove();
//            board.bitboard.pieces = oldBitboard;
//            board.bitboard.attackedSquares = oldAttackedSquares;
//        }
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
     * @param continueThroughPieces If true, continue transposing even after encountering a blocking piece; otherwise, stop transposing.
     */
    private static void pieceDirections(List<Move> boardLocations, BoardLocation selectedPieceLocation, Team team, Board board, int xDir, int yDir, boolean ignoreFriendlyPieces, boolean continueThroughPieces) {
        int x = selectedPieceLocation.getX();
        int y = selectedPieceLocation.getY();
        while (true) {
            x += xDir;
            y += yDir;
            BoardLocation move = new BoardLocation(x, y);
            if (!continueThroughPieces) {
                if (!checkIfInBounds(boardLocations, team, board, selectedPieceLocation, move, ignoreFriendlyPieces)) {
                    break;
                }
            } else {
                if (!board.isInBounds(move)) {
                    break;
                }
                Piece piece = board.getPiece(move);
                Move newMove = new Move(selectedPieceLocation, move, piece);
                boardLocations.add(newMove);
            }
        }
    }

    private static long getBitboardFileMoves(int arrayIndex, Bitboard bitboard) {
        long binaryArrayIndex = 1L << arrayIndex;
        long possibilitiesHorizontal =
                (bitboard.getBlockerSquares() - 2 * binaryArrayIndex) ^ Long.reverse(Long.reverse(bitboard.getBlockerSquares()) - 2 * Long.reverse(binaryArrayIndex));
        long possibilitiesVertical =
                ((bitboard.getBlockerSquares() & fileMasks8[arrayIndex % 8]) - (2 * binaryArrayIndex)) ^ Long.reverse(
                        Long.reverse(bitboard.getBlockerSquares() & fileMasks8[arrayIndex % 8]) - (2 * Long.reverse(binaryArrayIndex)));
        return (possibilitiesHorizontal & rankMasks8[arrayIndex / 8 % rankMasks8.length]) | (possibilitiesVertical & fileMasks8[arrayIndex % 8 % fileMasks8.length]);
    }
    private static long getBitboardDiagonalMoves(int arrayIndex, Bitboard bitboard) {
        long binaryArrayIndex = 1L << arrayIndex;
        long possibilitiesDiagonal =
                ((bitboard.getBlockerSquares() & diagonalMasks8[(arrayIndex / 8) + (arrayIndex % 8)]) - (2 * binaryArrayIndex)) ^ Long.reverse(
                        Long.reverse(bitboard.getBlockerSquares() & diagonalMasks8[(arrayIndex / 8) + (arrayIndex % 8)]) - (2 * Long.reverse(binaryArrayIndex)));
        long possibilitiesAntiDiagonal =
                ((bitboard.getBlockerSquares() & antiDiagonalMasks8[((arrayIndex / 8) + 7 - (arrayIndex % 8)) % antiDiagonalMasks8.length]) - (2 * binaryArrayIndex)) ^ Long.reverse(
                        Long.reverse(bitboard.getBlockerSquares() & antiDiagonalMasks8[((arrayIndex / 8) + 7 - (arrayIndex % 8)) % antiDiagonalMasks8.length]) - (2 * Long.reverse(
                                binaryArrayIndex)));
        return (possibilitiesDiagonal & diagonalMasks8[((arrayIndex / 8) + (arrayIndex % 8)) % diagonalMasks8.length]) | (possibilitiesAntiDiagonal & antiDiagonalMasks8[((arrayIndex / 8) + 7 - (arrayIndex % 8)) % antiDiagonalMasks8.length]);
    }

    public static List<Move> bitboardToMoves(long bitboard, BoardLocation selectedPieceLocation, Piece piece, Board board) {
        List<Move> moves = new ArrayList<>();
        while (bitboard != 0) {
            // Isolate the least significant bit (LSB)
            long lsb = bitboard & -bitboard;

            // Convert the LSB to a BoardLocation
            int index = Long.numberOfTrailingZeros(lsb);
            int x = index % 8;
            int y = index / 8;
            BoardLocation to = new BoardLocation(x, y);

            // Create a Move object and add it to the list
            Move move = new Move(selectedPieceLocation, to, piece);
            if (board.getPiece(to) != null) {
                move.setCapturedPiece(board.getPiece(to));
            }
            moves.add(move);

            // Remove the LSB from the bitboard
            bitboard &= bitboard - 1;
        }
        return moves;
    }

    /**
     * Method that returns legal moves for a given queen
     * @param legalQueenMoves list that the method will update
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the queen is
     * @param board the board where the pieces move
     * @param ignoreFriendlyPieces whether the queen can move through and capture friendly pieces
     * @param continueThroughPieces whether the queen can move through pieces
     * @return list of legal queen moves
     */
    private static List<Move> getValidQueenMoves(List<Move> legalQueenMoves, BoardLocation selectedPieceLocation, Team team, Board board, boolean ignoreFriendlyPieces, Bitboard bitboard) {
//        for (int xDir = -1; xDir <= 1; xDir++) {
//            for (int yDir = -1; yDir <= 1; yDir++) {
//                if (xDir == 0 && yDir == 0) {
//                    continue;
//                }
//                pieceDirections(legalQueenMoves, selectedPieceLocation, team, board, xDir, yDir, ignoreFriendlyPieces, continueThroughPieces);
//            }
//        }
//        return legalQueenMoves;

        var usedBitboard = bitboard == null ? board.bitboard : bitboard;
        long moves = getBitboardFileMoves(board.getArrayIndexFromLocation(selectedPieceLocation), usedBitboard);
        moves |= getBitboardDiagonalMoves(board.getArrayIndexFromLocation(selectedPieceLocation), usedBitboard);
        usedBitboard.attackedSquaresWithFriendlyPieces[usedBitboard.getIndex(PieceType.QUEEN, team)] = moves;
        long friendlyPieces = usedBitboard.getTeamBitboard(team);
        // TODO: Maybe add the enemy king to this list as well (don't forget to add it to Rook and Bishop as well):
        //  moves &= ~(friendlyPieces & ~usedBitboard.getBitboard(PieceType.KING, team == Team.WHITE ? Team.BLACK : Team.WHITE))
        moves &= ~friendlyPieces;

        return bitboardToMoves(moves, selectedPieceLocation, new Piece(team, PieceType.QUEEN), board);
    }


    /**
     * Method returning all legal moves for a given rook
     * @param selection where the rook is located on the board
     * @param team what team the rook is
     * @param board the board where the pieces move
     * @param ignoreFriendlyPieces whether the rook can move through and capture friendly pieces
     * @return returns list of all legal moves for a given rook
     */
    private static List<Move> getValidRookMoves(BoardLocation selection, Team team, Board board, boolean ignoreFriendlyPieces, Bitboard bitboard) {
//        List<Move> legalRookMoves = new ArrayList<>();
//        for (int direction = 0; direction < 2; direction++) {
//            for (int direction2 = -1; direction2 <= 1; direction2 += 2) {
//                int x = selection.getX();
//                int y = selection.getY();
//                while (true) {
//                    if (direction == 0) {
//                        x += direction2;
//                    } else {
//                        y += direction2;
//                    }
//                    BoardLocation move = new BoardLocation(x, y);
//                    if (!continueThroughPieces) {
//                        if (!checkIfInBounds(legalRookMoves, team, board, selection, move, ignoreFriendlyPieces)) {
//                            break;
//                        }
//                    } else {
//                        if (!board.isInBounds(move)) {
//                            break;
//                        }
//                        Piece piece = board.getPiece(move);
//                        Move newMove = new Move(selection, move, piece);
//                        legalRookMoves.add(newMove);
//                    }
//                }
//            }
//        }
//        return legalRookMoves;

        var usedBitboard = bitboard == null ? board.bitboard : bitboard;
        long moves = getBitboardFileMoves(board.getArrayIndexFromLocation(selection), usedBitboard);
        usedBitboard.attackedSquaresWithFriendlyPieces[usedBitboard.getIndex(PieceType.ROOK, team)] = moves;
//        System.out.println("File moves: ////////////////////////////////////");
//        board.bitboard.printBitboard(board.bitboard.getTeamBitboard(team));
//        System.out.println("File moves: ////////////////////////////////////");
        long friendlyPieces = usedBitboard.getTeamBitboard(team);
        moves &= ~friendlyPieces;
//        board.printBoardInConsole(true);
//        System.out.println("§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§");
//        board.bitboard.printBitboard(moves);
//        System.out.println("ALLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL");

//        System.out.println("????????????????????????????????????????");
//        usedBitboard.printBitboard();
//        System.out.println("????????????????????????????????????????");
//        usedBitboard.printBitboard(usedBitboard.getTeamAttackedSquares(team));
//        System.out.println("????????????????????????????????????????");

        return bitboardToMoves(moves, selection, new Piece(team, PieceType.ROOK), board);
    }

    /**
     * Method returning all legal moves for a given bishop
     * @param legalBishopMoves list the method will update
     * @param selectedPieceLocation where the piece for generating legal moves is located on the board
     * @param team what team the bishop is
     * @param board the board where the pieces move
     * @param ignoreFriendlyPieces whether the bishop can move through friendly pieces
     * @return returns a list of all legal moves for a given bishop
     */
    private static List<Move> getValidBishopMoves(List<Move> legalBishopMoves, BoardLocation selectedPieceLocation, Team team, Board board, boolean ignoreFriendlyPieces, Bitboard bitboard) {
//        for (int xDir = -1; xDir <= 1; xDir += 2) {
//            for (int yDir = -1; yDir <= 1; yDir += 2) {
//                pieceDirections(legalBishopMoves, selectedPieceLocation, team, board, xDir, yDir, ignoreFriendlyPieces, continueThroughPieces);
//            }
//        }
//        return legalBishopMoves;

        var usedBitboard = bitboard == null ? board.bitboard : bitboard;
        long moves = getBitboardDiagonalMoves(board.getArrayIndexFromLocation(selectedPieceLocation), usedBitboard);
        usedBitboard.attackedSquaresWithFriendlyPieces[usedBitboard.getIndex(PieceType.BISHOP, team)] = moves;
        long friendlyPieces = usedBitboard.getTeamBitboard(team);
        moves &= ~friendlyPieces;

        return bitboardToMoves(moves, selectedPieceLocation, new Piece(team, PieceType.BISHOP), board);
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
                int x = selectedPieceLocation.getX();
                int y = selectedPieceLocation.getY();
                if (direction == 0) {
                    x += longDir;
                    y += shortDir;
                } else {
                    x += shortDir;
                    y += longDir;
                }
                BoardLocation to = new BoardLocation(x, y);
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
                int x = selectedPieceLocation.getX() + xDir;
                int y = selectedPieceLocation.getY() + yDir;
                Piece target = board.getPiece(new BoardLocation(x, y));
                if (board.isInBounds(new BoardLocation(x, y)) && ((target == null || target.team != team) || ignoreFriendlyPieces)) {
                    legalKingMoves.add(new Move(selectedPieceLocation, new BoardLocation(x, y), board.getPiece(selectedPieceLocation), target));
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
        Piece piece = board.getPiece(from);
        Move move = new Move(from, to, piece);
        if (target != null) {
            if (target.team != team || ignoreFriendlyPieces) {
                move.setCapturedPiece(target);
                legalMoves.add(move);
            }
            return false;
        }

        legalMoves.add(move);
        return true;
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
        legalPawnMoves.addAll(getPawnCaptureMoves(pawnLocation, team, board, ignoreEnPassant, false));

        return legalPawnMoves;
    }

    /**
     * This method generates the capture moves for a pawn. It takes into account the unique capturing
     * rule of pawns, which is to capture diagonally. It also considers the en passant rule.
     *
     * @param pawnLocation The current location of the pawn on the board.
     * @param team The team to which the pawn belongs.
     * @param board The current state of the chess board.
     * @param ignoreEnPassant A flag to indicate whether to ignore en passant moves. If true, en passant moves are not considered.
     * @param addControlledSquares A flag to indicate whether to add the squares controlled by the pawn. If true, the squares controlled by the pawn are added to the list of moves (a.k.a. they will most likely be pseudo-legal).
     * @return A list of legal pawn capture moves.
     */
    public static List<Move> getPawnCaptureMoves(BoardLocation pawnLocation, Team team, Board board, boolean ignoreEnPassant, boolean addControlledSquares) {
        List<Move> legalPawnMoves = new ArrayList<>();
        int forwardDirection = (team == playerTeam) ? 1 : -1;
        BoardLocation leftCapture = new BoardLocation(pawnLocation.getX() - 1, pawnLocation.getY() + forwardDirection);
        BoardLocation rightCapture = new BoardLocation(pawnLocation.getX() + 1, pawnLocation.getY() + forwardDirection);
        legalPawnMoves.addAll(addPawnCaptureMoves(pawnLocation, team, board, leftCapture, ignoreEnPassant, addControlledSquares));
        legalPawnMoves.addAll(addPawnCaptureMoves(pawnLocation, team, board, rightCapture, ignoreEnPassant, addControlledSquares));
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
    public static List<Move> addPawnCaptureMoves(BoardLocation pawnLocation, Team team, Board board, BoardLocation captureLocation, boolean ignoreEnPassant, boolean addControlledSquares) {
        List<Move> legalPawnMoves = new ArrayList<>();
        if (board.isInBounds(captureLocation)) {
            Piece rightCapturePiece = board.getPiece(captureLocation);

            if ((rightCapturePiece != null && rightCapturePiece.team != team) || addControlledSquares) {
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
            move.setMoveFlag(MoveFlag.enPassant);
            List<Move> moveList = new ArrayList<>();
            moveList.add(move);

            return removePseudoLegalMoves(moveList, move.getFrom(), team, board);
//            board.playEnPassant(move);
//
//            System.out.println("######################");
//            board.printBoardInConsole(true);
//            System.out.println("######################");
//            board.bitboard.printBitboard();
//            System.out.println("######################");
//
//            boolean isKingInCheck = isKingInCheck(team, board);
//            board.undoEnPassant(move);
//
//            // If the king is not in check after the en passant capture, add it to the legal moves
//            if (!isKingInCheck) {
//                return Collections.singletonList(new Move(pawnLocation, captureMove, board.getPiece(pawnLocation), board.getPiece(pieceLocationNextToPawn)));
//            }
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

    public static boolean isKingInCheck(Team team, Board board, Bitboard bitboard) {
        BoardLocation kingPosition = team == Team.WHITE ? board.getKing(Team.WHITE) : board.getKing(Team.BLACK);
        return isSquareAttackedByEnemy(kingPosition, team, board, bitboard);
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
