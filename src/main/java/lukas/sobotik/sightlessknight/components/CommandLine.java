package lukas.sobotik.sightlessknight.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;
import lukas.sobotik.sightlessknight.views.play.PlayView;

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
            if (command.startsWith("/bitboard show")) {
                if (split.length == 4) {
                    var team = Team.valueOf(split[2].toUpperCase());
                    var pieceType = PieceType.valueOf(split[3].toUpperCase());

                    playView.showBitboardOverlay(pieceType, team);
                    return;
                }
                if (split.length == 3) {
                    var team = Team.valueOf(split[2].toUpperCase());

                    playView.showBitboardOverlay(null, team);
                    return;
                }
                Notification.show("Invalid amount of arguments");
            }
            if (command.startsWith("/bitboard hide")) {
                if (split.length == 4) {
                    var team = Team.valueOf(split[2].toUpperCase());
                    var pieceType = PieceType.valueOf(split[3].toUpperCase());

                    playView.hideBitboardOverlay(pieceType, team);
                    return;
                }
                if (split.length == 3) {
                    var team = Team.valueOf(split[2].toUpperCase());

                    playView.hideBitboardOverlay(null, team);
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
