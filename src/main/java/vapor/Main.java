package vapor;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import io.javalin.Javalin;

import vapor.repository.CartRepository;
import vapor.repository.CatalogRepository;
import vapor.repository.LibraryRepository;
import vapor.repository.OrderRepository;
import vapor.repository.UserRepository;
import vapor.service.AuthService;
import vapor.service.CatalogService;
import vapor.service.CheckoutService;
import vapor.service.DownloadService;
import vapor.service.PaymentGateway;
import vapor.web.AdminController;
import vapor.web.AuthController;
import vapor.web.CartController;
import vapor.web.CatalogController;
import vapor.web.LibraryController;
import vapor.web.OrderController;
import vapor.web.PublisherController;
import vapor.web.Security;
import vapor.web.Views;

public class Main {
    public static void main(String[] arguments) throws IOException, SQLException, GeneralSecurityException {
        Config config = Config.load();

        Database database = new Database(
                config.get("DATABASE_URL", "jdbc:postgresql://localhost:5432/vapor"),
                config.get("DATABASE_USER", "vapor"),
                config.require("DATABASE_PASSWORD"));
        database.createSchema();

        UserRepository userRepository = new UserRepository(database);
        CatalogRepository catalogRepository = new CatalogRepository(database);
        CartRepository cartRepository = new CartRepository(database);
        OrderRepository orderRepository = new OrderRepository(database);
        LibraryRepository libraryRepository = new LibraryRepository(database);

        PaymentGateway paymentGateway = new PaymentGateway();
        AuthService authService = new AuthService(userRepository);
        CatalogService catalogService = new CatalogService(catalogRepository);
        CheckoutService checkoutService = new CheckoutService(cartRepository, orderRepository,
                libraryRepository, paymentGateway);
        DownloadService downloadService = new DownloadService(config.require("SECRET_KEY"),
                libraryRepository, catalogRepository);

        authService.ensureAdministrator(config.require("ADMIN_EMAIL"), config.require("ADMIN_PASSWORD"));
        if (config.get("DEMO_DATA", "false").equals("true")) {
            new DemoData(userRepository, catalogRepository, cartRepository, authService, checkoutService).load();
        }

        Views views = new Views(cartRepository);
        AuthController auth = new AuthController(views, authService);
        CatalogController catalog = new CatalogController(views, catalogRepository, cartRepository,
                libraryRepository);
        CartController cart = new CartController(views, cartRepository, catalogRepository, libraryRepository);
        OrderController orders = new OrderController(views, orderRepository, checkoutService);
        LibraryController library = new LibraryController(views, libraryRepository, catalogRepository,
                downloadService);
        PublisherController publisher = new PublisherController(views, catalogRepository, orderRepository,
                catalogService);
        AdminController admin = new AdminController(views, catalogRepository);

        Javalin application = Javalin.create(javalin -> {
            javalin.jetty.port = config.getInt("PORT", 8080);
            javalin.staticFiles.add("/public");

            javalin.routes.before(new Security());

            javalin.routes.get("/", ctx -> catalog.list(ctx));
            javalin.routes.get("/games/{id}", ctx -> catalog.detail(ctx));

            javalin.routes.get("/register", ctx -> auth.showRegister(ctx));
            javalin.routes.post("/register", ctx -> auth.register(ctx));
            javalin.routes.get("/login", ctx -> auth.showLogin(ctx));
            javalin.routes.post("/login", ctx -> auth.login(ctx));
            javalin.routes.post("/logout", ctx -> auth.logout(ctx));
            javalin.routes.post("/language", ctx -> auth.changeLanguage(ctx));

            javalin.routes.get("/cart", ctx -> cart.show(ctx));
            javalin.routes.post("/cart/add", ctx -> cart.add(ctx));
            javalin.routes.post("/cart/remove", ctx -> cart.remove(ctx));
            javalin.routes.post("/checkout", ctx -> orders.checkout(ctx));
            javalin.routes.get("/orders", ctx -> orders.list(ctx));

            javalin.routes.get("/library", ctx -> library.list(ctx));
            javalin.routes.post("/library/download", ctx -> library.createDownloadLink(ctx));
            javalin.routes.get("/download/{gameId}/{assetId}", ctx -> library.download(ctx));

            javalin.routes.get("/publisher", ctx -> publisher.dashboard(ctx));
            javalin.routes.get("/publisher/games/new", ctx -> publisher.showGameForm(ctx));
            javalin.routes.post("/publisher/games", ctx -> publisher.createGame(ctx));
            javalin.routes.post("/publisher/price", ctx -> publisher.changePrice(ctx));

            javalin.routes.get("/admin", ctx -> admin.queue(ctx));
            javalin.routes.post("/admin/review", ctx -> admin.review(ctx));

            javalin.routes.exception(NotFoundException.class, (exception, ctx) -> {
                ctx.status(404);
                try {
                    views.renderMessage(ctx, "error.notFound");
                } catch (IOException | SQLException renderFailure) {
                    ctx.result("Not found");
                }
            });
        });
        application.start();
    }
}
