package com.example.mixin;

public class TypedActionResult<T> {
    private final T result;
    private final ActionType type;

    public TypedActionResult(ActionType type, T result) {
        this.type = type;
        this.result = result;
    }

    public static <T> TypedActionResult<T> success(T result) {
        return new TypedActionResult<>(ActionType.SUCCESS, result);
    }

    public T getResult() {
        return result;
    }

    public ActionType getType() {
        return type;
    }

    public enum ActionType {
        SUCCESS,
        FAIL,
        PASS
    }
}
