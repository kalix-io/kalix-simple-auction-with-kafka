package com.example.domain;

import com.example.Response;
import com.fasterxml.jackson.annotation.JsonIgnore;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.annotations.TypeName;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import com.example.domain.AuctionEntity.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedList;

@Id("auctionId")
@TypeId("auction-entity")
@RequestMapping("/auction/{auctionId}")
public class AuctionEntity extends EventSourcedEntity<Auction, AuctionEvent> {

  record Auction(int target, int currentValue, String currentBidId) {

    public Auction apply(AuctionEvent.Created created) {
      return new Auction(created.target(), 0, null);
    }

    public Auction apply(AuctionEvent.BidAccepted bidAccepted) {
      return  new Auction(this.target, bidAccepted.value, bidAccepted.bidId);
    }

    public Auction apply(AuctionEvent.Closed closed) {
      return this;
    }

    @JsonIgnore
    public boolean isClosed() {
      return currentValue >= target;
    }

  }

  public sealed interface AuctionEvent {
    @TypeName("auction-created")
    record Created(String id, long ts, int target) implements AuctionEvent {}

    @TypeName("auction-bid-accepted")
    record BidAccepted(String bidId, int value) implements AuctionEvent {}

    @TypeName("auction-closed")
    record Closed() implements AuctionEvent {}
  };

  @Override
  public Auction emptyState() {
    return new Auction(0, -1, "");
  }

  @PostMapping("/create")
  public Effect<Response> create(@RequestBody AuctionCommand.Create createCmd) {
    if (currentState().isClosed())
      return effects().error("Auction closed. Won by bid: " + currentState().currentBidId);

    return effects()
        .emitEvent(new AuctionEvent.Created(commandContext().entityId(), System.currentTimeMillis(), createCmd.target()))
        .thenReply(__ -> Response.Ok.of("Created"));
  }

  @PostMapping("/bid")
  public Effect<Response> bid(@RequestBody AuctionCommand.Bid bidCmd) {
    if (currentState().isClosed())
      return effects().error("Auction closed. Won by bid: " + currentState().currentBidId);

    if (bidCmd.value() <= currentState().currentValue)
      return effects().error("Bid value needs to be higher than current bid: " + currentState().currentValue);

    var events = new LinkedList<AuctionEvent>();
    events.add(new AuctionEvent.BidAccepted(bidCmd.id(), bidCmd.value()));

    if (bidCmd.value() >= currentState().target)
      events.add(new AuctionEvent.Closed());

    return effects()
        .emitEvents(events)
        .thenReply(__ -> Response.Ok.of("Bid accepted: "+ bidCmd.id()));
  }

  @EventHandler
  public Auction onEvent(AuctionEvent.Created created) {
    return currentState().apply(created);
  }

  @EventHandler
  public Auction onEvent(AuctionEvent.BidAccepted bidAccepted) {
    return currentState().apply(bidAccepted);
  }

  @EventHandler
  public Auction onEvent(AuctionEvent.Closed closed) {
    return currentState().apply(closed);
  }

}
