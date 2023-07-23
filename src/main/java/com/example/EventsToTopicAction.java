package com.example;

import com.example.domain.AuctionEntity;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;

@Subscribe.EventSourcedEntity(value = AuctionEntity.class, ignoreUnknown = true)
public class EventsToTopicAction extends Action {

  @Publish.Topic("auction-events")
  public Action.Effect<AuctionEntity.AuctionEvent> handleCreate(AuctionEntity.AuctionEvent.Created created) {
    return effects().reply(created);
  }

  @Publish.Topic("auction-events")
  public Action.Effect<AuctionEntity.AuctionEvent> handleCreate(AuctionEntity.AuctionEvent.BidAccepted bidAccepted) {
    return effects().reply(bidAccepted);
  }

  @Publish.Topic("auction-events")
  public Action.Effect<AuctionEntity.AuctionEvent> handleCreate(AuctionEntity.AuctionEvent.Closed closed) {
    return effects().reply(closed);
  }
}
