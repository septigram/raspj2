package jp.septigram.raspj2;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.stream.*;

/**
 * 休日カレンダー。
 * @author kurose
 *
 */
public class Holiday {

	static Holiday _instance;
	
	public static Holiday getInstance() {
		if (_instance == null) {
			Holiday holiday = new Holiday();
			holiday.loadHoliday();
			_instance = holiday;
		}
		return _instance;
	}

	public static void main(String[] args) throws IOException {
		int ymd = -1;
		int y = -1;
		boolean dumpMode = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				for (int j = 1; j < arg.length(); j++) {
					switch (arg.charAt(j)) {
					case 'y':
						y = Integer.parseInt(args[++i]);
						break;
					case 'd':
						ymd = Integer.parseInt(args[++i]);
						break;
					case 'D':
						dumpMode = true;
						break;
					}
				}
			}
		}
		
		Holiday holiday = Holiday.getInstance();
		Calendar cal = Calendar.getInstance();
		if (dumpMode) {
			System.out.println(new File(".").getAbsolutePath());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd(EEE)", Locale.JAPAN);
			PrintWriter o = new PrintWriter(new OutputStreamWriter(new FileOutputStream("holiday_jp.xml"),"UTF-8"));
			o.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			o.println("<holidays>");
			for (int year = 1970; year < 2100; year++) {
				cal.set(year, 0, 1);
				for (int i = 0; i < 365; i++) {
					if (year != cal.get(Calendar.YEAR)) {
						break;
					}
					String name = holiday.getHoliday(cal);
					if (name != null) {
						o.println("<holiday date=\"" + sdf.format(cal.getTime()) + "\" name=\"" + name + "\"/>");
					}
					cal.add(Calendar.DATE, 1);
				}
			}
			o.println("</holidays>");
			o.close();
		} else if (y > 0 || ymd > 0) {
			if (ymd > 0) {
				cal.set(ymd / 10000, (ymd / 100) % 100, ymd % 100);
			} else {
				cal.set(y,  0, 1);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd(EEE)", Locale.JAPAN);
			while (cal.get(Calendar.YEAR) == y) {
				System.out.println(sdf.format(cal.getTime()) + " " + holiday.getHoliday(cal));
				cal.add(Calendar.DATE,  1);
			}
		}
	}

	ArrayList<HolidayItem> _holidays = new ArrayList<HolidayItem>();

	void loadHoliday() {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		try {
			BufferedInputStream stream = new BufferedInputStream(new FileInputStream("holiday.xml"));
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			for (;reader.hasNext();reader.next()) {
				int eventType = reader.getEventType();
				if (eventType == XMLStreamConstants.START_ELEMENT) {
					String elementName = reader.getName().getLocalPart();
					if ("holiday".equals(elementName)) {
						HolidayItem item = new HolidayItem();
						int attCount = reader.getAttributeCount();
						for (int i = 0; i < attCount; i++) {
							String attName = reader.getAttributeName(i).getLocalPart();
							String attValue = reader.getAttributeValue(i);
							//	<holiday name="成人の日" year="2000;1999" month="1" date="8;14" weekday="Monday"/>
							item.parse(attName, attValue);
						}
						_holidays.add(item);
					}
					
				}
			}
			reader.close();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public String getHoliday(Calendar cal) {
		Calendar tmp = (Calendar)cal.clone();
		String n = getMajorHoliday(tmp);
		if (n != null) {
			return n;
		}
		int year = tmp.get(Calendar.YEAR);
		int month = tmp.get(Calendar.MONTH) + 1;
		int wday = tmp.get(Calendar.DAY_OF_WEEK);

		tmp.add(Calendar.DATE, 1);
		String nn = getMajorHoliday(tmp);
		tmp.add(Calendar.DATE, -2);
		String pn = getMajorHoliday(tmp);
		
		for (HolidayItem h : _holidays) {
			if (h._year.contains(year) && h._month.contains(month)) {
				if ("Natinal Holiday".equals(h._logic)) {
					if (nn != null && pn != null) {
						return h._name;
					}
				} else if ("Holiday in lieu".equals(h._logic)) {
					if (pn != null && wday == Calendar.MONDAY) {
						return h._name;
					}
				} else if ("Holiday in lieu(2)".equals(h._logic)) {
					while (pn != null) {
						if (tmp.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
							return h._name;
						}
						tmp.add(Calendar.DATE, -1);
						pn = getHoliday(tmp);
					}
				}
			}
		}
		
		return null;
	}
	
	String getMajorHoliday(Calendar cal) {
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int date = cal.get(Calendar.DATE);
		int wday = cal.get(Calendar.DAY_OF_WEEK);
		for (HolidayItem h : _holidays) {
			if (h._year.contains(year) && h._month.contains(month)) {
				if ("Vernal Equinox Day".equals(h._logic)) {
					int d = getVernalEquinoxDate(year);
					if (d == date) {
						return h._name;
					}
				} else if ("Autumnal Equinox Day".equals(h._logic)) {
					int d = getAutumnalEquinoxDate(year);
					if (d == date) {
						return h._name;
					}
				} else if (h._logic == null) {
					if (h._date.contains(date)) {
						if (h._weekday != -1) {
							if (wday == h._weekday) {
								return h._name;
							}
						} else {
							return h._name;
						}
					}
				}
			}
		}
		return null;
	}
	
	
	boolean contains(int min, int max, int value) {
		return value >= min && value <= max;
	}

	/*
	 * 春分の日 Vernal Equinox Day
	西暦年数の4での剰余が0の場合
	1900年 - 1956年までは3月21日
	1960年 - 2088年までは3月20日
	2092年 - 2096年までは3月19日
	西暦年数の4での剰余が1の場合
	1901年 - 1989年までは3月21日
	1993年 - 2097年までは3月20日
	西暦年数の4での剰余が2の場合
	1902年 - 2022年までは3月21日
	2026年 - 2098年までは3月20日
	西暦年数の4での剰余が3の場合
	1903年 - 1923年までは3月22日
	1927年 - 2055年までは3月21日
	2059年 - 2099年までは3月20日
	--wikipedia
	*/
	int getVernalEquinoxDate(int year) {
		switch (year % 4) {
		case 0:	return contains(1900, 1956, year) ? 21 : (contains(1960, 2088, year) ? 20 : 19);
		case 1:	return contains(1901, 1989, year) ? 21 : 20;
		case 2:	return contains(1902, 2022, year) ? 21 : 20;
		case 3:	return contains(1903, 1923, year) ? 22 : (contains(1927, 2055, year) ? 21 : 20);
		}
		return -1;
	}
	
	/*
	秋分の日 Autumnal Equinox Day
	西暦年数の4での剰余が0の場合
	1900年 - 2008年までは9月23日
	2012年 - 2096年までは9月22日
	西暦年数の4での剰余が1の場合
	1901年 - 1917年までは9月24日
	1921年 - 2041年までは9月23日
	2045年 - 2097年までは9月22日
	西暦年数の4での剰余が2の場合
	1902年 - 1946年までは9月24日
	1950年 - 2074年までは9月23日
	2078年 - 2098年までは9月22日
	西暦年数の4での剰余が3の場合
	1903年 - 1979年までは9月24日
	1983年 - 2099年までは9月23日
	--wikipedia
	*/
	int getAutumnalEquinoxDate(int year) {
		switch (year % 4) {
		case 0:	return contains(1900, 2008, year) ? 23 : 22;
		case 1:	return contains(1901, 1917, year) ? 24 : (contains(1921, 2041, year) ? 23 : 22);
		case 2:	return contains(1902, 1946, year) ? 24 : (contains(1950, 2074, year) ? 23 : 22);
		case 3:	return contains(1903, 1979, year) ? 24 : 23;
		}
		return -1;
	}
	
	static class Range {
		int _begin = -1;
		int _end = -1;
		Range() {
		}
		Range(int begin, int end) {
			_begin = begin;
			_end = end;
		}
		void parse(String v) {
			int semiColon = v.indexOf(';');
			if (semiColon == 0) {
				_end = Integer.parseInt(v.substring(1));
			} else if (semiColon > 0) {
				_begin = Integer.parseInt(v.substring(0, semiColon).trim());
				_end = Integer.parseInt(v.substring(semiColon + 1).trim());
			} else {
				_begin = Integer.parseInt(v.trim());
				_end = _begin;
			}
		}
		boolean contains(int v) {
			return v >= _begin && (_end == -1 || v <= _end);
		}
		public String toString() {
			return _begin + ";" + _end;
		}
		
	}
	
	static class HolidayItem {
		String _name;
		Range _year = new Range();
		Range _month = new Range();
		Range _date = new Range();
		int _weekday = -1;
		String _logic;
		
		public void parse(String attName, String attValue) {
			if ("name".equals(attName)) {
				_name = attValue;
			}
			if ("year".equals(attName)) {
				_year.parse(attValue);
			}
			if ("month".equals(attName)) {
				_month.parse(attValue);
			}
			if ("date".equals(attName)) {
				_date.parse(attValue);
			}
			if ("weekday".equals(attName)) {
				if (attValue.equalsIgnoreCase("MONDAY")) {
					_weekday = Calendar.MONDAY;
				} else if (attValue.equalsIgnoreCase("TUESDAY")) {
					_weekday = Calendar.TUESDAY;
				} else if (attValue.equalsIgnoreCase("WEDNESDAY")) {
					_weekday = Calendar.WEDNESDAY;
				} else if (attValue.equalsIgnoreCase("THURSDAY")) {
					_weekday = Calendar.THURSDAY;
				} else if (attValue.equalsIgnoreCase("FRIDAY")) {
					_weekday = Calendar.FRIDAY;
				} else if (attValue.equalsIgnoreCase("SATURDAY")) {
					_weekday = Calendar.SATURDAY;
				} else if (attValue.equalsIgnoreCase("SUNDAY")) {
					_weekday = Calendar.SUNDAY;
				}
			}
			if ("logic".equals(attName)) {
				_logic = attValue;
			}
		}
		
		public String toString() {
			return _name + _year + _month + _date + _weekday + _logic; 
		}
	}
}
