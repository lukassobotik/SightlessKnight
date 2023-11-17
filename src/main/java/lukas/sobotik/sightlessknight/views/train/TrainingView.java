package lukas.sobotik.sightlessknight.views.train;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lukas.sobotik.sightlessknight.views.MainLayout;
import lukas.sobotik.sightlessknight.views.play.PlayView;

@PageTitle("Train Piece Moves")
@Route(value = "train", layout = MainLayout.class)
public class TrainingView extends VerticalLayout implements HasUrlParameter<String> {

    /**
     * Sets the parameter for the view.
     * @param beforeEvent the event before navigation
     * @param s the optional parameter value to be set
     */
    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
        System.out.println(s);
        if (s == null) {
            initializeEmptyView();
        } else {
            var playView = new PlayView();
            playView.isTrainingMode = true;
            playView.generatePieceTrainingGame(s);
            add(playView);
        }
    }

    public TrainingView() {

    }

    /**
     * Initializes an empty view with buttons for selecting chess pieces.
     */
    public void initializeEmptyView() {
        add(new H1("Train Piece Moves"));

        String[] pieces = { "Pawn", "Rook", "Knight", "Bishop", "Queen", "King" };
        HorizontalLayout layout = new HorizontalLayout();

        for (String piece : pieces) {
            layout.add(new Button(piece, buttonClickEvent -> UI.getCurrent().navigate("/train/" + piece.toLowerCase())));
        }

        add(layout);
    }
}
