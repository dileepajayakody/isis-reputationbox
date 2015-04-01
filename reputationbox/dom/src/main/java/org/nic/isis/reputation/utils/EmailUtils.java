package org.nic.isis.reputation.utils;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.text.EnglishStemmer;
import jangada.ReplyToAnnotator;
import jangada.SigFilePredictor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Flags.Flag;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.ArrayUtils;
import org.apache.isis.applib.annotation.Programmatic;
import org.jfree.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nic.isis.clustering.EmailCluster;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailRecipientCluster;
import org.nic.isis.clustering.EmailWeightedSubjectBodyContentCluster;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailBody;
import org.nic.isis.reputation.dom.EmailReputationDataModel;
import org.nic.isis.reputation.dom.TextContent;
import org.nic.isis.reputation.dom.TextTokenMap;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ciranda.SpeechAct;

public final class EmailUtils {

	private static final Map<String, String> HTML_CODES = new HashMap<String, String>();
	private static final Map<String, String> LATIN1_CODES = new HashMap<String, String>();
	private final static Logger logger = LoggerFactory
			.getLogger(EmailUtils.class);

	private static Scanner scanner;
	private static ReplyToAnnotator replyExtractor = new ReplyToAnnotator();
	private static SigFilePredictor signatureDetector = new SigFilePredictor();
	private static SpeechAct speechAct = new SpeechAct();
	
	private static StanfordCoreNLP pipeline;

