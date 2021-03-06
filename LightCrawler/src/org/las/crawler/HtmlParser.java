package org.las.crawler;

import it.unimi.dsi.parser.BulletParser;
import it.unimi.dsi.parser.callback.TextExtractor;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.las.tools.*;
import org.las.tools.FingerPrinter.FingerPrinter;
import org.las.tools.LanguageIdentifier.LanguageIdentifier;
import org.las.tools.MIMEFormater.MIMEFormater;


public class HtmlParser {

	private BulletParser bulletParser;
	private TextExtractor textExtractor;
	private LinkExtractor linkExtractor;
	
	private LanguageIdentifier langIdentifier;
	private static final int MAX_OUT_LINKS = Config.getIntProperty(
			"fetcher.max_outlinks", 500);
	private static final String DEFAULT_ENCODING = "UTF-8";

	public HtmlParser() {
		
		langIdentifier = new LanguageIdentifier();
	}

	public Set<URLEntity> parse(PageEntity page) {
		bulletParser = new BulletParser();
		textExtractor = new TextExtractor();
		linkExtractor = new LinkExtractor();
		
		//Encode by Http protocal
		String encode = page.getEncode();
		if(encode==null){
			encode = DEFAULT_ENCODING;
		}
		if(!encode.equalsIgnoreCase("GBK")&&!encode.equalsIgnoreCase("GB2312")&&!encode.equalsIgnoreCase("ISO-8859-1")){
			encode = DEFAULT_ENCODING;
		}
		char[] chars = null;
		try {
			chars = new String(page.getContent(),encode).toCharArray();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//extract links 
		bulletParser.setCallback(linkExtractor);
		bulletParser.parse(chars);
		
		int urlCount = 0;
		Set<URLEntity> links = new HashSet<URLEntity>();
		for (URLEntity link : linkExtractor.getLinks()) {
			String href = link.getUrl();
			href = href.trim();
			if (href.length() == 0) {
				continue;
			}
			if (href.indexOf("javascript:") < 0
					&& href.indexOf("@") < 0) {
				String baseUrl = linkExtractor.base();
				if(baseUrl==null || baseUrl.length()==0){
					baseUrl = page.getUrl();
				}
				URL url = URLCanonicalizer.getCanonicalURL(baseUrl, href);
				if (url != null) {
					link.setUrl(url.toExternalForm());
					link.setParent_url(page.getUrl());
					link.setSuffix(MIMEFormater.JudgeURLFormat(link.getUrl()));
					links.add(link);
					urlCount++;
					if (urlCount > MAX_OUT_LINKS) {
						break;
					}	
				}				
			}
		}
		
		//reEncode by page contentType
		if(!encode.equals(linkExtractor.getCharset())){
			encode = linkExtractor.getCharset();
			if(encode==null){
				encode = DEFAULT_ENCODING;
			}
			if(!encode.equalsIgnoreCase("GBK")&&!encode.equalsIgnoreCase("GB2312")&&!encode.equalsIgnoreCase("ISO-8859-1")){
				encode = DEFAULT_ENCODING;
			}
			page.setEncode(encode);
			try {
				chars = new String(page.getContent(),encode).toCharArray();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//extract content and title
		bulletParser.setCallback(textExtractor);
		bulletParser.parse(chars);
		String title = textExtractor.title.toString().trim();
		String text = textExtractor.text.toString().trim();
		String lang = langIdentifier.identify(text);
		long fingerPrint = FingerPrinter.getLongCode(text);
		
		if(page.getTitle()==null){
			if(title == null || title.length()==0){
				page.setTitle(page.getAnchorText());
			}else{
				page.setTitle(title);
			}
		}
		page.setParseText(text);
		page.setLang(lang);
		page.setFingerPrint(fingerPrint);
		
		return links;
	}

}
