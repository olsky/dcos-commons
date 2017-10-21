package com.mesosphere.sdk.offer.evaluate;

public interface VisitorResultCollector<T> {

    void setResult(T result);

    T getResult();

    public static class Empty { }
}
