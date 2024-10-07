package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.fusesource.jansi.Ansi.ansi;

public class ColorHelper {
    public static String error(String msg) {
        return ansi().fgRed().bold().a("\nErreur\n").reset() + msg + "\n";
    }

    public static String warning(String msg) {
        return ansi().fgYellow().bold().a("\n/!\\ Avertissement /!\\\n").reset() + msg + "\n";
    }

    public static String info(String msg) {
        return ansi().fgCyan().bold().a(msg).reset() + "";
    }
}
