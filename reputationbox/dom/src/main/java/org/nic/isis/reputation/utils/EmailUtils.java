package org.nic.isis.reputation.utils;

import edu.ucla.sspace.text.EnglishStemmer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;

import org.nic.isis.reputation.dom.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmailUtils {

	private static final Map<String, String> HTML_CODES = new HashMap<String, String>();
	private static final Map<String, String> LATIN1_CODES = new HashMap<String, String>();
	private final static Logger logger = LoggerFactory
			.getLogger(EmailUtils.class);

	private static Scanner scanner;

	static {
		HTML_CODES.put("&nbsp;", " ");
		HTML_CODES.put("&Agrave;", "À");
		HTML_CODES.put("&Aacute;", "Á");
		HTML_CODES.put("&Acirc;", "Â");
		HTML_CODES.put("&Atilde;", "Ã");
		HTML_CODES.put("&Auml;", "Ä");
		HTML_CODES.put("&Aring;", "Å");
		HTML_CODES.put("&AElig;", "Æ");
		HTML_CODES.put("&Ccedil;", "Ç");
		HTML_CODES.put("&Egrave;", "È");
		HTML_CODES.put("&Eacute;", "É");
		HTML_CODES.put("&Ecirc;", "Ê");
		HTML_CODES.put("&Euml;", "Ë");
		HTML_CODES.put("&Igrave;", "Ì");
		HTML_CODES.put("&Iacute;", "Í");
		HTML_CODES.put("&Icirc;", "Î");
		HTML_CODES.put("&Iuml;", "Ï");
		HTML_CODES.put("&ETH;", "Ð");
		HTML_CODES.put("&Ntilde;", "Ñ");
		HTML_CODES.put("&Ograve;", "Ò");
		HTML_CODES.put("&Oacute;", "Ó");
		HTML_CODES.put("&Ocirc;", "Ô");
		HTML_CODES.put("&Otilde;", "Õ");
		HTML_CODES.put("&Ouml;", "Ö");
		HTML_CODES.put("&Oslash;", "Ø");
		HTML_CODES.put("&Ugrave;", "Ù");
		HTML_CODES.put("&Uacute;", "Ú");
		HTML_CODES.put("&Ucirc;", "Û");
		HTML_CODES.put("&Uuml;", "Ü");
		HTML_CODES.put("&Yacute;", "Ý");
		HTML_CODES.put("&THORN;", "Þ");
		HTML_CODES.put("&szlig;", "ß");
		HTML_CODES.put("&agrave;", "à");
		HTML_CODES.put("&aacute;", "á");
		HTML_CODES.put("&acirc;", "â");
		HTML_CODES.put("&atilde;", "ã");
		HTML_CODES.put("&auml;", "ä");
		HTML_CODES.put("&aring;", "å");
		HTML_CODES.put("&aelig;", "æ");
		HTML_CODES.put("&ccedil;", "ç");
		HTML_CODES.put("&egrave;", "è");
		HTML_CODES.put("&eacute;", "é");
		HTML_CODES.put("&ecirc;", "ê");
		HTML_CODES.put("&euml;", "ë");
		HTML_CODES.put("&igrave;", "ì");
		HTML_CODES.put("&iacute;", "í");
		HTML_CODES.put("&icirc;", "î");
		HTML_CODES.put("&iuml;", "ï");
		HTML_CODES.put("&eth;", "ð");
		HTML_CODES.put("&ntilde;", "ñ");
		HTML_CODES.put("&ograve;", "ò");
		HTML_CODES.put("&oacute;", "ó");
		HTML_CODES.put("&ocirc;", "ô");
		HTML_CODES.put("&otilde;", "õ");
		HTML_CODES.put("&ouml;", "ö");
		HTML_CODES.put("&oslash;", "ø");
		HTML_CODES.put("&ugrave;", "ù");
		HTML_CODES.put("&uacute;", "ú");
		HTML_CODES.put("&ucirc;", "û");
		HTML_CODES.put("&uuml;", "ü");
		HTML_CODES.put("&yacute;", "ý");
		HTML_CODES.put("&thorn;", "þ");
		HTML_CODES.put("&yuml;", "ÿ");
		HTML_CODES.put("&lt;", "<");
		HTML_CODES.put("&gt;", ">");
		HTML_CODES.put("&quot;", "\"");
		HTML_CODES.put("&amp;", "&");

		LATIN1_CODES.put("&#039;", "'");
		LATIN1_CODES.put("&#160;", " ");
		LATIN1_CODES.put("&#162;", "¢");
		LATIN1_CODES.put("&#164;", "¤");
		LATIN1_CODES.put("&#166;", "¦");
		LATIN1_CODES.put("&#168;", "¨");
		LATIN1_CODES.put("&#170;", "ª");
		LATIN1_CODES.put("&#172;", "¬");
		LATIN1_CODES.put("&#174;", "®");
		LATIN1_CODES.put("&#176;", "°");
		LATIN1_CODES.put("&#178;", "²");
		LATIN1_CODES.put("&#180;", "´");
		LATIN1_CODES.put("&#182;", "¶");
		LATIN1_CODES.put("&#184;", "¸");
		LATIN1_CODES.put("&#186;", "º");
		LATIN1_CODES.put("&#188;", "¼");
		LATIN1_CODES.put("&#190;", "¾");
		LATIN1_CODES.put("&#192;", "À");
		LATIN1_CODES.put("&#194;", "Â");
		LATIN1_CODES.put("&#196;", "Ä");
		LATIN1_CODES.put("&#198;", "Æ");
		LATIN1_CODES.put("&#200;", "È");
		LATIN1_CODES.put("&#202;", "Ê");
		LATIN1_CODES.put("&#204;", "Ì");
		LATIN1_CODES.put("&#206;", "Î");
		LATIN1_CODES.put("&#208;", "Ð");
		LATIN1_CODES.put("&#210;", "Ò");
		LATIN1_CODES.put("&#212;", "Ô");
		LATIN1_CODES.put("&#214;", "Ö");
		LATIN1_CODES.put("&#216;", "Ø");
		LATIN1_CODES.put("&#218;", "Ú");
		LATIN1_CODES.put("&#220;", "Ü");
		LATIN1_CODES.put("&#222;", "Þ");
		LATIN1_CODES.put("&#224;", "à");
		LATIN1_CODES.put("&#226;", "â");
		LATIN1_CODES.put("&#228;", "ä");
		LATIN1_CODES.put("&#230;", "æ");
		LATIN1_CODES.put("&#232;", "è");
		LATIN1_CODES.put("&#234;", "ê");
		LATIN1_CODES.put("&#236;", "ì");
		LATIN1_CODES.put("&#238;", "î");
		LATIN1_CODES.put("&#240;", "ð");
		LATIN1_CODES.put("&#242;", "ò");
		LATIN1_CODES.put("&#244;", "ô");
		LATIN1_CODES.put("&#246;", "ö");
		LATIN1_CODES.put("&#248;", "ø");
		LATIN1_CODES.put("&#250;", "ú");
		LATIN1_CODES.put("&#252;", "ü");
		LATIN1_CODES.put("&#254;", "þ");
		LATIN1_CODES.put("&#34;", "\"");
		LATIN1_CODES.put("&#38;", "&");
		LATIN1_CODES.put("&#8217;", "'");
	}

	/**
	 * Preprocesses the text
	 * 
	 * @param text
	 *            a document to process
	 * @return a pre processed version of the textcontent
	 */
	public static TextContent processText(String text) {

		String processedText = "";
		TextContent textContent = new TextContent();
		Map<String, Integer> wordFrequenceMap = new HashMap<String, Integer>();
		Map<String, Integer> urlFrequenceMap = new HashMap<String, Integer>();
		Map<String, Integer> numbersFrequenceMap = new HashMap<String, Integer>();
		Map<String, Integer> emailsFrequenceMap = new HashMap<String, Integer>();
		Map<String, Integer> emoticonsFrequenceMap = new HashMap<String, Integer>();

		StopwordFilter stopwordFilter = new StopwordFilter();
		//logger.info("processing text : \n" + text);
		try {
			// Replace any HTML-encoded elements
			text = unescapeHTML(text);
			// Removing HTML tags
			text = text.replaceAll("<.*?>", "");
			text = text.toLowerCase();

			scanner = new Scanner(text);
			StringTokenizer st = null;
			StringBuilder passedLine = null;
			String wordText = "";
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				// no quoted texts in the line
				if (!line.startsWith(">")) {
					st = new StringTokenizer(line);
					// urls, emoticons, numbers in analysis stage
					passedLine = new StringBuilder(line.length());
					while (st.hasMoreTokens()) {
						String tok = st.nextToken();
						if (tok.endsWith("?")) {
							passedLine.append(
									tok.substring(0, tok.length() - 1)).append(
									" ?");
						} else if (tok.endsWith(",")) {
							passedLine.append(
									tok.substring(0, tok.length() - 1)).append(
									" ,");
						} else if (tok.endsWith(".")) {
							passedLine.append(
									tok.substring(0, tok.length() - 1)).append(
									" .");
						} else if (tok.contains("@") && tok.contains(".")) {
							// assume it's an email address
							addTokenToMap(emailsFrequenceMap, tok);
						} else if (tok.startsWith("http")
								|| tok.startsWith("ftp")) {
							addTokenToMap(urlFrequenceMap, tok);
						} else if (tok.matches("[0-9]+")) {
							addTokenToMap(numbersFrequenceMap, tok);
						}
						// basic emotions
						else if ((tok.length() == 2 || tok.length() == 3)
								&& (tok.equals(":)") || tok.equals(":(")
										|| tok.equals(":/")
										|| tok.equals(":\\")
										|| tok.equals(":|") || tok.equals(":[")
										|| tok.equals(":]") || tok.equals(":X")
										|| tok.equals(":|") || tok.equals(":[")
										|| tok.equals(":]") || tok.equals(":X") || tok
											.equals(":D"))) {
							addTokenToMap(emoticonsFrequenceMap, tok);
						} else {
							// checking if it's a stopword
							if (!stopwordFilter.isStopword(tok)) {
								passedLine.append(tok);
							}
						}
						passedLine.append(" ");
					}
					wordText = wordText.concat(" " + passedLine.toString());
				}
			}
			// Discard any characters that are not accepted as tokens.
			//wordText = wordText.replaceAll("[^\\w\\s;:\\(\\)\\[\\]'!/&?\",\\.<>]", "");
			wordText = wordText.replaceAll("\\W"," ");
			
			// stemming actual words using English Stemmer	
			st = new StringTokenizer(wordText);
			EnglishStemmer stemmer = new EnglishStemmer();
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				String stemmedToken = stemmer.stem(token);
				processedText = processedText + " " + stemmedToken;
				//wordFrequenceMap = addTokenToMap(wordFrequenceMap, stemmedToken);
			}

			textContent.setTokenStream(processedText);
			/*textContent.setStringTokens(wordFrequenceMap);
			textContent.setEmails(emailsFrequenceMap);
			textContent.setEmoticons(emoticonsFrequenceMap);
			textContent.setNumbers(numbersFrequenceMap);
			textContent.setUrls(urlFrequenceMap);*/
		} catch (Exception ex) {
			logger.error("error processing email text", ex);
		}
		
		return textContent;
	}

	/**
	 * Returns the provided string where all HTML special characters have been
	 * replaced with their utf8 equivalents.
	 * 
	 * @param source
	 *            a String possibly containing escaped HTML characters
	 */
	public static String unescapeHTML(String source) {

		StringBuilder sb = new StringBuilder(source.length());

		// position markers for the & and ;
		int start = -1, end = -1;

		// the end position of the last escaped HTML character
		int last = 0;

		start = source.indexOf("&");
		end = source.indexOf(";", start);

		while (start > -1 && end > start) {
			String encoded = source.substring(start, end + 1);
			String decoded = HTML_CODES.get(encoded);

			// if encoded form wasn't in the HTML codes, try checking to see if
			// it was a Latin-1 code
			if (decoded == null) {
				decoded = LATIN1_CODES.get(encoded);
			}

			if (decoded != null) {
				// append the string containing all characters from the last
				// escaped
				// character to the current one
				String s = source.substring(last, start);
				sb.append(s).append(decoded);
				last = end + 1;
			}

			start = source.indexOf("&", end);
			end = source.indexOf(";", start);
		}
		// if there weren't any substitutions, don't bother to create a new
		// String
		if (sb.length() == 0)
			return source;

		// otherwise finish the substitution by appending all the text from the
		// last substitution until the end of the string
		sb.append(source.substring(last));
		return sb.toString();
	}

	private static Map<String, Integer> addTokenToMap(Map<String, Integer> map,
			String token) {
		if (map.containsKey(token)) {
			Integer tokenCount = (Integer) map.get(token);
			tokenCount = tokenCount + 1;
			map.put(token, tokenCount);
		} else {
			map.put(token, new Integer(1));
		}
		return map;
	}

}
