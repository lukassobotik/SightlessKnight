package lukas.sobotik.sightlessknight;




enum Team {
	WHITE,
	BLACK
}

enum PieceType {
	PAWN,
	BISHOP,
	KNIGHT,
	ROOK,
	KING,
	QUEEN
}


public class PieceInfo {
	Team team;
	PieceType type;
	int doublePawnMoveOnMoveNumber = -1;
	boolean hasMoved = false;
	public PieceInfo(Team team, PieceType type) {
		super();
		this.team = team;
		this.type = type;
	}
	public PieceInfo(Team team, PieceType type, int doublePawnMoveOnMoveNumber) {
		super();
		this.team = team;
		this.type = type;
		this.doublePawnMoveOnMoveNumber = doublePawnMoveOnMoveNumber;
	}
	
	public String getSpriteName() {
		return ((team == Team.WHITE) ? "w_" : "b_") + type.toString().toLowerCase();
	}
}