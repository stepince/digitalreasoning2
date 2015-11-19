/**
 * Provides the classes necessary to parse  
 * a text document into Sentences/Words/Non-words
 * 
 * <p>
 * The DocumentTokenizer framework involves four entities:
 * the DocumentTokenizer.Document a container class for SentenceTokens
 * the DocumentTokenizer.SentenceToken a container class for Word/NonWord tokens
 * the DocumentTokenizer.WordToken which encapsulates a Word type
 * the DocumentTokenizer.NonWordToken which encapsulates a NonWord type 
 *       (e.g. !,"')
 * </p>
 *
 * @author Stephen Ince
 */
package digitalreasoning;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Locale;
import java.text.BreakIterator;

import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;


import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlSeeAlso;


/**
 * This is a factory class to produce a ProperNameDocumentTokenizer.Document object.
 *
 */

public class ProperNameDocumentTokenizer extends DocumentTokenizer {
    
    private static class ProperWordResult {
        String properWord;
        int index;
    }
    
    private static class WordNode {
        boolean isWord = false;
        String key;
        Map<String, WordNode> map = new HashMap<String,WordNode>();
        WordNode(String str) {
            key = str; 
        }
    }
    
    /**
     * Lazy loaded holder for the default Proper Names Dictionary
     * May not be needed if a dictionary parameter is passed into the constructor,
     * so it is optional loaded.
     */
    private static class DefaultProperNamesHolder {
        public static final Set<String> defaultProperNames = getValues();

        static final Set<String> getValues() {
            try (InputStreamReader rdr = 
                    new InputStreamReader( ProperNameDocumentTokenizer.class .getResourceAsStream("NER.txt"))) {
                return createDictionary(rdr);
            }
            catch (Exception e) {
                System.err.println("ProperNameDocumentTokenizer.DefaultProperNamesHolder: warning failed to load default dictionary NER.txt: " + e);
                e.printStackTrace();
            }
            // on error return empty dictionary
            return new HashSet<String>();
        }
    }
    
    /**
     * Utility method to create a dictionary
     * 
     * @param filename the file name of the dictionary
     * 
     * @return Set contains all the words in file.
     */
    static Set<String> createDictionary(String filename)
            throws IOException
    {
        try (Reader rdr = new FileReader(filename)) {
            return createDictionary(rdr);
        }
    }
    
    /**
     * Utility method to create a dictionary
     * 
     * @param rdr a reader to load the dictionary
     * 
     * @return Set contains all the words in file.
     */
    static Set<String> createDictionary(Reader rdr) throws IOException {
        BufferedReader bufRdr = new BufferedReader(rdr);
        Set<String> dict = new HashSet<String>();
        String line = null;
        while ((line = bufRdr.readLine()) != null) {
            line = line.trim();
            if ( line.length() == 0 ) continue;
            dict.add(line);
        }
        return dict;
    }   
    
    /**
     * Encapsulates a proper word type.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProperWordToken extends WordToken {
        public ProperWordToken(String t) {
            super(t);
        }
    }
    
    /**
     * word tokenizer, tokenizes a sen
     */
    final private BreakIterator wordPhraseIterator;
    
    /**
     * The proper name dictionary. The first word is used as a key for the proper name.
     */    
    private final Map<String, WordNode> properNamesMap = new HashMap<String, WordNode>();

    /**
     * Empty Constructor - uses Locale.US as the default locale for parsing,
     * uses the NER.txt as the default dictionary
     *  
     */
    public ProperNameDocumentTokenizer() {
        this( DefaultProperNamesHolder.defaultProperNames, Locale.US);
    }
    
    /**
     * Constructor - uses Locale.US as the default locale for parsing
     * @param properNames the proper names dictionary
     *  
     */

    public ProperNameDocumentTokenizer(Set<String> properNames) {
        this(properNames, Locale.US);
    }

    /**
     * Constructor - use the specified locale when parsing a text document.
     * 
     * @param properNames the proper names dictionary 
     * @param l the locale
     *            
     */
    public ProperNameDocumentTokenizer(Set<String> properNames, Locale l) {
        super(l);
        this.wordPhraseIterator = BreakIterator.getWordInstance(l);
        
        // loop-thru all the names in the properNames dictionary
        // store the values in a tree
        Map<String,WordNode> currentMap = properNamesMap;
        for( String str: properNames ){

            String key = str;
            String currentKey = "";
            String currentStr =  str;
            int idx = str.indexOf(' ');
                        
            // get the current word
            while ( idx != -1 ) {
                key = currentStr.substring(0,idx).trim();

                WordNode wordNode = currentMap.get(key);
                
                currentKey = (currentKey + " " + key).trim() ;
                if ( wordNode == null ) {
                    wordNode = new WordNode( currentKey );
                    currentMap.put(key,wordNode);
                }
                currentMap = wordNode.map;
                currentStr = currentStr.substring(idx+1).trim();
                idx = currentStr.indexOf(' ');
            }
            
            WordNode wordNode = currentMap.get(key);
            if ( wordNode == null ) {
                String currentWord = (currentKey + " " + currentStr).trim() ;
                wordNode = new WordNode( currentWord );
                currentMap.put(currentStr,wordNode);
            }

            wordNode.isWord = true;
            currentMap = properNamesMap;
        }
    }
    
