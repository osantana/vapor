package vapor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import vapor.model.Cart;
import vapor.model.Game;
import vapor.model.GameStatus;
import vapor.model.PasswordHasher;
import vapor.model.Role;
import vapor.model.SalesMetrics;
import vapor.model.User;
import vapor.service.AuthService;
import vapor.service.CatalogService;
import vapor.service.DownloadService;
import vapor.service.RefundPolicy;
import vapor.service.SalesDashboardService;
import vapor.web.Messages;
import vapor.web.Template;

public class BusinessRulesTest {
    private Game newGame(String price) {
        return new Game(UUID.randomUUID(), UUID.randomUUID(), "Game", "Description", new BigDecimal(price),
                LocalDate.now(), GameStatus.APPROVED, OffsetDateTime.now());
    }

    @Test
    void passwordNeedsEightCharactersWithLetterAndNumber() {
        assertTrue(AuthService.isStrongPassword("abcdefg1"));
        assertFalse(AuthService.isStrongPassword("abcdef1"));
        assertFalse(AuthService.isStrongPassword("abcdefgh"));
        assertFalse(AuthService.isStrongPassword("12345678"));
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
    void fiveFailedLoginsLockTheAccountForFifteenMinutes() {
        User user = new User(UUID.randomUUID(), "Player", "player@example.com", "hash", Role.USER,
                OffsetDateTime.now());
        OffsetDateTime now = OffsetDateTime.now();
        for (int attempt = 1; attempt <= 4; attempt++) {
            user.registerFailedLogin(now);
        }
        assertFalse(user.isLocked(now));
        user.registerFailedLogin(now);
        assertTrue(user.isLocked(now));
        assertTrue(user.isLocked(now.plusMinutes(14)));
        assertFalse(user.isLocked(now.plusMinutes(16)));
        user.registerSuccessfulLogin();
        assertFalse(user.isLocked(now));
        assertEquals(0, user.getFailedLogins());
    }

    @Test
    void cartSumsPricesAndRejectsDuplicatedGame() {
        Cart cart = new Cart(UUID.randomUUID());
        Game game = newGame("59.90");
        cart.addItem(game);
        cart.addItem(newGame("10.10"));
        assertEquals(new BigDecimal("70.00"), cart.total());
        assertThrows(BusinessRuleException.class, () -> cart.addItem(game));
    }

    @Test
    void refundRequiresFourteenDaysAndLessThanTwoHoursPlayed() {
        RefundPolicy policy = new RefundPolicy(14);
        OffsetDateTime now = OffsetDateTime.now();
        assertTrue(policy.isEligible(now.minusDays(13), 119, now));
        assertFalse(policy.isEligible(now.minusDays(15), 0, now));
        assertFalse(policy.isEligible(now.minusDays(1), 120, now));
    }

    @Test
    void publisherReceivesSeventyPercent() {
        SalesMetrics metrics = new SalesMetrics(UUID.randomUUID(), "Game", 2, new BigDecimal("100.00"));
        assertEquals(new BigDecimal("70.00"), metrics.getNetRevenue());
    }

    @Test
    void downloadSignatureExpiresAndDetectsTampering() throws GeneralSecurityException {
        DownloadService service = new DownloadService("test-secret", null, null);
        UUID assetId = UUID.randomUUID();
        String signature = service.sign(assetId, 1000);
        assertTrue(service.isValidSignature(assetId, 1000, signature, 999));
        assertFalse(service.isValidSignature(assetId, 1000, signature, 1001));
        assertFalse(service.isValidSignature(assetId, 2000, signature, 999));
        assertFalse(service.isValidSignature(UUID.randomUUID(), 1000, signature, 999));
    }

    @Test
    void priceAcceptsCommaAndRejectsInvalidText() {
        assertEquals(new BigDecimal("59.90"), CatalogService.parsePrice("59,90"));
        assertEquals(new BigDecimal("0"), CatalogService.parsePrice("0"));
        assertThrows(BusinessRuleException.class, () -> CatalogService.parsePrice("-1"));
        assertThrows(BusinessRuleException.class, () -> CatalogService.parsePrice("abc"));
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

    @Test
    void csvNeutralizesFormulas() {
        assertEquals("\"'=SUM(A1)\"", SalesDashboardService.escape("=SUM(A1)"));
        assertEquals("\"My \"\"Game\"\"\"", SalesDashboardService.escape("My \"Game\""));
    }
}
