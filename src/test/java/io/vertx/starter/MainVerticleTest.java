package io.vertx.starter;

import fr.treeptik.MainVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

    private Vertx vertx;
    private RedisClient client;

    @Before
    public void setUp(TestContext tc) {
        vertx = Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("redis.location", "localhost"));
        vertx.deployVerticle(MainVerticle.class.getName(), options, tc.asyncAssertSuccess());
        client = RedisClient.create(vertx, new RedisOptions().setHost("localhost"));
    }

    @After
    public void tearDown(TestContext tc) {
        vertx.close(tc.asyncAssertSuccess());
    }

    @Test
    public void testThatTheServerIsStarted(TestContext tc) {
        Async async = tc.async();
        client.get("key", r -> {
            String previousCount = r.result();
            vertx.createHttpClient().getNow(8080, "localhost", "/", response -> {
                tc.assertEquals(response.statusCode(), 200);
                response.bodyHandler(body -> {
                    tc.assertTrue(body.length() > 0);
                    client.get("key", x -> {
                        String expectedCount = x.result();
                        Integer expected = Integer.parseInt(expectedCount);
                        Integer previous = Integer.parseInt(previousCount);
                        tc.assertTrue(expected > previous);
                    });
                    async.complete();
                });
            });
        });
    }


}