package com.example;

import com.example.domain.AuctionEntity;
import com.example.domain.AuctionEntity.AuctionEvent.*;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

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

  private EventingTestKit.Topic topic;

  @BeforeAll
  public void beforeAll() {
    this.topic = kalixTestKit.getTopic("auction-events");
  }

  @Test
  public void testAuctionFullCycle() {

    // given: a creation of an auction with name auction1 and target value of 50
    var response = webClient.post()
        .uri("/auctions/create/auction1/50")
        .retrieve()
        .bodyToMono(Response.Ok.class)
        .block();
    assertEquals("Created", response.msg());

    // when: 1 bid happens with value of 30
    var responseBid = webClient.post()
        .uri("/auctions/bid/auction1/30")
        .retrieve()
        .bodyToMono(Response.Ok.class)
        .block();
    assertTrue(responseBid.msg().contains("accepted"));

    // and: another bid with value 51
    // when: 1 bid happens with value of 30
    var responseBid2 = webClient.post()
        .uri("/auctions/bid/auction1/51")
        .retrieve()
        .bodyToMono(Response.Ok.class)
        .block();
    assertTrue(responseBid2.msg().contains("accepted"));

    // then: auction is closed and 4 events should be sent to broker (creation, bid, bid, closing)
    var createdEvent = topic.expectOneTyped(Created.class);
    assertEquals("auction1", createdEvent.getPayload().id());
    assertEquals(50, createdEvent.getPayload().target());

    var bid1Event = topic.expectOneTyped(BidAccepted.class);
    assertEquals(30, bid1Event.getPayload().value());

    var bid2Event = topic.expectOneTyped(BidAccepted.class);
    assertEquals(51, bid2Event.getPayload().value());

    topic.expectOneTyped(Closed.class);

    // and: no further events
    topic.expectNone();
  }
}