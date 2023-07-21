package com.example.domain;

public sealed interface AuctionCommand {
  record Create(int target) implements AuctionCommand {}

  record Bid(String id, int value) implements AuctionCommand {}
}
