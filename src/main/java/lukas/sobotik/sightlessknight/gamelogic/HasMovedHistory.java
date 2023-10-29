package lukas.sobotik.sightlessknight.gamelogic;

/**
 * Class used to store the history of moves of a piece.
 * It is used to determine if a piece has moved or not, thanks to that it is possible to determine whether the king
 * can castle or not.
 */
public class HasMovedHistory {
    private Step currentStep;

    /**
     * Method used to create a new instance of the HasMovedHistory class.
     * @param initialValue initial value of the HasMovedHistory class.
     */
    public HasMovedHistory(boolean initialValue) {
        currentStep = new Step(initialValue, null);
    }

    public void setValue(boolean value) {
        currentStep = new Step(value, currentStep);
    }

    public boolean getValue() {
        return currentStep.value;
    }

    public void goBack() {
        if (currentStep.previousStep != null) {
            currentStep = currentStep.previousStep;
        }
    }
}

class Step {
    boolean value;
    Step previousStep;

    public Step(boolean value, Step previousStep) {
        this.value = value;
        this.previousStep = previousStep;
    }
}