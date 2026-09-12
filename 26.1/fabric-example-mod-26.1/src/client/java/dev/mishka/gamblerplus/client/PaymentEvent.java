package dev.mishka.gamblerplus.client;

public record PaymentEvent(long timestampMs, String player, long amount, boolean incoming) {
}
