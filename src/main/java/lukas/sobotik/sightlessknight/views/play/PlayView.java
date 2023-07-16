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
import lukas.sobotik.sightlessknight.gamelogic.*;
import lukas.sobotik.sightlessknight.views.MainLayout;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@PageTitle("Play")
@Route(value = "play", layout = MainLayout.class)
public class PlayView extends VerticalLayout implements HasUrlParameter<String> {
    FenUtils fenUtils;
    GameState gameState;
    Board board;
    BoardLocation targetSquare = null;
    Piece pieceForKinglessGames = null;
    HorizontalLayout gameContentLayout;
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

        gameState = new GameState(board, fenUtils.getStartingTeam(), kinglessGame);

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
        add(gameContentLayout);

        horizontalLayout.add(textField, button);
        add(horizontalLayout);
        printBoard(pieces);
        createBoard(pieces);

    }

    private void playMove(BoardLocation from, BoardLocation to) {
        gameState.play(from, to);
        printBoard(board.pieces);
        createBoard(board.pieces);
        System.out.println(gameState.hasGameEnded);
        if (gameState.hasGameEnded) {
            Notification.show("Game Over!");
            if (Rules.isStalemate(GameState.currentTurn, board)) {
                VerticalLayout dialogLayout = new VerticalLayout();
                Text dialogText = new Text("Game Drawn by Stalemate");
                createGameOverDialog(dialogLayout, dialogText);
            }
            System.out.println(Rules.isStalemate(GameState.currentTurn, board));
            if (Rules.isCheckmate(GameState.currentTurn, board)) {
                VerticalLayout dialogLayout = new VerticalLayout();
                Text dialogText = new Text((GameState.currentTurn == Team.WHITE ? "Black" : "White") + " Won by Checkmate");
                createGameOverDialog(dialogLayout, dialogText);
            }
        }
        if (GameState.isPawnPromotionPending) {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Pawn Promotion");
            Image queenButton = new Image("images/sprites/" + (GameState.currentTurn == Team.WHITE ? "b" : "w" + "_queen.svg"), "Queen");
            queenButton.addClickListener(view -> {
                gameState.promotePawn(PieceType.QUEEN);
                dialog.close();
                createBoard(board.pieces);
            });
            Image rookButton = new Image("images/sprites/" + (GameState.currentTurn == Team.WHITE ? "b" : "w" + "_rook.svg"), "Rook");
            rookButton.addClickListener(view -> {
                gameState.promotePawn(PieceType.ROOK);
                dialog.close();
                createBoard(board.pieces);
            });
            Image knightButton = new Image("images/sprites/" + (GameState.currentTurn == Team.WHITE ? "b" : "w" + "_knight.svg"), "Knight");
            knightButton.addClickListener(view -> {
                gameState.promotePawn(PieceType.KNIGHT);
                dialog.close();
                createBoard(board.pieces);
            });
            Image bishopButton = new Image("images/sprites/" + (GameState.currentTurn == Team.WHITE ? "b" : "w" + "_bishop.svg"), "Bishop");
            bishopButton.addClickListener(view -> {
                gameState.promotePawn(PieceType.BISHOP);
                dialog.close();
                createBoard(board.pieces);
            });
            dialog.add(queenButton, rookButton, knightButton, bishopButton);
            dialog.open();
            add(dialog);
        }
        // Piece Move Drills
        if (targetSquare != null && Rules.isPieceOnTargetSquare(pieceForKinglessGames, targetSquare, board)) {
            generateKnightGame();
            createBoard(board.pieces);
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

    private void createBoard(Piece[] pieces) {
        // Remove old rows
        List<Component> componentsToRemove = gameContentLayout.getChildren().toList();
        for (Component component : componentsToRemove) {
            if (component.getClassNames().contains("board")) {
                gameContentLayout.remove(component);
            }
        }

        VerticalLayout boardLayout = new VerticalLayout();
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
                square.addClickListener(view -> {
                    if (gameState.hasGameEnded) return;
                    gameState.play(board.getPointFromArrayIndex(index), new BoardLocation(-1, -1));

                    AtomicReference<BoardLocation> selectedSquare = new AtomicReference<>(null);
                    boardLayout.getChildren().forEach(component -> component.getChildren().forEach(componentRow -> {
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
                    if (piece != null && (piece.team == GameState.currentTurn || gameState.kinglessGame)) {
                        boardLayout.getChildren().forEach(component -> component.getChildren()
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
        boardLayout.setAlignItems(Alignment.CENTER);
        boardLayout.setClassName("board");
        boardLayout.setWidth(boardLayout.getHeight());

        gameContentLayout.add(boardLayout);
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
        targetSquare = board.getPointFromArrayIndex(getRandomSquareWithinDistance(knightPos, 3));

        System.out.println(fenUtils.generateFenFromPosition(pieces));
        System.out.println(targetSquare.getStringLocation());

        Notification notification = new Notification(targetSquare.getStringLocation());
        notification.setPosition(Notification.Position.MIDDLE);
        notification.setDuration(1000);
        notification.open();
        return pieces;
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
