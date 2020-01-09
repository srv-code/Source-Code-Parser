package util.text.parser.src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PushbackReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;


/**
 * Does the basic parsing of the source code. <br>
 * Strips off any commented lines (both single and multi-line)
 *	from the source code.
 * */
public class SourceCodeParser {
    public static final float APP_VERSION = 1.10f;
    
	private final StringBuilder tmpCodeLine     = new StringBuilder();
	private final StringBuilder tmpCommentLine  = new StringBuilder();
	private final List<String>  codeLines       = new ArrayList<>();
	private final List<String>  commentLines    = new ArrayList<>();
    private final List<File>    fileList        = new ArrayList<>();

	private final boolean debugEnabled;
	private final PrintStream sourceStream;
	private final PrintStream commentStream;
	private final PrintStream debugStream;
    public  final static String defaultDebugFilePath = "debug.txt";

    
    public SourceCodeParser(final boolean      debugEnabled,
                            final String       sourceFilePath,
                            final String       commentFilePath,
                            final String       debugFilePath,
                            final List<String> filePathList) throws IOException {
        sourceStream  = sourceFilePath  == null ? System.out : new PrintStream(sourceFilePath);
        commentStream = commentFilePath == null ? System.out : new PrintStream(commentFilePath);
        
        this.debugEnabled = debugEnabled;
        debugStream = debugEnabled ? new PrintStream(debugFilePath == null ? defaultDebugFilePath : debugFilePath) : null;
        
        for(String path: filePathList) {
            File file = new File(path);
            if(!file.exists())
                throw new FileNotFoundException(path);
            fileList.add(file);
        }
	}
    
    private void printDebug(final String formatString, Object... args) {
		if(debugEnabled)
			debugStream.printf(formatString + "\n", args);
	}
    
    public void process() throws IOException {
        for(File file: fileList) {
            parse(file);
            printLines(file);
        }
    }

