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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.nic.isis.reputation.dom.RandomIndexVector;
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
			//lower case
			text = text.toLowerCase();
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
					//trim the token to length 255 due to persistence restrictions
					if(stemmedToken.length() >= 255){
						stemmedToken = stemmedToken.substring(0, 254);
					}
					
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

	/**
	 * Gets the reputation results object for the email
	 * @param email
	 * @return
	 */
	public static JSONObject getReputationObjectJSON(Email email){
		JSONObject reputationObj = new JSONObject();
		reputationObj.put("id", email.getMessageId());
		reputationObj.put("uid", email.getMsgUid());
		reputationObj.put("is_predicted", email.isPredicted());
		//total topic score : combined topic scores for reply, see, flag email similarity
		reputationObj
				.put("contentscore", email.getTotalTopicScore());
		//total people score : combined people scores for from,cc,to email similarity
		reputationObj.put("peoplescore",
				email.getTotalPeopleScore());
		reputationObj.put("contentclusterid", email.getTextClusterId());
		reputationObj.put("peopleclusterid", email.getPeopleClusterId());
		
		//adding new fields for speech acts and nlp fields/keywords
		String intentString = getMessageIntentString(email);
		reputationObj.put("emailintent",intentString);
		reputationObj.put("nlpkeywords", getNLPKeywordsFromEmail(email));
		
		
		//score calculations
		//reply scores
//		double totalReplyScore = email.getRepliedPeoplescore() + email.getRepliedTopicscore()
//								+ email.getRepliedKeywordscore();
		
		//separate calculations for separate vectors (to,from.cc & subject,body vectors)
		double peopleScoreSeparateVectors = (email.getRepliedPeopleFromscore() + email.getRepliedPeopleToscore() + email.getRepliedPeopleCCscore())/3;
		double topicScoreSeparateVectors = (email.getRepliedTopicSubjectscore() + email.getRepliedTopicBodyscore())/2;
		double totalReplySeparateScore = peopleScoreSeparateVectors + topicScoreSeparateVectors;
		
		//
		double replyScoreToSend = email.getRepliedPeoplescore() + topicScoreSeparateVectors;
		reputationObj.put("replyscore", replyScoreToSend);
		logger.info(email.getMsgUid()+ " : sending reply score (people, (subject+body)/2):" + replyScoreToSend + " separetly aggregated score ((from,to,cc)/3, (body+subject)/2 : " + totalReplySeparateScore);
		
		
		//flag scores
//		double totalFlaggedScore = email.getFlaggedPeoplescore() + email.getFlaggedTopicscore()
//				+ email.getFlaggedKeywordscore();
//	
		double peopleScoreSeparateFlaggedVectors = (email.getFlaggedPeopleFromscore() + email.getFlaggedPeopleToscore() + email.getFlaggedPeopleCCscore())/3;
		double topicScoreSeparateFlaggedVectors = (email.getFlaggedTopicSubjectscore() + email.getFlaggedTopicBodyscore())/2;
		double totalFlaggedSeparateScore = peopleScoreSeparateFlaggedVectors + topicScoreSeparateFlaggedVectors;
		
		double flagScoreToSend = email.getFlaggedPeoplescore() + topicScoreSeparateFlaggedVectors;
		reputationObj.put("flagscore", flagScoreToSend);
		logger.info(email.getMsgUid()+ " : sending flagged score  (people,(subject+body)/2):" + flagScoreToSend + " separetly aggregated score ((from,to,cc)/3, (body+subject)/2 : " + totalFlaggedSeparateScore);		
			
		
		//seen scores
//		double totalSeenScore = email.getSeenPeoplescore() + email.getSeenTopicscore()
//				+ email.getSeenKeywordscore();
		
		double peopleScoreSeparateSeenVectors = (email.getSeenPeopleFromscore() + email.getSeenPeopleToscore() + email.getSeenPeopleCCscore())/3;
		double topicScoreSeparateSeenVectors = (email.getSeenTopicSubjectscore() + email.getSeenTopicBodyscore())/2;
		double totalSeenSeparateScore = peopleScoreSeparateSeenVectors + topicScoreSeparateSeenVectors;
		
		double seenScoreToSend = email.getSeenPeoplescore() + topicScoreSeparateSeenVectors;
		reputationObj.put("seescore", seenScoreToSend);
		logger.info(email.getMsgUid()+ " : sending seen score :" + seenScoreToSend + " combined score ((from,to,cc)/3, (body.subject)/2 : " + totalSeenSeparateScore);		
		
		//send spam results also
		reputationObj.put("spamcontentscore", email.getSpamTopicScore());
		reputationObj.put("spampeoplescore", email.getSpamPeopleScore());
		reputationObj.put("spamkeywordscore", email.getSpamKeywordscore());
		
		
		logger.info("the scores in results email to be sent :  uid : " + email.getMsgUid() + " contentscore : " + email.getTotalTopicScore()
				+ " people score : " + email.getTotalPeopleScore() + " content cluster : " + email.getTextClusterId() + " content cluster rep score : " + email.getContentReputationScore() 
				+ " people cluster : " + email.getPeopleClusterId() + " people cluster rep score : " + email.getRecipientReputationScore()
				+  " email intent : " + intentString + " reply-score : " + replyScoreToSend + " flag score : " + flagScoreToSend + " see score: " + seenScoreToSend);
		return reputationObj;
	}
	
	public static void sendReputationResults(String emailId, List<Email> emails) {
		JSONObject root = new JSONObject();
		JSONArray resultsArray = new JSONArray();

		for (Email email : emails) {
			try{
			    JSONObject reputationObj = getReputationObjectJSON(email);
				resultsArray.put(reputationObj);
				
			}catch(Exception ex){
				logger.error("Error occured while processing reputation result JSON ", ex);
			}
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
				logger.info("email is set as spam due to X-Spam-Flag");
				newEmail.setSpam(true);
			}
		} else {
			String[] spamscore = msg.getHeader("X-Spam-Score");
			if (spamscore != null && spamscore.length > 0) {
				try {
					String spamScore = spamscore[0];
					spamScore = spamScore.replaceAll("[^-?0-9]+", " ");
					String[] numbers = spamScore.trim().split(" ");
					logger.info("email is set as spam due to spam score header : " + spamScore);
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
					newEmail.setIsImportantByHeader(true);
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
				logger.info(" Email is set as spam due to low precedence level set for a : " + precedenceHeader);
				newEmail.setImportanceLevelByHeader(5);
				newEmail.setSpam(true);
				
			} else if (precedenceHeader.equalsIgnoreCase("first-class")) {
				newEmail.setImportanceLevelByHeader(1);
				newEmail.setIsImportantByHeader(true);
				
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
		subject = subject.toLowerCase();
		if (subject.length() > 255) {
			subject = subject.substring(0, 254);
		}
		//processing nlp
		logger.info("processing NLP results for subject..");
		//subject = subject.toLowerCase();
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

		String cleanedText = emailBody.getMessageContent();
		//String cleanedText = EmailUtils.removeHtmlTags(messageText);
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

		//removed due to frequent OOMs
//		logger.info("processing NLP results for email body.");
//		cleanedText = cleanedText.toLowerCase();
//		newEmail = getNLPResults(cleanedText, newEmail);
				

		// System.out.println("message body after processing quoted text and signautre : \n"
		// + cleanedText);

		// getting the speech-act features, they are only important if the email
		// is directly sent or ccd and not sent on a bulk
		double[] speechActVector = new double[5];
		try{
			if (newEmail.isDirect() || newEmail.isCCd()) {
				logger.info("Size of the content to extract SA : " + cleanedText.length());
				String saText = cleanedText;
				if(cleanedText.length() >= 30000){
					saText = cleanedText.substring(0, 29999);
				}
				speechAct.loadMessage(msg.getSubject() + "\n " + saText);
				//boolean isCommit = speechAct.hasCommit();
				//boolean hasDData = speechAct.hasDdata();
				boolean isDelivery = speechAct.hasDeliver();
				boolean isMeeting = speechAct.hasMeet();
				boolean isProposal = speechAct.hasPropose();
				boolean isRequest = speechAct.hasRequest();

				newEmail.setMeeting(isMeeting);
				//newEmail.setCommit(isCommit);
				newEmail.setDelivery(isDelivery);
				newEmail.setPropose(isProposal);
				newEmail.setRequest(isRequest);
				
//				if (isCommit) {
//					logger.info("Speech act identified commit");				
//					speechActVector[0] = 1;
//				}
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
		}catch(Exception ex){
			logger.error("Exception occurred while processing email speech acts ", ex);
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
			        //String ne = token.get(NamedEntityTagAnnotation.class);
			        if(pos.equalsIgnoreCase("NN") || pos.equalsIgnoreCase("NNS")){
			        	//trim the word to 255 length due to storage requirements
						if(word.length() >= 255){
							word = word.substring(0, 254);
						}
						
			        	word = stemmer.stem(word);
			        	keywords.add(word);
			        	if(keywordMatrix.containsKey(word)) {
			        		Integer val = keywordMatrix.get(word);
			        		keywordMatrix.put(word, val+1);
			        	} else {
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
		logger.info("Dunn index for content clusters : " + model.calculateDunnIndexForContentClusters());
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
	
	
	/**
	 * create the reputation model again for all the email data
	 * @param mb
	 */
	public static UserMailBox calculateImportanceModel(UserMailBox mb){
		List<Email> allEmails = mb.getAllEmails();
		EmailReputationDataModel repModel = new EmailReputationDataModel();
		//reset all numbers for emails
		mb.setNofOfUnimportantEmails(0);
		mb.setNumberOfDirectEmails(0);
		mb.setNumberOfDirectEmailsFlagged(0);
		mb.setNumberOfDirectEmailsReplied(0);
		mb.setNumberOfDirectEmailsSeen(0);
		mb.setNumberOfListEmails(0);
		mb.setNumberOfListEmailsFlagged(0);
		mb.setNumberOfListEmailsReplied(0);
		mb.setNumberOfListEmailsSeen(0);
				
		//marking user flagged important and spam emails before calculating importance model
		List<Long> markedImportantEmails = mb.getMarkedImportantEmailUids();
		List<Long> markedSpamEmails = mb.getMarkedSpamEmailUids();
		logger.info("no. of flagged important emails from thunderbird client : " + markedImportantEmails.size() +
				" no. of marked spam emails from thunderbird client : " + markedSpamEmails.size());
		
		for(Email email : allEmails) {
			email.setModel(true);
			email.setPredicted(false);
			if(markedImportantEmails.contains(email.getMsgUid())){
				logger.info("marked email :" + email.getMsgUid() + " as important based on user's feedback");
				email.setSpam(false);
				email.setFlagged(true);
				int index = markedImportantEmails.indexOf(email.getMsgUid());
				markedImportantEmails.remove(index);
				
			}else if(markedSpamEmails.contains(email.getMsgUid())){
				logger.info("marked email :" + email.getMsgUid() + " as spam based on user's feedback");
				email.setSpam(true);
				email.setFlagged(false);
				email.setAnswered(false);
				int index = markedSpamEmails.indexOf(email.getMsgUid());
				markedSpamEmails.remove(index);
				
			}
			//clear the above markedimportant/unimportant emails lists
			//mb.setMarkedImportantEmailUids(new ArrayList<Long>());
			//mb.setMarkedSpamEmailUids(new ArrayList<Long>());
			
			//populate the profile vectors			
			//unimportant models
			if( email.isSpam() || email.isDeleted()){
				logger.info("this is a spam email recognized by flag or header : " + email.getMsgUid());
				double vectorTotal = EmailUtils.getVectorTotal(email.getTextContextVector());
				//if(!Double.isNaN(vectorTotal)){
					double[] unimportanttopicsVector = VectorsMath.addArrays(repModel.getSpamVector(), email.getTextContextVector());
					repModel.setSpamVector(unimportanttopicsVector);
					logger.info("Adding a Spam Email. Email text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "spam topic vector sum: " + EmailUtils.getVectorTotal(unimportanttopicsVector));						
				//}
				
				double[] unimportantPeopleVector = VectorsMath.addArrays(repModel.getSpamPeopleVector(), email.getRecipientContextVector());
				repModel.setSpamPeopleVector(unimportantPeopleVector);
				
				double[] unimportantDirectKeywordVector = VectorsMath.addArrays(repModel.getSpamNLPKeywordVector(), email.getNlpKeywordsVector());
				repModel.setSpamNLPKeywordVector(unimportantDirectKeywordVector);
				

				//nofOfUnimportantEmails++;
				//trying out the spam vector from reputation data model
				repModel.getSpamEmails().add(email);
			}else {
				//important emails
				if(email.isDirect() || email.isCCd() || email.isBCCd()){
					//direct emails
					//processing emails sent directly,ccd to user
					//numberOfDirectEmails++;
					if(email.isAnswered()){
						logger.info("this is a direct email answered : " + email.getMsgUid());
						double[] importantTopicsReplied = VectorsMath.addArrays(repModel.getImportantTopicsReplied(), email.getTextContextVector());
						repModel.setImportantTopicsReplied(importantTopicsReplied);
						double[] importantTopicsSubjectReplied = VectorsMath.addArrays(repModel.getImportantTopicsSubjectsReplied(), email.getSubjectContextVector());
						repModel.setImportantTopicsSubjectsReplied(importantTopicsSubjectReplied);
						double[] importantTopicsBodyReplied = VectorsMath.addArrays(repModel.getImportantTopicsBodyReplied(), email.getBodyContextVector());
						repModel.setImportantTopicsBodyReplied(importantTopicsBodyReplied);
						
						
						//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "direct replied important-topic vector sum: " + EmailUtils.getVectorTotal(importantTopicsReplied));
						
						double[] importantPeopleReplied = VectorsMath.addArrays(repModel.getImportantPeopleReplied(), email.getRecipientContextVector());
						repModel.setImportantPeopleReplied(importantPeopleReplied);
						double[] importantPeopleFromReplied = VectorsMath.addArrays(repModel.getImportantPeopleFromReplied(), email.getFromContextVector());
						repModel.setImportantPeopleFromReplied(importantPeopleFromReplied);
						double[] importantPeopleToReplied = VectorsMath.addArrays(repModel.getImportantPeopleToReplied(), email.getToContextVector());
						repModel.setImportantPeopleToReplied(importantPeopleToReplied);
						double[] importantPeopleCCReplied = VectorsMath.addArrays(repModel.getImportantPeopleCCReplied(), email.getCcContextVector());
						repModel.setImportantPeopleCCReplied(importantPeopleCCReplied);
						
						
						double[] importantNLPKeywordsReplied = VectorsMath.addArrays(repModel.getImportantNLPKeywordsReplied(), email.getNlpKeywordsVector());
						repModel.setImportantNLPKeywordsReplied(importantNLPKeywordsReplied);
						
						//numberOfDirectEmailsReplied++;
						repModel.getRepliedEmails().add(email);
					}
					else if(email.isSeen()){
						logger.info("this is a direct email seen : " + email.getMsgUid());
						
						double[] importantTopicsOnlySeen = VectorsMath.addArrays(repModel.getImportantTopicsOnlySeen(), email.getTextContextVector());
						repModel.setImportantTopicsOnlySeen(importantTopicsOnlySeen);
						//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "direct seen important-topic vector sum: " + EmailUtils.getVectorTotal(importantTopicsOnlySeen));
						double[] importantTopicsSubjectSeen = VectorsMath.addArrays(repModel.getImportantTopicsSubjectsOnlySeen(), email.getSubjectContextVector());
						repModel.setImportantTopicsSubjectsOnlySeen(importantTopicsSubjectSeen);
						double[] importantTopicsBodySeen = VectorsMath.addArrays(repModel.getImportantTopicsBodyOnlySeen(), email.getBodyContextVector());
						repModel.setImportantTopicsBodyOnlySeen(importantTopicsBodySeen);
						
						double[] importantPeopleOnlySeen = VectorsMath.addArrays(repModel.getImportantPeopleOnlySeen(), email.getRecipientContextVector());
						repModel.setImportantPeopleOnlySeen(importantPeopleOnlySeen);
						double[] importantPeopleFromOnlySeen = VectorsMath.addArrays(repModel.getImportantPeopleFromOnlySeen(), email.getFromContextVector());
						repModel.setImportantPeopleFromOnlySeen(importantPeopleFromOnlySeen);
						double[] importantPeopleToOnlySeen = VectorsMath.addArrays(repModel.getImportantPeopleToOnlySeen(), email.getToContextVector());
						repModel.setImportantPeopleToOnlySeen(importantPeopleToOnlySeen);
						double[] importantPeopleCCOnlySeen = VectorsMath.addArrays(repModel.getImportantPeopleCCOnlySeen(), email.getCcContextVector());
						repModel.setImportantPeopleCCOnlySeen(importantPeopleCCOnlySeen);
						
						double[] importantNLPKeywordsOnlySeen = VectorsMath.addArrays(repModel.getImportantNLPKeywordsOnlySeen(), email.getNlpKeywordsVector());
						repModel.setImportantNLPKeywordsOnlySeen(importantNLPKeywordsOnlySeen);
						
						//numberOfDirectEmailsSeen++;
						repModel.getSeenEmails().add(email);
					}
					if(email.isFlagged() || email.getIsImportantByHeader() || email.isSensitiveByHeader()){
						System.out.println("this is a direct email user has flagged or set important/sensitive by header : " + email.getMsgUid());
						
						double[] importantTopicsFlagged = VectorsMath.addArrays(repModel.getImportantTopicsFlagged(), email.getTextContextVector());
						repModel.setImportantTopicsFlagged(importantTopicsFlagged);
						double[] importantTopicsSubjectFlagged = VectorsMath.addArrays(repModel.getImportantTopicsSubjectsFlagged(), email.getSubjectContextVector());
						repModel.setImportantTopicsSubjectsFlagged(importantTopicsSubjectFlagged);
						double[] importantTopicsBodyFlagged = VectorsMath.addArrays(repModel.getImportantTopicsBodyFlagged(), email.getBodyContextVector());
						repModel.setImportantTopicsBodyFlagged(importantTopicsBodyFlagged);
						
						
						double[] importantPeopleFlagged = VectorsMath.addArrays(repModel.getImportantPeopleFlagged(), email.getRecipientContextVector());
						repModel.setImportantPeopleFlagged(importantPeopleFlagged);
						double[] importantPeopleFromFlagged = VectorsMath.addArrays(repModel.getImportantPeopleFromFlagged(), email.getFromContextVector());
						repModel.setImportantPeopleFromFlagged(importantPeopleFromFlagged);
						double[] importantPeopleToFlagged = VectorsMath.addArrays(repModel.getImportantPeopleToFlagged(), email.getToContextVector());
						repModel.setImportantPeopleToFlagged(importantPeopleToFlagged);
						double[] importantPeopleCCFlagged = VectorsMath.addArrays(repModel.getImportantPeopleCCFlagged(), email.getCcContextVector());
						repModel.setImportantPeopleCCFlagged(importantPeopleCCFlagged);
						
						double[] importantNLPKeywordsFlagged = VectorsMath.addArrays(repModel.getImportantNLPKeywordsFlagged(), email.getNlpKeywordsVector());
						repModel.setImportantNLPKeywordsFlagged(importantNLPKeywordsFlagged);
						
						//numberOfDirectEmailsFlagged++;
						repModel.getFlaggedEmails().add(email);
				
					}
					
				}else {
					//list emails..
					
					//numberOfListEmails++;
					if(email.isAnswered()){
						logger.info("this is a list email answered : " + email.getMsgUid());
						double[] importantListTopicsReplied = VectorsMath.addArrays
								(repModel.getImportantListTopicsReplied(), email.getTextContextVector());
						repModel.setImportantListTopicsReplied(importantListTopicsReplied);		
						//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "list-replied-topic vector sum: " + EmailUtils.getVectorTotal(importantListTopicsReplied));
								
						double[] importantListTopicsSubjectReplied = VectorsMath.addArrays(repModel.getImportantListTopicsSubjectsReplied(), email.getSubjectContextVector());
						repModel.setImportantListTopicsSubjectsReplied(importantListTopicsSubjectReplied);
						double[] importantListTopicsBodyReplied = VectorsMath.addArrays(repModel.getImportantListTopicsBodyReplied(), email.getBodyContextVector());
						repModel.setImportantListTopicsBodyReplied(importantListTopicsBodyReplied);
						
						
						double[] importantListPeopleReplied = VectorsMath.addArrays
								(repModel.getImportantListPeopleReplied(), email.getRecipientContextVector());
						repModel.setImportantListPeopleReplied(importantListPeopleReplied);
						double[] importantListFromPeopleReplied = VectorsMath.addArrays
								(repModel.getImportantListPeopleFromReplied(), email.getFromContextVector());
						repModel.setImportantListPeopleFromReplied(importantListFromPeopleReplied);
						double[] importantListToPeopleReplied = VectorsMath.addArrays
								(repModel.getImportantListPeopleToReplied(), email.getToContextVector());
						repModel.setImportantListPeopleToReplied(importantListToPeopleReplied);
						double[] importantListCCPeopleReplied = VectorsMath.addArrays
								(repModel.getImportantListPeopleCCReplied(), email.getCcContextVector());
						repModel.setImportantListPeopleCCReplied(importantListCCPeopleReplied);
						
						
						
						double[] importantListNLPKeywordsReplied = VectorsMath.addArrays
								(repModel.getImportantListNLPKeywordsReplied(), email.getNlpKeywordsVector());
						repModel.setImportantListNLPKeywordsReplied(importantListNLPKeywordsReplied);
						
						//numberOfListEmailsReplied++;
						repModel.getRepliedListEmails().add(email);
										
					}
					else if(email.isSeen()){
						logger.info("this is a list email seen : "  + email.getMsgUid());
						double[] importantListTopicsOnlySeen = VectorsMath.addArrays
								(repModel.getImportantListTopicsOnlySeen(), email.getTextContextVector());
						repModel.setImportantListTopicsOnlySeen(importantListTopicsOnlySeen);
						//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "list-seen-topic vector sum: " + EmailUtils.getVectorTotal(importantListTopicsOnlySeen));
						double[] importantListTopicsSubjectSeen = VectorsMath.addArrays(repModel.getImportantListTopicsSubjectsOnlySeen(), email.getSubjectContextVector());
						repModel.setImportantListTopicsSubjectsOnlySeen(importantListTopicsSubjectSeen);
						double[] importantListTopicsBodySeen = VectorsMath.addArrays(repModel.getImportantListTopicsBodyOnlySeen(), email.getBodyContextVector());
						repModel.setImportantListTopicsBodyOnlySeen(importantListTopicsBodySeen);
						
						
						double[] importantListPeopleOnlySeen = VectorsMath.addArrays(repModel.getImportantListPeopleOnlySeen(), email.getRecipientContextVector());
						repModel.setImportantListPeopleOnlySeen(importantListPeopleOnlySeen);
						//logger.info("emai recipient vector sum : " + EmailUtils.getVectorTotal(email.getRecipientContextVector()) + "list-seen-people vector sum: " + EmailUtils.getVectorTotal(importantListPeopleOnlySeen));
						double[] importantListFromPeopleOnlySeen = VectorsMath.addArrays
								(repModel.getImportantListPeopleFromOnlySeen(), email.getFromContextVector());
						repModel.setImportantListPeopleFromOnlySeen(importantListFromPeopleOnlySeen);
						double[] importantListToPeopleOnlySeen = VectorsMath.addArrays
								(repModel.getImportantListPeopleToOnlySeen(), email.getToContextVector());
						repModel.setImportantListPeopleToOnlySeen(importantListToPeopleOnlySeen);
						double[] importantListCCPeopleOnlySeen = VectorsMath.addArrays
								(repModel.getImportantListPeopleCCOnlySeen(), email.getCcContextVector());
						repModel.setImportantListPeopleCCOnlySeen(importantListCCPeopleOnlySeen);
						
						
						double[] importantListNLPKeywordsOnlySeen = VectorsMath.addArrays(repModel.getImportantListNLPKeywordsOnlySeen(), email.getNlpKeywordsVector());
						repModel.setImportantListNLPKeywordsOnlySeen(importantListNLPKeywordsOnlySeen);
						//logger.info("emai nlp keyword vector sum : " + EmailUtils.getVectorTotal(email.getNlpKeywordsVector()) + "list-seen-keyword vector sum: " + EmailUtils.getVectorTotal(importantListNLPKeywordsOnlySeen));
						
						//numberOfListEmailsSeen++;
						repModel.getSeenListEmails().add(email);
					}
					if(email.isFlagged() || email.getIsImportantByHeader() || email.isSensitiveByHeader()){
						System.out.println("this is a list email user has flagged or set important/sensitive by header :" + email.getMsgUid());
						
						double[] importantListTopicsFlagged = VectorsMath.addArrays(repModel.getImportantListTopicsFlagged(), email.getTextContextVector());
						repModel.setImportantListTopicsFlagged(importantListTopicsFlagged);
						//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "flagged important-list-topic vector sum: " + EmailUtils.getVectorTotal(importantListTopicsFlagged));
						double[] importantListTopicsSubjectFlagged = VectorsMath.addArrays(repModel.getImportantListTopicsSubjectsFlagged(), email.getSubjectContextVector());
						repModel.setImportantListTopicsSubjectsFlagged(importantListTopicsSubjectFlagged);
						double[] importantListTopicsBodyFlagged = VectorsMath.addArrays(repModel.getImportantListTopicsBodyFlagged(), email.getBodyContextVector());
						repModel.setImportantListTopicsBodyFlagged(importantListTopicsBodyFlagged);
						
						
						double[] importantListPeopleFlagged = VectorsMath.addArrays(repModel.getImportantListPeopleFlagged(), email.getRecipientContextVector());
						repModel.setImportantListPeopleFlagged(importantListPeopleFlagged);
						
						double[] importantListFromPeopleFlagged = VectorsMath.addArrays
								(repModel.getImportantListPeopleFromFlagged(), email.getFromContextVector());
						repModel.setImportantListPeopleFromFlagged(importantListFromPeopleFlagged);
						double[] importantListToPeopleFlagged = VectorsMath.addArrays
								(repModel.getImportantListPeopleToFlagged(), email.getToContextVector());
						repModel.setImportantListPeopleToFlagged(importantListToPeopleFlagged);
						double[] importantListCCPeopleFlagged = VectorsMath.addArrays
								(repModel.getImportantListPeopleCCFlagged(), email.getCcContextVector());
						repModel.setImportantListPeopleCCFlagged(importantListCCPeopleFlagged);						
						
						double[] importantListNLPKeywordsFlagged = VectorsMath.addArrays(repModel.getImportantListNLPKeywordsFlagged(), email.getNlpKeywordsVector());
						repModel.setImportantListNLPKeywordsFlagged(importantListNLPKeywordsFlagged);
						
						//numberOfListEmailsFlagged++;
						repModel.getFlaggedListEmails().add(email);
					}
				}
			}
		}
		
		mb.setReputationDataModel(repModel);
		//no need to average the vectors as when taking cosine sim, the index values are not important
		//only the direction of the vector is important.
		//mb = EmailUtils.updateAverageImportanceVectors(mb);
		
//		//printing profile vector totals for reference
//		System.out.println("calculating importance model with averaging importance");
//		System.out.println(" topics flagged profile : " + EmailUtils.getVectorTotal(repModel.getImportantTopicsFlagged()));
//		System.out.println(" topic subjects flagged profile : " + EmailUtils.getVectorTotal(repModel.getImportantTopicsSubjectsFlagged()));
//		System.out.println(" keywords flagged profile : " + EmailUtils.getVectorTotal(repModel.getImportantNLPKeywordsFlagged()));
//		System.out.println(" from people flagged profile : " + EmailUtils.getVectorTotal(repModel.getImportantPeopleFromFlagged()));
//		System.out.println(" to people flagged profile : " + EmailUtils.getVectorTotal(repModel.getImportantPeopleToFlagged()));
//	
		return mb;
		
	}
	
	
@Deprecated
	public static UserMailBox updateAverageImportanceVectors(UserMailBox mailBox) {
		EmailReputationDataModel model = mailBox.getReputationDataModel();
		
		double[] directTopicsFlagged = model.getImportantTopicsFlagged();
		
		if(model.getFlaggedEmails() != null && model.getFlaggedEmails().size() > 0){
			directTopicsFlagged = VectorsMath.devideArray(directTopicsFlagged, model.getFlaggedEmails().size());
			model.setImportantTopicsFlagged(directTopicsFlagged);
		}
		double[] directTopicsReplied = model.getImportantTopicsReplied();
		if(model.getRepliedEmails() != null && model.getRepliedEmails().size() > 0){
			directTopicsReplied = VectorsMath.devideArray(directTopicsReplied, model.getRepliedEmails().size());
			model.setImportantTopicsReplied(directTopicsReplied);
		}
		double[] directTopicsSeen = model.getImportantTopicsOnlySeen();
		if(model.getSeenEmails() != null && model.getSeenEmails().size() > 0){
			directTopicsSeen = VectorsMath.devideArray(directTopicsSeen, model.getSeenEmails().size());
			model.setImportantTopicsOnlySeen(directTopicsSeen);
		}
		//subject only
		double[] directTopicSubjectsFlagged = model.getImportantTopicsSubjectsFlagged();
		if(model.getFlaggedEmails() != null && model.getFlaggedEmails().size() > 0){
			directTopicSubjectsFlagged = VectorsMath.devideArray(directTopicSubjectsFlagged, model.getFlaggedEmails().size());
			model.setImportantTopicsSubjectsFlagged(directTopicSubjectsFlagged);
		}
		double[] directTopicsSubjectsReplied = model.getImportantTopicsSubjectsReplied();
		if(model.getRepliedEmails() != null && model.getRepliedEmails().size() > 0){
			directTopicsSubjectsReplied = VectorsMath.devideArray(directTopicsSubjectsReplied, model.getRepliedEmails().size());
			model.setImportantTopicsSubjectsReplied(directTopicsSubjectsReplied);
		}
		double[] directTopicsSubjectsSeen = model.getImportantTopicsSubjectsOnlySeen();
		if(model.getSeenEmails() != null && model.getSeenEmails().size() > 0){
			directTopicsSubjectsSeen = VectorsMath.devideArray(directTopicsSubjectsSeen, model.getSeenEmails().size());
			model.setImportantTopicsSubjectsOnlySeen(directTopicsSubjectsSeen);
		}
		
		//body only
		double[] directTopicBodyFlagged = model.getImportantTopicsBodyFlagged();
		if(model.getFlaggedEmails() != null && model.getFlaggedEmails().size() > 0){
			directTopicBodyFlagged = VectorsMath.devideArray(directTopicBodyFlagged, model.getFlaggedEmails().size());
			model.setImportantTopicsBodyFlagged(directTopicBodyFlagged);
		}
		double[] directTopicsBodyReplied = model.getImportantTopicsBodyReplied();
		if(model.getRepliedEmails() != null && model.getRepliedEmails().size() > 0){
			directTopicsBodyReplied = VectorsMath.devideArray(directTopicsBodyReplied, model.getRepliedEmails().size());
			model.setImportantTopicsBodyReplied(directTopicsBodyReplied);
		}
		double[] directTopicsBodySeen = model.getImportantTopicsBodyOnlySeen();
		if(model.getSeenEmails() != null && model.getSeenEmails().size() > 0){
			directTopicsBodySeen = VectorsMath.devideArray(directTopicsBodySeen, model.getSeenEmails().size());
			model.setImportantTopicsBodyOnlySeen(directTopicsBodySeen);
		}
		
		
		double[] listTopicsFlagged = model.getImportantListTopicsFlagged();
		if(model.getFlaggedListEmails() != null && model.getFlaggedListEmails().size() > 0){
			listTopicsFlagged = VectorsMath.devideArray(listTopicsFlagged, model.getFlaggedListEmails().size());
			model.setImportantListTopicsFlagged(listTopicsFlagged);
		}
		double[] listTopicsReplied = model.getImportantListTopicsReplied();
		if(model.getRepliedListEmails() != null && model.getRepliedListEmails().size() > 0){
			listTopicsReplied = VectorsMath.devideArray(listTopicsReplied, model.getRepliedListEmails().size());
			model.setImportantListTopicsReplied(listTopicsReplied);
		}
		double[] listTopicsSeen = model.getImportantListTopicsOnlySeen();
		if(model.getSeenListEmails() != null && model.getSeenListEmails().size() > 0){
			listTopicsSeen = VectorsMath.devideArray(listTopicsSeen, model.getSeenListEmails().size());
			model.setImportantListTopicsOnlySeen(listTopicsSeen);
		}
		
		//subject only
		double[] listSubjectsFlagged = model.getImportantListTopicsSubjectsFlagged();
		if(model.getFlaggedListEmails() != null && model.getFlaggedListEmails().size() > 0){
			listSubjectsFlagged = VectorsMath.devideArray(listSubjectsFlagged, model.getFlaggedListEmails().size());
			model.setImportantListTopicsSubjectsFlagged(listSubjectsFlagged);
		}
		double[] listSubjectsReplied = model.getImportantListTopicsSubjectsReplied();
		if(model.getRepliedListEmails() != null && model.getRepliedListEmails().size() > 0){
			listSubjectsReplied = VectorsMath.devideArray(listSubjectsReplied, model.getRepliedListEmails().size());
			model.setImportantListTopicsSubjectsReplied(listSubjectsReplied);
		}
		double[] listSubjectsSeen = model.getImportantListTopicsSubjectsOnlySeen();
		if(model.getSeenListEmails() != null && model.getSeenListEmails().size() > 0){
			listSubjectsSeen = VectorsMath.devideArray(listSubjectsSeen, model.getSeenListEmails().size());
			model.setImportantListTopicsSubjectsOnlySeen(listSubjectsSeen);
		}
		//body only
		double[] listBodyFlagged = model.getImportantListTopicsBodyFlagged();
		if(model.getFlaggedListEmails() != null && model.getFlaggedListEmails().size() > 0){
			listBodyFlagged = VectorsMath.devideArray(listBodyFlagged, model.getFlaggedListEmails().size());
			model.setImportantListTopicsBodyFlagged(listBodyFlagged);
		}
		double[] listBodyReplied = model.getImportantListTopicsBodyReplied();
		if(model.getRepliedListEmails() != null && model.getRepliedListEmails().size() > 0){
			listBodyReplied = VectorsMath.devideArray(listBodyReplied, model.getRepliedListEmails().size());
			model.setImportantListTopicsBodyReplied(listBodyReplied);
		}
		double[] listBodySeen = model.getImportantListTopicsBodyOnlySeen();
		if(model.getSeenListEmails() != null && model.getSeenListEmails().size() > 0){
			listBodySeen = VectorsMath.devideArray(listBodySeen, model.getSeenListEmails().size());
			model.setImportantListTopicsBodyOnlySeen(listBodySeen);
		}
		
		
		//set all other vectors to the model
		double[] directPeopleFlagged = model.getImportantPeopleFlagged();
		if(model.getFlaggedEmails() != null && model.getFlaggedEmails().size() > 0){
			directPeopleFlagged = VectorsMath.devideArray(directPeopleFlagged, model.getFlaggedEmails().size() );
			model.setImportantPeopleFlagged(directPeopleFlagged);
		}
		double[] directPeopleReplied = model.getImportantPeopleReplied();
		if(model.getRepliedEmails() != null && model.getRepliedEmails().size() > 0){
			directPeopleReplied = VectorsMath.devideArray(directPeopleReplied, model.getRepliedEmails().size());
			model.setImportantPeopleReplied(directPeopleReplied);
		}
		double[] directPeopleSeen = model.getImportantPeopleOnlySeen();
		if(model.getSeenEmails() != null && model.getSeenEmails().size() > 0){
			directPeopleSeen = VectorsMath.devideArray(directPeopleSeen, model.getSeenEmails().size());
			model.setImportantPeopleOnlySeen(directPeopleSeen);
		}
		//to only
		double[] directPeopleToFlagged = model.getImportantPeopleToFlagged();
		if(model.getFlaggedEmails() != null && model.getFlaggedEmails().size() > 0){
			directPeopleToFlagged = VectorsMath.devideArray(directPeopleToFlagged, model.getFlaggedEmails().size());
			model.setImportantPeopleToFlagged(directPeopleToFlagged);
		}
		double[] directPeopleToReplied = model.getImportantPeopleToReplied();
		if(model.getRepliedEmails() != null && model.getRepliedEmails().size() > 0){
			directPeopleToReplied = VectorsMath.devideArray(directPeopleToReplied, model.getRepliedEmails().size());
			model.setImportantPeopleToReplied(directPeopleToReplied);
		}
		double[] directPeopleToSeen = model.getImportantPeopleToOnlySeen();
		if(model.getSeenEmails() != null && model.getSeenEmails().size() > 0){
			directPeopleToSeen = VectorsMath.devideArray(directPeopleToSeen, model.getSeenEmails().size());
			model.setImportantPeopleToOnlySeen(directPeopleToSeen);
		}
		//from only
		double[] directPeopleFromFlagged = model.getImportantPeopleFromFlagged();
		if(model.getFlaggedEmails() != null && model.getFlaggedEmails().size() > 0){
			directPeopleFromFlagged = VectorsMath.devideArray(directPeopleFromFlagged, model.getFlaggedEmails().size());
			model.setImportantPeopleFromFlagged(directPeopleFromFlagged);
		}
		double[] directPeopleFromReplied = model.getImportantPeopleFromReplied();
		if(model.getRepliedEmails() != null && model.getRepliedEmails().size() > 0){
			directPeopleFromReplied = VectorsMath.devideArray(directPeopleFromReplied, model.getRepliedEmails().size() );
			model.setImportantPeopleFromReplied(directPeopleFromReplied);
		}
		double[] directPeopleFromSeen = model.getImportantPeopleFromOnlySeen();
		if(model.getSeenEmails() != null && model.getSeenEmails().size() > 0){
			directPeopleFromSeen = VectorsMath.devideArray(directPeopleFromSeen, model.getSeenEmails().size() );
			model.setImportantPeopleFromOnlySeen(directPeopleFromSeen);
		}
		//cc only
		double[] directPeopleCCFlagged = model.getImportantPeopleCCFlagged();
		if(model.getFlaggedEmails() != null && model.getFlaggedEmails().size() > 0){
			directPeopleCCFlagged = VectorsMath.devideArray(directPeopleCCFlagged, model.getFlaggedEmails().size());
			model.setImportantPeopleCCFlagged(directPeopleCCFlagged);
		}
		double[] directPeopleCCReplied = model.getImportantPeopleCCReplied();
		if(model.getRepliedEmails() != null && model.getRepliedEmails().size() > 0){
			directPeopleCCReplied = VectorsMath.devideArray(directPeopleCCReplied, model.getRepliedEmails().size());
			model.setImportantPeopleCCReplied(directPeopleCCReplied);
		}
		double[] directPeopleCCSeen = model.getImportantPeopleCCOnlySeen();
		if(model.getSeenEmails() != null && model.getSeenEmails().size() > 0){
			directPeopleCCSeen = VectorsMath.devideArray(directPeopleCCSeen, model.getSeenEmails().size());
			model.setImportantPeopleCCOnlySeen(directPeopleCCSeen);
		}
		
		
		//list people
		double[] listPeopleFlagged = model.getImportantListPeopleFlagged();
		if(model.getFlaggedListEmails() != null && model.getFlaggedListEmails().size() > 0){
			listPeopleFlagged = VectorsMath.devideArray(listPeopleFlagged, model.getFlaggedListEmails().size());
			model.setImportantListPeopleFlagged(listPeopleFlagged);
		}
		double[] listPeopleReplied = model.getImportantListPeopleReplied();
		if(model.getRepliedListEmails() != null && model.getRepliedListEmails().size() > 0){
			listPeopleReplied = VectorsMath.devideArray(listPeopleReplied, model.getRepliedListEmails().size());
			model.setImportantListPeopleReplied(listPeopleReplied);
		}
		double[] listPeopleSeen = model.getImportantListPeopleOnlySeen();
		if(model.getSeenListEmails() != null && model.getSeenListEmails().size() > 0){
			listPeopleSeen = VectorsMath.devideArray(listPeopleSeen, model.getSeenListEmails().size());
			model.setImportantListPeopleOnlySeen(listPeopleSeen);
		}
		//to people
		double[] listPeopleToFlagged = model.getImportantListPeopleToFlagged();
		if(model.getFlaggedListEmails() != null && model.getFlaggedListEmails().size() > 0){
			listPeopleToFlagged = VectorsMath.devideArray(listPeopleToFlagged,  model.getFlaggedListEmails().size());
			model.setImportantListPeopleToFlagged(listPeopleToFlagged);
		}
		double[] listPeopleToReplied = model.getImportantListPeopleToReplied();
		if(model.getRepliedListEmails() != null && model.getRepliedListEmails().size() > 0){
			listPeopleToReplied = VectorsMath.devideArray(listPeopleToReplied, model.getRepliedListEmails().size());
			model.setImportantListPeopleToReplied(listPeopleToReplied);
		}
		double[] listPeopleToSeen = model.getImportantListPeopleToOnlySeen();
		if(model.getSeenListEmails() != null && model.getSeenListEmails().size() > 0){
			listPeopleToSeen = VectorsMath.devideArray(listPeopleToSeen,  model.getSeenListEmails().size());
			model.setImportantListPeopleToOnlySeen(listPeopleToSeen);
		}
		//from
		double[] listPeopleFromFlagged = model.getImportantListPeopleFromFlagged();
		if(model.getFlaggedListEmails() != null && model.getFlaggedListEmails().size() > 0){
			listPeopleFromFlagged = VectorsMath.devideArray(listPeopleFromFlagged,  model.getFlaggedListEmails().size());
			model.setImportantListPeopleFromFlagged(listPeopleFromFlagged);
		}
		double[] listPeopleFromReplied = model.getImportantListPeopleFromReplied();
		if(model.getRepliedListEmails() != null && model.getRepliedListEmails().size() > 0){
			listPeopleFromReplied = VectorsMath.devideArray(listPeopleFromReplied, model.getRepliedListEmails().size());
			model.setImportantListPeopleFromReplied(listPeopleFromReplied);
		}
		double[] listPeopleFromSeen = model.getImportantListPeopleFromOnlySeen();
		if(model.getSeenListEmails() != null && model.getSeenListEmails().size() > 0){
			listPeopleFromSeen = VectorsMath.devideArray(listPeopleFromSeen, model.getSeenListEmails().size());
			model.setImportantListPeopleFromOnlySeen(listPeopleFromSeen);
		}
		//cc
		double[] listPeopleCCFlagged = model.getImportantListPeopleCCFlagged();
		if(model.getFlaggedListEmails() != null && model.getFlaggedListEmails().size() > 0){
			listPeopleCCFlagged = VectorsMath.devideArray(listPeopleCCFlagged, model.getFlaggedListEmails().size());
			model.setImportantListPeopleCCFlagged(listPeopleCCFlagged);
		}
		double[] listPeopleCCReplied = model.getImportantListPeopleCCReplied();
		if(model.getRepliedListEmails() != null && model.getRepliedListEmails().size() > 0){
			listPeopleCCReplied = VectorsMath.devideArray(listPeopleCCReplied, model.getRepliedListEmails().size());
			model.setImportantListPeopleCCReplied(listPeopleCCReplied);
		}
		double[] listPeopleCCSeen = model.getImportantListPeopleCCOnlySeen();
		if(model.getSeenListEmails() != null && model.getSeenListEmails().size() > 0){
			listPeopleCCSeen = VectorsMath.devideArray(listPeopleCCSeen, model.getSeenListEmails().size());
			model.setImportantListPeopleCCOnlySeen(listPeopleCCSeen);
		}
		
		//nlp keywords
		double[] importantNLPKeywordsReplied = model.getImportantNLPKeywordsReplied();
		if(model.getRepliedEmails() != null && model.getRepliedEmails().size() > 0){
			importantNLPKeywordsReplied = VectorsMath.devideArray(importantNLPKeywordsReplied, model.getRepliedEmails().size());
			model.setImportantNLPKeywordsReplied(importantNLPKeywordsReplied);
		}
		double[] importantNLPKeywordsFlagged = model.getImportantNLPKeywordsFlagged();
		if(model.getFlaggedEmails() != null && model.getFlaggedEmails().size() > 0){
			importantNLPKeywordsFlagged = VectorsMath.devideArray(importantNLPKeywordsFlagged, model.getFlaggedEmails().size());
			model.setImportantNLPKeywordsFlagged(importantNLPKeywordsFlagged);
		}
		double[] importantNLPKeywordsSeen = model.getImportantNLPKeywordsOnlySeen();
		if(model.getSeenEmails() != null && model.getSeenEmails().size() > 0){
			importantNLPKeywordsSeen = VectorsMath.devideArray(importantNLPKeywordsSeen, model.getSeenEmails().size());
			model.setImportantNLPKeywordsOnlySeen(importantNLPKeywordsSeen);
		}
		//list nlp keywords
		double[] importantListNLPKeywordsReplied = model.getImportantListNLPKeywordsReplied();
		if(model.getRepliedListEmails() != null && model.getRepliedListEmails().size() > 0){
			importantListNLPKeywordsReplied = VectorsMath.devideArray(importantListNLPKeywordsReplied, model.getRepliedListEmails().size());
			model.setImportantListNLPKeywordsReplied(importantListNLPKeywordsReplied);
		}
		double[] importantListNLPKeywordsFlagged = model.getImportantListNLPKeywordsFlagged();
		if(model.getFlaggedListEmails() != null && model.getFlaggedListEmails().size() > 0){
			importantListNLPKeywordsFlagged = VectorsMath.devideArray(importantListNLPKeywordsFlagged, model.getFlaggedListEmails().size());
			model.setImportantListNLPKeywordsFlagged(importantListNLPKeywordsFlagged);
		}
		double[] importantListNLPKeywordsSeen = model.getImportantListNLPKeywordsOnlySeen();
		if(model.getSeenListEmails() != null && model.getSeenListEmails().size() > 0){
			importantListNLPKeywordsSeen = VectorsMath.devideArray(importantListNLPKeywordsSeen,  model.getSeenListEmails().size());
			model.setImportantListNLPKeywordsOnlySeen(importantListNLPKeywordsSeen);
		}
								
		//spam 
		double[] spamContentVector = model.getSpamVector();
		double[] spamPeopleVector = model.getSpamPeopleVector();
		double[] spamNLPKeywordVector = model.getSpamNLPKeywordVector();
		if(model.getSpamEmails() != null && model.getSpamEmails().size() > 0){
			spamContentVector = VectorsMath.devideArray(spamContentVector, model.getSpamEmails().size());
			model.setSpamVector(spamContentVector);
			spamPeopleVector = VectorsMath.devideArray(spamPeopleVector, model.getSpamEmails().size());
			model.setSpamPeopleVector(spamPeopleVector);
			spamNLPKeywordVector = VectorsMath.devideArray(spamNLPKeywordVector, model.getSpamEmails().size());
			model.setSpamNLPKeywordVector(spamNLPKeywordVector);
		}
		
		mailBox.setReputationDataModel(model);
		//need to clear all mailbox data of reputation email data for the next iteration
		return mailBox;
		
	}	

	public static void persistIndexVectors(String fileName,
			List<RandomIndexVector> indexVectors) {
		File indexVectorFile = new File(fileName);
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(indexVectorFile);
			String headerRow = "word,frequency,positiveIndexes,negativeIndexes";
			// writer.println(headerRow);

			for (RandomIndexVector vector : indexVectors) {
				int[] positiveIndexes = vector.getPositiveIndexes();
				int[] negativeIndexes = vector.getNegativeIndexes();
				String posIndString = "";
				String negIndString = "";
				for (int posInd : positiveIndexes) {
					if(posIndString.length() == 0){
						posIndString += posInd;	
					} else {
						posIndString += ":" + posInd ;
					}
					
				}
				// removing the last trailing |;
//				posIndString = posIndString.substring(0,
//						posIndString.length() - 2);
				for (int negInd : negativeIndexes) {
					if(negIndString.length() == 0){
						negIndString += negInd;	
					} else{
						negIndString += ":" + negInd;
					}
				}
//				negIndString = negIndString.substring(0,
//						negIndString.length() - 2);

				String row = vector.getWord() + ","
						+ vector.getWordDocFrequency() + "," + posIndString
						+ "," + negIndString;
				System.out.println("writing indexvector to file : " + row);
				writer.println(row);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("Error occured while writing the indexvector file", e);
		}finally{
			writer.close();
		}
	}

	public static List<RandomIndexVector> getVectorsFromFile(File vectorFile) {
		logger.info("retrieving random index vectors from : " + vectorFile.getAbsolutePath());
		List<RandomIndexVector> indexVectors = new ArrayList<RandomIndexVector>();
		try {
			BufferedReader bf = new BufferedReader(new FileReader(vectorFile));
			String line = null;
			while ((line = bf.readLine()) != null) {
				logger.info("parsing the line: " + line);
				String[] resultRow = line.split(",");
				String word = resultRow[0];
				int frequency = Integer.parseInt(resultRow[1]);
				// process positive indexes
				String posString = resultRow[2];
//				if(posString.endsWith("|")){
//					posString = posString.substring(0, posString.lastIndexOf("|"));
//				}
				int[] posIndexes = new int[4];
				String posIndexesString = "";				
				if(posString.length() > 0){
					String[] pos = posString.split(":");
					posIndexes = new int[pos.length];
					//logger.info("parsing pos string : " + posString + " :split for :" + pos.length );
					
					for (int x = 0; x < pos.length; x++) {
							//logger.info("parsing the posindex val : " + pos[x]);
							try{
								int indexVal = Integer.parseInt(pos[x]);
								posIndexes[x] = indexVal;	
								posIndexesString = indexVal + ","; 	
							}catch(NumberFormatException nex){
								logger.error("error parsing as number", nex);
							}
					}	
				}
				
				// process negative indexes
				//process only if there are negative indexes
				String negIndexesString = "";
				int[] negIndexes = new int[4];
				if(resultRow.length > 3){
					String negString = resultRow[3];
//					if(negString.endsWith("|")){
//						negString = negString.substring(0, negString.lastIndexOf("|"));
//					}
					if(negString.length() > 0){
						String[] neg = negString.split(":");
						negIndexes = new int[neg.length];
						logger.info("parsing negative string : " + negString);
						for (int x = 0; x < neg.length; x++) {
							try{
								int indexVal = Integer.parseInt(neg[x]);
								negIndexes[x] = indexVal;	
								negIndexesString = indexVal + ",";
							}catch(NumberFormatException nex){
								logger.error("error parsing as number", nex);
							}
								
							
						}	
					}
				}

				
				logger.info("loaded index vector for word : "+ word 
						+ " frequency : " + frequency + " positive indexes : " + posIndexesString 
						+ " negative indexes : " + negIndexesString);
				// creating the RIVector object
				RandomIndexVector riVector = new RandomIndexVector();
				riVector.setWord(word);
				riVector.setWordDocFrequency(frequency);
				riVector.setPositiveIndexes(posIndexes);
				riVector.setNegativeIndexes(negIndexes);
				indexVectors.add(riVector);

			}
		} catch (Exception e) {
			logger.error("error occured while retrieving random index vectors from file", e);
		}
	
		return indexVectors;
	}
}
