package lukas.sobotik.sightlessknight;

import java.util.HashMap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class Board {
    PieceInfo[][] pieces;
    int size;
    int squareSize;
    Texture boardTexture;
    TextureRegion boardTextureRegion;
	TextureAtlas pieceAtlas;
    HashMap<String, Integer> spriteIndexMap;
    Array<Sprite> sprites;
    IntPoint2D whiteKing;
    IntPoint2D blackKing;
    IntPoint2D lastFrom;
    IntPoint2D lastTo;
    PieceInfo lastRemoved;
    IntPoint2D lastMovedDoubleWhitePawn;
    IntPoint2D lastMovedDoubleBlackPawn;

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
                String name = new PieceInfo(team, type).getSpriteName();
                spriteIndexMap.put(name, count++);
            }
        }

        pieces = new PieceInfo[8][8];

        pieces[0][0] = new PieceInfo(Team.BLACK, PieceType.ROOK);
        pieces[1][0] = new PieceInfo(Team.BLACK, PieceType.KNIGHT);
        pieces[2][0] = new PieceInfo(Team.BLACK, PieceType.BISHOP);
        pieces[3][0] = new PieceInfo(Team.BLACK, PieceType.KING);
        pieces[4][0] = new PieceInfo(Team.BLACK, PieceType.QUEEN);
        pieces[5][0] = new PieceInfo(Team.BLACK, PieceType.BISHOP);
        pieces[6][0] = new PieceInfo(Team.BLACK, PieceType.KNIGHT);
        pieces[7][0] = new PieceInfo(Team.BLACK, PieceType.ROOK);
        for (int i = 0; i < 8; i++) {
            pieces[i][1] = new PieceInfo(Team.BLACK, PieceType.PAWN);
        }

        pieces[0][7] = new PieceInfo(Team.WHITE, PieceType.ROOK);
        pieces[1][7] = new PieceInfo(Team.WHITE, PieceType.KNIGHT);
        pieces[2][7] = new PieceInfo(Team.WHITE, PieceType.BISHOP);
        pieces[3][7] = new PieceInfo(Team.WHITE, PieceType.KING);
        pieces[4][7] = new PieceInfo(Team.WHITE, PieceType.QUEEN);
        pieces[5][7] = new PieceInfo(Team.WHITE, PieceType.BISHOP);
        pieces[6][7] = new PieceInfo(Team.WHITE, PieceType.KNIGHT);
        pieces[7][7] = new PieceInfo(Team.WHITE, PieceType.ROOK);
        for (int i = 0; i < 8; i++) {
            pieces[i][6] = new PieceInfo(Team.WHITE, PieceType.PAWN);
        }

        whiteKing = new IntPoint2D(3, 7);
        blackKing = new IntPoint2D(3, 0);

        generateTexture();
    }

    private void generateTexture() {
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

        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                drawPiece(batch, c, r);
            }
        }
    }

    private void drawPiece(SpriteBatch batch, int col, int row) {
        PieceInfo info = pieces[col][row];

        if (info == null) {
            return;
        }

        String name = info.getSpriteName();

        Sprite sprite = sprites.get(spriteIndexMap.get(name));
        sprite.setX(col * squareSize + (float) squareSize / 2 - sprite.getWidth() / 2);
        sprite.setY(row * squareSize + (float) squareSize / 2 - sprite.getHeight() / 2);
        sprite.draw(batch);
    }

    IntPoint2D getKing(Team team) {
        return (team == Team.WHITE) ? whiteKing : blackKing;
    }

    public PieceInfo getPiece(IntPoint2D location) {
        if (!isInBounds(location)) {
            return null;
        }
        return pieces[location.getX()][location.getY()];
    }

    public void movePiece(IntPoint2D from, IntPoint2D to) {
        movePieceWithoutSpecialMoves(from, to);

        // Check if the moved piece is a pawn and moved two squares
        PieceInfo movedPiece = pieces[to.getX()][to.getY()];
        if (movedPiece.type == PieceType.PAWN && Math.abs(from.getY() - to.getY()) == 2) {
            System.out.println("Double pawn move");
            if (movedPiece.team == Team.WHITE) lastMovedDoubleWhitePawn = to;
            if (movedPiece.team == Team.BLACK) lastMovedDoubleBlackPawn = to;
            movedPiece.doublePawnMoveOnMoveNumber = GameState.moveNumber;
        } else {
            System.out.println("Not a double pawn move");
            System.out.println(GameState.moveNumber);
        }

        // Handle en passant capture
        IntPoint2D enPassantCapture = new IntPoint2D(to.getX(), from.getY());
        if (getPiece(enPassantCapture) != null && from.getX() != to.getX() && getPiece(enPassantCapture).doublePawnMoveOnMoveNumber == GameState.moveNumber - 1) {
            removePiece(enPassantCapture);
        }

        lastFrom = from;
        lastTo = to;
    }
    public void tryMovingPieces(IntPoint2D from, IntPoint2D to) {
        movePieceWithoutSpecialMoves(from, to);

        lastFrom = from;
        lastTo = to;
    }
    private void movePieceWithoutSpecialMoves(IntPoint2D from, IntPoint2D to) {
        if (from.equals(whiteKing)) {
            whiteKing = to;
        } else if (from.equals(blackKing)) {
            blackKing = to;
        }

        lastRemoved = pieces[to.getX()][to.getY()];

        pieces[to.getX()][to.getY()] = pieces[from.getX()][from.getY()];
        pieces[from.getX()][from.getY()] = null;
    }
    public void removePiece(IntPoint2D location) {
        if (!isInBounds(location)) {
            return;
        }
        pieces[location.getX()][location.getY()] = null;
    }

    public void undoMove() {
        PieceInfo temp = lastRemoved;

        movePiece(lastTo, lastFrom);

        pieces[lastFrom.getX()][lastFrom.getY()] = temp;
    }

    public IntPoint2D getPoint(int x, int y) {
        return new IntPoint2D(x / squareSize, 7 - y / squareSize);
    }

    public IntRect getRectangle(IntPoint2D point) {
        return new IntRect(point.getX() * squareSize, (point.getY()) * squareSize, squareSize, squareSize);
    }

    public boolean isInBounds(IntPoint2D location) {
        return location.getX() < 8 && location.getX() >= 0 && location.getY() < 8 && location.getY() >= 0;
    }
}
