package test;

public class Main {
    public static void main(String[] args) {
        // I'll uncomment this and comment the other one to generate the jar file of this (gui.jar)
        //GraphicalInterface.start(); # it doesn't exist yet
        //
        // I'll be using this to generate the jar file (cli.jar)
        ConsoleInterface.start(args);
    }
}
