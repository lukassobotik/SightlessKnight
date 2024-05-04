import {Notification} from "@hilla/react-components/Notification.js";
import {ChessEndpoint} from "Frontend/generated/endpoints.js";
import {useEffect, useState} from "react";
import {Chessboard} from "react-chessboard";
import styles from "../themes/sightlessknight/views/play-view.module.css"
import Move from "Frontend/generated/lukas/sobotik/sightlessknight/gamelogic/Move";
import PieceType from "Frontend/generated/lukas/sobotik/sightlessknight/gamelogic/entity/PieceType";
import CommandLine from "Frontend/themes/components/CommandLine";

function MainView() {
    const [currentFen, setCurrentFen] = useState("");
    const [validMoves, setValidMoves] = useState<Move[]>([]);
    const [gameEnded, setGameEnded] = useState("");
    const [moveHistory, setMoveHistory] = useState<string[]>([]);

    useEffect(() => {
        const fetchData = async () => {
            await ChessEndpoint.initialize();
            await ChessEndpoint.printBoard();
            await setCurrentPosition();
            await getValidMoves();
        };
        fetchData().then(() => {
            console.log("Game initialized successfully.");
        });
    }, []);

    async function getValidMoves() {
        const moves = await ChessEndpoint.getValidMovesForPosition();
        setValidMoves(moves);
    }

    async function setCurrentPosition() {
        const pos = await ChessEndpoint.getCurrentPosition();
        setCurrentFen(pos);
    }

    async function getCurrentPosition() {
        return await ChessEndpoint.getCurrentPosition();
    }

    async function playMove(move : Move) {
        await ChessEndpoint.playMove(move);
        await setCurrentPosition();
        await getValidMoves();
        await updateMoveHistory();
    }

    async function updateMoveHistory() {
        const moves = await ChessEndpoint.getMoveHistory();
        setMoveHistory(moves);
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
                ChessEndpoint.checkIfGameEnded().then((gameEnded) => {
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

    async function onCommandSubmit(command : string) {
        if (command.startsWith("/")) {
            console.log("Command is a command");
            if (command == "/undo") {
                await ChessEndpoint.undoMove();
                setCurrentFen(await getCurrentPosition());
                await getValidMoves();
                await updateMoveHistory();
            } else if (command.startsWith("/perft")) {
                const depth = parseInt(command.split(" ")[1]);
                if (depth > 4) {
                    console.warn("Perft depth is too high.");
                    Notification.show("Perft depth is too high.");
                    return;
                }
                const result = await ChessEndpoint.playPerftTest(depth);
                console.log("Perft result: ", result);
                Notification.show("Perft result: " + result);
            }
        } else {
            await ChessEndpoint.playMoveFromText(command);
            setCurrentFen(await ChessEndpoint.getCurrentPosition());
            await getValidMoves();
            await updateMoveHistory();
        }
    }

    return (
        <>
            <div className={styles.game_container}>
                <div className={styles.board_parent}>
                    <div className={styles.board}>
                        <Chessboard id="MainBoard" arePremovesAllowed={true} boardOrientation={"black"}
                                    position={currentFen} onPieceDrop={onDrop}/>
                    </div>
                </div>
                <div className={styles.game_info}>
                    <div className={styles.target_square}>
                        e4
                    </div>
                    <div className={styles.move_history}>
                        <div className={styles.moves}>
                            <div className={styles.white_moves}>
                                {moveHistory.map((move, index) => {
                                    const moveNumber = Math.floor(index / 2) + 1;
                                    if (index % 2 == 0) {
                                        return (<div id={moveNumber.toString()} key={index} className={styles.move}>{moveNumber}: {move}</div>)
                                    } else return null;
                                })}
                            </div>
                            <div className={styles.black_moves}>
                                {moveHistory.map((move, index) => {
                                    if (index % 2 != 0) {
                                        return (<div key={index} className={styles.move}>{move}</div>)
                                    } else return null;
                                })}
                            </div>
                        </div>
                    </div>
                    <div className={styles.command_line}>
                        <CommandLine onCommandSubmit={onCommandSubmit} />
                    </div>
                </div>
            </div>
        </>
    );
}

export default MainView;