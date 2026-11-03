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
        DownloadService downloadService = new DownloadService(libraryRepository, catalogRepository);

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

            javalin.routes.get("/", catalog::list);
            javalin.routes.get("/games/{id}", catalog::detail);

            javalin.routes.get("/register", auth::showRegister);
            javalin.routes.post("/register", auth::register);
            javalin.routes.get("/login", auth::showLogin);
            javalin.routes.post("/login", auth::login);
            javalin.routes.post("/logout", auth::logout);

            javalin.routes.get("/cart", cart::show);
            javalin.routes.post("/cart/add", cart::add);
            javalin.routes.post("/cart/remove", cart::remove);
            javalin.routes.post("/checkout", orders::checkout);
            javalin.routes.get("/orders", orders::list);

            javalin.routes.get("/library", library::list);
            javalin.routes.post("/library/download", library::download);

            javalin.routes.get("/publisher", publisher::dashboard);
            javalin.routes.get("/publisher/games/new", publisher::showGameForm);
            javalin.routes.post("/publisher/games", publisher::createGame);
            javalin.routes.post("/publisher/price", publisher::changePrice);

            javalin.routes.get("/admin", admin::queue);
            javalin.routes.post("/admin/review", admin::review);

            javalin.routes.exception(NotFoundException.class, (exception, ctx) -> {
                ctx.status(404);
                try {
                    views.renderMessage(ctx, "Página não encontrada", "Página não encontrada.");
                } catch (IOException | SQLException renderFailure) {
                    ctx.result("Página não encontrada");
                }
            });
        });
        application.start();
    }
}
