package com.example.demo.dto.response;

/**
 * Lightweight payload for the "Redeem e-Points" checkbox.
 *
 * Deliberately NEVER 404s: a non-cardholder gets cardholder=false and
 * pointsBalance=0, so the UI can render the checkbox state without having to
 * treat "no card" as an error.
 */
public class PointsBalanceResponse {

    private boolean cardholder;
    private int pointsBalance;
    private String cardStatus;

    public PointsBalanceResponse() {
    }

    public PointsBalanceResponse(boolean cardholder, int pointsBalance, String cardStatus) {
        this.cardholder = cardholder;
        this.pointsBalance = pointsBalance;
        this.cardStatus = cardStatus;
    }

    public boolean isCardholder() { return cardholder; }
    public void setCardholder(boolean v) { this.cardholder = v; }
    public int getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(int v) { this.pointsBalance = v; }
    public String getCardStatus() { return cardStatus; }
    public void setCardStatus(String v) { this.cardStatus = v; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean cardholder;
        private int pointsBalance;
        private String cardStatus;
        public Builder cardholder(boolean v) { this.cardholder = v; return this; }
        public Builder pointsBalance(int v) { this.pointsBalance = v; return this; }
        public Builder cardStatus(String v) { this.cardStatus = v; return this; }
        public PointsBalanceResponse build() {
            return new PointsBalanceResponse(cardholder, pointsBalance, cardStatus);
        }
    }
}
