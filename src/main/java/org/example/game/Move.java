package org.example.game;

import java.io.Serial;
import java.io.Serializable;

public class Move implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L; // Good practice for Serializable classes
    public Position from, to;
    public String pieceType; // e.g., "PAWN", "ROOK"
    // You might also include:
    // - promotionPiece (if a pawn is promoted)
    // - isCastlingMove
    // - isEnPassant
    // - special messages like "RESET_GAME", "RESIGN", "DRAW_OFFER"


    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    public Move(Position from, Position to, String pieceType) {
        this.from = from;
        this.to = to;
        this.pieceType = pieceType;
    }

    @Override
    public String toString() {
        return pieceType + " from (" + from.getRow() + "," + from.getColumn() + ") to (" + to.getRow() + "," + to.getColumn() + ")";
    }
}