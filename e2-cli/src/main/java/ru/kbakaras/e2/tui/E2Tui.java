package ru.kbakaras.e2.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;

import java.io.IOException;

public class E2Tui {


    public static void main(String[] args) {

        TerminalScreen screen = null;

        try {

            screen = new DefaultTerminalFactory().createScreen();
            screen.getTerminal().addResizeListener(new TerminalResizeListener() {
                @Override
                public void onResized(Terminal terminal, TerminalSize newSize) {
                    System.out.println("resized");
                }
            });

            screen.startScreen();

            final WindowBasedTextGUI textGUI = new MultiWindowTextGUI(screen);

            final Window window = new BasicWindow("My Root Window");

            textGUI.addWindowAndWait(window);

//            screen.setCursorPosition(null);
//            screen.clear();
//            screen.refresh();

            //KeyStroke stroke = screen.readInput();

            screen.stopScreen();

        } catch (IOException e) {
            throw new RuntimeException(e);

        } finally {
            if (screen != null) {
                try {
                    screen.stopScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}