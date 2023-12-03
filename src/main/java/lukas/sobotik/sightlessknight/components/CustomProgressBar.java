package lukas.sobotik.sightlessknight.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * CustomProgressBar represents a custom progress bar component that extends HorizontalLayout.
 * It displays a progress bar and a percentage value.
 */
public class CustomProgressBar extends HorizontalLayout {

    private Div progressBarContainer;
    private Div progressBar;
    private Paragraph boardSizeNumber;
    private double value;
    private boolean isDisabled;
    private String labelQuery;

    private final String DISABLED_OPACITY = "0.25";

    /**
     * Constructs a new CustomProgressBar.
     * The CustomProgressBar is a custom implementation of a progress bar.
     * It creates a progress bar container with a progress bar inside, and a board size number.
     * The progress bar container is styled with the class name "progress-container".
     * The progress bar is styled with the class name "progress-bar".
     * The board size number is styled with the class name "board_size_number".
     */
    public CustomProgressBar() {
        setClassName("progress-bar-parent");

        progressBarContainer = new Div();
        progressBarContainer.setClassName("progress-container");
        progressBarContainer.getStyle().set("position", "relative");
        progressBarContainer.getStyle().set("opacity", "1");
        progressBar = new Div();
        progressBar.setClassName("progress-bar");

        progressBarContainer.add(progressBar);
        add(progressBarContainer);

        boardSizeNumber = new Paragraph();
        boardSizeNumber.addClassName("board_size_number");

        add(boardSizeNumber);
    }

    /**
     * Sets the CSS query for the Custom Label above CustomProgressBar
     *
     * @param labelQuery the label query to set
     */
    public void setLabelQuery(String labelQuery) {
        this.labelQuery = labelQuery;
    }

    /**
     * Sets the disabled state of the component.
     *
     * @param isDisabled true if the component should be disabled, false otherwise
     */
    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
        if (isDisabled) {
            progressBarContainer.getStyle().set("opacity", DISABLED_OPACITY);
            boardSizeNumber.getStyle().set("opacity", DISABLED_OPACITY);
            if (labelQuery != null) {
                this.getElement().executeJs("document.querySelector('" + labelQuery + "').style.opacity = '" + DISABLED_OPACITY + "';");
            }
            progressBarContainer.getStyle().set("cursor", "initial");
        } else {
            progressBarContainer.getStyle().set("opacity", "1");
            boardSizeNumber.getStyle().set("opacity", "1");
            if (labelQuery != null) {
                this.getElement().executeJs("document.querySelector('" + labelQuery + "').style.opacity = '1';");
            }
            progressBarContainer.getStyle().set("cursor", "pointer");
        }
    }

    /**
     * Sets the value of the progress bar.
     * The value represents the progress amount of the progress bar.
     *
     * @param value the value to set for the progress bar
     */
    public void setValue(double value) {
        if (isDisabled) {
            return;
        }

        this.value = value;
        updateProgressBar();
    }

    /**
     * Updates the progress bar based on the current value.
     * This method sets the width of the progress bar and updates the displayed value percentage.
     */
    private void updateProgressBar() {
        progressBar.getStyle().set("width", value * 100 + "%");
        boardSizeNumber.setText((int)(value * 100) + "%");
    }

    /**
     * {@inheritDoc}
     *
     * This method is invoked when the component is attached to the UI.
     * It calls the {@link #updateProgressBar()} method to update the progress bar.
     *
     * @param event the attach event
     */
    @Override
    protected void onAttach(AttachEvent event) {
        updateProgressBar();
    }

}