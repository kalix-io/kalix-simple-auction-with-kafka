package com.example;

public sealed interface Response {
  record Ok(String msg) implements Response {
    public static Ok of(String created) {
      return new Ok(created);
    }
  }
}