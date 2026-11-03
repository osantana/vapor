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
import vapor.service.AuthService;
import vapor.service.CatalogService;
import vapor.service.DownloadService;
import vapor.service.PasswordHasher;
import vapor.web.Messages;
import vapor.web.Template;

public class BusinessRulesTest {
    private Game newGame(long id, String price) {
        return new Game(id, 1, "Game", "Description", new BigDecimal(price), LocalDate.now(),
                GameStatus.APPROVED, OffsetDateTime.now());
    }

    @Test
    void emailFormatIsValidated() {
        assertTrue(AuthService.isValidEmail("player@example.com"));
        assertFalse(AuthService.isValidEmail("player@example"));
        assertFalse(AuthService.isValidEmail("player example@example.com"));
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
    void downloadSignatureExpiresAndDetectsTampering() throws GeneralSecurityException {
        DownloadService service = new DownloadService("test-secret", null, null);
        String signature = service.sign(7, 1000);
        assertTrue(service.isValidSignature(7, 1000, signature, 999));
        assertFalse(service.isValidSignature(7, 1000, signature, 1001));
        assertFalse(service.isValidSignature(7, 2000, signature, 999));
        assertFalse(service.isValidSignature(8, 1000, signature, 999));
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
    void templateEscapesValuesAndTranslatesMessages() throws Exception {
        Template template = Template.load("flash", Messages.forLanguage("pt_BR"));
        template.set("kind", "error");
        template.set("text", "<script>alert(1)</script>");
        String html = template.render();
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertEquals("Catálogo", Messages.forLanguage("pt_BR").get("catalog.title"));
        assertEquals("Catalog", Messages.forLanguage("en").get("catalog.title"));
    }
}
