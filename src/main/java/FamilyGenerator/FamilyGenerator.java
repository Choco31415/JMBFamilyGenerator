package FamilyGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FamilyGenerator {

	private static final String RESOURCES_PATH = "src/main/resources";
	
	private BufferedReader br;
	private String input = null;
	
	private String familyName;
	private ArrayList<String> wikiPrefixes = new ArrayList<String>();
	private ArrayList<String> wikiURLs = new ArrayList<String>();
	private ArrayList<String> MWversions = new ArrayList<String>();
	
	//These variables are used for networking purposes (GET and POST requests).
    private HttpClient httpclient;
	private HttpClientContext context;
	
	//Special characters.
	private static final String UTF8_BOM = "\uFEFF";
	
	// One-off SSL trust manager class.
	public class HttpsTrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			// TODO Auto-generated method stub
			return new X509Certificate[]{};
		}

	}
	
	private void setupSSLclient() {
		// Handle SSL
		SSLContext sslcontext = null;
		SSLConnectionSocketFactory factory = null;

			
			try {
				sslcontext = SSLContext.getInstance("TLSv1.2");
				sslcontext.init(null, new X509TrustManager[]{new HttpsTrustManager()}, new SecureRandom());
		        factory = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1", "TLSv1.1", "TLSv1.2"  }, null, new NoopHostnameVerifier());
			} catch (KeyManagementException | NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				System.out.println("Could not create SSL context. Entering fail state.");
				throw new Error(e.getMessage());
			}


			    
		// Create client and context for web stuff.
		httpclient = HttpClientBuilder.create().setSSLSocketFactory(factory).build();
	}

	private FamilyGenerator() {
		setupSSLclient();
		context =  HttpClientContext.create();
	}

	static public void main(String[] args) throws IOException, URISyntaxException {
		FamilyGenerator instance = new FamilyGenerator();
		instance.run();
	}
	
	/**
	 * The entry point for making a new family file.
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void run() throws IOException, URISyntaxException {
		System.out.println("You are running the script that makes a new wiki family.");
		
		//Get user input
		br = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("What is the name of the wiki family?");
		
		input = br.readLine();
		familyName = input;
		
		System.out.println("Creating family " + familyName + ".");
		
		manuallyBuildWikiFamily();
		writeFamily();
		System.exit(0);
	}
	
	private void manuallyBuildWikiFamily() throws IOException, URISyntaxException {
		//CUI
		boolean running = true;
		boolean legibleInput = true;
		do {
			do {
				legibleInput = true;
				
				//User options
				System.out.println("a - Add a wiki");
				System.out.println("i - Add interwiki family (Requires MW v.1.11+)");
				System.out.println("r - Remove a wiki");
				System.out.println("v - View current family");
				System.out.println("e - Finish and write family");
				
				//Read in user input
				input = br.readLine();
				
				switch (input) {
					case "a":
						addWiki();
						break;
					case "i":
						addInterwikiFamily();
						break;
					case "r":
						removeWiki();
						break;
					case "v":
						printWiki();
						break;
					case "e":
						running = false;
						break;
					default:
						System.out.println("Invalid input. Please only enter: a, i, r, v, e");
						legibleInput = false;
						break;
				}
			} while (!legibleInput);
		} while (running);
	}
	
	private void addInterwikiFamily() throws IOException, URISyntaxException  {
		//Check a few things with the user first.
		boolean excludeDefaultInterwiki = true;
		boolean localOnly = true;
		boolean languageOnly = true;
		
		boolean legibleInput = true;
		do {
			legibleInput = true;
			
			//User options
			System.out.println("Do you want to include all interwikis, or exclude the default mediawiki interwikis?");
			System.out.println("All will include: Wikipedia, Wikimedia, Commons, Acronym Finder, ect...");
			System.out.println("i - Include");
			System.out.println("e - Exclude");
			
			//Read in user input
			input = br.readLine();
			
			switch (input) {
				case "i":
					excludeDefaultInterwiki = false;
					break;
				case "e":
					excludeDefaultInterwiki = true;
					break;
				default:
					System.out.println("Invalid input. Please only enter: a, e");
					legibleInput = false;
					break;
			}
		} while (!legibleInput);
		
		do {
			legibleInput = true;
			
			//User options
			System.out.println("Do you want to include local wikis?");
			System.out.println("i - Include");
			System.out.println("e - Exclude");
			
			//Read in user input
			input = br.readLine();
			
			switch (input) {
				case "i":
					languageOnly = false;
					break;
				case "e":
					languageOnly = true;
					break;
				default:
					System.out.println("Invalid input. Please try again.");
					legibleInput = false;
					break;
			}
		} while (!legibleInput);
		
		ArrayList<String> defaultInterwikis = new ArrayList<String>();
		if (excludeDefaultInterwiki) {
			System.out.println("If any wikis are included by mistake, update "
					+ "Families/Miscalleneous/DefaultInterwikis.txt and contact "
					+ "JMB's owner Choco31415.");
			defaultInterwikis = readClassFile("DefaultInterwikis.txt"); // TODO : Add file.
		}
		
		//Ask for a wiki url, so we can get all wikis in the wiki group.
		System.out.println("What is a URL to a wiki in the family?");
		
		input = br.readLine();
		
		//Check wiki for interwikis
		String url = input;
		String URLapi = getAPIurl(url);
		
		//Get the interwiki map. MW v.1.11+ required
		if (localOnly) {
			url = URLapi + "/api.php?action=query&meta=siteinfo&siprop=interwikimap&format=json";
		} else {
			url = URLapi + "/api.php?action=query&meta=siteinfo&siprop=interwikimap&sifilteriw=local&format=json";
		}
		
		String serverOutput = removeBOM(EntityUtils.toString(getURL(url)));
		serverOutput = StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeHtml4(serverOutput));
		
		// Read in the Json!!!
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readValue(serverOutput, JsonNode.class);
		} catch (IOException e1) {
			System.out.println("Was expecting Json, but did not receive Json from server.");
			return;
		}
		
		JsonNode mapList = rootNode.findValue("interwikimap");
		
		System.out.print("Detected wikis: ");
		for (JsonNode mapNode : mapList) {
			if (!languageOnly || mapNode.has("language")) { // Language only check
				String wikiUrl = mapNode.get("url").asText();
				String prefix = mapNode.get("prefix").asText();
				
				if (!wikiPrefixes.contains(prefix) && !defaultInterwikis.contains(prefix)) { // Default interwiki check
					System.out.print(prefix + " ");
					
					String APIurl;
					try {
						APIurl = getAPIurl(wikiUrl);
						if (APIurl.equals("")) {
							System.out.println("Skipping.");
							continue;
						}
					} catch (Exception e) {
						System.out.println("Failed to add " + url + ". Skipping for now.");
						continue;
					}
					
					wikiURLs.add(APIurl);
					wikiPrefixes.add(prefix);
					
					String version = getMWversion(APIurl);
					MWversions.add(version);
				}
			}
		}
		System.out.println("");
	}
	
	private void addWiki() throws IOException, URISyntaxException {
		System.out.println("What is the prefix of your wiki?");
		System.out.println("This is preferably the interwiki prefix of the wiki, if there exists one.");
		
		input = br.readLine();
		String prefix = input.toLowerCase();
		
		System.out.println("What is a wiki url?");
		
		input = br.readLine();

		String URLapi = getAPIurl(input);
		if (URLapi != null) {
			System.out.println("The api url for this wiki is: " + URLapi);
			
			System.out.println("Getting wiki MW version...");
			String version = getMWversion(URLapi);
			
			wikiPrefixes.add(prefix);
			wikiURLs.add(URLapi);
			MWversions.add(version);
			
			System.out.println("Detected MW version " + version);
		}
	}
	
	private void removeWiki() throws IOException {
		System.out.println("What is prefix of the wiki you want to remove?");
		System.out.println("Hit enter to go back.");
		
		input = br.readLine();
		
		if (!input.equals("")) {
			//Remove wiki.
			int index = wikiPrefixes.indexOf(input);
			
			if (index != -1) {
				System.out.println("Removing wiki " + input);
				
				wikiURLs.remove(index);
				wikiPrefixes.remove(index);
				MWversions.remove(index);
			} else {
				System.out.println("Could not find wiki.");
			}
		}
	}
	
	private void printWiki() throws IOException {
		System.out.println("Currently creating wiki family " + familyName);
		if (wikiPrefixes.size() == 0) {
			System.out.println("No wikis in the current family so far.");
		}
		
		for (int i = 0; i < wikiPrefixes.size(); i++) {
			System.out.println("wiki " + (i+1) + ": " + wikiPrefixes.get(i) + " : " + MWversions.get(i) + " : " + wikiURLs.get(i));
		}
	}
	
	/*
	 * 
	 * Some methods for getting wiki information.
	 * 
	 */
	
	/**
	 * Gets the API url from any wiki url.
	 * @param wikiURL
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	private String getAPIurl(String url) throws URISyntaxException, IOException {
		String mediawikiURL = getMediawikiURL(url);
		
		String directory = null;
		
		// The API path is unknown. Try and make a few intelligent guesses.
		String APIurl = mediawikiURL + "/w/api.php";
		int responseCode = getResponseCode(APIurl);
		if (responseCode == 200) {
			directory = "/w";
		} else {
			//Let's try /wiki.
			APIurl = mediawikiURL + "/wiki/api.php";
			responseCode = getResponseCode(APIurl);
			if (responseCode == 200) {
				directory = "/wiki";
			} else {
				//Let's try /
				APIurl = mediawikiURL + "/api.php";
				responseCode = getResponseCode(APIurl);
				if (responseCode == 200) {
					directory = "";
				} else {
					System.out.println("\nThe api path for this wiki could not be determined."
							+ "\nMediawiki URL: " + url
							+ "\nPlease go to Special:Version on the wiki, and copy"
							+ "\nthe API path here. Leave out the /api.php."
							+ "\nEnter nothing to skip.");
					
					String input = br.readLine();
					
					if (input.equals("")) {
						return "";
					} else {
						directory = input;
					}
					
				}
			}
		}
		
		return mediawikiURL + directory;
	}
	
	/**
	 * Find the shortest url that responds.
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 */
	private String getMediawikiURL(String url) throws URISyntaxException {
		URI uri;
		String URLheader;
		String host;
		int port;
		String path;
		try {
			uri = new URI(url);
			URLheader = uri.getScheme() + "://";
			host = uri.getHost();
			port = uri.getPort();
			path = uri.getPath();
		} catch (Throwable e) {
			//We're dealing with Russian, Japanese, or some sort of non-standard characters. Probably.
			uri = new URI(StringEscapeUtils.escapeHtml4(url));
			URLheader = StringEscapeUtils.unescapeHtml4(uri.getScheme()) + "://";
			host = StringEscapeUtils.unescapeHtml4(uri.getHost());
			port = uri.getPort();
			path = StringEscapeUtils.unescapeHtml4(uri.getPath());
		}
		
		String mediawikiURL;
		if (port == -1) {
			mediawikiURL = URLheader + host;
		} else {
			mediawikiURL = URLheader + host + ":" + port;
		}
		
		// Dig through the path, from the bottom up, to see when we start receiving 200's.
		String[] pathSegments = path.split("/");
		int segmentID = 0;
		int responseCode = 404;
		do {	
			if (responseCode != 200 && segmentID < pathSegments.length) {
				// Let's go up a directory.
				mediawikiURL += pathSegments[segmentID]; // Try adding another path section.
				if (segmentID != pathSegments.length-1) {
					mediawikiURL += "/";
				}
			}
			segmentID++;
			
			responseCode = getResponseCode(mediawikiURL);
		} while (responseCode != 200 && segmentID <= pathSegments.length);
		
		if (responseCode != 200) {
			System.out.println("Bad url provided.");
			throw new URISyntaxException("", "");
		}
		
		if ( !(pathSegments.length == 1 && pathSegments[0].equals("")) ) {
			// Strip URL of any leading query or "/"
			if (segmentID < pathSegments.length ) {
				// Leading "/"
				mediawikiURL = mediawikiURL.substring(0, mediawikiURL.length()-1);
			} else {
				// Leading query
				String query = pathSegments[pathSegments.length-1];
				int index = mediawikiURL.indexOf(query);
				mediawikiURL = mediawikiURL.substring(0, index-1);
			}
		}
		
		return mediawikiURL;
	}
	
	/**
	 * Get the MW version of an MW installation.
	 * 
	 * @param apiURL The MW's api url.
	 * @return 
	 * @throws IOException
	 */
	private String getMWversion(String apiURL) throws IOException {
		String serverOutput = removeBOM(EntityUtils.toString(getURL(apiURL + "/api.php?action=query&meta=siteinfo&format=json")));
		
		// Read in the JSON!!!
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readValue(serverOutput, JsonNode.class);
		} catch (IOException e1) {
			System.out.println("ERROR: Was expecting JSON, but did not receive JSON from server.");
			return "";
		}
		
		// Find generator tag.
		JsonNode generatorTag = rootNode.findValue("generator");
		String generatorText = generatorTag.asText();

		String version = generatorText.split(" ")[1];
		
		return version;
	}
	
	private void writeFamily() throws IOException {
		if (wikiPrefixes.size() > 0) {
			boolean done = false;
			
			do {
				System.out.println("Please input a directory to write the family file to.");
				System.out.println("Please include the leading slash.");
				System.out.println("Input blank for this project's resource folder.");
				String directory = br.readLine();
				
				// Check that directory is valid.
				boolean validDirectory = true;
				File familyFile;
				if (directory.length() == 0) {
					directory = RESOURCES_PATH + "/Families/";
				}
				familyFile = new File(directory);

				validDirectory = familyFile.isDirectory() && familyFile.canWrite();
				if (!validDirectory) {
					System.out.println("Invalid directory or can't write.");
				}
				
				if (validDirectory) {
					// The directory is valid, so we can create the family file.
					familyFile = new File(directory + familyName + ".txt");
					
					String fileContent = "[";
					
					for (int i = 0; i < wikiPrefixes.size(); i++) {
						fileContent += "{\"prefix\": \"" + wikiPrefixes.get(i) + "\", ";
						fileContent += "\"version\": \"" + MWversions.get(i) + "\", ";
						fileContent += "\"url\": \"" + wikiURLs.get(i) + "\"}";
						if (i != wikiPrefixes.size()-1) {
							fileContent += ",\n";
						}
					}
					
					fileContent += "]";
					
					writeFile(Paths.get(familyFile.getAbsolutePath()), fileContent);
					
					done = true;
				}
			} while (!done);
		}
	}
	
	/**
	 * Utility Code
	 */
	
	public int getResponseCode(String url) {
  		//This method actual fetches a web page, and returns the response code.
		HttpResponse response = null;
	 	HttpHead httpost = new HttpHead(url);
		
		// Fetch the url.
		try {
			response = httpclient.execute(httpost, context);
		} catch (SocketException|NoHttpResponseException e) {
			System.out.println("Cannot connect to server at: " + url);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return response.getStatusLine().getStatusCode();
	}
	
	/**
	 * @param ur The url you want to get.
	 * @param unescapeHTML4 Unescapes HTML4 text. Ex: & #039;
	 */
	protected HttpEntity getURL(String ur) throws IOException {	  		
  		//This method actual fetches a web page, and turns it into a more easily use-able format.		  		//This method actual fetches a web page, and turns it into a more easily use-able format.
		HttpResponse response = null;
	 	HttpGet httpost = new HttpGet(ur);
		
		// Fetch the url.
		try {
			response = httpclient.execute(httpost, context);
		} catch (SocketException|NoHttpResponseException e) {
			throw new Error("Cannot connect to server at: " + ur);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response.getEntity();
	}
	
	public String removeBOM(String text) {
		String toReturn = text;
		
		if (text.startsWith(UTF8_BOM)) {
			toReturn = toReturn.substring(1);
		}
		
		return toReturn;
	}
	
	public ArrayList<String> readClassFile(String filename) throws IOException {
		ClassLoader loader = FamilyGenerator.class.getClassLoader();
		InputStream inputStream = loader.getResourceAsStream(filename);
		InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(streamReader);
		
		ArrayList<String> lines = new ArrayList<String>();
		String line = reader.readLine();
		while (line != null) {
			lines.add(line);
			line = reader.readLine();
		}
		
		return lines;
	}
	
	public void writeFile(Path path, String content) {
		PrintWriter writer = null;
		
		try {
			System.out.println("Writting file: " + path);
			writer = new PrintWriter(path.toString(), "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		writer.write(content);
		writer.close();
	}
}
