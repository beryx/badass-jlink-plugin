package org.example.modular;

public class Hello {
	static record Point(int x, int y) {}
	static record Line(Point start, Point end) {}

	private static void print(Object object) {
		switch(object) {
			case Point(int x, int y) -> System.out.println("point: (" + x + " / " + y + ")");
			case Line(Point(int x1, int y1), Point(int x2, int y2)) -> System.out.println("line from: (" + x1 + "," + y1 + ") to: (" + x2 + "," + y2 + ")");
			default -> System.out.println("object: " + object);
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
