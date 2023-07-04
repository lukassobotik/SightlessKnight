package lukas.sobotik.sightlessknight;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class ChessboardUI extends Actor {
    private Board board;
    private GameState gameState;

    public ChessboardUI(TextureAtlas myTextures, int squareSize) {
        board = new Board(squareSize * 8, myTextures);
        setWidth(squareSize * 8);
        setHeight(squareSize * 8);
    }
    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        board.draw((SpriteBatch) batch);
        gameState.draw((SpriteBatch) batch);
    }
    public void resizeBoard(int squareSize) {
        setWidth(squareSize * 8);
        setHeight(squareSize * 8);
    }
    public Board getBoard() {
        return board;
    }
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }
}
