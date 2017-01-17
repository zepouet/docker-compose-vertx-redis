package fr.treeptik;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainVerticle extends AbstractVerticle {

    private String hostName;
    private String redisLocation;
    private RedisClient client = null;

    @Override
    public void start(Future<Void> fut) throws Exception {
        Router router = Router.router(vertx);
        router.get("/").handler(this::handleIncrement);
        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
            .createHttpServer()
            .requestHandler(router::accept)
            .listen(
                    config().getInteger("http.port", 8080),
                    result -> {
                        if (result.succeeded()) {
                            fut.complete();
                            hostName = getHostName();
                            redisLocation = getRedisLocation();
                            client = RedisClient.create(vertx, new RedisOptions().setHost(redisLocation));
                            client.set("key", "0", r -> {
                                if (r.succeeded()) {
                                    System.out.println("key initialized with : 0");
                                } else {
                                    System.out.println("Connection or Operation Failed " + r.cause());
                                }
                            });
                        } else {
                            fut.fail(result.cause());
                        }
                    }
            );
    }

    private void handleIncrement(RoutingContext routingContext) {
        client.incr("key", r -> {
            if (r.succeeded()) {
                routingContext.response().putHeader("content-type", "text/html")
                        .end("<h1>"+hostName+"</h1><h2>Calls :"+r.result()+"</h2>");
            } else {
                System.out.println("Connection or Operation Failed " + r.cause());
                routingContext.response().setStatusCode(500).end();
            }
        });
    }

    private String getHostName() {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            hostName = "error";
        }
        return hostName;
    }

    private String getRedisLocation() {
        String redisLocation = config().getString("redis.location");
        // If not present, we are in container context so we will use 'redis' from docker dns
        if (redisLocation == null || redisLocation.isEmpty()) {
            redisLocation = "redis";
        }
        return redisLocation;
    }
}
