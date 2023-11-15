package lukas.sobotik.sightlessknight.views.train;

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
    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
        System.out.println(s);
        if (s == null) {

        } else {
            var playView = new PlayView();
            playView.isTrainingMode = true;
            playView.generatePieceTrainingGame(s);
            add(playView);
        }
    }

    public TrainingView() {

    }
}
