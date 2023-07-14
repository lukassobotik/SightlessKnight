package lukas.sobotik.sightlessknight.views.play;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import lukas.sobotik.sightlessknight.gamelogic.*;
import lukas.sobotik.sightlessknight.views.MainLayout;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ PageTitle("Play")
@Route(value = "play", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class PlayView extends VerticalLayout {
    FenUtils fenUtils;
    GameState gameState;
    Board board;
    public PlayView() {
        Piece[] pieces = new Piece[64];
        board = new Board(64);
        gameState = new GameState(board);
        setAlignItems(Alignment.CENTER);

        fenUtils = new FenUtils(pieces, null, null, null, null, null);
        pieces = fenUtils.generatePositionFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

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
        }
    }

    private void createBoard(Piece[] pieces) {
        // Remove old rows
        List<Component> componentsToRemove = getChildren().toList();
        for (Component component : componentsToRemove) {
            if (component.getClassNames().contains("board")) {
                remove(component);
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
                    if ((piece == null || piece.team != gameState.currentTurn) && selectedSquare.get() != null) {
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
                    if (piece != null && piece.team == gameState.currentTurn) {
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

        add(boardLayout);
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
            add(rankParagraph);
        }
        System.out.println("-----------------------------");
    }
}
