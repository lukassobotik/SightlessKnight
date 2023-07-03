/*
 * TODO:
 * Display check status
 * Display end of game status (check vs stale mate)
 *
 * Change to highlighting tiles by making different colored (or tinted?) textures instead of drawing rectangles
 *
 */


package lukas.sobotik.sightlessknight;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;

public class GameState implements InputProcessor {
    Board board;
    Texture overlayBoxTexture;
    Sprite overlayBoxSprite;
    int size;
    Team currentTurn;
    ArrayList<IntPoint2D> validMoves;
    IntPoint2D selected;
    static int moveNumber = 0;
    static final Team playerTeam = Team.WHITE;

    GameState(int size, Board board) {
        validMoves = new ArrayList<>();

        currentTurn = Team.WHITE;

        this.size = size;

        IntRect rect = board.getRectangle(new IntPoint2D(0, 0));
        int nextPow2 = Integer.highestOneBit(rect.getHeight() - 1) << 1;
        Pixmap pixmap = new Pixmap(nextPow2, nextPow2, Format.RGBA8888);
        int borderWidth = rect.getWidth() / 10 + 1;
        pixmap.setColor(Color.WHITE);
        for (int i = 0; i < borderWidth; i++) {
            pixmap.drawRectangle(rect.getX() + i, rect.getY() + i,
                    rect.getWidth() - 2 * i, rect.getHeight() - 2 * i);
        }
        overlayBoxTexture = new Texture(pixmap);
        pixmap.dispose();
        overlayBoxSprite = new Sprite(overlayBoxTexture, rect.getWidth(), rect.getHeight());

        this.board = board;
    }

    public void draw(SpriteBatch batch) {
        if (selected != null) {
            IntRect tile = board.getRectangle(selected);
            overlayBoxSprite.setPosition(tile.getX(), tile.getY());
            overlayBoxSprite.setColor(Color.GREEN);
            overlayBoxSprite.draw(batch);
        }

        for (IntPoint2D moveTile : validMoves) {
            IntRect tile = board.getRectangle(moveTile);
            overlayBoxSprite.setPosition(tile.getX(), tile.getY());
            Color color = (board.getPiece(moveTile) == null) ? Color.YELLOW : Color.RED;
            overlayBoxSprite.setColor(color);
            overlayBoxSprite.draw(batch);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        IntPoint2D tileIdx = board.getPoint(x, y);

        if (!validMoves.isEmpty()) {
            for (IntPoint2D move : validMoves) {
                if (tileIdx.equals(move)) {
                    moveNumber++;
                    System.err.println("moveNumber: " + moveNumber);
                    movePieceAndEndTurn(tileIdx);
                    break;
                }
            }
            validMoves.clear();
            selected = null;

            FenUtils fenUtils = new FenUtils(board.pieces, board.whiteKing, board.blackKing, board.lastTo, board.lastMovedDoubleWhitePawn, board.lastMovedDoubleBlackPawn);
            System.out.println(fenUtils.generateFenFromCurrentPosition());
        } else {
            PieceInfo piece = board.getPiece(tileIdx);
            if (piece == null) return false;
            System.out.println(Rules.isEnemyAttackingThisSquare(new IntPoint2D(4, 4), piece.team, board));
            if (piece.team == currentTurn) {
                selected = tileIdx;
                Rules.getValidMoves(validMoves, tileIdx, piece, board);
            }
        }

        return false;
    }

    private void movePieceAndEndTurn(IntPoint2D destination) {
        board.movePiece(selected, destination);
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }


    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}