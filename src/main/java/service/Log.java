package service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import javafx.scene.control.TextArea;

public class Log {
    private static TextArea textArea;

    public static void init(TextArea ta) {
        textArea = ta;
    }

    public static void info(String msg) {
        if (textArea == null) {
            return;
        }
        textArea.appendText(DatePattern.NORM_TIME_FORMAT.format(new DateTime()) + ">" + msg);
        textArea.appendText("\n");
    }

    public static void error(String msg, Throwable e) {
        if (textArea == null) {
            return;
        }
        textArea.appendText(DatePattern.NORM_TIME_FORMAT.format(new DateTime()) + ">" + msg + e.getMessage());
        textArea.appendText("\n");

        StackTraceElement[] stackTraceArray = e.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceArray) {
            textArea.appendText(">");
            textArea.appendText(stackTraceElement.toString());
            textArea.appendText("\n");
        }
    }
}
