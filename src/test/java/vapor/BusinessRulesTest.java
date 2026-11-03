package vapor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import vapor.model.Cart;
import vapor.model.Game;
import vapor.model.GameStatus;
import vapor.model.SalesMetrics;
import vapor.service.CatalogService;
import vapor.service.PasswordHasher;
import vapor.web.Template;

public class BusinessRulesTest {
    private Game newGame(long id, String price) {
        return new Game(id, 1, "Game", "Description", new BigDecimal(price), LocalDate.now(),
                GameStatus.APPROVED, OffsetDateTime.now());
    }

    @Test
    void passwordHashMatchesOnlyTheOriginalPassword() throws GeneralSecurityException {
        String hash = PasswordHasher.hash("secret123");
        assertTrue(PasswordHasher.matches("secret123", hash));
        assertFalse(PasswordHasher.matches("secret124", hash));
        assertFalse(hash.contains("secret123"));
    }

    @Test
    void cartSumsPricesAndRejectsDuplicatedGame() {
        Cart cart = new Cart(1);
        Game game = newGame(10, "59.90");
        cart.addItem(game);
        cart.addItem(newGame(11, "10.10"));
        assertEquals(new BigDecimal("70.00"), cart.total());
        assertThrows(BusinessRuleException.class, () -> cart.addItem(game));
    }

    @Test
    void publisherReceivesSeventyPercent() {
        SalesMetrics metrics = new SalesMetrics(1, "Game", 2, new BigDecimal("100.00"));
        assertEquals(new BigDecimal("70.00"), metrics.getNetRevenue());
    }

    @Test
    void priceAcceptsCommaAndRejectsInvalidText() {
        assertEquals(new BigDecimal("59.90"), CatalogService.parsePrice("59,90"));
        assertEquals(new BigDecimal("0.00"), CatalogService.parsePrice("0"));
        assertThrows(BusinessRuleException.class, () -> CatalogService.parsePrice("-1"));
        assertThrows(BusinessRuleException.class, () -> CatalogService.parsePrice("abc"));
        assertThrows(BusinessRuleException.class, () -> CatalogService.parsePrice("1.999"));
        assertThrows(BusinessRuleException.class, () -> CatalogService.parsePrice("100000000"));
    }

    @Test
    void templateEscapesValues() throws Exception {
        Template template = Template.load("flash");
        template.set("kind", "error");
        template.set("text", "<script>alert(1)</script>");
        String html = template.render();
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }
}
