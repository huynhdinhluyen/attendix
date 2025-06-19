package com.fpt.attendix.data.repository;

import androidx.annotation.NonNull;

public class Result<T> {
    private Result() {}

    @NonNull
    @Override
    public String toString() {
        if (this instanceof Success) {
            Success<T> success = (Success<T>) this;
            return "Success[data=" + success.getData().toString() + "]";
        } else if (this instanceof Error) {
            Error<T> error = (Error<T>) this;
            return "Error[exception=" + error.getError().toString() + "]";
        }
        return "";
    }

    // Success sub-class
    public final static class Success<T> extends Result<T> {
        private final T data;
        public Success(T data) {
            this.data = data;
        }
        public T getData() {
            return this.data;
        }
    }

    public final static class Error<T> extends Result<T> {
        private final Exception exception;
        public Error(Exception exception) {
            this.exception = exception;
        }
        public Exception getError() {
            return this.exception;
        }
    }
}
