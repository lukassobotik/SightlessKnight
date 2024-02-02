package lukas.sobotik.sightlessknight.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import lukas.sobotik.sightlessknight.ai.PerftFunction;
import lukas.sobotik.sightlessknight.gamelogic.GameState;
import lukas.sobotik.sightlessknight.gamelogic.Move;
import lukas.sobotik.sightlessknight.views.play.PlayView;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class CommandLine extends HorizontalLayout {

    Button enterButton, tooltipButton;
    TextField textField;
    CommandLineTooltip tooltip = new CommandLineTooltip();
    public CommandLine(PlayView playView) {
        setClassName("text_field_layout");
        addClassName("command_line_parent");
        setWidthFull();
        textField = new TextField();
        textField.setAutofocus(true);
        textField.addClassName("command_line");
        textField.setPlaceholder("Enter move or /command");
        add(tooltip);
        configureTextFieldTooltipBehavior();

        enterButton = new Button(new Icon("vaadin", "enter-arrow"));
        enterButton.addClassName("command_line_button");
        tooltipButton = new Button(new Icon("vaadin", "question"));
        tooltipButton.addClassName("command_line_tooltip_button");
        tooltipButton.addClickListener(event -> tooltip.toggle());
        textField.addKeyDownListener(Key.ENTER, keyDownEvent -> enterButton.click());
        textField.addBlurListener(event -> tooltip.hide());
        textField.addFocusListener(event -> {
            if (textField.getValue().startsWith("/")) tooltip.show();
        });
        add(tooltipButton, textField, enterButton);
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
                        var positions = perftFunction.playMoves(depth, GameState.currentTurn, false, true, true);
                        System.out.println("Positions: " + positions);
                    });
                    return;
                }
                Notification.show("Invalid amount of arguments");
            }
        });
    }

    private void configureTextFieldTooltipBehavior() {
        final AtomicReference<String> textInTextField = new AtomicReference<>("");
        textField.addKeyDownListener(event -> {
            String value = textInTextField.get();

            if (event.getKey().equals(Key.BACKSPACE)) {
                textInTextField.set(value.substring(0, value.length() - 1));
            } else {
                if (event.getKey().getKeys().get(0).matches("[a-zA-Z0-9 ]") || event.getKey().getKeys().get(0).matches("[!@#$%^&*()_+/-]") || event.getKey().getKeys().get(0).matches("[0-9]")) {
                    textInTextField.set(value + event.getKey().getKeys().get(0));
                }
            }

            value = textInTextField.get();

            if (value.startsWith("/")) {
                tooltip.show();
                tooltip.setContent(value);
            } else {
                tooltip.hide();
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
