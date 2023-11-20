package lukas.sobotik.sightlessknight.gamelogic;

/**
 * Class used to store the history of moves of a piece.
 * It is used to determine if a piece has moved or not, thanks to that it is possible to determine whether the king
 * can castle or not.
 */
public class MoveHistoryStack {
    private Step currentStep;

    /**
     * Method used to create a new instance of the MoveHistoryStack class.
     * @param initialValue initial value of the MoveHistoryStack class.
     */
    public MoveHistoryStack(boolean initialValue) {
        currentStep = new Step(initialValue, null);
    }

    /**
     * Method used to set the value of the MoveHistoryStack class.
     * @param value the new value to be set for the MoveHistoryStack class.
     */
    public void setValue(boolean value) {
        currentStep = new Step(value, currentStep);
    }

    /**
     * Method used to retrieve the value of the MoveHistoryStack class.
     *
     * @return the current value of the MoveHistoryStack class.
     */
    public boolean getValue() {
        return currentStep.value;
    }

    /**
     * Method used to go back to the previous step in the HasMovedHistory class.
     * If a previous step exists, it sets the current step to the previous step.
     */
    public void goBack() {
        if (currentStep.previousStep != null) {
            currentStep = currentStep.previousStep;
        }
    }
}

/**
 * Represents a step in a process.
 */
class Step {
    boolean value;
    Step previousStep;

    /**
     * Constructor for creating a new Step object with a boolean value and a previous Step.
     *
     * @param value the boolean value of the Step
     * @param previousStep the previous Step object
     */
    public Step(boolean value, Step previousStep) {
        this.value = value;
        this.previousStep = previousStep;
    }
}