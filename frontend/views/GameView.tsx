import {Notification} from "@hilla/react-components/Notification.js";
import {ChessEndpoint} from "Frontend/generated/endpoints.js";
import React, {useEffect, useMemo, useState} from "react";
import {Chessboard} from "react-chessboard";
import styles from "../themes/sightlessknight/views/play-view.module.css"
import Move from "Frontend/generated/lukas/sobotik/sightlessknight/gamelogic/Move";
import PieceType from "Frontend/generated/lukas/sobotik/sightlessknight/gamelogic/entity/PieceType";
import GameInfoSideBar from "Frontend/themes/components/GameInfoSideBar";
import {useParams} from "react-router-dom";

function GameView({train} : {train?: boolean}) {
    const [fetchedData, setFetchedData] = useState<boolean>(false);
    const [currentFen, setCurrentFen] = useState("");
    const [validMoves, setValidMoves] = useState<Move[]>([]);
    const [gameEnded, setGameEnded] = useState("");
    const [moveHistory, setMoveHistory] = useState<string[]>([]);
    const [targetSquare, setTargetSquare] = useState<string>("");
    const [showBoard, setShowBoard] = useState<boolean>(true);
    const [showPieces, setShowPieces] = useState<boolean>(true);

    const { id } = useParams<{ id: string }>();

    useEffect(() => {
        const fetchData = async () => {
            await ChessEndpoint.initializeBoard(id ? id : null);
            await ChessEndpoint.printBoard();
            await setCurrentPosition();
            await getValidMoves();
            await showTargetSquare();
            localStorage.getItem('showBoard') ? setShowBoard(localStorage.getItem('showBoard') == "true") : setShowBoard(true);
            localStorage.getItem('showPieces') ? setShowPieces(localStorage.getItem('showPieces') == "true") : setShowPieces(true);
        };
        fetchData().then(() => {
            console.info("Game initialized successfully.");
            setFetchedData(true);
            console.warn("id: ", id);
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
        await showTargetSquare();
    }

    async function playMoveFromText(move : string) {
        await ChessEndpoint.playMoveFromText(move);
        setCurrentFen(await ChessEndpoint.getCurrentPosition());
        await getValidMoves();
        await updateMoveHistory();
        await showTargetSquare();
    }

    async function showTargetSquare() {
        const startSquare = await ChessEndpoint.getStartSquare();
        const targetSquare = await ChessEndpoint.getTargetSquare();
        setTargetSquare(startSquare.algebraicNotationLocation + " → " + targetSquare.algebraicNotationLocation);
    }

    async function undoMove() {
        await ChessEndpoint.undoMove();
        setCurrentFen(await getCurrentPosition());
        await getValidMoves();
        await updateMoveHistory();
    }

    async function resetGame() {
        await ChessEndpoint.resetGame();
        setCurrentFen(await getCurrentPosition());
        setGameEnded("");
        await getValidMoves();
        await updateMoveHistory();
    }

    async function updateMoveHistory() {
        const moves = await ChessEndpoint.getMoveHistory();
        setMoveHistory(moves);
    }

    function findValidMove(move : any) {
        for (let validMove of validMoves) {
            if (validMove.from.algebraicNotationLocation == move.from && validMove.to.algebraicNotationLocation == move.to) {
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
                        console.info("Game ended by ", gameEnded);
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

    const pieces = [
        "wP",
        "wN",
        "wB",
        "wR",
        "wQ",
        "wK",
        "bP",
        "bN",
        "bB",
        "bR",
        "bQ",
        "bK",
    ];

    const customPieces = useMemo(() => {
        const pieceComponents = {};
        pieces.forEach((piece) => {
            pieceComponents[piece] = ({ squareWidth }) => (
                <div
                    style={{
                        width: squareWidth,
                        height: squareWidth,
                        backgroundImage: `url(/${piece}.png)`,
                        backgroundSize: "100%",
                    }}
                />
            );
        });
        return pieceComponents;
    }, []);

    return (
        <>
            <div className={styles.game_container}>
                <div className={styles.board_parent}>
                    <div id="board" className={showBoard ? styles.board : styles.board_hidden}>
                        <Chessboard id="MainBoard" arePremovesAllowed={true} boardOrientation={"black"}
                                    position={currentFen} onPieceDrop={onDrop}
                                    customPieces={showPieces ? null : customPieces}/>
                    </div>
                </div>
                <div className={showBoard ? styles.game_info : styles.game_info_extended}>
                    <GameInfoSideBar
                        moveHistory={moveHistory}
                        onResetGame={resetGame}
                        onUndo={undoMove}
                        onPlayFromText={playMoveFromText}
                        onShowBoardChange={(show) => setShowBoard(show)}
                        onShowPiecesChange={(show) => setShowPieces(show)}
                        targetSquare={targetSquare} />
                </div>
            </div>
        </>
    );
}

export default GameView;