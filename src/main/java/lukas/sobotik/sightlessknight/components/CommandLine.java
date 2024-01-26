package lukas.sobotik.sightlessknight.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import lukas.sobotik.sightlessknight.ai.PerftFunction;
import lukas.sobotik.sightlessknight.gamelogic.Move;
import lukas.sobotik.sightlessknight.views.play.PlayView;

import java.util.concurrent.Executors;

public class CommandLine extends HorizontalLayout {

    Button enterButton;
    TextField textField;
    public CommandLine(PlayView playView) {
        setClassName("text_field_layout");
        setWidthFull();
        textField = new TextField();
        textField.setAutofocus(true);
        enterButton = new Button("Play");
        textField.addKeyDownListener(Key.ENTER, keyDownEvent -> enterButton.click());
        add(textField, enterButton);
        getStyle().set("width", "100%");
        getStyle().set("height", "100%");
        addClassName("game_info_layout_child");

//        onTextChange();

        enterButton.addClickListener(event -> {
            var command = textField.getValue();
            if (!command.startsWith("/")) {
                return;
            }

            System.out.println("Command: " + command);

            var split = command.split(" ");
            if (command.startsWith("/undo")) {
                Move lastMove = playView.gameState.moveHistory.get(playView.gameState.moveHistory.size() - 1);
                playView.gameState.undoMove(lastMove);
                playView.createBoard(playView.gameState.board.pieces);
                playView.gameState.moveHistory.remove(playView.gameState.moveHistory.size() - 1);
            }
            if (command.startsWith("/perft")) {
                if (split.length == 2) {
                    var depth = Integer.parseInt(split[1]);
                    var perftFunction = new PerftFunction(playView.gameState.board, playView.gameState, playView);
                    Executors.newSingleThreadExecutor().execute(() -> {
                        var positions = perftFunction.playMoves(depth, playView.gameState.currentTurn, false, true, true);
                        System.out.println("Positions: " + positions);
                    });
                    return;
                }
                Notification.show("Invalid amount of arguments");
            }
        });
    }

    public void setClickListener(ComponentEventListener<ClickEvent<Button>> clickListener) {
        enterButton.addClickListener(clickListener);
    }

    public String getCommand() {
        return textField.getValue();
    }

    public void clear() {
        textField.clear();
    }

    public void setCommand(String command) {
        textField.setValue(command);
    }

    public void onTextChange() {
        textField.addKeyDownListener(event -> {
            System.out.println(textField.getValue());
            if (getCommand().startsWith("/")) {
                System.out.println("Command: " + getCommand());
            }
        });
    }
}