    /**
     * Method to retrieve the proper name. It searches
     * the sentence source for all the strings under the key word. The 
     * key is the first word in the proper name.
     * 
     * @param word the key word, the first word in the proper name
     * @param source the sentence source.
     * @param idx the index starting point in the sentence
     * 
     * @return String a found proper name in the sentence source.
     *            
     */    
    ProperWordResult getProperWord(String word, String source, int idx) {

        WordNode wordNode = properNamesMap.get(word);
        // loop-thru all the all the prop names stored under the word key
        // return the the proper name if a match is found at the index
        if ( wordNode != null ) {            
            int currentIndex = 0;
            ProperWordResult result = new ProperWordResult();
            
            if ( wordNode.isWord ) {
                result.properWord = word;
            }
            
            this.wordPhraseIterator.setText(source);

            int firstIndex = wordPhraseIterator.following(idx);
            int lastIndex = wordPhraseIterator.next();

            // loop-thru all the tokens (words/non-words) and
            // add the tokens to the sentence token
            while (lastIndex != BreakIterator.DONE) {
                final String token = source.substring(firstIndex, lastIndex);
                
                if (Character.isLetterOrDigit(token.charAt(0))) {                    
                    wordNode = wordNode.map.get(token);
                    if ( wordNode == null ) break;
                    currentIndex += token.length();
                    if ( wordNode.isWord ) {
                        result.index = currentIndex;
                        result.properWord = wordNode.key;
                    }
                } 
                else if ( !token.matches("\\s+") ){
                    break;
                }
                else {
                    currentIndex += token.length();
                }
                
                firstIndex = lastIndex;
                lastIndex = this.wordPhraseIterator.next();
            }
            
            if ( result.properWord != null ) return result;
        }
        
        // return null if no match is found.
        return null;
    }
    
    /** 
     * Utility method to retrieve a list of proper names from a parsed Docoument.
     * 
     * @param doc a parsed Document
     * @return List a list of proper names in the Document
     * 
     */
    public static List<String> getAllProperNames( DocumentTokenizer.Document doc ){
        Set<String> set = new HashSet<String>();
        for ( WordToken tok: doc.getAllWords() ) {
            if ( tok instanceof ProperWordToken ) {
                set.add(tok.getText() );
            }
        }
        List<String> properNames = new ArrayList<String>(set);
        Collections.sort(properNames);
        return properNames;
    }

    /**
     * Parse a sentence string into a sentence object
     * 
     * @param source the string contents of a sentence
     *            
     * @return SentenceToken which encapsulates a sentence
     */
    SentenceToken parseSentence(String source) {

        final SentenceToken sentenceTok = new SentenceToken();
        
        BreakIterator wordIterator = getWordIterator();
        wordIterator.setText(source);
        
        int firstIndex = wordIterator.first();
        int lastIndex = wordIterator.next();
        int count = 0;
        // loop-thru all the tokens (words/non-words) and
        // add the tokens to the sentence token
        while (lastIndex != BreakIterator.DONE) {
            final String token = source.substring(firstIndex, lastIndex);
            String properWord = null;
            ProperWordResult wordResult = null;
            // check the case for a proper name
            if ( (wordResult = getProperWord(token, source, lastIndex )) != null ) {
                sentenceTok.tokens.add(new ProperWordToken(wordResult.properWord));
                // fast-forward to the index after the properWord
                firstIndex = wordIterator.following( lastIndex + wordResult.index );
                lastIndex = wordIterator.next();
            }
            // check the case for a word
            else if (Character.isLetterOrDigit(token.charAt(0))) {
                sentenceTok.tokens.add(new WordToken(token));
                firstIndex = lastIndex;
                lastIndex = wordIterator.next();
            }
            // else is a single character non-word token
            else {
                sentenceTok.tokens.add(new NonWordToken(token));
                firstIndex = lastIndex;
                lastIndex = wordIterator.next();
            }
        }
        return sentenceTok;
    }

    /**
     * Helper routine to xml pretty print a Document.
     * 
     * @param doc
     *            the Document object represents a text document
     * @param out
     *            the PrintStream which to print
     */
    public void outputXml(Document doc, PrintStream out)
            throws JAXBException {
        final JAXBContext jc = JAXBContext
                .newInstance(DocumentTokenizer.Document.class, ProperNameDocumentTokenizer.ProperWordToken.class);
        final Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(doc, out);
    }

    /**
     *  This is main method which tests the ProperNameDocumentTokenizer class.
     * 
     * @param args takes two arguments
     *      <p>args[0] the file word source parameter
     *      <p>args[1] is an optional dictionary parameter
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Missing filename parameter.");
            System.err.println("\tUsage: java digitalreasoning.ProperNameDocumentTokenizer <filename>");
            System.exit(-1);
        }
        
        try {
            final String filename = args[0];

            final DocumentTokenizer tokenizer = ( args.length > 1) 
                    ? new ProperNameDocumentTokenizer( createDictionary(args[1]) )
                    : new ProperNameDocumentTokenizer();
                                    
            final String fileContents = IOUtil.getFileContents(filename);
            
            final Document doc = tokenizer.parseDocument(fileContents);
            
            List<String> names = ProperNameDocumentTokenizer.getAllProperNames( doc );
            for( String n: names) {
                System.out.println(n);
            }
            //tokenizer .outputXml(doc, System.out);
        }
        catch (Exception e) {
            System.err.println("Exception: " + e);
            e.printStackTrace();
        }
    }
}