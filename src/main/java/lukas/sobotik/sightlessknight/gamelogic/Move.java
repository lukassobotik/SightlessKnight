package lukas.sobotik.sightlessknight.gamelogic;

import lukas.sobotik.sightlessknight.gamelogic.entity.MoveFlag;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;

public class Move {
    private BoardLocation from, to;
    private Piece movedPiece, capturedPiece;
    private PieceType promotionPiece;
    private MoveFlag moveFlag = MoveFlag.none;
    public Move(BoardLocation from, BoardLocation to) {
        this.from = from;
        this.to = to;
    }
    public Move(BoardLocation from, BoardLocation to, Piece movedPiece) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
    }
    public Move(BoardLocation from, BoardLocation to, Piece movedPiece, Piece capturedPiece) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
    }
    public BoardLocation getFrom() {
        return from;
    }
    public void setFrom(BoardLocation from) {
        this.from = from;
    }
    public BoardLocation getTo() {
        return to;
    }
    public void setTo(BoardLocation to) {
        this.to = to;
    }
    public Piece getMovedPiece() {
        return movedPiece;
    }
    public void setMovedPiece(Piece movedPiece) {
        this.movedPiece = movedPiece;
    }
    public Piece getCapturedPiece() {
        return capturedPiece;
    }
    public void setCapturedPiece(Piece capturedPiece) {
        this.capturedPiece = capturedPiece;
    }
    public PieceType getPromotionPiece() {
        return promotionPiece;
    }
    public void setPromotionPiece(PieceType promotionPiece) {
        this.promotionPiece = promotionPiece;
    }
    public MoveFlag getMoveFlag() {
        return moveFlag;
    }
    public void setMoveFlag(MoveFlag moveFlag) {
        this.moveFlag = moveFlag;
    }
    public String getMoveNotation(Piece[] pieces, GameState gameState, Board board) {
        return new AlgebraicNotationUtils(this, new FenUtils(pieces), gameState, board).getParsedMove(this);
    }
}
