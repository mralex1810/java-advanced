package info.kgeorgiy.ja.chulkov.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

public class Scanner implements Closeable {

    private final Reader reader;
    private String cachedNext;
    private int lineIndex = 0;
    private int readed = -1;

    public Scanner(final InputStream source) {
        reader = new BufferedReader(new InputStreamReader(source));
    }

    Scanner(final File source, final Charset charset) throws FileNotFoundException {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(source), charset));
    }

    /*
     * @return lineIndex of last cashed token
     */
    public int getLineIndex() {
        return lineIndex;
    }

    private void tryCacheNext(final CharPredicate allowingSymbFunc) {
        if (cachedNext != null) {
            return;
       	}
	checkLineSeparator();
        while (true) {
	    getNextChar();
            checkLineSeparator();
	    if (readed == -1 || allowingSymbFunc.test((char) readed)) {
		break;
	    }
	}
        final StringBuilder stringBuilder = new StringBuilder();
        while (readed != -1 && allowingSymbFunc.test((char) readed)) {
            stringBuilder.append((char) readed);
	    checkLineSeparator();
	    getNextChar();
	}    
	if (stringBuilder.isEmpty()) {
            cachedNext = null;
        } else {
            cachedNext = stringBuilder.toString();
        }
    }

    public boolean cachNext(final CharPredicate allowingSymbFunction) {
        tryCacheNext(allowingSymbFunction);
        return cachedNext != null;
    }

    public String next() {
        cachNext(new CharPredicate() {
            public boolean test(final char it) {
                return !Character.isWhitespace(it);
            }
        });
        if (cachedNext == null) {
            throw new NoSuchElementException();
        }
        final String ans = cachedNext;
        cachedNext = null;
        return ans;
    }

    private void getNextChar() {
	try {
            readed = reader.read();
	} catch (final IOException e) {
	    System.err.println("Error while reading symb" + e.getMessage());
	}
    }

    private void checkLineSeparator() {
        if (readed == -1) {
	    return;
	}
	char it = (char) readed;
	if (readed == '\r') {
	    lineIndex++;
	    final int next;
            try {
            	next = reader.read();
 	    } catch (final IOException e) {
	    	System.err.println("Error while reading symb" + e.getMessage());
	    	readed = -1;
		return;
	    }
	    if (next == -1 || next == '\n') {
                readed = next;
		return;
            }
            it = (char) readed; 
        }
        // https://en.wikipedia.org/wiki/Newline
        
	if (it == '\n' || it == '\u000B' || it == '\f' || it == '\r' || it == '\u0085' || it == '\u2028'
                || it == '\u2029') {
            lineIndex++;
        }
	readed = it;

        // return switch (buffer[readingIndex]) {
        // case '\n', '\u000B', '\f', '\r', '\u0085', '\u2028', '\u2029' -> true;
        // default -> false;
        // };

        // List<Character> lineSeparators = List.of('\n', '\u000B', '\f', '\r',
        // '\u0085', '\u2028', '\u2029');
        // return lineSeparators.stream().anyMatch((it) -> it == buffer[readingIndex]);

    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (final IOException e) {
            System.err.println("Reader close error: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
