import {Button} from "@hilla/react-components/Button.js";
import {Notification} from "@hilla/react-components/Notification.js";
import {TextField} from "@hilla/react-components/TextField.js";
import {PlayGameEndpoint} from "Frontend/generated/endpoints.js";
import {useEffect, useState} from "react";
import {Chessboard} from "react-chessboard";
import styles from "../themes/sightlessknight/views/play-view.module.css"
import Move from "Frontend/generated/lukas/sobotik/sightlessknight/gamelogic/Move";
import PieceType from "Frontend/generated/lukas/sobotik/sightlessknight/gamelogic/entity/PieceType";

function MainView() {
    const [name, setName] = useState("");
    const [currentFen, setCurrentFen] = useState("");
    const [validMoves, setValidMoves] = useState<Move[]>([]);
    const [gameEnded, setGameEnded] = useState("");

    useEffect(() => {
        const fetchData = async () => {
            await PlayGameEndpoint.initialize();
            await PlayGameEndpoint.printBoard();
            await setCurrentPosition();
            await getValidMoves();
        };
        fetchData().then(() => {
            Notification.show("Game initialized");
        });
    }, []);

    async function getValidMoves() {
        const moves = await PlayGameEndpoint.getValidMovesForPosition();
        setValidMoves(moves);
    }

    async function setCurrentPosition() {
        const pos = await PlayGameEndpoint.getCurrentPosition();
        setCurrentFen(pos);
    }

    async function getCurrentPosition() {
        return await PlayGameEndpoint.getCurrentPosition();
    }

    async function playMove(move : Move) {
        await PlayGameEndpoint.playMove(move);
        await setCurrentPosition();
        await getValidMoves();
    }

    function doesMoveEqual(move1 : Move, move2 : any) {
        return move1.from.algebraicNotationLocation == move2.from && move1.to.algebraicNotationLocation == move2.to;
    }

    function algebraicToRankFile(notation: string): { x: number; y: number } {
        const fileIndex = notation.charCodeAt(0) - 'a'.charCodeAt(0);

        const rankIndex = 8 - parseInt(notation.charAt(1));

        return { x: fileIndex, y: rankIndex };
    }

    function findValidMove(move : any) {
        for (let validMove of validMoves) {
            if (doesMoveEqual(validMove, move)) {
                if (validMove.to.y == 0 && validMove.movedPiece.type == "PAWN" || validMove.to.y == 7 && validMove.movedPiece.type == "PAWN") {
                    console.log("Promotion move", getPromotionPiece(move.promotion));
                    validMove.promotionPiece = getPromotionPiece(move.promotion);
                }
                return validMove;
            }
        }
        console.info("Move is not valid in list of ", validMoves, " for move ", move);
        return null;
    }

    function getPromotionPiece(piece : string) : PieceType {
        switch (piece) {
            case "q":
                return PieceType.QUEEN;
            case "r":
                return PieceType.ROOK;
            case "b":
                return PieceType.BISHOP;
            case "n":
                return PieceType.KNIGHT;
            default:
                return PieceType.QUEEN;
        }
    }

    function onDrop(sourceSquare : any, targetSquare : any, piece : any) {
        if (gameEnded != "" && gameEnded != null) {
            return false;
        }

        const move = {
            from: sourceSquare,
            to: targetSquare,
            promotion: piece[1].toLowerCase() ?? "q"
        }

        const validMove = findValidMove(move);
        if (validMove != null) {
            playMove(validMove).then(() => {
                PlayGameEndpoint.checkIfGameEnded().then((gameEnded) => {
                    if (gameEnded != "" && gameEnded != null) {
                        console.log("Game ended by ", gameEnded);
                        Notification.show("Game ended by " + gameEnded);
                        setGameEnded(gameEnded);
                        return;
                    }
                });
            }).catch((e) => {
                console.error("Error playing move ", e);
            });
            return true;
        } else {
            return false;
        }
    }

    return (
        <>
            <TextField
                label="Your name"
                onValueChanged={(e) => {
                    setName(e.detail.value);
                }}
            />
            <Button
                onClick={async () => {
                    await PlayGameEndpoint.playMoveFromText(name);
                    setCurrentFen(await PlayGameEndpoint.getCurrentPosition());
                    await getValidMoves();
                }}
            >
                Say hello
            </Button>

            <div className={styles.board_parent}>
                <div className={styles.board}>
                    <Chessboard id="BasicBoard" arePremovesAllowed={true} boardOrientation={"black"} position={currentFen} onPieceDrop={onDrop}/>
                </div>
            </div>
        </>
    );
}

export default MainView;