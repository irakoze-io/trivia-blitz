package dev.irakodes.triviablitz.utils;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public final class UuidGenerator {

    private UuidGenerator() {
    }

    public static UUID newId() { return UuidCreator.getTimeOrderedEpoch(); }
}
