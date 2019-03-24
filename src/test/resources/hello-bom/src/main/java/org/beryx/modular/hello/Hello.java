package org.beryx.modular.hello;

import com.google.gson.Gson;

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;

public class Hello {
	private static class Greeting {
		public String from;
		public String to;
		public String greeting;
	}
	public static void main(String[] args) throws Exception {
		StringReader reader = new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?><greeting from=\"Alice\" to=\"Bob\" greeting=\"Hello\"/>");
		Document document = new SAXReader().read(reader);

		Greeting greeting = new Greeting();
		Element root = document.getRootElement();
		greeting.from = root.attribute("from").getValue();
		greeting.to = root.attribute("to").getValue();
		greeting.greeting = root.attribute("greeting").getValue();

		Gson gson = new Gson();
		System.out.println(gson.toJson(greeting));
	}
}
