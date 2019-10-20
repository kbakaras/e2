package ru.kbakaras.e2.cli.support;

import java.util.ArrayList;
import java.util.List;

public class ConsoleTable {
    private String[] headers;
    private int[] widths;
    private List<String[]> lines = new ArrayList<String[]>();

    private int column = 0;
    private String[] line;

    public void setHeaders(String[] headers) {
        this.headers = headers;
        widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
    }

    public void addValue(String value) {
        if (column == 0) {
            line = new String[headers.length];
            lines.add(line);
        }

        line[column] = value;
        if (value != null) {
            widths[column] = Math.max(widths[column], value.length());
        }

        column++;
        if (column == headers.length) {
            column = 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        String[] formats = new String[headers.length];

        for (int i = 0; i < headers.length; i++) {
            formats[i] = (i > 0 ? "  %" : "%") + widths[i] + "s";
            str.append(String.format(formats[i], headers[i]));
        }
        str.append("\n");

        for (String[] line: lines) {
            for (int i = 0; i < headers.length; i++) {
                str.append(String.format(formats[i], suppressNull(line[i])));
            }
            str.append("\n");
        }

        return str.toString();
    }

    private String suppressNull(String value) {
        return value != null ? value : "";
    }
}