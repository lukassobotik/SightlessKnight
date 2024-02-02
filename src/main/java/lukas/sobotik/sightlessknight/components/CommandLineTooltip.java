package lukas.sobotik.sightlessknight.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;

public class CommandLineTooltip extends VerticalLayout {

    public final String[] commands = {
            "/undo",
            "/perft <depth>"
    };

    Div content;
    public CommandLineTooltip() {
        setClassName("tooltip_layout");
        setPadding(false);
        setSpacing(false);

        content = new Div();
        content.setText("""
                        Enter a command
                        Commands:
                        /undo
                        """);
        content.getStyle().set("white-space", "pre-line");
        add(content);

        hide();
    }

    public void hide() {
        this.setVisible(false);
    }

    public void show() {
        this.setVisible(true);
    }

    public void toggle() {
        this.setVisible(!this.isVisible());
    }

    public void setContent(String typedCommand) {
        String[] typedWords = typedCommand.split(" ");
        List<String> possibleOptions = new ArrayList<>();

        for (String command : commands) {
            String[] commandWords = command.split(" ");

            for (int i = 0; i < typedWords.length; i++) {
                if (commandWords.length <= i) {
                    possibleOptions.remove(command);
                    break;
                }
                if (commandWords[i].startsWith(typedWords[i])) {
                    possibleOptions.add(command);
                }
            }
        }

        String allOptions = String.join("\n", possibleOptions);
        if (allOptions.isEmpty()) {
            allOptions = "No commands found";
        }
        content.setText(allOptions);
    }
}
