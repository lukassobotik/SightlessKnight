package lukas.sobotik.sightlessknight;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class SightlessKnight implements ApplicationListener {
    private Stage stage;
    private ChessboardUI chessboardUI;
    private GameState gameState;

    @Override
    public void create() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        TextureAtlas chessSpriteAtlas = new TextureAtlas("data/chess_sprites.txt");

        int squareSize = (int) Math.min(h, w) / 8;
        stage = new Stage(new FitViewport(w, h));
        chessboardUI = new ChessboardUI(chessSpriteAtlas, squareSize);
        stage.addActor(chessboardUI);

        gameState = new GameState(squareSize, chessboardUI.getBoard());
        chessboardUI.setGameState(gameState);
        Gdx.input.setInputProcessor(gameState);
        gameState.setStage(stage);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        int squareSize = (int) Math.min(stage.getWidth(), stage.getHeight()) / 8;
        chessboardUI.resizeBoard(squareSize);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }
}
