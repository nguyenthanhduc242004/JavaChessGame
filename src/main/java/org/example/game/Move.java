package org.example.game;

import java.io.Serial;
import java.io.Serializable;

public class Move implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L; // Good practice for Serializable classes
    public int fromRow, fromCol, toRow, toCol;
    public String pieceType; // e.g., "PAWN", "ROOK"
    // You might also include:
    // - promotionPiece (if a pawn is promoted)
    // - isCastlingMove
    // - isEnPassant
    // - special messages like "RESET_GAME", "RESIGN", "DRAW_OFFER"

    public Move(int fromRow, int fromCol, int toRow, int toCol, String pieceType) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.pieceType = pieceType;
    }

    @Override
    public String toString() {
        return pieceType + " from (" + fromRow + "," + fromCol + ") to (" + toRow + "," + toCol + ")";
    }
}