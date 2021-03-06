package rawServer.service;

import com.sun.istack.internal.NotNull;
import com.sun.net.httpserver.HttpServer;
import rawServer.repository.SimpleRepository;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServiceImpl implements Service {

    private HttpServer server;

    public ServiceImpl(SimpleRepository repo) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/v0/status", exchange -> {
            String responseString = "ONLINE\n";
            exchange.sendResponseHeaders(200, responseString.getBytes().length);
            exchange.getResponseBody().write(responseString.getBytes());
            exchange.close();
        });

        server.createContext("/v0/entity", new HttpErrorHandler(exchange -> {
            final String httpMethod = exchange.getRequestMethod();
            final String id = getEntityId(exchange.getRequestURI().getQuery());
            switch (httpMethod) {
                case "GET":
                    byte[] value = repo.get(id);
                    exchange.sendResponseHeaders(200, value.length);
                    exchange.getResponseBody().write(value);
                    break;

                case "PUT":
                    int bodyLength = Integer.valueOf(
                            exchange.getRequestHeaders().getFirst("Content-length")
                    );
                    byte[] values = new byte[bodyLength];
                    exchange.getRequestBody().read(values);
                    repo.upsert(id, values);
                    exchange.sendResponseHeaders(201, 0);
                    break;

                case "DELETE":
                    repo.delete(id);
                    exchange.sendResponseHeaders(202, 0);
                    break;

                default:
                    exchange.sendResponseHeaders(405, 0);
            }
            exchange.close();
        }));
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(0);
    }

    @NotNull
    private String getEntityId(@NotNull String query) {
        String prefix ="id=";
        if (!query.startsWith(prefix)) throw new IllegalArgumentException("Wrong query parameter");
        return query.substring(prefix.length());
    }
}
