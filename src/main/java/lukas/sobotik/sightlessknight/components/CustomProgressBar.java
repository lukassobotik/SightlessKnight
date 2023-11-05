package lukas.sobotik.sightlessknight.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class CustomProgressBar extends HorizontalLayout {

    private Div progressBarContainer;
    private Div progressBar;
    private Paragraph boardSizeNumber;
    private double value;

    public CustomProgressBar() {
        setClassName("progress-bar-parent");

        progressBarContainer = new Div();
        progressBarContainer.setClassName("progress-container");
        progressBarContainer.getStyle().set("position", "relative");
        progressBar = new Div();
        progressBar.setClassName("progress-bar");

        progressBarContainer.add(progressBar);
        add(progressBarContainer);

        boardSizeNumber = new Paragraph();
        boardSizeNumber.addClassName("board_size_number");

        add(boardSizeNumber);
    }

    public void setValue(double value) {
        this.value = value;

        updateProgressBar();
    }

    private void updateProgressBar() {
        progressBar.getStyle().set("width", value * 100 + "%");
        boardSizeNumber.setText((int)(value * 100) + "%");
    }

    @Override
    protected void onAttach(AttachEvent event) {

        updateProgressBar();

    }

}