	static {
		Properties props = new Properties(); 
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
		pipeline = new StanfordCoreNLP(props);
		
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

	public static String getEmailAddressString(String fullAddressString) {
		// logger.info("processing email address:  " + fullAddressString);
		String address = fullAddressString;

		if (fullAddressString.contains("<") && fullAddressString.contains(">")) {
			int beginIndex = fullAddressString.indexOf("<");
			int endIndex = fullAddressString.indexOf(">");
			address = fullAddressString.substring(beginIndex + 1, endIndex);
		}
		return address;
	}

	/**
	 * cleans text by removing all html tags
	 * 
	 * @param text
	 * @return
	 */
	public static String removeHtmlTags(String text) {
		String processedText = "";
		// Replace any HTML-encoded elements
		processedText = unescapeHTML(text);
		// Removing HTML tags
		processedText = processedText.replaceAll("<.*?>", "");

		return processedText;
	}

	/**
	 * gets the text content of the message part
	 * 
	 * @param p
	 * @return
	 * @throws MessagingException
	 * @throws IOException
	 */
	public static EmailBody getEmailBody(Part p, EmailBody body)
			throws MessagingException, IOException {
		//System.out.println("Inside getEmailBody method...........");

		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			// textIsHtml = p.isMimeType("text/html");
			body.setMessageContent(s);
			System.out.println("the content is a text/* returning body");
			return body;
		} else if (p.isMimeType("image/*")) {
			body.setHasImages(true);
			body.setNoOfImages(body.getNoOfImages() + 1);
			System.out.println("the content is a image/* adding to the no.of images and returning body");			
			return body;
		} else if (p.isMimeType("application/*")) {
			body.setHasAttachments(true);
			body.setNoOfAttachments(body.getNoOfAttachments() + 1);
			System.out.println("the content is a application/* adding no.of attachments and returning body");
			
			return body;
		} else if (p.isMimeType("multipart/alternative")) {
			// prefer html text over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = "";
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);

				if (bp.isMimeType("text/plain")) {
					// if (text == null){
					// if there is a text/plain part in alternative part's body
					// process only that and disregard the html part
					// System.out.println("processing the text/plain part of the multipart/alternative email");
					String s = (String) bp.getContent();
					text += "\n" + s;
					System.out.println("the content is a multipart/alternative"
							+ "  this body part + " + i + "is text/plain"
									+ " adding to the text field and continuing");
					
					// System.out.println(" the text : " + text);
					// textIsHtml = p.isMimeType("text/html");
					// body.setMessageContent(s);
					// return body;
					continue;
					// }
				}
				// else {
				// return getEmailBody(bp, body);
				// }

				// else if (bp.isMimeType("text/html")) {
				// body.setHasHtml(true);
				// //don't process the text/html part in an
				// multipart/alternative message
				// //body = getEmailBody(bp,body);
				// continue;
				// }
			}
			body.setMessageContent(text);
			return body;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			System.out.println("this is a multipart/mixed email with parts : "
			 + mp.getCount());
			for (int i = 0; i < mp.getCount(); i++) {
				System.out.println("processing part : " + i +
				 " content type of the part: " + mp.getContentType());
				body = getEmailBody(mp.getBodyPart(i), body);
				if (body.getMessageContent() != null) {
					System.out.println("returning body since message content is there");
					return body;
				}
			}
			return body;
		}
		return null;
	}

	/**
	 * gets the text content of the message part
	 * 
	 * @param p
	 * @return
	 * @throws MessagingException
	 * @throws IOException
	 */
	public static String getTextFromEmail(Part p) throws MessagingException,
			IOException {

		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			// textIsHtml = p.isMimeType("text/html");
			return s;
		}

		if (p.isMimeType("multipart/alternative")) {
			// prefer html text over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount(); i++) {

				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getTextFromEmail(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					String s = getTextFromEmail(bp);
					if (s != null)
						return s;
				} else {
					return getTextFromEmail(bp);
				}
			}
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getTextFromEmail(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}

		return null;
	}

	/**
	 * Preprocesses the text content
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
		
		// logger.info("processing text : \n" + text);
		try {

			text = removeHtmlTags(text);
			scanner = new Scanner(text);
			StringTokenizer st = null;
			StringBuilder passedLine = null;
			String wordText = "";

			// String numberRegex = "[0-9]+";
			String numberRegex = "((-|\\+)?[0-9]+(\\.[0-9]+)?)+";

			int tokenNumber = 0;

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				// no quoted texts in the line
				if (!line.startsWith(">")) {
					st = new StringTokenizer(line);
					// urls, emoticons, numbers in analysis stage
					passedLine = new StringBuilder(line.length());
					while (st.hasMoreTokens()) {
						tokenNumber++;

						String tok = st.nextToken();
						// word should be more than one char
						if (tok.length() > 1) {
							if (tok.endsWith("?")) {
								passedLine.append(
										tok.substring(0, tok.length() - 1))
										.append(" ?");
							} else if (tok.endsWith(",")) {
								passedLine.append(
										tok.substring(0, tok.length() - 1))
										.append(" ,");
							} else if (tok.endsWith(".")) {
								passedLine.append(
										tok.substring(0, tok.length() - 1))
										.append(" .");
							} else if (tok.contains("@") && tok.contains(".")) {
								// assume it's an email address
								passedLine.append(tok);
								addTokenToMap(emailsFrequenceMap, tok);
							} else if (tok.startsWith("http://")
									|| tok.startsWith("ftp://")) {
								passedLine.append(tok);
								addTokenToMap(urlFrequenceMap, tok);
							} else if (tok.matches(numberRegex)) {
								addTokenToMap(numbersFrequenceMap, tok);
							}
							// basic emotions
							else if ((tok.length() == 2 || tok.length() == 3)
									&& (tok.equals(":)") || tok.equals(":(")
											|| tok.equals(":/")
											|| tok.equals(":\\")
											|| tok.equals(":|")
											|| tok.equals(":[")
											|| tok.equals(":]")
											|| tok.equals(":X")
											|| tok.equals(":|")
											|| tok.equals(":[")
											|| tok.equals(":]")
											|| tok.equals(":X") || tok
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

					}
					wordText = wordText.concat(" " + passedLine.toString());
				}
			}
			// Discard any characters that are not accepted as tokens.
			// wordText =
			// wordText.replaceAll("[^\\w\\s;:\\(\\)\\[\\]'!/&?\",\\.<>]", "");
			wordText = wordText.replaceAll("\\W", " ");

			// stemming actual words using English Stemmer
			st = new StringTokenizer(wordText);
			EnglishStemmer stemmer = new EnglishStemmer();

			int processedTokens = 0;
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if(token.length() > 1 || token.equalsIgnoreCase("i")){
					//not adding tokens with length 1
					String stemmedToken = stemmer.stem(token);
					processedText = processedText + " " + stemmedToken;
					wordFrequenceMap = addTokenToMap(wordFrequenceMap, stemmedToken);
					processedTokens++;	
				}
			}

			// also needs to lemmatize text...

			textContent.setTokenStream(processedText);
			// setting wordFrequncy as a wrapped map object
			// TextTokenMap textTokenMap = new TextTokenMap();
			// textTokenMap.setWordFrequencyMap(wordFrequenceMap);
			// textContent.setTextTokenMap(textTokenMap);
			textContent.setStringTokens(wordFrequenceMap);

			textContent.setEmailTokens(emailsFrequenceMap);
			textContent.setEmoticonTokens(emoticonsFrequenceMap);
			textContent.setNumberTokens(numbersFrequenceMap);
			textContent.setUrlTokens(urlFrequenceMap);

			// logger.info(" original token number : " + tokenNumber +
			// "  processed token number : " + processedTokens);
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
			map.put(token, 1);
		}
		return map;
	}

	/**
	 * normalize the count of a particular word based on total number of words
	 * 
	 * @param wordCount
	 * @param totalWords
	 * @return
	 */
	public static double getNormalizedFrequency(int wordCount, int totalWords) {
		double normalizedFrequency = ((double) wordCount / (double) totalWords) * 100;
		return normalizedFrequency;
	}

	/**
	 * returns the timestamp of the date prior to the given time period (in
	 * months) from current date
	 * 
	 * @param months
	 * @return
	 */
	public static long getMonthsBeforeDateTimeStamp(int months) {
		Date today = new Date();
		long todayTime = today.getTime() / 1000;
		logger.info("Today's date timestamp : " + todayTime);
		long monthsTime = months * 2628000;
		long dateMonthAgo = todayTime - monthsTime;
		return dateMonthAgo;
	}

	public static long getTodayTimestamp() {
		Date today = new Date();
		// have to devide by 1000 to match timestamp format
		long todayTime = today.getTime() / 1000;
		return todayTime;
	}

	public static void sendReputationResults(String emailId, List<Email> emails) {
		JSONObject root = new JSONObject();

		JSONArray resultsArray = new JSONArray();

		for (Email email : emails) {
			JSONObject reputationObj = new JSONObject();
			reputationObj.put("id", email.getMessageId());
			reputationObj.put("uid", email.getMsgUid());
			reputationObj
					.put("contentscore", email.getContentReputationScore());
			reputationObj.put("peoplescore",
					email.getRecipientReputationScore());
			reputationObj.put("contentclusterid", email.getTextClusterId());
			reputationObj.put("peopleclusterid", email.getPeopleClusterId());
			
			//adding new fields for speech acts and nlp fields/keywords
			String intentString = getMessageIntentString(email);
			reputationObj.put("emailintent",intentString);
			reputationObj.put("nlpkeywords", getNLPKeywordsFromEmail(email));
			double totalReplyScore = email.getRepliedPeoplescore() + email.getRepliedTopicscore()
									+ email.getRepliedKeywordscore();
			reputationObj.put("replyscore", totalReplyScore);
			double totalFlaggedScore = email.getFlaggedPeoplescore() + email.getFlaggedTopicscore()
					+ email.getFlaggedKeywordscore();
		
			reputationObj.put("flagscore", totalFlaggedScore);
			
			double totalSeenScore = email.getSeenPeoplescore() + email.getSeenTopicscore()
					+ email.getSeenKeywordscore();
			reputationObj.put("seescore", totalSeenScore);
			logger.info("the scores in results email to be sent :  uid : " + email.getMsgUid() + " contentscore : " + email.getContentReputationScore() 
					+ " people score : " + email.getPeopleClusterId() + " content cluster : " + email.getTextClusterId() + " people cluster : " + email.getPeopleClusterId()
					+  " email intent : " + intentString + " reply-score : " + totalReplyScore + " flag score : " + totalFlaggedScore + " see score: " + totalSeenScore);
			
			resultsArray.put(reputationObj);
		}
		root.put("reputation", resultsArray);

		String jsonString = root.toString();

		// needs to print the results for analysis

		sendMessage(emailId, jsonString);

	}

	public static String getMessageIntentString(Email email){
		String intentString = "";
		if(email.isCommit()){
			intentString += "commitment,";
		}
		if(email.isDelivery()){
			intentString += "delivery,";
		}
		if(email.isMeeting()){
			intentString += "meeting,";
		}
		if(email.isRequest()){
			intentString += "request,";
		}

		//for list emails set email intent as list
		if(email.isListMail()){
			intentString += "forum,";
		}
		if(intentString.length() > 1){
			intentString = intentString.substring(0, (intentString.length()-1));
		}
		return intentString;
		
	}
	
	public static String getNLPKeywordsFromEmail(Email email){
		String keywords = "";
		if(email.getKeywords() != null){
			for(String keyword : email.getKeywords()){
				keywords += keyword+",";
			}
		}
//
//		if(email.getPersons() != null){
//			for(String person: email.getPersons()){
//				keywords += person+",";
//			}
//		}
//		if(email.getLocations() != null){
//			for(String location : email.getLocations()){
//				keywords += location+",";
//			}
//		}
//		if(email.getOrganizations() != null){
//			for(String org : email.getOrganizations()){
//				keywords += org+",";
//			}
//		}
		if(keywords.length() > 1){
			keywords = keywords.substring(0, (keywords.length()-1));
		}
		return keywords;
	}


	/**
	 * processes the JavaMail message object and returns a Email object
	 * 
	 * @param msg
	 * @param mailBoxId
	 * @param uid
	 * @return
	 * @throws MessagingException
	 * @throws IOException
	 */
	public static Email processEmail(Message msg, String mailBoxId, long uid)
			throws MessagingException, IOException {

		Address[] from = msg.getFrom();
		String fromAddress = null;
		if (from != null && from.length > 0) {
			fromAddress = EmailUtils.getEmailAddressString(from[0].toString());

		}
		Email newEmail = new Email();
		newEmail.setMailboxId(mailBoxId);

		String[] msgIds = msg.getHeader("Message-ID");
		long msgUID = uid;
		String msgId = msgIds[0];
		newEmail.setMessageId(msgId);
		newEmail.setMsgUid(msgUID);

		long sentTimeStamp = msg.getSentDate().getTime();
		newEmail.setSentTimestamp(sentTimeStamp);
		newEmail.setFromAddress(fromAddress);
	

		Address[] toRecipients = msg.getRecipients(Message.RecipientType.TO);
		Address[] ccRecipients = msg.getRecipients(Message.RecipientType.CC);
		Address[] bccRecipients = msg.getRecipients(Message.RecipientType.BCC);

		logger.info("-----------------------------------------------------------------------------------------------------");
		logger.info("Processing email : " + msgId + " uid : " + msgUID
				+ " subject : " + msg.getSubject());

		List<String> toAddressList = new ArrayList<String>();
		if (toRecipients != null) {
			for (Address address : toRecipients) {
				String toAddress = EmailUtils.getEmailAddressString(address
						.toString());
				toAddressList.add(toAddress);
				if (toAddress.equalsIgnoreCase(mailBoxId)) {
					// logger.info(" this email is sent direct to user");
					newEmail.setDirect(true);
				}

			}
			newEmail.setToAddresses(toAddressList);
		}
		List<String> ccAddressList = new ArrayList<String>();

		if (ccRecipients != null) {
			// logger.info("\n\n HAVE CC addresses...");
			for (Address address : ccRecipients) {
				String ccAddress = EmailUtils.getEmailAddressString(address
						.toString());
				ccAddressList.add(ccAddress);
				if (ccAddress.equalsIgnoreCase(mailBoxId)) {
					// logger.info(" this email is CCd to user");
					newEmail.setCCd(true);
				}

			}
			newEmail.setCcAddresses(ccAddressList);
		}

		List<String> bccAddressList = new ArrayList<String>();

		if (bccRecipients != null) {
			// logger.info("\n\n HAVE BCC addresses...");
			for (Address address : bccRecipients) {
				String bccAddress = EmailUtils.getEmailAddressString(address
						.toString());
				bccAddressList.add(bccAddress);
				if (bccAddress.equalsIgnoreCase(mailBoxId)) {
					// logger.info(" this email is CCd to user");
					newEmail.setBCCd(true);
				}
			}
			newEmail.setBccAddresses(bccAddressList);
		}

		String[] listId = msg.getHeader("List-Id");
		if (listId != null && listId.length > 0) {
			String listAddress = listId[0];
			if (listAddress.startsWith("<") && listAddress.endsWith(">")) {
				listAddress.substring(1, listAddress.length() - 2);
			}
			if(listAddress.length() < 255){
				newEmail.setListAddress(listAddress);	
			} else {
				newEmail.setListAddress(listAddress.substring(0, 254));
			}
			newEmail.setListMail(true);

		} else {
			String[] listHelps = msg.getHeader("List-Help");
			if (listHelps != null && listHelps.length > 0) {
				String listAddress = listHelps[0];
				if (listAddress.startsWith("<") && listAddress.endsWith(">")) {
					listAddress.substring(1, listAddress.length() - 2);
				}
				if(listAddress.length() < 255){
					newEmail.setListAddress(listAddress);	
				} else {
					newEmail.setListAddress(listAddress.substring(0, 254));
				}
				newEmail.setListMail(true);

			} else {
				String[] listSubscribe = msg.getHeader("List-Subscribe");
				if (listSubscribe != null && listSubscribe.length > 0) {
					String listAddress = listSubscribe[0];
					if (listAddress.startsWith("<")
							&& listAddress.endsWith(">")) {
						listAddress.substring(1, listAddress.length() - 2);
					}
					if(listAddress.length() < 255){
						newEmail.setListAddress(listAddress);	
					} else {
						newEmail.setListAddress(listAddress.substring(0, 254));
					}
					newEmail.setListMail(true);

				}else {
					String[] listUnSubscribe = msg.getHeader("List-Unsubscribe");
					if (listUnSubscribe != null && listUnSubscribe.length > 0) {
						String listAddress = listUnSubscribe[0];
						if (listAddress.startsWith("<")
								&& listAddress.endsWith(">")) {
							listAddress.substring(1, listAddress.length() - 2);
						}
						if(listAddress.length() < 255){
							newEmail.setListAddress(listAddress);	
						} else {
							newEmail.setListAddress(listAddress.substring(0, 254));
						}
						newEmail.setListMail(true);

					}
				}
			} 
		}

		// processing spam related headers
		String[] spamheader = msg.getHeader("X-Spam-Flag");
		if (spamheader != null && spamheader.length > 0) {
			String spamFlag = spamheader[0];
			if (spamFlag.equalsIgnoreCase("yes")) {
				newEmail.setSpam(true);
			}
		} else {
			String[] spamscore = msg.getHeader("X-Spam-Score");
			if (spamscore != null && spamscore.length > 0) {
				try {
					String spamScore = spamscore[0];
					spamScore = spamScore.replaceAll("[^-?0-9]+", " ");
					String[] numbers = spamScore.trim().split(" ");
					logger.info(" Has spam score header : " + spamScore);
					double score = Double.parseDouble(numbers[0]);
					if (score > 3) {
						newEmail.setSpam(true);
					}
				} catch (Exception ex) {
					logger.error("Error occured while paring X-Spam-Score "
									+ ex.getMessage());
					ex.printStackTrace();
				}

			}

		}

		// processing email importance headers
		String[] priorityHeaders = msg.getHeader("X-Priority");
		if (priorityHeaders != null && priorityHeaders.length > 0) {
			try{
				String importanceHeader = priorityHeaders[0];
				int priorityLevel = Integer.parseInt(importanceHeader);
				newEmail.setImportanceLevelByHeader(priorityLevel);
				if (priorityLevel < 3) {
					logger.info(" Has priority level header : " + priorityLevel);
					newEmail.setIsImportantByHeader(true);
				}
			}catch(Exception ex){
				logger.error("Error occured while paring X-Spam-Score "
						+ ex.getMessage());
				ex.printStackTrace();
			}
			
		} else {
			// process importance
			String[] importanceHeaders = msg.getHeader("Importance");
			if (importanceHeaders != null && importanceHeaders.length > 0) {
				String importanceHeader = importanceHeaders[0];
				if (importanceHeader.equalsIgnoreCase("high")) {
					logger.info(" Has importance header : " + importanceHeader);
					newEmail.setImportanceLevelByHeader(1);
				}

			}
		}

		String[] sensitivityHeaders = msg.getHeader("Sensitivity");
		if (sensitivityHeaders != null && sensitivityHeaders.length > 0) {
			String sensitivityHeader = sensitivityHeaders[0];
			if (sensitivityHeader.equalsIgnoreCase("personal")
					|| sensitivityHeader.equalsIgnoreCase("private")
					|| sensitivityHeader.contains("confidential")) {
				logger.info(" Has sensitivity header : " + sensitivityHeader);
				newEmail.setSensitiveByHeader(true);
			}
		}

		String[] precedenceHeaders = msg.getHeader("Precedence");
		if (precedenceHeaders != null && precedenceHeaders.length > 0) {
			String precedenceHeader = precedenceHeaders[0];
			newEmail.setPrecedenceLevelByHeader(precedenceHeader);
			if (precedenceHeader.equalsIgnoreCase("bulk")
					|| precedenceHeader.contains("junk")) {
				logger.info(" Has precedence level set for a : " + precedenceHeader);
				newEmail.setImportanceLevelByHeader(5);
				newEmail.setSpam(true);
				
			} else if (precedenceHeader.equalsIgnoreCase("first-class")) {
				newEmail.setImportanceLevelByHeader(1);
			}
		}

		// processing email flags
		if (msg.isSet(Flags.Flag.ANSWERED)) {
			logger.info("This is an answered message");
			newEmail.setAnswered(true);

		}
		//normally all answered emails are also seen; so to distinguish only seen emails use else-if
		else if (msg.isSet(Flags.Flag.SEEN)) {
			logger.info("This is a seen message");
			newEmail.setSeen(true);
		}
		if (msg.isSet(Flags.Flag.FLAGGED)) {
			logger.info("This is a flagged message");
			newEmail.setFlagged(true);
		}
		if (msg.isSet(Flags.Flag.DELETED)) {
			logger.info("This is a deleted message");
			newEmail.setDeleted(true);
		}
		if (msg.isSet(Flags.Flag.USER)) {
			// We don't know what the user flags might be in advance
			// so they're returned as an array of strings
			String[] userFlags = msg.getFlags().getUserFlags();
			List<String> flags = new ArrayList<String>();
			for (int j = 0; j < userFlags.length; j++) {
				logger.info("User flag: " + userFlags[j]);
				flags.add(userFlags[j]);
			}
			newEmail.setUserFlags(flags);

		}

		// processing email subject
		String subject = msg.getSubject();
		if (subject.length() > 255) {
			subject = subject.substring(0, 254);
		}
		//processing nlp
		logger.info("processing NLP results for subject..");
		subject = subject.toLowerCase();
		newEmail = getNLPResults(subject, newEmail);
		TextContent subjectTextContent = EmailUtils.processText(subject);
		newEmail.setSubject(subject);
		newEmail.setSubjectContent(subjectTextContent);

		// processing email body
		String contentType = msg.getContentType();
		newEmail.setContentType(contentType);
		logger.info("The content type of the message : " + contentType);

		TextContent bodyTextContent = null;

		EmailBody emailBody = EmailUtils.getEmailBody(msg, new EmailBody());
		// String mailContent = EmailUtils.getTextFromEmail(msg);
		// EmailBody emailBody = new EmailBody();
		// emailBody.setMessageContent(mailContent);

		String messageText = emailBody.getMessageContent();
		String cleanedText = EmailUtils.removeHtmlTags(messageText);
		newEmail.setNoOfAttachments(emailBody.getNoOfAttachments());
		newEmail.setNoOfImages(emailBody.getNoOfImages());
		// set attributes to the email from email-body
		// System.out.println("Raw mail body content from getEmailBody method : \n"
		// + cleanedText);
		// logger.info("\n cleaned text : \n" + cleanedText);
		if (cleanedText != null && !cleanedText.equals("")) {
			String msgWithoutQuotes = replyExtractor
					.deleteReplyLinesFromMsg(cleanedText);
			//removing signatures from list emails..coz of sheer load
			//but this part may take time to execute
			if(newEmail.isListMail()){
				msgWithoutQuotes =
				signatureDetector.getMsgWithoutSignatureLines(msgWithoutQuotes);	
			}
			cleanedText = msgWithoutQuotes;
		}
		//processing nlp

		logger.info("processing NLP results for email body.");
		cleanedText = cleanedText.toLowerCase();
		newEmail = getNLPResults(cleanedText, newEmail);
				

		// System.out.println("message body after processing quoted text and signautre : \n"
		// + cleanedText);

		// getting the speech-act features, they are only important if the email
		// is directly sent or ccd and not sent on a bulk
		double[] speechActVector = new double[5];
		if (newEmail.isDirect() || newEmail.isCCd()) {
			speechAct.loadMessage(msg.getSubject() + "\n " + cleanedText);
			boolean isCommit = speechAct.hasCommit();
			boolean hasDData = speechAct.hasDdata();
			boolean isDelivery = speechAct.hasDeliver();
			boolean isMeeting = speechAct.hasMeet();
			boolean isProposal = speechAct.hasPropose();
			boolean isRequest = speechAct.hasRequest();

			newEmail.setMeeting(isMeeting);
			newEmail.setCommit(isCommit);
			newEmail.setDelivery(isDelivery);
			newEmail.setPropose(isProposal);
			newEmail.setRequest(isRequest);
			
			if (isCommit) {
				logger.info("Speech act identified commit");				
				speechActVector[0] = 1;
			}
			if (isDelivery) {
				logger.info("Speech act identified delivery");
				speechActVector[1] = 1;
			}
			if (isMeeting) {
				logger.info("Speech act identified meeting");
				speechActVector[2] = 1;
			}
			if (isProposal) {
				logger.info("Speech act identified proposal");
				speechActVector[3] = 1;
			}
			if (isRequest) {
				logger.info("Speech act identified request");
				speechActVector[4] = 1;
			}
		}
		
		newEmail.setSpeechActVector(speechActVector);

		bodyTextContent = EmailUtils.processText(cleanedText);
		logger.info("\n After tokenizing, stemming content. tokenStream : \n" + bodyTextContent.getTokenStream());
		newEmail.setBodyContent(bodyTextContent);

		// Flags flags = msg.getFlags();
		// Flag[] allFlags = flags.getSystemFlags();
		// logger.info("Flags count : " + allFlags.length);
		// for (Flag f : allFlags) {
		//
		// if (f == Flags.Flag.DELETED) {
		// logger.info("This is a deleted message");
		// newEmail.setDeleted(true);
		// } else if (f == Flags.Flag.SEEN) {
		// logger.info("This is a seen message");
		// newEmail.setSeen(true);
		// } else if (f == Flags.Flag.ANSWERED) {
		// logger.info("This is an answered message");
		// newEmail.setAnswered(true);
		// } else if (f == Flags.Flag.FLAGGED) {
		// logger.info("This is a flagged message");
		// newEmail.setFlagged(true);
		// }
		//
		// }

		TextTokenMap textTokenMap = new TextTokenMap();
		if (subjectTextContent != null) {
			textTokenMap
					.populateWordFrequenciesFromTextContent(subjectTextContent);
		}
		if (bodyTextContent != null) {
			textTokenMap
					.populateWordFrequenciesFromTextContent(bodyTextContent);

		}

		newEmail.setWordFrequencyMap(textTokenMap);
		return newEmail;
	}

	/**
	 * combine content vector, recipient vector, speech act vector, list-header,
	 * spam-header,is direct, is ccd, is bccd
	 * 
	 * @param email
	 * @return
	 */
	public static double[] getAllFeatureVector(Email email) {
		// getting similarit of all features
		double[] recipeintFeatureVector = email.getRecipientContextVector();
		double[] contentFeatureVector = email.getTextContextVector();
		// building speech act feature vectors
		boolean isCommit = email.isCommit();
		boolean isDelivery = email.isDelivery();
		boolean isMeeting = email.isMeeting();
		boolean isPropose = email.isPropose();
		boolean isRequest = email.isRequest();

		// commit, delivery, meeting, proposal, request
		double[] speechActVector = new double[5];
		if (isCommit) {
			speechActVector[0] = 1;
		}
		if (isDelivery) {
			speechActVector[1] = 1;
		}
		if (isMeeting) {
			speechActVector[2] = 1;
		}
		if (isPropose) {
			speechActVector[3] = 1;
		}
		if (isRequest) {
			speechActVector[4] = 1;
		}

		// processing extra headers
		boolean isListMail = email.isListMail();
		boolean isDirect = email.isDirect();
		boolean isCCd = email.isCCd();
		boolean isBccd = email.isBCCd();
		boolean isSeen = email.isSeen();
		boolean isReplied = email.isAnswered();
		boolean isFlagged = email.isFlagged();

		int importanceLevel = email.getImportanceLevelByHeader();
		boolean isImportant = false;
		if (importanceLevel < 3) {
			isImportant = true;
		}
		boolean isSensitive = email.isSensitiveByHeader();

		boolean isSpam = email.isSpam();
		boolean isDeleted = email.isDeleted();

		double[] msgStatusFeatureVector = new double[11];
		if (isListMail) {
			msgStatusFeatureVector[0] = 1;
		}
		if (isDirect) {
			msgStatusFeatureVector[1] = 1;
		}
		if (isCCd) {
			msgStatusFeatureVector[2] = 1;
		}
		if (isBccd) {
			msgStatusFeatureVector[3] = 1;
		}
		if (isSeen) {
			msgStatusFeatureVector[4] = 1;
		}
		if (isReplied) {
			msgStatusFeatureVector[5] = 1;
		}
		if (isFlagged) {
			msgStatusFeatureVector[6] = 1;
		}
		if (isImportant) {
			msgStatusFeatureVector[7] = 1;
		}
		if (isSensitive) {
			msgStatusFeatureVector[8] = 1;
		}
		if (isSpam) {
			msgStatusFeatureVector[9] = 1;
		}
		if (isDeleted) {
			msgStatusFeatureVector[10] = 1;
		}

		double[] totalArray = ArrayUtils.addAll(contentFeatureVector,
				recipeintFeatureVector);
		totalArray = ArrayUtils.addAll(totalArray, speechActVector);
		totalArray = ArrayUtils.addAll(totalArray, msgStatusFeatureVector);
		email.setAllFeatureVector(totalArray);
		return email.getAllFeatureVector();
	}

	public static Email getNLPResults(String content, Email email){
        
		try{
			//convert the string to lower case;
			
			//Set<String> persons = new HashSet<String>();
			//Set<String> locations = new HashSet<String>();
			//Set<String> organizations = new HashSet<String>();
			List<String> keywords = new ArrayList<String>();
			EnglishStemmer stemmer = new EnglishStemmer();

			Map<String, Integer> keywordMatrix = new HashMap<String, Integer>();
			if(content.length() > 5000){
				logger.info("content too large to handle for NLP pipeline; ;length : " + content.length()); 
				content = content.substring(0, 5000);
			}
			if(content != null && !content.equals("")){
				Annotation document = new Annotation(content);
			    // run all Annotators on this text
			    pipeline.annotate(document);
			    
			    // these are all the sentences in this document
			    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
			    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			    
			    for(CoreMap sentence: sentences) {
			      // traversing the words in the current sentence
			      // a CoreLabel is a CoreMap with additional token-specific methods
			      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			        // this is the text of the token
			        String word = token.get(TextAnnotation.class);
			        // this is the POS tag of the token
			        String pos = token.get(PartOfSpeechAnnotation.class);
			        // this is the NER label of the token
			        String ne = token.get(NamedEntityTagAnnotation.class);
			        if(pos.equalsIgnoreCase("NN") || pos.equalsIgnoreCase("NNS")){
			        	word = stemmer.stem(word);
			        	keywords.add(word);
			        	if(keywordMatrix.containsKey(word)){
			        		Integer val = keywordMatrix.get(word);
			        		keywordMatrix.put(word, val+1);
			        	}else{
			        		keywordMatrix.put(word, new Integer(1));
			        	}

			        	logger.info("Adding keyword : " + word);
			        	//System.out.println("The word :" + word + " is a " + pos + " and added as a keyword");
			        }

//			        if(ne.equalsIgnoreCase("PERSON")){
//			        	persons.add(word);
//				        System.out.println("NLP word : " + word + " pos : " + pos + " Named entity : " + ne );
//			        } else if(ne.equalsIgnoreCase("LOCATION")){
//			        	locations.add(word);
//				        System.out.println("NLP word : " + word + " pos : " + pos + " Named entity : " + ne );
//			        }else if(ne.equalsIgnoreCase("ORGANIZATION")){
//			        	organizations.add(word);
//				        System.out.println("NLP word : " + word + " pos : " + pos + " Named entity : " + ne );
//			        }
			      }
			    }
			    //email.setPersons(persons);
			    //email.setOrganizations(organizations);
			    //email.setLocations(locations);
			    email.setKeywords(keywords);
			    //for NLP tag analysis using random indexing
			    email.setKeywordMatrix(keywordMatrix);
			    
			}
		}catch(Exception ex){
			logger.error("Error when getting NLP results", ex);
		}
		
	    return email; 
	}


	/**
	 * Sending the reputation results email
	 * 
	 * @param toEmailId
	 * @param messageString
	 */
	public static void sendMessage(String toEmailId, String messageString) {
		final String username = "reputationbox1@gmail.com";
		final String password = "repuboxtest123";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});

		try {

			String emailIdPart = toEmailId.substring(0, toEmailId.indexOf("@"));
			logger.info("Sending the results email to : " + toEmailId);
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("reputationbox1@gmail.com"));
			message.setRecipients(
					Message.RecipientType.TO,
					InternetAddress.parse(emailIdPart
							+ "+reputationbox@gmail.com"));
			// message.setSubject("Reputation Results 3");
			// message.addHeader("TEST_HEADER", "this is a test header");

			message.setSubject("Reputation Results");
			message.setText(messageString);
			Transport.send(message);
			logger.info("Result Mail Sent to " + emailIdPart
					+ "+reputationbox@gmail.com");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static void printModelResults(UserMailBox mailbox){
		EmailReputationDataModel reputationModel = mailbox.getReputationDataModel();
		Set<Email> flaggedDirectEmails = reputationModel.getFlaggedEmails();
		Set<Email> repliedDirectEmails = reputationModel.getRepliedEmails();
		Set<Email> seenDirectEmails = reputationModel.getSeenEmails();
		if(flaggedDirectEmails != null && repliedDirectEmails != null && seenDirectEmails != null){
			logger.info("Number of direct emails flagged : " + flaggedDirectEmails.size()
					+ " replied : " + repliedDirectEmails.size()
					+" seen : " + seenDirectEmails.size());
			
		}
		Map<String, Integer> flaggedDirectMatrix = new HashMap<String, Integer>();
		
		for(Email email : flaggedDirectEmails){
			
			Map<String, Integer> keywords = email.getKeywordMatrix();
			if(keywords != null){
				for(String key: keywords.keySet()){
					if(flaggedDirectMatrix.containsKey(key)){
						Integer freq = flaggedDirectMatrix.get(key);
						Integer newNumber = keywords.get(key);
						flaggedDirectMatrix.put(key, freq + newNumber);
					} else {
						flaggedDirectMatrix.put(key, keywords.get(key));
					}
				}
			}
			
		}
		
		Map<String, Integer> repliedDirectMatrix = new HashMap<String, Integer>();
		
		for(Email email : repliedDirectEmails){
			
			Map<String, Integer> keywords = email.getKeywordMatrix();
			if(keywords != null){
				for(String key: keywords.keySet()){
					if(repliedDirectMatrix.containsKey(key)){
						Integer freq = repliedDirectMatrix.get(key);
						Integer newNumber = keywords.get(key);
						repliedDirectMatrix.put(key, freq + newNumber);
					} else {
						repliedDirectMatrix.put(key, keywords.get(key));
					}
				}
			}
			
			
		}
		
		Map<String, Integer> seenDirectMatrix = new HashMap<String, Integer>();
		
		for(Email email : seenDirectEmails){
			
			Map<String, Integer> keywords = email.getKeywordMatrix();
			if(keywords != null){
				for(String key: keywords.keySet()){
					if(seenDirectMatrix.containsKey(key)){
						Integer freq = seenDirectMatrix.get(key);
						Integer newNumber = keywords.get(key);
						seenDirectMatrix.put(key, freq + newNumber);
					} else {
						seenDirectMatrix.put(key, keywords.get(key));
					}
				}
			}
		}
		
		Set<Email> flaggedListEmails = reputationModel.getFlaggedListEmails();
		Set<Email> repliedListEmails = reputationModel.getRepliedListEmails();
		Set<Email> seenListEmails = reputationModel.getSeenListEmails();
		
		Map<String, Integer> flaggedListMatrix = new HashMap<String, Integer>();
		
		for(Email email : flaggedListEmails){
			
			Map<String, Integer> keywords = email.getKeywordMatrix();
			if(keywords != null){
				for(String key: keywords.keySet()){
					if(flaggedListMatrix.containsKey(key)){
						Integer freq = flaggedListMatrix.get(key);
						Integer newNumber = keywords.get(key);
						flaggedListMatrix.put(key, freq + newNumber);
					} else {
						flaggedListMatrix.put(key, keywords.get(key));
					}
				}
			}
			
			
		}
		
		Map<String, Integer> repliedListMatrix = new HashMap<String, Integer>();
		
		for(Email email : repliedListEmails){
			
			Map<String, Integer> keywords = email.getKeywordMatrix();
			if(keywords != null){
				for(String key: keywords.keySet()){
					if(repliedListMatrix.containsKey(key)){
						Integer freq = repliedListMatrix.get(key);
						Integer newNumber = keywords.get(key);
						repliedListMatrix.put(key, freq + newNumber);
					} else {
						repliedListMatrix.put(key, keywords.get(key));
					}
				}
			}		
		}
		
		Map<String, Integer> seenListMatrix = new HashMap<String, Integer>();
		
		for(Email email : seenListEmails){
			
			Map<String, Integer> keywords = email.getKeywordMatrix();
			if(keywords != null){
				for(String key: keywords.keySet()){
					if(seenListMatrix.containsKey(key)){
						Integer freq = seenListMatrix.get(key);
						Integer newNumber = keywords.get(key);
						seenListMatrix.put(key, freq + newNumber);
					} else {
						seenListMatrix.put(key, keywords.get(key));
					}
				}
			}
			
		}
		
		if(flaggedListEmails != null && repliedListEmails != null && seenListEmails != null){
			logger.info("Number of List emails flagged : " + flaggedListEmails.size()
					+ " replied : " + repliedListEmails.size()
					+" seen : " + seenListEmails.size() + "\n");
			
		}
		
		logger.info("Printing keyword profile for flagged direct emails");
		for(String key : flaggedDirectMatrix.keySet()){
			logger.info(key + " : " + flaggedDirectMatrix.get(key));
			
		}
		logger.info("\n Printing keyword profile for replied direct emails");
		for(String key : repliedDirectMatrix.keySet()){
			logger.info(key + " : " + repliedDirectMatrix.get(key));
			
		}
		logger.info("\nPrinting keyword profile for seen direct emails");
		for(String key : seenDirectMatrix.keySet()){
			logger.info(key + " : " + seenDirectMatrix.get(key));
			
		}
		
		logger.info("Printing keyword profile for flagged list emails");
		for(String key : flaggedListMatrix.keySet()){
			logger.info(key + " : " + flaggedListMatrix.get(key));
			
		}
		logger.info("\n Printing keyword profile for replied list emails");
		for(String key : repliedListMatrix.keySet()){
			logger.info(key + " : " + repliedListMatrix.get(key));
			
		}
		logger.info("\nPrinting keyword profile for seen list emails");
		for(String key : seenListMatrix.keySet()){
			logger.info(key + " : " + seenListMatrix.get(key));
			
		}
		
	}
	
	public static double getVectorTotal(double[] v){
		boolean isEmpty = true;
		int length = v.length;
		double total = 0;
		for(int i = 0; i < length; i++ ){
			total += v[i];
		}
		return total;
	}
	
	public static void printImportanceModelForMailBox(UserMailBox mb){
		EmailReputationDataModel model = mb.getReputationDataModel();
		
		//flagged
		double[] importantTopicsFlaggedProfile = model.getImportantTopicsFlagged();
		double[] importantPeopleFlaggedProfile = model.getImportantPeopleFlagged();
		double[] importantNLPKeywordsFlaggedProfile = model.getImportantNLPKeywordsFlagged();
		
		double flaggedTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsFlaggedProfile);
		logger.info(" flagged topic profile vector sum :" + flaggedTopicProfileScore +" no : " + mb.getNumberOfDirectEmailsFlagged());
		double flaggedPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleFlaggedProfile);
		logger.info(" flagged people profile vector sum :" + flaggedPeopleProfileScore );
		double flaggedNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsFlaggedProfile);
		logger.info(" flagged keywords profile vector sum :" + flaggedNLPProfileScore);
		
		double[] importantListTopicsFlaggedProfile = model.getImportantListTopicsFlagged();
		double[] importantListPeopleFlaggedProfile = model.getImportantListPeopleFlagged();
		double[] importantListNLPKeywordsFlaggedProfile = model.getImportantListNLPKeywordsFlagged();
		
		double flaggedListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsFlaggedProfile);
		logger.info("List flagged topic profile vector sum :" + flaggedListTopicProfileScore + " no : " + mb.getNumberOfListEmailsFlagged());
		double flaggedListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleFlaggedProfile);
		logger.info("List flagged people profile vector sum :" + flaggedListPeopleProfileScore);
		double flaggedListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsFlaggedProfile);
		logger.info("List flagged keywords profile vector sum :" + flaggedListNLPProfileScore);
		
		
		
		double[] importantTopicsRepliedProfile = model.getImportantTopicsReplied();
		double[] importantPeopleRepliedProfile = model.getImportantPeopleReplied();
		double[] importantNLPKeywordsRepliedProfile = model.getImportantNLPKeywordsReplied();
		
		
		double repliedTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsRepliedProfile);
		logger.info(" replied topic profile vector sum :" + repliedTopicProfileScore + " no : " + mb.getNumberOfDirectEmailsReplied());
		double repliedPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleRepliedProfile);
		logger.info(" replied people profile vector sum :" + repliedPeopleProfileScore);
		double repliedNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsRepliedProfile);
		logger.info(" replied keywords profile vector sum :" + repliedNLPProfileScore);
		
		
		double[] importantListTopicsRepliedProfile = model.getImportantListTopicsReplied();
		double[] importantListPeopleRepliedProfile = model.getImportantListPeopleReplied();
		double[] importantListNLPKeywordsRepliedProfile = model.getImportantListNLPKeywordsReplied();
		
		double repliedListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsRepliedProfile);
		logger.info("List replied topic profile vector sum :" + repliedListTopicProfileScore + " no : " + mb.getNumberOfListEmailsReplied());
		double repliedListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleRepliedProfile);
		logger.info("List replied people profile vector sum :" + repliedListPeopleProfileScore);
		double repliedListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsRepliedProfile);
		logger.info("List replied keywords profile vector sum :" + repliedListNLPProfileScore);
		
		
		double[] importantTopicsSeenProfile = model.getImportantTopicsOnlySeen();
		double[] importantPeopleSeenProfile = model.getImportantPeopleOnlySeen();
		double[] importantNLPKeywordsSeenProfile = model.getImportantNLPKeywordsOnlySeen();
		
		double seenTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsSeenProfile);
		logger.info("seen topic profile vector sum :" + seenTopicProfileScore+ " no :" + mb.getNumberOfDirectEmailsSeen());
		double seenPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleSeenProfile);
		logger.info("seen people profile vector sum :" + seenPeopleProfileScore);
		double seenNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsSeenProfile);
		logger.info("seen keywords profile vector sum :" + seenNLPProfileScore);
		
		
		double[] importantListTopicsSeenProfile = model.getImportantListTopicsOnlySeen();
		double[] importantListPeopleSeenProfile = model.getImportantListPeopleOnlySeen();
		double[] importantListNLPKeywordsSeenProfile = model.getImportantListNLPKeywordsOnlySeen();
		
		double seenListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsSeenProfile);
		logger.info("List seen topic profile vector sum :" + seenListTopicProfileScore + " no : " + mb.getNumberOfListEmailsSeen());
		double seenListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleSeenProfile);
		logger.info("List seen people profile vector sum :" + seenListPeopleProfileScore);
		double seenListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsSeenProfile);
		logger.info("List seen keywords profile vector sum :" + seenListNLPProfileScore);
		
		
		double[] spamTopicsVector = model.getSpamVector();
		double[] spamPeopleVector = model.getSpamPeopleVector();
		double[] spamKeywordsVector = model.getSpamNLPKeywordVector();
		
		double spamTopicProfileScore = EmailUtils.getVectorTotal(spamTopicsVector);
		logger.info("spam topic profile vector sum :" + spamTopicProfileScore + " no : " + mb.getNofOfUnimportantEmails());
		double spamPeopleProfileScore = EmailUtils.getVectorTotal(spamPeopleVector);
		logger.info("spam replied people profile vector sum :" + spamPeopleProfileScore);
		double spamNLPProfileScore = EmailUtils.getVectorTotal(spamKeywordsVector);
		
		logger.info("\n\nSpam replied keywords profile vector sum :" + spamNLPProfileScore);
		logger.info("Dunn index for content clusters : " + model.getDunnIndexForContentClusters());
	}


	/**
	 * returns the positive cosine similarity without looking at minus sim and very small sim
	 * @param a vector
	 * @param b vector
	 * @return
	 */
	public static double calculateCosineSimilarity(double[] a , double[] b){
		double result = 0;
		result = Similarity.cosineSimilarity(a, b);
		if(result < 0.00001){
			result = 0;
		}
		return result;
	}
	
	public static double getIncrementalLogIDF(int noOfDocsIndexed, int noOfDocsWithTheWord){
		double result = 0;
		if(noOfDocsIndexed > 0 && noOfDocsWithTheWord > 0){
			double logidf =Math.log(noOfDocsIndexed/noOfDocsWithTheWord);		
			result = 1 + logidf;
			//logger.info("the incremental log idf result : " + result);	
		}else {
			throw new IllegalArgumentException("The no of docs indexed or no of docs with the word is zero");
		}
		return result;
	}
	
	/**
	 * for content clusters
	 * if min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are compact and well clustered
	 * @return dunnIndex to validate cluster quality
	 */
	public static double getDunnIndexForContentClusters(List<EmailContentCluster> clusters){
		//min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are CWS
		double di = 0;
		double minInterClusterDistance = 0;
		double maxIntraClusterDistance = 0;
		
		for(EmailContentCluster c1: clusters){
			for(EmailContentCluster c2 : clusters){
				
				if(c1.getId() != c2.getId()){
					double[] v1 = c1.getCentroid();
					double[] v2 = c2.getCentroid();
					double dis = VectorsMath.getDistance(v1, v2);
					logger.info("inter-cluster distance between : " + c1.getId() + " and " + c2.getId() 
								+ " distance: "+ dis);
					
					if(minInterClusterDistance == 0) {
						minInterClusterDistance = dis;
					} else {
						if(dis < minInterClusterDistance) {
							minInterClusterDistance = dis;
						}
					}
				}
			}
		}
		
		for(EmailContentCluster c: clusters){
			double sumOfIntraClusterDistance = 0;
			double intraClusterDistance = 0;
			
			List<Email> emails = null;
			EmailContentCluster contentCluster = (EmailContentCluster)c;
			emails = contentCluster.getContentEmails();
			
			for(Email mail : emails){
				//distance from the centroid
				double[] v1 = mail.getTextContextVector();
				double dis = VectorsMath.getDistance(v1, c.getCentroid());
				sumOfIntraClusterDistance += dis;
			}
			
			intraClusterDistance = sumOfIntraClusterDistance / emails.size();
			//logger.info("intracluster distance : " + intraClusterDistance);
			if(maxIntraClusterDistance < intraClusterDistance){
				maxIntraClusterDistance = intraClusterDistance;
			}
		}
		
		logger.info("min inter-cluster distance : " + minInterClusterDistance + " max. intracluster distance : " + maxIntraClusterDistance );
		double dunnIndex = minInterClusterDistance/maxIntraClusterDistance;
		
		return dunnIndex;
	}
	
	/**
	 * for weighted subject body content clusters
	 * if min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are compact and well clustered
	 * @return dunnIndex to validate cluster quality
	 */
	public static double getDunnIndexForWeightedSubBodyContentClusters(List<EmailWeightedSubjectBodyContentCluster> clusters, String vectorType){
		//min Distance(Ci,Cj) / max Diam(Cx) >1 ; then clusters are CWS
		double di = 0;
		double minInterClusterDistance = 0;
		double maxIntraClusterDistance = 0;
		
		for(EmailWeightedSubjectBodyContentCluster c1: clusters){
			for(EmailWeightedSubjectBodyContentCluster c2 : clusters){
				double[] v1 = null;
				double[] v2 = null;
				if(c1.getId() != c2.getId()){
					if(vectorType.equals(EmailWeightedSubjectBodyContentCluster.subjectVectorType)){
						v1 = c1.getSubjectCentroid();
						v2 = c2.getSubjectCentroid();	
					}else if(vectorType.equals(EmailWeightedSubjectBodyContentCluster.bodyVectorType)){
						v1 = c1.getBodyCentroid();
						v2 = c2.getBodyCentroid();	
					}
					double dis = VectorsMath.getDistance(v1, v2);
					logger.info("inter-cluster vector distance between : " + c1.getId() + " and " + c2.getId() 
								+ " distance: "+ dis);
					
					if(minInterClusterDistance == 0) {
						minInterClusterDistance = dis;
					} else {
						if(dis < minInterClusterDistance) {
							minInterClusterDistance = dis;
						}
					}
				}
			}
		}
		
		for(EmailWeightedSubjectBodyContentCluster c: clusters){
			double sumOfIntraClusterDistance = 0;
			double intraClusterDistance = 0;
			
			List<Email> emails = null;
			emails = c.getSubjectBodyContentEmails();
			
			for(Email mail : emails){
				//distance from the centroid
				if(vectorType.equals(EmailWeightedSubjectBodyContentCluster.subjectVectorType)){
					double[] v1 = mail.getSubjectContextVector();
					double dis = VectorsMath.getDistance(v1, c.getSubjectCentroid());
					sumOfIntraClusterDistance += dis;	
				} else if (vectorType.equals(EmailWeightedSubjectBodyContentCluster.bodyVectorType)){
					double[] v1 = mail.getBodyContextVector();
					double dis = VectorsMath.getDistance(v1, c.getBodyCentroid());
					sumOfIntraClusterDistance += dis;	
				}
				
			}
			
			intraClusterDistance = sumOfIntraClusterDistance / emails.size();
			//logger.info("intracluster distance : " + intraClusterDistance);
			if(maxIntraClusterDistance < intraClusterDistance){
				maxIntraClusterDistance = intraClusterDistance;
			}
		}
		
		logger.info("min inter-cluster distance : " + minInterClusterDistance + " max. intracluster distance : " + maxIntraClusterDistance );
		double dunnIndex = minInterClusterDistance/maxIntraClusterDistance;
		
		return dunnIndex;
	}
	
}
