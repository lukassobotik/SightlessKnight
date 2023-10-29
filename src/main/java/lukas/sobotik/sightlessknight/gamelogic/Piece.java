package lukas.sobotik.sightlessknight.gamelogic;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lukas.sobotik.sightlessknight.gamelogic.entity.PieceType;
import lukas.sobotik.sightlessknight.gamelogic.entity.Team;

@Builder (toBuilder = true)
public class Piece {
	public Team team;
	public PieceType type;
	public int doublePawnMoveOnMoveNumber = -10;
	boolean hasMoved = false;
	public boolean enPassant = false;
	@Getter
	@Setter
	public String castling = null;
	public PieceType promotion = null;

	public Piece(Team team, PieceType type) {
		this.team = team;
		this.type = type;
	}

	public Piece(Team team, PieceType type, int doublePawnMoveOnMoveNumber) {
		this.team = team;
		this.type = type;
		this.doublePawnMoveOnMoveNumber = doublePawnMoveOnMoveNumber;
	}

	public Piece (Team team, PieceType type, int doublePawnMoveOnMoveNumber, boolean hasMoved, boolean enPassant, String castling, PieceType promotion) {
		this.team = team;
		this.type = type;
		this.doublePawnMoveOnMoveNumber = doublePawnMoveOnMoveNumber;
		this.hasMoved = hasMoved;
		this.enPassant = enPassant;
		this.castling = castling;
		this.promotion = promotion;
	}
}