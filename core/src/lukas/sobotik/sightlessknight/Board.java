package lukas.sobotik.sightlessknight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

public class Board {
    Piece[] pieces;
    int size;
    int squareSize;
    Texture boardTexture;
    TextureRegion boardTextureRegion;
	TextureAtlas pieceAtlas;
    HashMap<String, Integer> spriteIndexMap;
    Array<Sprite> sprites;
    BoardLocation whiteKingLocation;
    BoardLocation blackKingLocation;
    BoardLocation lastFromLocation;
    BoardLocation lastToLocation;
    Piece lastRemovedPiece;
    BoardLocation lastDoublePawnMoveWithWhitePieces;
    BoardLocation lastDoublePawnMoveWithBlackPieces;
    FenUtils fenUtils;
    static final Team playerTeam = GameState.playerTeam;

    public static final String STARTING_FEN_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public Board(int size, TextureAtlas pieceAtlas) {
        this.size = size;
        squareSize = size / 8;
        this.pieceAtlas = pieceAtlas;
        sprites = pieceAtlas.createSprites();
        float spriteSize = (int) sprites.get(0).getHeight();
        float scale = ((float) squareSize) / spriteSize;

        for (Sprite sprite : sprites) {
            sprite.setScale(scale);
        }

        spriteIndexMap = new HashMap<>();

        int count = 0;
        for (PieceType type : PieceType.values()) {

            for (Team team : Team.values()) {
                String name = new Piece(team, type).getSpriteName();
                spriteIndexMap.put(name, count++);
            }
        }

        // TODO: Automatically set this from FEN
        if (playerTeam == Team.WHITE) {
            whiteKingLocation = new BoardLocation(4, 0);
            blackKingLocation = new BoardLocation(4, 7);
        } else {
            whiteKingLocation = new BoardLocation(3, 7);
            blackKingLocation = new BoardLocation(3, 0);
        }

        fenUtils = new FenUtils(pieces, whiteKingLocation, blackKingLocation, lastToLocation, lastDoublePawnMoveWithWhitePieces, lastDoublePawnMoveWithBlackPieces);
        pieces = fenUtils.generatePositionFromFEN(STARTING_FEN_POSITION);

        System.out.println(fenUtils.generateFenFromCurrentPosition());
        printBoardInConsole();

        generateAllSquaresTexture();
    }
    public void resetBoardPosition() {
        pieces = fenUtils.generatePositionFromFEN(STARTING_FEN_POSITION);

        System.out.println(fenUtils.generateFenFromCurrentPosition());
        printBoardInConsole();
    }
    public void printBoardInConsole() {
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece piece = pieces[index];

                if (piece == null) {
                    System.out.print(". "); // Empty square
                } else {
                    System.out.print(fenUtils.getSymbolFromPieceType(piece.type, piece.team) + " "); // Piece character
                }
            }
            System.out.println(); // Move to the next line for the next rank
        }
    }

    private void generateAllSquaresTexture() {
        int nextPow2 = Integer.highestOneBit(size - 1) << 1;

        Pixmap pixmap = new Pixmap(nextPow2, nextPow2, Format.RGBA8888);

        pixmap.setColor(Color.GRAY);
        pixmap.fillRectangle(0, 0, size, size);

        pixmap.setColor(Color.WHITE);

        int y = 0;
        int x = 0;
        for (int i = 0; i < 32; i++) {
            pixmap.fillRectangle(x * squareSize, y * squareSize, squareSize, squareSize);
            x += 2;
            if (x >= 8) {
                y++;
                x = 1 - x % 8;
            }
        }

        boardTexture = new Texture(pixmap);
        pixmap.dispose();
        boardTextureRegion = new TextureRegion(boardTexture, 0, 0, size, size);
    }

    public void draw(SpriteBatch batch) {
        batch.draw(boardTextureRegion, 0, 0);

        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                drawPiece(batch, rank, file);
            }
        }
    }
    private void drawPiece(SpriteBatch batch, int rank, int file) {
        int index = rank * 8 + file;
        Piece info = pieces[index];

        if (info == null) {
            return;
        }

        String name = info.getSpriteName();

        Sprite sprite = sprites.get(spriteIndexMap.get(name));
        sprite.setX(file * squareSize + (float) squareSize / 2 - sprite.getWidth() / 2);
        sprite.setY(rank * squareSize + (float) squareSize / 2 - sprite.getHeight() / 2);
        sprite.draw(batch);
    }

    BoardLocation getKing(Team team) {
        return (team == Team.WHITE) ? whiteKingLocation : blackKingLocation;
    }

    public Piece getPiece(BoardLocation boardLocation) {
        if (!isInBounds(boardLocation)) {
            return null;
        }
        return pieces[boardLocation.getX() + boardLocation.getY() * 8];
    }

    public void movePiece(BoardLocation from, BoardLocation to) {
        if (from == null || to == null) return;
        movePieceWithoutSpecialMoves(from, to);

        Piece movedPiece = pieces[to.getX() + to.getY() * 8];
        if (movedPiece == null) return;

        // Move the rook when the king castles
        if (movedPiece.type == PieceType.KING && Math.abs(from.getX() - to.getX()) == 2) {
            // Queenside Castling
            if (from.getX() > to.getX()) {
                movePieceWithoutSpecialMovesAndSave(
                        movedPiece.team == Team.WHITE
                            ? new BoardLocation(0, 0)
                            : new BoardLocation(0, 7),
                        movedPiece.team == Team.WHITE
                            ? new BoardLocation(3, 0)
                            : new BoardLocation(3, 7));
            } // Kingside Castling
            else {
                movePieceWithoutSpecialMovesAndSave(
                        movedPiece.team == Team.WHITE
                            ? new BoardLocation(7, 0)
                            : new BoardLocation(7, 7),
                        movedPiece.team == Team.WHITE
                            ? new BoardLocation(5, 0)
                            : new BoardLocation(5, 7));
            }
        }

        // Save the last move that was a double pawn move
        if (movedPiece.type == PieceType.PAWN && Math.abs(from.getY() - to.getY()) == 2) {
            if (movedPiece.team == Team.WHITE) lastDoublePawnMoveWithWhitePieces = to;
            if (movedPiece.team == Team.BLACK) lastDoublePawnMoveWithBlackPieces = to;
            movedPiece.doublePawnMoveOnMoveNumber = GameState.moveNumber;
        }

        // Handle en passant capture
        BoardLocation enPassantCapture = new BoardLocation(to.getX(), from.getY());
        if (getPiece(enPassantCapture) != null && from.getX() != to.getX() && getPiece(enPassantCapture).doublePawnMoveOnMoveNumber == GameState.moveNumber - 1) {
            removePiece(enPassantCapture);
        }

        // Check for promotion moves
        if ((to.getY() == 7 || to.getY() == 0) && movedPiece.type.equals(PieceType.PAWN)) {
            GameState.promotionLocation = to;
            GameState.isPawnPromotionPending = true;
        }

        movedPiece.hasMoved = true;

        lastFromLocation = from;
        lastToLocation = to;
    }
    public void movePieceWithoutSpecialMovesAndSave(BoardLocation from, BoardLocation to) {
        movePieceWithoutSpecialMoves(from, to);

        lastFromLocation = from;
        lastToLocation = to;
    }
    public void movePieceWithoutSpecialMoves(BoardLocation from, BoardLocation to) {
        if (from == null || to == null) return;
        if (from.equals(whiteKingLocation)) {
            whiteKingLocation = to;
        } else if (from.equals(blackKingLocation)) {
            blackKingLocation = to;
        }

        lastRemovedPiece = pieces[to.getX() + to.getY() * 8];

        pieces[to.getX() + to.getY() * 8] = pieces[from.getX() + from.getY() * 8];
        pieces[from.getX() + from.getY() * 8] = null;
    }
    public void removePiece(BoardLocation boardLocation) {
        if (!isInBounds(boardLocation)) {
            return;
        }
        pieces[boardLocation.getX() + boardLocation.getY() * 8] = null;
    }

    public void undoMove() {
        if (lastFromLocation == null || lastToLocation == null) return;
        Piece temp = lastRemovedPiece;

        movePieceWithoutSpecialMovesAndSave(lastToLocation, lastFromLocation);

        pieces[lastFromLocation.getX() + lastFromLocation.getY() * 8] = temp;
    }

    public BoardLocation getPointFromArrayIndex(int index) {
        int x = index % 8;
        int y = index / 8;
        return new BoardLocation(x, y);
    }

    public void promotePawn(BoardLocation pawnLocation, PieceType selectedPiece) {
        Team team;
        if (pawnLocation.getY() == 7) team = Team.WHITE;
        else team = Team.BLACK;
        Piece piece = new Piece(team, selectedPiece);
        pieces[pawnLocation.getX() + pawnLocation.getY() * 8] = piece;
    }

    public BoardLocation getPoint(int x, int y) {
        return new BoardLocation(x / squareSize, 7 - y / squareSize);
    }

    public Rectangle getRectangle(BoardLocation point) {
        return new Rectangle(point.getX() * squareSize, (point.getY()) * squareSize, squareSize, squareSize);
    }

    public boolean isInBounds(BoardLocation boardLocation) {
        return boardLocation.getX() < 8 && boardLocation.getX() >= 0 && boardLocation.getY() < 8 && boardLocation.getY() >= 0;
    }
}
