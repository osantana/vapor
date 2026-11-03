package vapor;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import vapor.model.Cart;
import vapor.model.CartItem;
import vapor.model.Game;
import vapor.model.GameAsset;
import vapor.model.GameStatus;
import vapor.model.Genre;
import vapor.model.PaymentMethod;
import vapor.model.Platform;
import vapor.model.User;
import vapor.repository.CartRepository;
import vapor.repository.CatalogRepository;
import vapor.repository.UserRepository;
import vapor.service.AuthService;
import vapor.service.CheckoutService;

// Fills an empty database with demo users, games and one purchase.
// Demo accounts: publisher@example.com and player@example.com, both with the password demo12345.
public class DemoData {
    private static final String PASSWORD = "demo12345";
    private static final long MEGABYTE = 1024 * 1024;

    private final UserRepository userRepository;
    private final CatalogRepository catalogRepository;
    private final CartRepository cartRepository;
    private final AuthService authService;
    private final CheckoutService checkoutService;

    public DemoData(UserRepository userRepository, CatalogRepository catalogRepository,
                    CartRepository cartRepository, AuthService authService, CheckoutService checkoutService) {
        this.userRepository = userRepository;
        this.catalogRepository = catalogRepository;
        this.cartRepository = cartRepository;
        this.authService = authService;
        this.checkoutService = checkoutService;
    }

    public void load() throws SQLException, GeneralSecurityException {
        if (userRepository.emailExists("publisher@example.com")) {
            return;
        }
        User publisher = authService.register("Indie Studio", "publisher@example.com", PASSWORD);
        User player = authService.register("Player One", "player@example.com", PASSWORD);

        Game nebula = game(publisher, "Nebula Drift", "Explore a procedurally generated galaxy and trade "
                + "between colonies.", "59.90", "2025-03-14", GameStatus.APPROVED, 1,
                new String[]{"Adventure", "Simulation"}, Platform.values());
        Game pixel = game(publisher, "Pixel Kart Rush", "Fast arcade racing with up to eight players "
                + "on the same screen.", "29.90", "2024-11-02", GameStatus.APPROVED, 2,
                new String[]{"Racing", "Sports"}, new Platform[]{Platform.WINDOWS, Platform.LINUX});
        game(publisher, "Dungeon of Echoes", "A roguelike RPG where every sound reveals a secret.",
                "79.90", "2025-08-21", GameStatus.APPROVED, 3,
                new String[]{"RPG", "Action"}, new Platform[]{Platform.WINDOWS});
        game(publisher, "Tiny Farm Tales", "Grow crops, raise animals and rebuild the village.",
                "0", "2024-06-30", GameStatus.APPROVED, 4,
                new String[]{"Simulation"}, new Platform[]{Platform.WINDOWS, Platform.MACOS});
        game(publisher, "Mind Blocks", "Three hundred logic puzzles with a relaxing soundtrack.",
                "19.90", "2023-12-05", GameStatus.APPROVED, 5,
                new String[]{"Puzzle"}, Platform.values());
        game(publisher, "Iron Front Tactics", "Turn-based strategy across a fictional 1940s war.",
                "49.90", "2025-01-10", GameStatus.APPROVED, 6,
                new String[]{"Strategy"}, new Platform[]{Platform.WINDOWS, Platform.LINUX});
        game(publisher, "Skybound Legends", "Open world adventure above the clouds. Waiting for review.",
                "69.90", "2026-02-01", GameStatus.PENDING, 7,
                new String[]{"Adventure", "RPG"}, new Platform[]{Platform.WINDOWS});
        Game rejected = game(publisher, "Crash Test Alpha", "Early prototype sent by mistake.",
                "9.90", "2025-09-01", GameStatus.REJECTED, 8,
                new String[]{"Action"}, new Platform[]{Platform.WINDOWS});
        rejected.reject("The installation file is corrupted. Upload a new build and resubmit.");
        catalogRepository.updateReview(rejected);

        buy(player, nebula);
        buy(player, pixel);
        checkoutService.checkout(player.getId(), PaymentMethod.PIX, "");
    }

    private Game game(User publisher, String title, String description, String price, String releaseDate,
                      GameStatus status, int coverSeed, String[] genreNames, Platform[] platforms)
            throws SQLException {
        Game game = new Game(0, publisher.getId(), title, description, new BigDecimal(price),
                LocalDate.parse(releaseDate), status, OffsetDateTime.now().minusDays(coverSeed));
        game.setSystemRequirements("OS: 64-bit\nMemory: 8 GB RAM\nStorage: 2 GB available space");
        game.setCoverUrl("https://picsum.photos/seed/vapor" + coverSeed + "/300/400");
        for (Genre genre : catalogRepository.listGenres()) {
            if (List.of(genreNames).contains(genre.getName())) {
                game.addGenre(genre);
            }
        }
        for (Platform platform : platforms) {
            game.addAsset(new GameAsset(0, platform, 1500 * MEGABYTE,
                    "https://example.com/downloads/" + platform.getCode() + ".zip"));
        }
        return catalogRepository.insert(game);
    }

    private void buy(User player, Game game) throws SQLException {
        Cart cart = cartRepository.findByUser(player.getId());
        CartItem item = cart.addItem(game);
        cartRepository.addItem(player.getId(), item);
    }
}
