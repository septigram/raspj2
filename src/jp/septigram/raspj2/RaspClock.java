package jp.septigram.raspj2;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Raspberry PI用の時計。
 * @author kurose
 *
 */
public class RaspClock extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		new RaspClock().exec(args);
	}

	String _jpDateFmt = "M月d日(E) HH:mm";
	String _usDateFmt = "d MMM EEE HH:mm";

	String _fontName = "Kochi Gothic"; 
	
	JFrame _mainFrame;
	Font _font;
	Font _font2;
	Thread _thread;
	
	long _intervalTick = 40;
	
	int _width = 320;
	int _height = 240;
	int _newsOffsetX;
	int _scrollSpeed = 3;

	long _lastClick;
	long _doubleClickInterval = 500;
	
	double _th = 0;
	
	
	boolean _showCal = true;
	
	boolean _jpMode = true;
	String _rssURL1 = "https://news.yahoo.co.jp/rss/categories/domestic.xml";
	String _rssURL2 = "https://news.yahoo.co.jp/rss/categories/world.xml";
		
	String _rssURLWeather = "https://rss-weather.yahoo.co.jp/rss/days/4410.xml";
	
	int _pal = 3;
	int _rebootHm = 100;
	
	BufferedImage[] _images0 = new BufferedImage[7];
	BufferedImage[] _images1 = new BufferedImage[7];
	BufferedImage[] _images = _images0;
	
	Color _fg = Color.WHITE;
	Color _bg = Color.BLACK;
	
	int _offsetX;
	boolean _slide = false;

	int monthOffset = 0;
	
	int _newsInterval = 10;
	boolean _newsUpdate;
	int _newsPos = 0;
	long _newsLastupdate;
	long _newsUpdateInterval = 10 * 60 * 1000;
	//ArrayList<String> _rss;
	String _headline;
	String _weaterLine;
	String[] _keywords = {
		"【動画】",
		"動画解説",
		"日の天気（西日本）(共同通信)",
		"日の天気（東日本）(共同通信)",
	};

	ArrayList<Sprite> _sprites = new ArrayList<Sprite>();
	
	long _bootTime = System.currentTimeMillis();
	
	int _interval = 20;

	boolean _doAntiariasing = false;
	boolean _doStar = false;
	boolean _doTick = false;

	Holiday _holiday;
	
	String[] _weekdays = {
			"SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"
	};
	String[] _weekimgs0 = {
			"res/sat2.gif", "res/sun2.gif", "res/mon2.gif", "res/tue2.gif", "res/wed2.gif", "res/thu2.gif", "res/fri2.gif", "res/sat2.gif"
	};
	String[] _weekimgs1 = {
			"res/sat1.gif", "res/sun1.gif", "res/mon1.gif", "res/tue1.gif", "res/wed1.gif", "res/thu1.gif", "res/fri1.gif", "res/sat1.gif"
	};
	Color[] _weekColors0 = {
			new Color(0x99ffff),
			new Color(0xff9999),
			new Color(0xffffff),
			new Color(0xffffff),
			new Color(0xffffff),
			new Color(0xffffff),
			new Color(0xffffff),
			new Color(0x99ffff),
	};
	Color[] _weekColors1 = {
			new Color(0x006699),
			new Color(0x990000),
			new Color(0x000000),
			new Color(0x000000),
			new Color(0x000000),
			new Color(0x000000),
			new Color(0x000000),
			new Color(0x006699),
	};
	Color[] _weekColors2 = {
			new Color(0x00a0e9),
			new Color(0xe60012),
			new Color(0xf39800),
			new Color(0xccc100),
			new Color(0x8fc31f),
			new Color(0x009944),
			new Color(0x009e96),
			new Color(0x00a0e9),
	};
	Color[] _weekColors = _weekColors0;

	int _weekBegin = Calendar.SUNDAY;

	void exec(String[] args) {
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				for (int j = 1; j < arg.length(); j++) {
					switch (arg.charAt(j)) {
					case 'a':
						_doAntiariasing = true;
						break;
					case 'w':
						_width = Integer.parseInt(args[++i]);
						break;
					case 'h':
						_height = Integer.parseInt(args[++i]);
						break;
					case 'i':
						_intervalTick = Integer.parseInt(args[++i]);
						break;
					case 'n':
						_newsInterval = Integer.parseInt(args[++i]);
						break;
					case 'f':
						_fontName = args[++i];
						break;
					case 'j':
						_jpMode = true;
						break;
					case 's':
						_doStar = true;
						break;
					case 't':
						_doTick = true;
						break;
					case 'b':
						String x = args[++i];
						if (x.equalsIgnoreCase("MONDAY")) {
							_weekBegin = Calendar.MONDAY;
						} else if (x.equalsIgnoreCase("TUESDAY")) {
							_weekBegin = Calendar.TUESDAY;
						} else if (x.equalsIgnoreCase("WEDNESDAY")) {
							_weekBegin = Calendar.WEDNESDAY;
						} else if (x.equalsIgnoreCase("THURSDAY")) {
							_weekBegin = Calendar.THURSDAY;
						} else if (x.equalsIgnoreCase("FRIDAY")) {
							_weekBegin = Calendar.FRIDAY;
						} else if (x.equalsIgnoreCase("SATURDAY")) {
							_weekBegin = Calendar.SATURDAY;
						} else if (x.equalsIgnoreCase("SUNDAY")) {
							_weekBegin = Calendar.SUNDAY;
						}
						break;
					}
				}
			}
		}

		_mainFrame = new JFrame();
		_mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		_mainFrame.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				long l = System.currentTimeMillis();
				if (l - _lastClick < _doubleClickInterval) {
					System.exit(0);
				}
				_pal = (_pal + 1) % 4;
				if (_pal == 0) {
					_fg = Color.WHITE;
					_bg = Color.BLACK;
					_weekColors = _weekColors0;
					_images = _images0;
				} else if (_pal == 1) {
					_fg = Color.BLACK;
					_bg = Color.WHITE;
					_weekColors = _weekColors1;
					_images = _images1;
				} else if (_pal == 2) {
					_fg = new Color(0x330000);
					_bg = new Color(0xfffcee);
					_weekColors = _weekColors2;
					_images = _images1;
				} else if (_pal == 3) {
					_fg = Color.WHITE;
					_bg = Color.BLACK;
					_weekColors = _weekColors0;
					_images = _images0;
				}
			
				_lastClick = l;
			}
		});
		_mainFrame.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					monthOffset++;
					repaint();
				}
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					monthOffset--;
					repaint();
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					monthOffset = 0;
					repaint();
				}
			}
		});
		JRootPane root = _mainFrame.getRootPane();
		root.setLayout(new BorderLayout());
		root.add(this, BorderLayout.CENTER);
		_mainFrame.setUndecorated(true);
		_mainFrame.setBounds(0,0,_width,_height);
		int sz = Math.min(_width, _height);
		//320,240
		_font = new Font(_fontName, Font.PLAIN, sz / 11);//22
		_font2 = new Font(_fontName, Font.PLAIN, sz / 15);//16
		_mainFrame.setVisible(true);
		_thread = new Thread(new Runnable() {
			public void run() {
				while (_thread == Thread.currentThread()) {
					try {
						tick();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					try {
						Thread.sleep(_intervalTick);
					} catch (InterruptedException ex) {
						break;
					}
				}
			}
		});
		if (_doStar) {
			Random random = new Random();
			for (int i = 0; i < 1000; i++) {
				Sprite sp = new Sprite();
				sp.random(random, _width, _height);
				_sprites.add(sp);
			}
		}
		for (int i = 0; i < 7; i++) {
			try {
				_images0[i] = ImageIO.read(new File(_weekimgs0[i]));
				_images1[i] = ImageIO.read(new File(_weekimgs1[i]));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		_holiday = Holiday.getInstance();
		
		_thread.start();
		eraseCursor();
	}
	
	void tick() {
		Calendar cal = Calendar.getInstance();
		int s = cal.get(Calendar.SECOND);
		int mil = cal.get(Calendar.MILLISECOND);

		long now = System.currentTimeMillis();
		int hm = cal.get(Calendar.HOUR_OF_DAY) * 100 + cal.get(Calendar.MINUTE);
		if (now - _bootTime > 60 * 1000 && hm == _rebootHm) {
			System.exit(0);
		}

		
		if (_slide) {
			if (s % _interval == 0) {
				_offsetX = (int)((Math.cos(mil / 1000D * Math.PI) + 1) * _width / 2D);
			} else if (s % _interval == _interval / 2) {
				_offsetX = (int)((Math.cos(mil / 1000D * Math.PI) - 1) * _width / 2D);
			} else if (s % _interval < _interval / 2) {
				_offsetX = 0;
			} else {
				_offsetX = 320;
			}
		}
		_newsOffsetX -= _scrollSpeed;
		if (now - _newsLastupdate > _newsUpdateInterval) {
			_newsLastupdate = now;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						load(cal);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}).start();
		}

		double cx = _width / 4;
		double cy = _height / 2;
		for (Sprite sp : _sprites) {
			double dx = sp.point.getX()- cx;
			double dy = sp.point.getY()- cy;
			double dxy = dx * dx + dy * dy;
			double ax = -dx / dxy * 10;
			double ay = -dy / dxy * 10;
			sp.setAcc(ax, ay);
			sp.update();
		}
		
		_th += 0.1;
		repaint();
	}

	void load(Calendar cal) {
		RssLoader rssLoader = new RssLoader();
		rssLoader.loadRSS(_rssURL1);
		rssLoader.loadRSS(_rssURL2);
		TreeSet<String> tree = new TreeSet<String>();
        SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd");
        SimpleDateFormat sdf3 = new SimpleDateFormat("HH:mm");
        SimpleDateFormat sdf4 = new SimpleDateFormat("yyyyMMddHHmmss");
        String todayMd = sdf2.format(new Date());
		for (RssLoader.RssItem item : rssLoader.getItems()) {
			String title = item.getTitle();
			boolean skip = false;
			for (String keyword : _keywords) {
				if (title.indexOf(keyword) >= 0) {
					skip = true;
					break;
				}
			}
			if (skip) {
				continue;
			}
			String t = title;
			Date pubDate = item.getPubDate();
            //末尾の括弧を取る
            if (title.endsWith(")")) {
            	int ix = title.lastIndexOf("(");
            	if (ix > 0) {
            		title = title.substring(0,  ix);
            	}
            }
            String md = sdf2.format(pubDate);
            if (!todayMd.equals(md)) {
            	title += "(" + md + ")";
            }
        	tree.add(sdf4.format(pubDate) + sdf3.format(pubDate) + " " + title);
		}
        String[] a = new String[tree.size()];
        tree.toArray(a);
		StringBuilder headline = new StringBuilder();
        for (String v : tree) {
        	headline.append(v.substring(14)).append("　　");
        }
		_headline = headline.toString();
        
        rssLoader.clearItems();
        rssLoader.loadRSS(_rssURLWeather);
        SimpleDateFormat sdf5 = new SimpleDateFormat("d'日（'E'）'", Locale.JAPAN);
        String head = "今日の天気 ";
        if (cal.get(Calendar.HOUR_OF_DAY) >= 15) {
        	cal.add(Calendar.DATE, 1);
        	head = "明日の天気 ";
        }
        String weatherDay = sdf5.format(cal.getTime());
        //【 31日（木） 東京（東京） 】 曇時々晴 - 20℃/9℃ - Yahoo!天気・災害
        /* Get Node list of RSS items */
		for (RssLoader.RssItem item : rssLoader.getItems()) {
			String title = item.getTitle();
			// Date pubDate = item.getPubDate();
            if (title != null && title.indexOf(weatherDay) > 0) {
            	int sp = title.indexOf("】");
            	if (sp > 0) {
            		title = title.substring(sp + 1);
            	}
        		int ep = title.indexOf(" - Yahoo!天気・災害");
        		if (ep > 0) {
        			title = title.substring(0, ep);
        		}
        		_weaterLine = head + title.replace('-', '(').replaceAll(" ", "") + ")";
            }
        }
	}
	
	void eraseCursor() {
		BufferedImage image = new BufferedImage(16,16,BufferedImage.TYPE_4BYTE_ABGR);  
		Graphics2D g2 = image.createGraphics();  
		//黒で透明 black & transparency  
		g2.setColor(new Color(0,0,0,0));
		g2.fillRect(0,0, 16,16);  
		g2.dispose();  
		_mainFrame.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0,0), "null_cursor"));  
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}

	void drawString(Graphics2D g, String[] vs, Color[] colors, int tx, int ty, int tw, int th, FontMetrics fm) {
		int totalw = 0;
		int[] ws = new int[vs.length];
		for (int i = 0; i < vs.length; i++) {
			int w = fm.stringWidth(vs[i]);
			ws[i] = w;
			totalw += w;
		}
		int tx0 = (tw - totalw) / 2 + tx;
		int ty0 = (th - fm.getHeight()) / 2 + fm.getAscent() + ty;
		for (int i = 0; i < vs.length; i++) {
			g.setColor(colors[i]);
			g.drawString(vs[i], tx0, ty0);
			tx0 += ws[i];
		}		
	}
	
	@Override
	public void paint(Graphics graphics) {
		if (!(graphics instanceof Graphics2D)) {
			return;
		}
		Graphics2D g = (Graphics2D)graphics;
		if (_doAntiariasing) {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		}
		g.setColor(_bg);
		g.fillRect(0, 0, _width, _height);

		g.setColor(Color.BLUE);
		for (Sprite s : _sprites) {
			Ellipse2D e = new Ellipse2D.Double(s.point.getX(), s.point.getY(), s.r, s.r);
			g.fill(e);
		}
		
		FontMetrics fm = g.getFontMetrics(_font);
		FontMetrics fm2 = g.getFontMetrics(_font2);

		g.setColor(_fg);
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		SimpleDateFormat sdf = new SimpleDateFormat(_jpMode ? _jpDateFmt : _usDateFmt, _jpMode ? Locale.JAPAN : Locale.US);
		String hm = sdf.format(date);
		if (_pal == 3) {
			hm = hm.substring(0, hm.length() - 5) + "??:??";
		}
		g.setFont(_font);
		int w = fm.stringWidth(hm);
		g.drawString(hm, (_width - w) / 2 + _offsetX, _height -fm.getDescent() - fm2.getHeight() * 2);

		String weather = _weaterLine;
		if (weather == null) {
			weather = "";
		}
		String holiday = _holiday == null ? null : _holiday.getHoliday(cal);
		if (holiday == null) {
			holiday = "";
		}
		String[] vs = new String[] { weather + " ", holiday };
		Color[] colors = new Color[] { _fg, Color.RED };
		g.setFont(_font2);
		drawString(g, vs, colors, 0, _height - fm2.getHeight() * 2, _width, fm2.getHeight(), fm2);
		
		g.setColor(_fg);
		if (_headline != null) {
			int headlineWidth = fm2.stringWidth(_headline);
			g.drawString(_headline, _newsOffsetX + 2, _height - fm2.getDescent());
			if (_newsOffsetX + headlineWidth < _width) {
				g.drawString(_headline, _newsOffsetX + headlineWidth + 2, _height - fm2.getDescent());
			} if (_newsOffsetX + headlineWidth < 0) {
				_newsOffsetX += headlineWidth;
			}
		}

		double cx = _showCal ? (_width / 4D + _offsetX) : (_width / 2D + _offsetX);
		double cy = _height / 3D;
		double sz = Math.min(_width, _height);
		double cr76 = Math.floor(sz / 3.15); //76
		double cr74 = Math.floor(cr76 * 0.974);
		double cr68 = Math.floor(cr76 * 0.895); //68
		double cr66 = Math.floor(cr76 * 0.868); //66
		double cr64 = Math.floor(cr76 * 0.842); //64
		double cr60 = Math.floor(cr76 * 0.789);
		double cr54 = Math.floor(cr76 * 0.711);
		double cr40 = Math.floor(cr76 * 0.526);
		double cr16 = Math.floor(cr76 * 0.211);
		double cr8 = Math.floor(cr76 * 0.105);
		double cr6 = Math.floor(cr76 * 0.079);
		double cr4 = Math.floor(cr76 * 0.053);
		double cr3 = Math.floor(cr76 * 0.039);
		double cr2 = Math.floor(cr76 * 0.026);
		double cr1 = Math.floor(cr76 * 0.013);
		Stroke stroke8 = new BasicStroke((float)cr8);
		Stroke stroke6 = new BasicStroke((float)cr6);
		Stroke stroke4 = new BasicStroke((float)cr4);
		Stroke stroke3 = new BasicStroke((float)cr3);
		Stroke stroke2 = new BasicStroke((float)cr2);
		Stroke stroke1 = new BasicStroke((float)cr1);
		if (_pal == 0 || _pal == 1) {
			Ellipse2D el = new Ellipse2D.Double(cx - cr76, cy - cr76, cr76 * 2, cr76 * 2); 
			g.setStroke(stroke6);
			g.draw(el);
			g.setColor(_fg);
			for (int i = 0; i < 12; i++) {
				double th = i / 12D * Math.PI * 2;
				Point2D p1 = new Point2D.Double(Math.cos(th) * cr68 + cx, Math.sin(th) * cr68 + cy);
				Point2D p2 = new Point2D.Double(Math.cos(th) * cr64 + cx, Math.sin(th) * cr64 + cy);
				Line2D l = new Line2D.Double(p1, p2);
				g.draw(l);
			}
	
			g.setStroke(stroke2);
			for (int i = 0; i < 60; i++) {
				if (i % 5 == 0) {
					continue;
				}
				double th = i / 60D * Math.PI * 2;
				Point2D p1 = new Point2D.Double(Math.cos(th) * cr68 + cx, Math.sin(th) * cr68 + cy);
				Point2D p2 = new Point2D.Double(Math.cos(th) * cr64 + cx, Math.sin(th) * cr64 + cy);
				Line2D l = new Line2D.Double(p1, p2);
				g.draw(l);
			}
	
			double h = cal.get(Calendar.HOUR_OF_DAY) % 12;
			double m = cal.get(Calendar.MINUTE);
			double s = cal.get(Calendar.SECOND);
			double ms = cal.get(Calendar.MILLISECOND);
			if (_doTick) {
				ms = 0;
			}
			double lh = (-0.25 + (h / 12D + m / 720D)) * 2 * Math.PI;
			double mh = (-0.25 + (m / 60D + s / 3600D)) * 2 * Math.PI;
			double sh = (-0.25 + (s / 60D + ms / 60000D)) * 2 * Math.PI;
			
			Point2D pl1 = new Point2D.Double(Math.cos(lh) * -cr4 + cx, Math.sin(lh) * -cr4 + cy);
			Point2D pl2 = new Point2D.Double(Math.cos(lh) * cr40 + cx, Math.sin(lh) * cr40 + cy);
			Line2D ll = new Line2D.Double(pl1, pl2);
			g.setStroke(stroke6);
			g.draw(ll);
			
			g.setStroke(stroke4);
	
			Point2D pm1 = new Point2D.Double(Math.cos(mh) * -cr4 + cx, Math.sin(mh) * -cr4 + cy);
			Point2D pm2 = new Point2D.Double(Math.cos(mh) * cr60 + cx, Math.sin(mh) * cr60 + cy);
			Line2D lm = new Line2D.Double(pm1, pm2);
			g.draw(lm);
	
			g.setStroke(stroke2);
			g.setColor(Color.RED);
	
			Point2D ps1 = new Point2D.Double(Math.cos(sh) * -cr2 + cx, Math.sin(sh) * -cr2 + cy);
			Point2D ps2 = new Point2D.Double(Math.cos(sh) * cr54 + cx, Math.sin(sh) * cr54 + cy);
			Line2D ls = new Line2D.Double(ps1, ps2);
			g.draw(ls);
			
			Ellipse2D ec = new Ellipse2D.Double(ps2.getX() - cr4, ps2.getY() - cr4, cr4 * 2, cr4 * 2); 
			g.fill(ec);
		} else if (_pal == 2) {
			Ellipse2D el = new Ellipse2D.Double(cx - cr74, cy - cr74, cr74 * 2, cr74 * 2); 
			g.setStroke(stroke1);
			g.draw(el);
			g.setColor(_fg);
			for (int i = 0; i < 12; i++) {
				double th = i / 12D * Math.PI * 2;
				Point2D p1 = new Point2D.Double(Math.cos(th) * cr66 + cx, Math.sin(th) * cr66 + cy);
				Ellipse2D dot1 = new Ellipse2D.Double(p1.getX() - cr4, p1.getY() - cr4, cr4 * 2, cr4 * 2);
				g.fill(dot1);
			}
	
			g.setStroke(stroke1);
			for (int i = 0; i < 60; i++) {
				if (i % 5 == 0) {
					continue;
				}
				double th = i / 60D * Math.PI * 2;
				Point2D p1 = new Point2D.Double(Math.cos(th) * cr68 + cx, Math.sin(th) * cr68 + cy);
				Point2D p2 = new Point2D.Double(Math.cos(th) * cr66 + cx, Math.sin(th) * cr66 + cy);
				Line2D l = new Line2D.Double(p1, p2);
				g.draw(l);
			}
	
			double h = cal.get(Calendar.HOUR_OF_DAY) % 12;
			double m = cal.get(Calendar.MINUTE);
			double s = cal.get(Calendar.SECOND);
			double ms = cal.get(Calendar.MILLISECOND);
			if (_doTick) {
				ms = 0;
			}
			double lh = (-0.25 + (h / 12D + m / 720D)) * 2 * Math.PI;
			double mh = (-0.25 + (m / 60D + s / 3600D)) * 2 * Math.PI;
			double sh = (-0.25 + (s / 60D + ms / 60000D)) * 2 * Math.PI;
			
			Point2D pl1 = new Point2D.Double(Math.cos(lh) * -cr16 + cx, Math.sin(lh) * -cr16 + cy);
			Point2D pl2 = new Point2D.Double(Math.cos(lh) * cr40 + cx, Math.sin(lh) * cr40 + cy);
			Line2D ll = new Line2D.Double(pl1, pl2);
			g.setStroke(stroke8);
			g.draw(ll);
			
			g.setStroke(stroke3);
	
			Point2D pm1 = new Point2D.Double(Math.cos(mh) * -cr16 + cx, Math.sin(mh) * -cr16 + cy);
			Point2D pm2 = new Point2D.Double(Math.cos(mh) * cr60 + cx, Math.sin(mh) * cr60 + cy);
			Line2D lm = new Line2D.Double(pm1, pm2);
			g.draw(lm);
	
		} else if (_pal == 3) {
			int level = cal.get(Calendar.DAY_OF_WEEK) - 1;
			int h = cal.get(Calendar.HOUR_OF_DAY);
			int m = cal.get(Calendar.MINUTE);
			int s = cal.get(Calendar.SECOND);
			
			g.setColor(new Color(0,0x60,0));
			g.setStroke(new BasicStroke((float)cr16, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			double x0 = cx - cr60;
			double y0 = cy - cr60;
			g.draw(new Arc2D.Double(x0, y0, cr60 * 2, cr60 * 2, 90, (60 - s) * 6, Arc2D.OPEN));
			
			String hmVal = h + ":" + m;
			if (!hmVal.equals(_hmVal)) {
				_qz = numToQuiz(h, m, level);
				_hmVal = hmVal;
			}
			String[] qs = _qz.split("\n");
			if (qs.length == 2) {
				g.setColor(Color.WHITE);
				g.setFont(_font);
				g.drawString(qs[0], (float)(cr74 * 2) - fm.stringWidth(qs[0]), 50 + fm.getAscent());
				g.drawString(qs[1], (float)(cr74 * 2) - fm.stringWidth(qs[1]), 50 + fm.getHeight() * 2 + fm.getAscent());
			}
		}
		
		if (_offsetX != 0) {
			double rx = _offsetX > 0 ? _offsetX - _width : _offsetX + _width;
			Rectangle2D rc = new Rectangle2D.Double(rx + 4, 0 + 4, _width - 8, _height - 8);
			g.setColor(Color.BLUE);
			g.fill(rc);
		}
		
		if (_showCal) {
			drawCal(g);
 		}
		
	}
	String _qz;
	String _hmVal;

	void drawString(Graphics2D g, String text, int x, int y, int w, int h, FontMetrics fm, int align) {
		int tw = fm.stringWidth(text);
		int th = fm.getHeight();
		int tx = x + (w - tw) / 2;
		int ty = align == 0 ? (y + (h - th) / 2 + fm.getAscent()) : (y + h - fm.getDescent());
		g.drawString(text,  tx,  ty);
	}

	void drawCal(Graphics2D g) {
		if (_holiday == null) {
			return;
		}
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, monthOffset);
		int y0 = cal.get(Calendar.YEAR);
		int m0 = cal.get(Calendar.MONTH);
		int d0 = cal.get(Calendar.DATE);
		FontMetrics fm1 = g.getFontMetrics(_font);
		FontMetrics fm2 = g.getFontMetrics(_font2);
		int ox = 3 + _width / 2; //160;
		int oy = 3;
		int sz = Math.min(_width, _height);
		int ux = sz / 11;//22
		int uy = sz / 11;//22

		String title = y0 + "/" + (m0 + 1);
		if (_jpMode) {
			title = y0 + "年" + (m0 + 1) + "月";
		}
		g.setFont(_font);
		g.setColor(_fg);
		drawString(g, title, ox, oy, ux * 7, uy, fm1, 0);

		int w0 = _weekBegin;
		g.setFont(_font2);
		for (int i = 0; i < 7; i++) {
			Image img = _images[w0 % 7];
			g.drawImage(img,  i * ux + ox + 1,  oy + uy + 11, 20,  10, null);
			w0++;
		}
		w0 = _weekBegin;
		g.setFont(_font2);
		double cr2 = 0.0083 * sz;
		g.setStroke(new BasicStroke((float)cr2));
		cal.set(y0,  m0, 1);
		int row = 0;
		while (cal.get(Calendar.MONTH) == m0) {
			int d = cal.get(Calendar.DATE);
			int w = cal.get(Calendar.DAY_OF_WEEK);
			String h = _holiday.getHoliday(cal);
			if (w == w0 && d > 1) {
				row++;
			}
			int col = (w - w0 + 7) % 7;
			String sd = String.valueOf(d);
			if (d == d0) {
				if (_pal == 2) {
					g.setColor(new Color(0xff99ff));
					Ellipse2D el = new Ellipse2D.Double(col * ux + ox + 2, row * uy + oy + uy * 2 + 2, ux - 4, uy - 4);
					g.draw(el);
				} else {
					g.setColor(Color.GREEN);
					g.drawLine(col * ux + ox, row * uy + oy + uy * 3,  col * ux + ox + ux - 1,  row * uy + oy + uy * 3);
				}
			}
			g.setColor(h == null ? _weekColors[w] : Color.RED);
			drawString(g, sd, col * ux + ox, row * uy + oy + uy * 2, ux, uy, fm2, 0);
			cal.add(Calendar.DATE,  1);
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

	//0: h: a+b m:a+b
	//1: h: a+b m:a*b+c
	//2: h: a*b+c m:a*b+c
	//3: h: a+b m:a/b+c
	//4: h: a*b+c m:a/b+c
	//5: h: a/b+c m:a/b+c
	//6: h*m=a, h+m=b
	static String numToQuiz(int h, int m, int level) {
		switch (level) {
		case 0:	return numToQuiz(h, 0) + "時\n" + numToQuiz(m, 0) + "分"; 
		case 1:	return numToQuiz(h, 0) + "時\n" + numToQuiz(m, 1) + "分"; 
		case 2:	return numToQuiz(h, 1) + "時\n" + numToQuiz(m, 1) + "分"; 
		case 3:	return numToQuiz(h, 2) + "時\n" + numToQuiz(m, 1) + "分"; 
		case 4:	return numToQuiz(h, 0) + "時\n" + numToQuiz(m, 2) + "分"; 
		case 5:	return numToQuiz(h, 1) + "時\n" + numToQuiz(m, 2) + "分"; 
		case 6:	return numToQuiz(h, 2) + "時\n" + numToQuiz(m, 2) + "分"; 
		}
		return String.valueOf(h + "\n" + m);
	}

	//0:a+b
	//1:a*b+c
	//2:a/b+c
	static String numToQuiz(int n, int level) {
		if (level == 0) {
			int seed = (int)Math.ceil(Math.random() * 20 - 10);
			return (n - seed) + (seed >= 0 ? "+" : "") + seed;
		} else if (level == 1) {
			int seed1 = (int)Math.floor(Math.random() * 8) + 2;
			int seed2 = (int)Math.floor(Math.random() * 8) + 2;
			int r = n - seed1 * seed2;
			return seed1 + "×" + seed2 + (r == 0 ? "" : ((r > 0 ? "+" : "") + r));
		} else if (level == 2) {
			int seed1 = (int)Math.floor(Math.random() * 8) + 2;
			int seed2 = (int)Math.floor(Math.random() * 8) + 2;
			int r = n - seed2;
			return (seed1 * seed2) + "÷" + seed1 + (r == 0 ? "" : ((r > 0 ? "+" : "") + r));
		}
		return String.valueOf(n);
	}
	
	class Sprite {
		double r = 4;
		Point2D point = new Point2D.Double();
		Point2D vector = new Point2D.Double();
		Point2D acc = new Point2D.Double();
		void update() {
			vector.setLocation(vector.getX() + acc.getX(), vector.getY() + acc.getY());
			point.setLocation(point.getX() + vector.getX(), point.getY() + vector.getY());
		}
		void setAcc(double ax, double ay) {
			acc.setLocation(ax, ay);
		}
		void random(Random random, double w, double h) {
			r = random.nextDouble() * 2 + 2;
			point.setLocation(random.nextDouble() * 1000 - 500, random.nextDouble() * 1000 - 500);
			double x = point.getX();
			double y = point.getY();
			double cx = _width / 2 + Math.cos(_th) * 0;
			double cy = _height / 2 + Math.sin(_th) * 0;
			double dx = x - cx;
			double dy = y - cy;
			double dxy = Math.sqrt(dx * dx + dy * dy);
			double v = random.nextDouble() * r * 10D;
			double vx = v * dy / dxy;
			double vy = v * dx / dxy;
			vector.setLocation(vx, vy);
			//setAcc(random.nextDouble() * 100 - 50,random.nextDouble() * 100 - 50);
		}
	}
	
}
