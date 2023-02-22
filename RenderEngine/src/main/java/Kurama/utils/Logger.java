package Kurama.utils;

import Kurama.game.Game;

import java.util.ArrayList;
import java.util.List;

public class Logger {

    public static boolean showLogs = true;
    public static boolean showErrors = true;

    public static void log(String text) {
        if (showLogs) {
            System.out.println(text);
        }
    }

    public static void logPerSec(String text) {
        if(showLogs && Game.isOneSecond) {
            System.out.println(text);
        }
    }
    public static void logPerSec(int text) {
        if(showLogs && Game.isOneSecond) {
            System.out.println(text);
        }
    }
    public static void logPerSec(float text) {
        if(showLogs && Game.isOneSecond) {
            System.out.println(text);
        }
    }
    public static void logPerSec(double text) {
        if(showLogs && Game.isOneSecond) {
            System.out.println(text);
        }
    }
    public static void logPerSec(long text) {
        if(showLogs && Game.isOneSecond) {
            System.out.println(text);
        }
    }

    public static void logPerSec() {
        if(showLogs && Game.isOneSecond) {
            System.out.println("\n");
        }
    }

    public static void log() {
        if (showLogs) {
            System.out.println();
        }
    }

    public static void log(Object text) {
        if (showLogs) {
            System.out.println(text.toString());
        }
    }

    public static void logError(String text) {
        if (showErrors) {
            System.err.println(text);
        }
    }

}
