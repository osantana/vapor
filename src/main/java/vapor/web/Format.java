package vapor.web;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Format {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(amount);
    }

    public static String price(BigDecimal amount, Messages messages) {
        if (amount.signum() == 0) {
            return messages.get("game.free");
        }
        return money(amount);
    }

    public static String date(LocalDate date) {
        return date.format(DATE);
    }

    public static String dateTime(OffsetDateTime dateTime) {
        return dateTime.format(DATE_TIME);
    }
}
