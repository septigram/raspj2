package jp.septigram.raspj2;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RssLoader {

    DocumentBuilder _builder;
    ArrayList<RssItem> _items = new ArrayList<RssItem>();
    
	public RssLoader() {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    try {
			_builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public int size() {
		return _items.size();
	}
	
	public void clearItems() {
		_items.clear();
	}
	
	public RssItem get(int index) {
		return _items.get(index);
	}
	
	public ArrayList<RssItem> getItems() {
		return _items;
	}
	
	public void loadRSS(String url) {
	    DocumentBuilder builder = _builder;
		if (builder == null) {
			return;
		}
        try {
        	/*
        	URL urlObj = new URL(url);
        	StringBuilder sb = new StringBuilder();
        	BufferedReader in = new BufferedReader(new InputStreamReader(urlObj.openStream(), "UTF-8"));
        	while (true) {
        		String line = in.readLine();
        		if (line == null) {
        			break;
        		}
        		if (sb.length() == 0) {
        			line = line.replaceAll("'", "\"");
        		}
        		sb.append(line);
        	}
        	System.out.println(sb.toString());
            Document document = builder.parse(new ByteArrayInputStream(sb.toString().getBytes("UTF-8")));
            */
        	Document document = builder.parse(url);
            Element root = document.getDocumentElement();
            SimpleDateFormat sdf1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-DD'T'HH:mm:ss.SSS'Z'", Locale.US);
            /* Get Node list of RSS items */
            SimpleDateFormat sdf = url.indexOf("news") >= 0 ? sdf2 : sdf1;
            NodeList itemList = root.getElementsByTagName("item");
            for (int i = 0; i <itemList.getLength(); i++) {
                Element  element = (Element)itemList.item(i);
                RssItem item = new RssItem();
                item.parse(element, sdf);
                _items.add(item);
            }
        } catch (IOException e) {
            System.out.println("IO Exception: " + url);
        	e.printStackTrace();
        } catch (SAXException e) {
            System.out.println("SAX Exception: " + url);
        	//e.printStackTrace();
        }
    }

	String nodeValue(Element element, String key) {
		try {
	        NodeList textElements = element.getElementsByTagName(key);
	        return textElements.item(0).getFirstChild().getNodeValue().trim();
		} catch (NullPointerException ex) {
		}
		return null;
	}

	public class RssItem {
		String _title;
		Date _pubDate;
		public String getTitle() {
			return _title;
		}
		public Date getPubDate() {
			return _pubDate;
		}
		public void parse(Element element, SimpleDateFormat sdf) {
            _title = nodeValue(element, "title");
            String pubDateStr = nodeValue(element, "pubDate");
            try {
				_pubDate = sdf.parse(pubDateStr);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
}
