package text.parser.src;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Tester {
	/* DEBUG */
	private static boolean DEBUG;
	private static PrintStream debugOut;

	private static void initDebugModule() throws IOException {
		DEBUG = true;
		debugOut = new PrintStream("debug.txt"); // System.out;
	}

	private static void d(final String fmtStr, Object ... args) {
		if(DEBUG)
			debugOut.printf("  // " + fmtStr + "\n", args);
	}
	/* DEBUG */

	StringBuilder tmpCodeLine = new StringBuilder();
	StringBuilder tmpCommentLine = new StringBuilder();
	List<String> codeLines = new ArrayList<>(), commentLines = new ArrayList<>();
	File file;


	/**
	 * Considerations: <br>
	 	1. If a string or character sequence starts ignore all other sequence. <br>
	 	2. If any of the comment type is started then continue with it (ignoring all other
	 		sequences) untill the ending sequence is found. </br>
	   <br><br>
	 * All states are maintained by a flag. All of which are mutex at any time,
	 	noncompliance of which will throw a IllegalStateException 
	 	denoting a non-mutex condition. <br>
	 * No checks for syntactical validity, that should be done by the much smarter compiler 
	 	and is beyond the context of this program.
	*/
	public void test2() throws IOException, IllegalStateException { // approach 2
		final String filename = "Real World Apps/src/text/parser/src/A.txt";
		file = new File(filename);

		try (PushbackReader reader = new PushbackReader(new FileReader(file))) {
			boolean	// mutex flags
					codeLineContinues = true,
					charSequenceStarted = false,
					stringSequenceStarted = false,
					singleLineCommentStarted = false,
					multiLineCommentStarted = false,
					// other flags
					escaping = false;
			int readChar;

			while ((readChar = reader.read()) != -1) {
				checkMutexState(charSequenceStarted, stringSequenceStarted, singleLineCommentStarted, multiLineCommentStarted, codeLineContinues);

				d("-- readChar=%c (%<d), escaping=%b --", readChar, escaping);
				if (charSequenceStarted) {
					// ignore all chars for comment checking
					addCharToBuffer(readChar, tmpCodeLine, codeLines, false);

					// search for char ending sequence
					if (readChar == '\'' && !escaping) {
						codeLineContinues = true;
						charSequenceStarted = false;
						d("  char sequence ended");
					} // else - already added to buffer
					else d("  char sequence continues");
				} else if (stringSequenceStarted) {
					// ignore all chars for comment checking
					addCharToBuffer(readChar, tmpCodeLine, codeLines, false);

					// search for string ending sequence
					if (readChar == '"' && !escaping) {
						codeLineContinues = true;
						stringSequenceStarted = false;
						d("  string sequence ended");
					} // else - already added to buffer
					else d("  string sequence continues");
				} else if (singleLineCommentStarted) {
					// put all the chars to 'comment buffer'
					addCharToBuffer(readChar, tmpCommentLine, commentLines, readChar == '\n');

					// search for single line comment ending sequence
					if (readChar == '\n') {
						codeLineContinues = true;
						singleLineCommentStarted = false;
						d("  single line comment ended");
						addCharToBuffer('\n', tmpCommentLine, commentLines, true);
					} // else - already added to buffer
					else d("  single line comment continues");
				} else if (multiLineCommentStarted) {
					// search for multi line comment ending sequence
					if (readChar == '*') {
						if ((readChar = reader.read()) == '/') {
							codeLineContinues = true;
							multiLineCommentStarted = false;
							d("  single line comment ended");
							addCharToBuffer('\n', tmpCommentLine, commentLines, true);
						} else {
							addCharToBuffer('*', tmpCommentLine, commentLines, false);
							reader.unread(readChar);
							d("  multi line comment continues");
						}
					} else {
						// put all the chars to 'comment buffer'
						addCharToBuffer(readChar, tmpCommentLine, commentLines, readChar == '\n');
					}
				} else if (codeLineContinues) {
					// put all the chars to 'code buffer'
					// check for start sequences for:
					//		char - '
					//		string - "
					//		single line comment - //
					//		multi line comment -  /*

					switch (readChar) {
						case '\'':
							charSequenceStarted = true;
							codeLineContinues = false;
							addCharToBuffer('\'', tmpCodeLine, codeLines, false);
							d("  char sequence started");
							break;

						case '"':
							stringSequenceStarted = true;
							codeLineContinues = false;
							addCharToBuffer('"', tmpCodeLine, codeLines, false);
							d("  string sequence started");
							break;

						case '/':
							d("  suspecting a comment line");
							if ((readChar = reader.read()) == '/') {
								singleLineCommentStarted = true;
								codeLineContinues = false;
//								if(tmpCodeLine.toString().trim().length()==0)
								addCharToBuffer('\n', tmpCodeLine, codeLines, true);
								d("  single line comment started");
							} else if (readChar == '*') {
								multiLineCommentStarted = true;
								codeLineContinues = false;
								d("  multi line comment started");
							} else { // push back and continue normally
								addCharToBuffer('/', tmpCodeLine, codeLines, false);
								reader.unread(readChar);
								d("    wrong suspect");
							}
							break;

						default:
							addCharToBuffer(readChar, tmpCodeLine, codeLines, readChar == '\n');
							d("  code line continues");
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

	public void test1() throws IOException { // approach 1
		final String filename = "Real World Apps/src/text/parser/src/A.txt";
		file = new File(filename);
		boolean commentStarted = false, multiLineCommentStarted = false, singleLineCommentStarted = false;
		StringBuilder currentTmpLine = tmpCodeLine;
		boolean hasStringSequenceStarted = false;
		boolean seemsLikeComment = false;

		try (FileReader reader = new FileReader(file)) {
			int readChar;
			while ((readChar = reader.read()) != -1) {
				d("-- char read=%c (%d) --", (char)readChar, readChar);
				if (singleLineCommentStarted) {
					if (readChar == '\n') {
						singleLineCommentStarted = false;
						d("  single line comment ended");
						addCharToBuffer(0, tmpCommentLine, commentLines, true);
						addCharToBuffer(0, tmpCodeLine, codeLines, true);
						currentTmpLine = tmpCodeLine;
						continue;
					} else {
						d("  single line comment continues...");
						addCharToBuffer(readChar, tmpCommentLine, commentLines, false);
						continue;
					}
				} else {
					d("  still writing to code lines");
					if (readChar == '/') {
						readChar = reader.read();
						if(readChar == '/') {
							singleLineCommentStarted = true;
							currentTmpLine = tmpCommentLine;
							d("  single line comment started");
							continue;
						} else {
							// write out the suspected char to code line buffer
							d("  code line continues...");
							addCharToBuffer(readChar, tmpCodeLine, codeLines, false);
						}
					} else {
						d("  code line continues..., will flush?=%b", readChar=='\n');
						addCharToBuffer(readChar, tmpCodeLine, codeLines, readChar == '\n');
					}
				}
			}
			addCharToBuffer('\n', tmpCodeLine, codeLines, true);
			addCharToBuffer('\n', tmpCommentLine, commentLines, true);
		}
	}

	private void checkMutexState(boolean ... flags) throws IllegalStateException {
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

	private void addCharToBuffer(   final int ch,
			                        final StringBuilder tmpLine,
			                        final List<String> lines,
			                        final boolean flush) {
		if(flush) {
			if(tmpLine.toString().trim().length()>0)
				lines.add(tmpLine.toString());
			d("  tmpLine=<%s>, flushing...", tmpLine.toString());
			tmpLine.setLength(0); // deletes all contents
		} else {
			// if(ch!='\r')
			tmpLine.append((char)ch);
			d("  tmpLine=<%s>", tmpLine.toString());
		}
	}

	private static void display(Tester tester) {
		System.out.printf("[Code lines (%d)]]\n", tester.codeLines.size());
		for(String line : tester.codeLines)
			System.out.println(line);

		System.out.printf("\n[Comment lines (%d)]\n", tester.commentLines.size());
		for(String line : tester.commentLines)
			System.out.println(line);
	}

	public static void main(String[] args) throws IOException {
		initDebugModule();
		Tester tester = new Tester();
		tester.test2();
		display(tester);
	}
}