package com.example;

public record Response(String msg) {
    public static Response ok = new Response("ok");

    public static Response of(String msg) {
      return new Response(msg);
    }
}