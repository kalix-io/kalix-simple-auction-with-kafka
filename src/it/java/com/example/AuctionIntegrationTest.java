package com.example;

import com.example.domain.AuctionEntity;
import com.example.domain.AuctionEntity.AuctionEvent.*;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * This is a skeleton for implementing integration tests for a Kalix application built with the Java SDK.
 *
 * This test will initiate a Kalix Proxy using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
@SpringBootTest(classes = Main.class)
public class AuctionIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

  @Autowired
  private KalixTestKit kalixTestKit;

  @Autowired
  private ComponentClient componentClient;

  private EventingTestKit.Topic topic;

  @BeforeAll
  public void beforeAll() {
    this.topic = kalixTestKit.getTopic("auction-events");
  }

  @Test
  public void testAuctionFullCycle() {

    var auctionId = "auction1";
       var response2 = webClient.post()
        .uri("/auctions/create/auctionxxx/50")
        .retrieve()
        .bodyToMono(Response.class)
        .block();

    // given: a creation of an auction with name auction1 and target value of 50
    var response = execute(
        componentClient
          .forAction()
          .call(AuctionControllerAction::create)
          .params(auctionId, 50));
    assertEquals("Created", response.msg());

    // when: 1 bid happens with value of 30
    var responseBid = execute(
        componentClient
            .forAction()
            .call(AuctionControllerAction::bid)
            .params(auctionId, 30));
    assertTrue(responseBid.msg().contains("accepted"));

    // and: another bid with value 51
    // when: 1 bid happens with value of 30
    var responseBid2 = execute(
        componentClient
            .forAction()
            .call(AuctionControllerAction::bid)
            .params(auctionId, 51));
    assertTrue(responseBid2.msg().contains("accepted"));

    // then: auction is closed and 4 events should be sent to broker (creation, bid, bid, closing)
    var createdEvent = topic.expectOneTyped(Created.class);
    assertEquals(auctionId, createdEvent.getPayload().id());
    assertEquals(50, createdEvent.getPayload().target());

    List<EventingTestKit.Message<?>> bids = topic.expectN(3);
    var bid1Event = bids.get(0).expectType(BidAccepted.class);
    assertEquals(30, bid1Event.value());

    var bid2Event = bids.get(1).expectType(BidAccepted.class);
    assertEquals(51, bid2Event.value());

    bids.get(2).expectType(Closed.class);

    // and: no further events
    topic.expectNone();
  }

  private Duration timeout = Duration.of(10, SECONDS);

  private <T> T execute(DeferredCall<Any, T> deferredCall) {
    try {
      return deferredCall.execute().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}