package org.example.modular;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hello {
	private static final Logger logger = LoggerFactory.getLogger(Hello.class);

	static record Point(int x, int y) {}
	static record Line(Point start, Point end) {}

	private static void print(Object object) {
		switch(object) {
			case Point(int x, int y) -> logger.info("point: (" + x + " / " + y + ")");
			case Line(Point(int x1, int y1), Point(int x2, int y2)) -> logger.info("line from: (" + x1 + "," + y1 + ") to: (" + x2 + "," + y2 + ")");
			default -> logger.info("object: " + object);
		}
	}

	public static void main(String[] args) {
		var p1 = new Point(30, 80);
		var p2 = new Point(20, 50);
		var line = new Line(p1, p2);
//		print("Hello, world!");
//		print(p1);
		print(line);
	}
}
