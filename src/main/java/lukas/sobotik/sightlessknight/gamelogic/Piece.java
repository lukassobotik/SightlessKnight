package lukas.sobotik.sightlessknight.gamelogic;


import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

public class Piece {
	public Team team;
	public PieceType type;
	public int doublePawnMoveOnMoveNumber = -10;
	boolean hasMoved = false;
	public boolean enPassant = false;
	String castling = null;
	public PieceType promotion = null;
	public Piece(Team team, PieceType type) {
		super();
		this.team = team;
		this.type = type;
	}
	public Piece(Team team, PieceType type, int doublePawnMoveOnMoveNumber) {
		super();
		this.team = team;
		this.type = type;
		this.doublePawnMoveOnMoveNumber = doublePawnMoveOnMoveNumber;
	}
	
	public String getSpriteName() {
		return ((team == Team.WHITE) ? "w_" : "b_") + type.toString().toLowerCase();
	}
}