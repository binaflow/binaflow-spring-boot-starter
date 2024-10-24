package com.github.binaflow.util;

public class StackTraceUtils {
    public static String toString(StackTraceElement[] stackTraceElements) {
        var sb = new StringBuilder();
        for (var element : stackTraceElements) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
