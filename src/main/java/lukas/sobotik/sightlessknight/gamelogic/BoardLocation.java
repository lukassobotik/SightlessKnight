package lukas.sobotik.sightlessknight.gamelogic;

public class BoardLocation {
    private int x;
    private int y;
    public BoardLocation(int x, int y) {
        super();
        this.x = x;
        this.y = y;
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BoardLocation)) {
            return false;
        }
        BoardLocation other = (BoardLocation) obj;

        return other.getX() == x && other.getY() == y;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public BoardLocation transpose(int x, int y) {
        return new BoardLocation(this.x + x, this.y + y);
    }
    public String getStringLocation() {
        return x + "," + y;
    }
    public String getAlgebraicNotationLocation() {
        char file = (char) ('a' + x);
        int rank = y + 1;
        return file + String.valueOf(rank);
    }
    public boolean isOnSameDiagonalAs(BoardLocation other) {
        int deltaX = Math.abs(this.getX() - other.getX());
        int deltaY = Math.abs(this.getY() - other.getY());
        return deltaX == deltaY;
    }
    public boolean isOnSameFileAs(BoardLocation other) {
        return this.getX() == other.getX();
    }

    public boolean isOnSameRankAs(BoardLocation other) {
        return this.getY() == other.getY();
    }
}
