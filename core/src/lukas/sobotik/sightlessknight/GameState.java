package lukas.sobotik.sightlessknight;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.ArrayList;

public class GameState implements InputProcessor {
    Board board;
    Texture overlayBoxTexture;
    Sprite overlayBoxSprite;
    int size;
    Team currentTurn;
    boolean hasGameEnded = false;
    ArrayList<BoardLocation> validMoves;
    BoardLocation selectedPieceLocation;
    static int moveNumber = 0;
    static final Team playerTeam = Team.WHITE;
    static boolean isPawnPromotionPending = false;
    static BoardLocation promotionLocation;
    static PieceType selectedPromotionPieceType;
    Stage stage;
    Dialog pawnPromotionDialog, gameOverDialog;

    GameState(int size, Board board) {
        validMoves = new ArrayList<>();
        currentTurn = Team.WHITE;
        this.size = size;
        this.board = board;
        createOverlays();
    }
    private void createOverlays() {
        Rectangle rect = board.getRectangle(new BoardLocation(0, 0));
        int nextPow2 = Integer.highestOneBit(rect.getHeight() - 1) << 1;
        Pixmap pixmap = new Pixmap(nextPow2, nextPow2, Format.RGBA8888);
        int borderWidth = rect.getWidth() / 30 + 1;
        pixmap.setColor(Color.WHITE);
        for (int i = 0; i < borderWidth; i++) {
            pixmap.drawRectangle(rect.getX() + i, rect.getY() + i,
                    rect.getWidth() - 2 * i, rect.getHeight() - 2 * i);
        }
        overlayBoxTexture = new Texture(pixmap);
        pixmap.dispose();
        overlayBoxSprite = new Sprite(overlayBoxTexture, rect.getWidth(), rect.getHeight());
    }
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    public void draw(SpriteBatch batch) {
        if (selectedPieceLocation != null) {
            Rectangle tile = board.getRectangle(selectedPieceLocation);
            overlayBoxSprite.setPosition(tile.getX(), tile.getY());
            overlayBoxSprite.setColor(Color.GREEN);
            overlayBoxSprite.draw(batch);
        }

        for (BoardLocation moveTile : validMoves) {
            Rectangle tile = board.getRectangle(moveTile);
            overlayBoxSprite.setPosition(tile.getX(), tile.getY());
            Color color = (board.getPiece(moveTile) == null) ? Color.YELLOW : Color.RED;
            overlayBoxSprite.setColor(color);
            overlayBoxSprite.draw(batch);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isPawnPromotionPending) {
            switch (keycode) {
                case Input.Keys.Q:
                    promotePawn(PieceType.QUEEN);
                    break;
                case Input.Keys.R:
                    promotePawn(PieceType.ROOK);
                    break;
                case Input.Keys.K:
                    promotePawn(PieceType.KNIGHT);
                    break;
                case Input.Keys.B:
                    promotePawn(PieceType.BISHOP);
            }
        }
        if (hasGameEnded) {
            if (keycode == Input.Keys.R) {
                board.resetBoardPosition();
                hasGameEnded = false;
                moveNumber = 0;
                currentTurn = Team.WHITE;
                gameOverDialog.cancel();
                gameOverDialog.hide();
                gameOverDialog = null;
            }
        }

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
        if (hasGameEnded) {
            if (Rules.isStalemate(currentTurn, board)) {
                showGameOverDialog(CheckState.STALEMATE);
                return false;
            }
            if (Rules.isCheckmate(currentTurn, board)) {
                showGameOverDialog(CheckState.CHECKMATE);
                return false;
            }
        }
        if (isPawnPromotionPending) {
            return false;
        }

        BoardLocation clickedLocation = board.getPoint(x, y);

        if (!validMoves.isEmpty()) {
            for (BoardLocation move : validMoves) {
                if (clickedLocation.equals(move)) {
                    moveNumber++;
                    System.err.println("moveNumber: " + moveNumber);
                    movePieceAndEndTurn(clickedLocation);
                    System.out.println("isCheckmate: " + Rules.isCheckmate(currentTurn, board));
                    System.out.println("isStalemate: " + Rules.isStalemate(currentTurn, board));
                    hasGameEnded = Rules.isCheckmate(currentTurn, board) || Rules.isStalemate(currentTurn, board);
                    break;
                }
            }
            validMoves.clear();
            selectedPieceLocation = null;

            FenUtils fenUtils = new FenUtils(board.pieces, board.whiteKingLocation, board.blackKingLocation, board.lastToLocation, board.lastDoublePawnMoveWithWhitePieces, board.lastDoublePawnMoveWithBlackPieces);
            System.out.println(fenUtils.generateFenFromCurrentPosition());
        } else {
            Piece piece = board.getPiece(clickedLocation);
            if (piece == null) return false;
            if (piece.team == currentTurn) {
                selectedPieceLocation = clickedLocation;
                Rules.getValidMoves(validMoves, clickedLocation, piece, board);
            }
        }
        return false;
    }
    private void movePieceAndEndTurn(BoardLocation destination) {
        if (destination != null) {
            board.movePiece(selectedPieceLocation, destination);
        }
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }
    private void showPawnPromotionDialog() {
        pawnPromotionDialog = new Dialog("Promotion", new Skin(Gdx.files.internal("ui-skin.json")));
        pawnPromotionDialog.text("Press the corresponding letter for a piece to promote the pawn to:");
        pawnPromotionDialog.button("Queen (q)", PieceType.QUEEN).pad(10);
        pawnPromotionDialog.button("Bishop (b)", PieceType.BISHOP).pad(10);
        pawnPromotionDialog.button("Rook (r)", PieceType.ROOK).pad(10);
        pawnPromotionDialog.button("Knight (k)", PieceType.KNIGHT).pad(10);
        pawnPromotionDialog.show(stage);
    }
    private void showGameOverDialog(CheckState state) {
        if (state.equals(CheckState.CHECKMATE)) {
            gameOverDialog = new Dialog((currentTurn == Team.WHITE ? "Black" : "White") + " Wins!", new Skin(Gdx.files.internal("ui-skin.json")));
            gameOverDialog.text((currentTurn == Team.WHITE ? "Black" : "White") + " wins by checkmate.");
            gameOverDialog.button("Restart (r)", "restart").pad(10);
            gameOverDialog.show(stage);
        } else if (state.equals(CheckState.STALEMATE)) {
            gameOverDialog = new Dialog("Draw", new Skin(Gdx.files.internal("ui-skin.json")));
            gameOverDialog.text("by stalemate");
            gameOverDialog.button("Restart (r)", "restart").pad(10);
            gameOverDialog.show(stage);
        }
    }
    private void promotePawn(PieceType selectedPieceType) {
        board.promotePawn(promotionLocation, selectedPieceType);
        currentTurn = (currentTurn == Team.WHITE) ? Team.BLACK : Team.WHITE;
        isPawnPromotionPending = false;
        promotionLocation = null;
        selectedPromotionPieceType = null;
        pawnPromotionDialog.hide();
        movePieceAndEndTurn(null);
    }
    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        if (isPawnPromotionPending && promotionLocation != null) {
            showPawnPromotionDialog();
        }
        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}