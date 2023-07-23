package lukas.sobotik.sightlessknight.views.play;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import lukas.sobotik.sightlessknight.ai.PerftFunction;
import lukas.sobotik.sightlessknight.gamelogic.*;
import lukas.sobotik.sightlessknight.views.MainLayout;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@PageTitle("Play")
@Route(value = "play", layout = MainLayout.class)
public class PlayView extends VerticalLayout implements HasUrlParameter<String> {
    FenUtils fenUtils;
    AlgebraicNotationUtils algebraicNotationUtils;
    GameState gameState;
    Board board;

    // Knight Game
    BoardLocation targetSquare = null;
    BoardLocation startSquare = null;

    Piece pieceForKinglessGames = null;
    HorizontalLayout gameContentLayout, targetSquareLayout;
    VerticalLayout algebraicNotationHistoryLayout, gameInfoLayout;
    public static String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
        System.out.println(s);
        if (s == null) {
            initialize();
        } else {
            Piece[] pieces = new Piece[64];
            switch (s) {
                case "knight" -> {
                    pieces = generateKnightGame();
                }
            }
            initialize(pieces, true);
            showTargetSquare();
        }
    }
    public PlayView() {

    }

    private void initialize() {
        Piece[] pieces = new Piece[64];
        fenUtils = new FenUtils(pieces);
        pieces = fenUtils.generatePositionFromFEN(STARTING_POSITION);
        board = new Board(64, pieces, fenUtils);
        initialize(pieces, false);
    }
    private void initialize(Piece[] pieces, boolean kinglessGame) {
        setAlignItems(Alignment.CENTER);
        setHeight("100%");

        gameState = new GameState(board, fenUtils.getStartingTeam(), kinglessGame);
        algebraicNotationUtils = new AlgebraicNotationUtils(fenUtils, gameState, board);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        TextField textField = new TextField();
        textField.setAutofocus(true);
        Button button = new Button("Play");
        button.addClickListener(buttonClickEvent -> {
            Notification.show(textField.getValue());

            var splitFromAndTo = textField.getValue().split(":");
            var from = splitFromAndTo[0].split(",");
            var to = splitFromAndTo[1].split(",");

            BoardLocation fromLoc = new BoardLocation(Integer.parseInt(from[0]), Integer.parseInt(from[1]));
            BoardLocation toLoc = new BoardLocation(Integer.parseInt(to[0]), Integer.parseInt(to[1]));

            playMove(fromLoc, toLoc);
        });

        gameContentLayout = new HorizontalLayout();
        gameContentLayout.addClassName("game_content_layout");

        var quickSettingsLayout = new VerticalLayout();
        quickSettingsLayout.addClassName("game_content_layout_child");
        gameContentLayout.add(quickSettingsLayout);

        gameContentLayout.setHeightFull();
        gameContentLayout.setWidthFull();
        add(gameContentLayout);

        horizontalLayout.add(textField, button);
        add(horizontalLayout);
        createBoard(pieces);

        gameInfoLayout = new VerticalLayout();
        gameInfoLayout.setClassName("game_content_layout_child");

        targetSquareLayout = new HorizontalLayout();
        targetSquareLayout.setVisible(false);
        targetSquareLayout.setWidthFull();
        targetSquareLayout.setAlignItems(Alignment.CENTER);
        gameInfoLayout.add(targetSquareLayout);

        algebraicNotationHistoryLayout = new VerticalLayout();
        algebraicNotationHistoryLayout.addClassName("move_history");

        gameInfoLayout.add(algebraicNotationHistoryLayout);
        gameContentLayout.add(gameInfoLayout);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> System.out.println("number of pos 2: " + new PerftFunction(board, gameState, this).playMoves(2, Team.WHITE, false)));
        executorService.execute(() -> System.out.println("number of pos 3: " + new PerftFunction(board, gameState, this).playMoves(3, Team.BLACK, false)));
        executorService.execute(() -> System.out.println("number of pos 4: " + new PerftFunction(board, gameState, this).playMoves(4, Team.WHITE, false)));
        executorService.execute(() -> System.out.println("number of pos 5: " + new PerftFunction(board, gameState, this).playMoves(5, Team.WHITE, false)));
        executorService.shutdown();
    }

    private void playMove(BoardLocation from, BoardLocation to) {
        Move move = new Move(from, to, board.getPiece(from), board.getPiece(to));
        gameState.play(new Move(from, to));
        board = gameState.getBoard();
        printBoard(board.pieces);
        createBoard(board.pieces);
        checkIfGameEnded();
        if (GameState.isPawnPromotionPending) {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Pawn Promotion");
            Image queenButton = initializePawnPromotionButton("_queen.svg", "Queen", PieceType.QUEEN, move, dialog);
            Image rookButton = initializePawnPromotionButton("_rook.svg", "Rook", PieceType.ROOK, move, dialog);
            Image knightButton = initializePawnPromotionButton("_knight.svg", "Knight", PieceType.KNIGHT, move, dialog);
            Image bishopButton = initializePawnPromotionButton("_bishop.svg", "Bishop", PieceType.BISHOP, move, dialog);
            dialog.add(queenButton, rookButton, knightButton, bishopButton);
            dialog.setCloseOnOutsideClick(false);
            dialog.open();
            add(dialog);
        } else {
            getAlgebraicNotation();
        }

        // Piece Move Drills
        if (targetSquare != null && Rules.isPieceOnTargetSquare(pieceForKinglessGames, targetSquare, board)) {
            generateKnightGame();
            createBoard(board.pieces);
            showTargetSquare();
        }
    }

    private Image initializePawnPromotionButton(String imagePath, String pieceString, PieceType pieceType, Move move, Dialog dialog) {
        Image button = new Image("images/sprites/" + ((GameState.currentTurn == Team.WHITE ? "w" : "b") + imagePath), pieceString);
        button.addClickListener(view -> {
            gameState.movePieceAndEndTurn(GameState.promotionLocation);
            gameState.promotePawn(pieceType);
            move.getMovedPiece().promotion = pieceType;
            board.getPiece(move.getTo()).promotion = pieceType;
            gameState.createParsedMoveHistory(move);
            getAlgebraicNotation();
            dialog.close();
            createBoard(board.pieces);
            checkIfGameEnded();
        });
        return button;
    }

    private void getAlgebraicNotation() {
        showAlgebraicNotationHistory();
        System.out.println("moveHistory: " + gameState.moveHistory);
        System.out.println("parsedMoveHistory: " + gameState.parsedMoveHistory);
        System.out.println("fenMoveHistory: " + gameState.fenMoveHistory);
    }

    private void showAlgebraicNotationHistory() {
        // Remove old items
        List<Component> componentsToRemove = algebraicNotationHistoryLayout.getChildren().toList();
        for (Component component : componentsToRemove) {
            algebraicNotationHistoryLayout.remove(component);
        }

        for (int i = ((int) algebraicNotationHistoryLayout.getChildren().count()); i < gameState.parsedMoveHistory.size(); i += 2) {
            String parsedMove = gameState.parsedMoveHistory.get(i);

            Paragraph whiteMove = new Paragraph(parsedMove);
            whiteMove.addClassName("algebraic_history_item");
            Paragraph blackMove;
            if (!(i + 1 >= gameState.moveHistory.size())) {
                String nextParsedMove = gameState.parsedMoveHistory.get(i + 1);
                blackMove = new Paragraph(nextParsedMove);
            } else {
                blackMove = new Paragraph("");
            }
            blackMove.addClassName("algebraic_history_item");

            HorizontalLayout moveLayout = new HorizontalLayout();
            moveLayout.add(whiteMove, blackMove);
            moveLayout.setWidth("auto");

            algebraicNotationHistoryLayout.add(moveLayout);
            algebraicNotationHistoryLayout.getElement().executeJs("this.scrollTop = this.scrollHeight");
        }
    }

    private void checkIfGameEnded() {
        if (gameState.hasGameEnded) {
            if (Rules.isStalemate(GameState.currentTurn, board) && !GameState.isPawnPromotionPending) {
                Notification.show("Game Over!");
                VerticalLayout dialogLayout = new VerticalLayout();
                Text dialogText = new Text("Game Drawn by Stalemate");
                createGameOverDialog(dialogLayout, dialogText);
            }
            if (Rules.isCheckmate(GameState.currentTurn, board)) {
                Notification.show("Game Over!");
                VerticalLayout dialogLayout = new VerticalLayout();
                Text dialogText = new Text((GameState.currentTurn == Team.WHITE ? "Black" : "White") + " Won by Checkmate");
                createGameOverDialog(dialogLayout, dialogText);
            }
        }
    }

    private void createGameOverDialog(VerticalLayout dialogLayout, Text dialogText) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Game Over!");
        dialog.setCloseOnOutsideClick(true);
        dialogLayout.add(dialogText);
        Button newGameButton = new Button("New Game");
        newGameButton.addClickListener(view -> {
            board.resetBoardPosition(STARTING_POSITION);
            gameState.resetHistory();
            GameState.moveNumber = 0;
            gameState.hasGameEnded = false;
            dialog.close();
            createBoard(board.pieces);
        });
        dialogLayout.add(newGameButton);
        dialog.add(dialogLayout);
        dialog.open();
        add(dialog);
    }

    public void createBoard(Piece[] pieces) {
        VerticalLayout boardLayout = findBoardLayout();
        if (boardLayout == null) {
            boardLayout = new VerticalLayout();
            boardLayout.setAlignItems(Alignment.CENTER);
            boardLayout.setClassName("board");
            gameContentLayout.add(boardLayout);
        } else {
            boardLayout.removeAll();
        }

        for (int rank = 7; rank >= 0; rank--) {
            HorizontalLayout rowLayout = new HorizontalLayout();
            rowLayout.addClassName("board_row");

            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece piece = pieces[index];

                String cellValue;
                VerticalLayout square = new VerticalLayout();
                square.addClassName("square");

                if ((rank + file) % 2 == 0) {
                    square.addClassName("dark_square");
                } else {
                    square.addClassName("light_square");
                }
                square.addClassName(board.getPointFromArrayIndex(index).getX() + "-" + board.getPointFromArrayIndex(index).getY());
                VerticalLayout finalBoardLayout = boardLayout;
                square.addClickListener(view -> {
                    if (gameState.hasGameEnded) return;
                    gameState.play(new Move(board.getPointFromArrayIndex(index), new BoardLocation(-1, -1)));

                    AtomicReference<BoardLocation> selectedSquare = new AtomicReference<>(null);
                    finalBoardLayout.getChildren().forEach(component -> component.getChildren().forEach(componentRow -> {
                        if (componentRow.getClassNames().contains("selected")) {
                            componentRow.getClassNames().forEach(className -> {
                                if (className.contains("-")) {
                                    var coordinates = className.split("-");
                                    selectedSquare.set(new BoardLocation(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
                                }
                            });
                        }
                    }));

                    AtomicReference<BoardLocation> toLocation = new AtomicReference<>(null);
                    if ((piece == null || piece.team != GameState.currentTurn) && selectedSquare.get() != null) {
                        square.getClassNames().forEach(className -> {
                            if (className.contains("-")) {
                                var coordinates = className.split("-");
                                toLocation.set(new BoardLocation(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
                            }
                        });
                        System.out.println("MOVE to " + toLocation.get().getX() + "," + toLocation.get().getY() + " from " + selectedSquare.get().getX() + "," + selectedSquare.get().getY());
                        playMove(selectedSquare.get(), toLocation.get());
                    }

                    // Remove all previously selected squares
                    if (piece != null && (piece.team == GameState.currentTurn || GameState.kinglessGame)) {
                        finalBoardLayout.getChildren().forEach(component -> component.getChildren()
                                .forEach(componentRow -> componentRow.getClassNames().remove("selected")));
                    } else {
                        return;
                    }

                    square.addClassName("selected");
                });
                if (piece == null) {
                    cellValue = "";
                } else {
                    cellValue = String.valueOf(fenUtils.getSymbolFromPieceType(piece.type, piece.team));
                    Image image = new Image(getChessPieceUrl(cellValue), cellValue);
                    image.addClassName("square_image");
                    square.add(image);
                }


                rowLayout.add(square);
            }
            boardLayout.add(rowLayout);
        }
    }

    private VerticalLayout findBoardLayout() {
        AtomicReference<VerticalLayout> layout = new AtomicReference<>();
        gameContentLayout.getChildren().forEach(component -> {
            if (component instanceof VerticalLayout && component.getClassNames().contains("board")) {
                layout.set((VerticalLayout) component);
            }
        });
        return layout.get();
    }


    private String getChessPieceUrl(String s) {
        if (s == null) {
            return "";
        }

        switch (s) {
            case "P" -> {
                return "images/sprites/w_pawn.svg";
            }
            case "N" -> {
                return "images/sprites/w_knight.svg";
            }
            case "B" -> {
                return "images/sprites/w_bishop.svg";
            }
            case "R" -> {
                return "images/sprites/w_rook.svg";
            }
            case "Q" -> {
                return "images/sprites/w_queen.svg";
            }
            case "K" -> {
                return "images/sprites/w_king.svg";
            }
            case "p" -> {
                return "images/sprites/b_pawn.svg";
            }
            case "n" -> {
                return "images/sprites/b_knight.svg";
            }
            case "b" -> {
                return "images/sprites/b_bishop.svg";
            }
            case "r" -> {
                return "images/sprites/b_rook.svg";
            }
            case "q" -> {
                return "images/sprites/b_queen.svg";
            }
            case "k" -> {
                return "images/sprites/b_king.svg";
            }
        }
        return "";
    }

    private void printBoard(Piece[] pieces) {
        // Remove old boards
        String classNameToRemove = "board";
        List<Component> componentsToRemove = getChildren().toList();
        for (Component component : componentsToRemove) {
            if (component.getClassNames().contains(classNameToRemove)) {
                remove(component);
            }
        }

        for (int rank = 7; rank >= 0; rank--) {
            Paragraph rankParagraph = new Paragraph();
            StringBuilder builder = new StringBuilder();
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                Piece piece = pieces[index];

                if (piece == null) {
                    System.out.print(". "); // Empty square
                    builder.append(". ");
                } else {
                    System.out.print(fenUtils.getSymbolFromPieceType(piece.type, piece.team) + " "); // Piece character
                    builder.append(fenUtils.getSymbolFromPieceType(piece.type, piece.team)).append(" ");
                }
            }
            System.out.println();
            rankParagraph.setText(builder.toString());
            rankParagraph.setId(String.valueOf(rank));
            rankParagraph.addClassName("board");
        }
        System.out.println("-----------------------------");
    }

    private Piece[] generateKnightGame() {
        Piece piece = new Piece(Team.WHITE, PieceType.KNIGHT);
        pieceForKinglessGames = piece;

        Piece[] pieces = new Piece[64];
        fenUtils = new FenUtils(pieces);

        int knightPos = new Random().nextInt(64);
        pieces[knightPos] = piece;

        board = new Board(64, pieces, fenUtils);
        gameState = new GameState(board, fenUtils.getStartingTeam(), true);
        startSquare = board.getPointFromArrayIndex(knightPos);
        targetSquare = board.getPointFromArrayIndex(getRandomSquareWithinDistance(knightPos, 3));

        System.out.println(fenUtils.generateFenFromPosition(pieces));
        System.out.println(targetSquare.getAlgebraicNotationLocation());
        return pieces;
    }
    public void showTargetSquare() {
        targetSquareLayout.removeAll();
        targetSquareLayout.setVisible(true);
        var paragraph = new Paragraph(startSquare.getAlgebraicNotationLocation() + " → " + targetSquare.getAlgebraicNotationLocation());
        paragraph.setWidthFull();
        paragraph.addClassName("target_square");
        targetSquareLayout.setHeight("auto");
        targetSquareLayout.add(paragraph);
    }
    public static int getRandomSquareWithinDistance(int square, int distance) {
        Random random = new Random();
        int targetSquare;
        do {
            int file = square % 8;
            int rank = square / 8;

            int minFile = Math.max(0, file - distance);
            int maxFile = Math.min(7, file + distance);
            int minRank = Math.max(0, rank - distance);
            int maxRank = Math.min(7, rank + distance);

            int randomFile = random.nextInt(maxFile - minFile + 1) + minFile;
            int randomRank = random.nextInt(maxRank - minRank + 1) + minRank;

            targetSquare = randomRank * 8 + randomFile;
        } while (targetSquare == square);

        return targetSquare;
    }
}
