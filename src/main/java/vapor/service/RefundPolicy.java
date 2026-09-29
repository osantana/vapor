package vapor.service;

import java.time.OffsetDateTime;

public class RefundPolicy {
    private static final int MAXIMUM_MINUTES_PLAYED = 120;

    private int refundDays;

    public RefundPolicy(int refundDays) {
        this.refundDays = refundDays;
    }

    public boolean isEligible(OffsetDateTime purchasedAt, int minutesPlayed, OffsetDateTime now) {
        return rejectionReason(purchasedAt, minutesPlayed, now).isEmpty();
    }

    // Returns the message key that explains the rejection, or an empty text when the refund is allowed.
    public String rejectionReason(OffsetDateTime purchasedAt, int minutesPlayed, OffsetDateTime now) {
        OffsetDateTime deadline = purchasedAt.plusDays(refundDays);
        if (now.isAfter(deadline)) {
            return "error.refundExpired";
        }
        if (minutesPlayed >= MAXIMUM_MINUTES_PLAYED) {
            return "error.refundPlayedTooMuch";
        }
        return "";
    }
}
