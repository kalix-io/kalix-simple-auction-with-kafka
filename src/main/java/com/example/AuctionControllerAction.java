package com.example;

import com.example.domain.AuctionCommand;
import com.example.domain.AuctionEntity;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Acl;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@RequestMapping("/auctions")
public class AuctionControllerAction extends Action {

  private ComponentClient componentClient;

  public AuctionControllerAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @PostMapping("/create/{auctionId}/{target}")
  public Action.Effect<Response> create(
      @PathVariable String auctionId,
      @PathVariable int target) {
    if (target <= 0)
      return effects().error("Target needs to be higher than 0.");

    var createCmd = new AuctionCommand.Create(target);

    var defCall = componentClient
        .forEventSourcedEntity(auctionId)
        .call(AuctionEntity::create)
        .params(createCmd);
    return effects().forward(defCall);
  }

  @PostMapping("/bid/{auctionId}/{value}")
  public Action.Effect<Response> bid(@PathVariable String auctionId, @PathVariable int value) {
    if (value <= 0)
      return effects().error("Bid value needs to be higher than 0.");

    var bid = new AuctionCommand.Bid(UUID.randomUUID().toString(), value);
    var defCall = componentClient.forEventSourcedEntity(auctionId).call(AuctionEntity::bid).params(bid);

    return effects().forward(defCall);
  }
}