	/**
	 * Considerations: <br>
	 1. If a string or character sequence starts ignore all other sequence. <br>
	 2. If any of the comment type is started then continue with it (ignoring all other
	 sequences) until the ending sequence is found. </br>
	 <br><br>
	 * All states are maintained by a flag. All of which are mutex at any time,
	 noncompliance of which will throw a IllegalStateException
	 denoting a non-mutex condition. <br>
	 * No checks for syntactical validity, that should be done by the much smarter compiler
	 and is beyond the context of this program.
	 * */
	private void parse(final File file) throws IOException, IllegalStateException {
		try (PushbackReader reader = new PushbackReader(new FileReader(file))) {
			boolean	/* mutex flags */
					codeLineContinues        = true,
					charSequenceStarted      = false,
					stringSequenceStarted    = false,
					singleLineCommentStarted = false,
					multiLineCommentStarted  = false,
					/* other flags */
					escaping                 = false;
			int readChar;

			while ((readChar = reader.read()) != -1) {
				checkMutexState(charSequenceStarted, stringSequenceStarted,
								singleLineCommentStarted, multiLineCommentStarted, codeLineContinues);

				printDebug("-- readChar=%c (%<d), escaping=%b --", readChar, escaping);
				if (charSequenceStarted) {
					/* ignore all chars for comment checking */
					addCharToBuffer(readChar, tmpCodeLine, codeLines, false);

					/* search for char ending sequence */
					if (readChar == '\'' && !escaping) {
						codeLineContinues = true;
						charSequenceStarted = false;
						printDebug("  char sequence ended");
					} /* else - already added to buffer */
					else printDebug("  char sequence continues");
				} else if (stringSequenceStarted) {
					/* ignore all chars for comment checking */
					addCharToBuffer(readChar, tmpCodeLine, codeLines, false);

					/* search for string ending sequence */
					if (readChar == '"' && !escaping) {
						codeLineContinues = true;
						stringSequenceStarted = false;
						printDebug("  string sequence ended");
					} /* else - already added to buffer */
					else printDebug("  string sequence continues");
				} else if (singleLineCommentStarted) {
					/* put all the chars to 'comment buffer' */
					addCharToBuffer(readChar, tmpCommentLine, commentLines, readChar == '\n');

					/* search for single line comment ending sequence */
					if (readChar == '\n') {
						codeLineContinues = true;
						singleLineCommentStarted = false;
						printDebug("  single line comment ended");
						addCharToBuffer('\n', tmpCommentLine, commentLines, true);
					} /* else - already added to buffer */
					else printDebug("  single line comment continues");
				} else if (multiLineCommentStarted) {
					/* search for multi line comment ending sequence */
					if (readChar == '*') {
						if ((readChar = reader.read()) == '/') {
							codeLineContinues = true;
							multiLineCommentStarted = false;
							printDebug("  single line comment ended");
							addCharToBuffer('\n', tmpCommentLine, commentLines, true);
						} else {
							addCharToBuffer('*', tmpCommentLine, commentLines, false);
							reader.unread(readChar);
							printDebug("  multi line comment continues");
						}
					} else {
						/* put all the chars to 'comment buffer' */
						addCharToBuffer(readChar, tmpCommentLine, commentLines, readChar == '\n');
					}
				} else if (codeLineContinues) {
					/* put all the chars to 'code buffer'
						 check for start sequences for:
							char - '
							string - "
							single line comment - //
							multi line comment -  /*
					*/

					switch (readChar) {
						case '\'':
							charSequenceStarted = true;
							codeLineContinues = false;
							addCharToBuffer('\'', tmpCodeLine, codeLines, false);
							printDebug("  char sequence started");
							break;

						case '"':
							stringSequenceStarted = true;
							codeLineContinues = false;
							addCharToBuffer('"', tmpCodeLine, codeLines, false);
							printDebug("  string sequence started");
							break;

						case '/':
							printDebug("  suspecting a comment line");
							if ((readChar = reader.read()) == '/') {
								singleLineCommentStarted = true;
								codeLineContinues = false;
								addCharToBuffer('\n', tmpCodeLine, codeLines, true);
								printDebug("  single line comment started");
							} else if (readChar == '*') {
								multiLineCommentStarted = true;
								codeLineContinues = false;
								printDebug("  multi line comment started");
							} else { /* push back and continue normally */
								addCharToBuffer('/', tmpCodeLine, codeLines, false);
								reader.unread(readChar);
								printDebug("    wrong suspect");
							}
							break;

						default:
							addCharToBuffer(readChar, tmpCodeLine, codeLines, readChar == '\n');
							printDebug("  code line continues");
					}
				}

				if(escaping)
					escaping = false;
				else
					escaping = readChar=='\\';
			}
			/* flush all buffers */
			addCharToBuffer('\n', tmpCodeLine, codeLines, true);
			addCharToBuffer('\n', tmpCommentLine, commentLines, true);
		}
	}

	private void checkMutexState(boolean... flags) throws IllegalStateException {
		boolean alreadySet = false;
		for(boolean flagSet : flags) {
			if(flagSet)
				if(alreadySet)
					throw new IllegalStateException("Internal error: Non-mutex state of flags");
				else
					alreadySet = true;
		}
		if(!alreadySet)
			throw new IllegalStateException("Internal error: All flags are disabled");
	}

	private void addCharToBuffer(final int ch,
	                             final StringBuilder tmpLine,
	                             final List<String> lines,
	                             final boolean flush) {
		if(flush) {
			if(tmpLine.toString().trim().length()>0)
				lines.add(tmpLine.toString());
			printDebug("  tmpLine=<%s>, flushing...", tmpLine.toString());
			tmpLine.setLength(0); /* deletes all contents */
		} else {
			if(ch!='\r') /* shows unnecessary new lines in some text editors */
				tmpLine.append((char)ch);
			printDebug("  tmpLine=<%s>", tmpLine.toString());
		}
	}

	private void printLines(final File file) {
        if(fileList.size() > 1)
            sourceStream.printf("\n\n-----[File: Name=%s, Path=%s]-----\n", file.getName(), file.getAbsolutePath());
        
		sourceStream.printf("[Code lines (%d)]\n", codeLines.size());
		for(String line: codeLines)
			sourceStream.println(line);
        
		commentStream.printf("\n\n[Comment lines (%d)]\n", commentLines.size());
		for(String line: commentLines)
			commentStream.println(line);
	}
}
