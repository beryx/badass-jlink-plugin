package org.example.props;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.TreeMap;

public class Main {
    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        for (URL url : Collections.list(Main.class.getClassLoader().getResources("extension.properties"))) {
            try (InputStream in = url.openStream()) {
                properties.load(in);
            }
        }
        System.out.println(new TreeMap<>(properties));
    }
}
