package com.mesosphere.sdk.offer.evaluate;

import java.util.Optional;

public class VisitorResult<S, T> {
    private final S value;
    private final T delegateValue;

    public VisitorResult(S value, T delegateValue) {
        // Optional empty at the end of the line.
        this.value = value;
        this.delegateValue = delegateValue;
    }

    public S getValue() {
        return value;
    }

    public T getDelegateValue() {
        return delegateValue;
    }
    // Static methods for raising regular values into a result? from(A, B), and from(A) just gives empty B.

    public static <S, T> VisitorResult<S, T> from(S s, T t) {
        return new VisitorResult<>(s, t);
    }

    public static <S> VisitorResult<S, Optional<Empty>> from(S s) {
        return new VisitorResult<>(s, Optional.empty());
    }

    public static class Empty { }
}
