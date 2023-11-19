package lukas.sobotik.sightlessknight.gamelogic;

public class BoardLocation {
    private int x;
    private int y;

    /**
     * Constructs a new BoardLocation object with the specified x and y coordinates.
     *
     * @param x the x coordinate of the location
     * @param y the y coordinate of the location
     */
    public BoardLocation(int x, int y) {
        super();
        this.x = x;
        this.y = y;
    }

    /**
     * Compares this BoardLocation object to the specified object for equality.
     * Two BoardLocation objects are considered equal if they have the same values for their x and y coordinates.
     *
     * @param obj the object to be compared for equality
     * @return true if the specified object is equal to this BoardLocation object, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BoardLocation)) {
            return false;
        }
        BoardLocation other = (BoardLocation) obj;

        return other.getX() == x && other.getY() == y;
    }

    /**
     * Returns the x coordinate of this BoardLocation object.
     * @return the x coordinate of this BoardLocation object
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the x coordinate value of this BoardLocation object.
     * @param x the value to be set as the new x coordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Returns the y coordinate of this BoardLocation object.
     * @return the y coordinate of this BoardLocation object
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the y coordinate of this BoardLocation object.
     * @param y the new value for the y coordinate
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Returns a new BoardLocation object that is the result of transposing the current BoardLocation object by the specified x and y values.
     * The x and y values are added to the current coordinates of the BoardLocation object to determine the coordinates of the new BoardLocation object.
     *
     * @param x the value to be added to the x coordinate of the current BoardLocation object
     * @param y the value to be added to the y coordinate of the current BoardLocation object
     * @return a new BoardLocation object that is the result of transposing the current BoardLocation object by the specified x and y values
     */
    public BoardLocation transpose(int x, int y) {
        return new BoardLocation(this.x + x, this.y + y);
    }

    /**
     * Returns a string representation of the location coordinates in the format "x,y".
     *
     * @return a string representation of the location coordinates
     */
    public String getStringLocation() {
        return x + "," + y;
    }

    /**
     * Returns the algebraic notation representation of the location coordinates.
     * The algebraic notation consists of a letter representing the file (column)
     * and a number representing the rank (row).
     *
     * @return the algebraic notation representation of the location coordinates
     */
    public String getAlgebraicNotationLocation() {
        char file = (char) ('a' + x);
        int rank = y + 1;
        return file + String.valueOf(rank);
    }

    /**
     * Checks if the current BoardLocation is on the same diagonal as another BoardLocation.
     *
     * @param other the other BoardLocation to compare against
     * @return true if the BoardLocations are on the same diagonal, false otherwise
     */
    public boolean isOnSameDiagonalAs(BoardLocation other) {
        int deltaX = Math.abs(this.getX() - other.getX());
        int deltaY = Math.abs(this.getY() - other.getY());
        return deltaX == deltaY;
    }

    /**
     * Checks if the current BoardLocation is on the same file as another BoardLocation.
     *
     * @param other the other BoardLocation to compare against
     * @return true if the BoardLocations are on the same file, false otherwise
     */
    public boolean isOnSameFileAs(BoardLocation other) {
        return this.getX() == other.getX();
    }

    /**
     * Checks if the current BoardLocation is on the same rank as another BoardLocation.
     *
     * @param other the other BoardLocation to compare against
     * @return true if the BoardLocations are on the same rank, false otherwise
     */
    public boolean isOnSameRankAs(BoardLocation other) {
        return this.getY() == other.getY();
    }
}
