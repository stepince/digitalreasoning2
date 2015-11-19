/**
 * This contains the Unit Test class for testing ProperNameDocumentTokenizer
 * 
 */
package digitalreasoning;


public class TestProperNameDocumentTokenizer {

    public void testContains(String source, String word) {
        final DocumentTokenizer tokenizer = new ProperNameDocumentTokenizer();
        DocumentTokenizer.Document doc = tokenizer.parseDocument(source);
        assert( doc.getAllWordsAsText().contains(word) );
    }

    public void testNotContains(String source, String word) {        
        final DocumentTokenizer tokenizer = new ProperNameDocumentTokenizer();
        DocumentTokenizer.Document doc = tokenizer.parseDocument(source);
        assert( doc.getAllWordsAsText().contains(word) == false );
    }

    public static void main(String[] args) {
        try {
            TestProperNameDocumentTokenizer testTokenizer = new TestProperNameDocumentTokenizer();
            testTokenizer.testContains("hello world","hello");
            testTokenizer.testNotContains("hello word","planet");
            testTokenizer.testNotContains("Gavrilo Principe","Gavrilo Princip");
            testTokenizer.testContains("hello1 hello2 Ernst Haeckel, who claimed that ","Ernst Haeckel");
        }
        catch ( Exception e ) {
            System.err.println("Exception: " + e);
            e.printStackTrace();
        }
    }
}
