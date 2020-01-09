package tester;

import java.util.List;
import java.util.ArrayList;
import util.text.parser.src.SourceCodeParser;


public class Tester {
    private static boolean debugEnabled            = false; /* default value set */
    private static String commentFilePath          = null;  /* default value set */
    private static String sourceFilePath           = null;  /* default value set */
    private static String debugFilePath            = null;  /* default value set */
    private static final List<String> filePathList = new ArrayList<>();
    
    
    public static void main(String[] args) throws Exception {
        parseOptions(args);

		new SourceCodeParser(debugEnabled, 
                             sourceFilePath, commentFilePath, debugFilePath, 
                             filePathList)
                                    .process();
	}
    
    private static void parseOptions(final String[] args) {
        int i=0, len;
        try {
            for(i=0, len=args.length; i<len; i++) {
                switch(args[i]) {
                    case "-d":
                    case "--debug":
                        debugEnabled = true;
                        break;

                    case "-S":
                    case "--srcfile":
                        sourceFilePath = args[++i];                    
                        break;

                    case "-C":
                    case "--comfile":
                        commentFilePath = args[++i];
                        break;

                    case "-D":
                    case "--dbgfile":
                        debugFilePath = args[++i]; /* overrides default path */
                        break;

                    case "-h":
                    case "--help":
                        showHelpMessage();
                        System.exit(StandardExitCodes.NORMAL);

                    default:
                        filePathList.add(args[i]);
                }
            }
        } catch(IndexOutOfBoundsException e) {
            System.err.printf("Err: Argument for option '%s' not provided!\n", args[i-1]);
            System.exit(StandardExitCodes.ERROR);
        }
        
        if(!debugEnabled && debugFilePath != null) {
            System.err.println("Err: Debug file mentioned but mode not enabled!");
            System.exit(StandardExitCodes.ERROR);
        }
        
        if(filePathList.isEmpty()) {
            System.err.println("Err: Provide at least one file path.");
            System.exit(StandardExitCodes.ERROR);
        }
    }
    
    private static void showHelpMessage() {
        System.out.println("Source Code Parser");
        System.out.printf ("Version: %.2f\n", SourceCodeParser.APP_VERSION);
        System.out.println("Purpose: Strips off the comments from the source codes. "
                + "Standard C-style comments are considered i.e. a single line comment starts "
                + "with a double slash ('//') string sequence running till the end of line mark "
                + "and a multi-line comment starts with a slash and asterix ('/*') string sequence "
                + "and ends with a asterix and slash ('*/') string sequence.");
        System.out.println("Usage:   srcparser [option] <file1> [<file2> [<file3> ...]]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("    --debug,   -d                Enables the debug mode.");
        System.out.println("    --srcfile, -S <file-path>    File path to write source lines.");
        System.out.println("    --comfile, -C <file-path>    File path to write comment lines.");
        System.out.printf ("    --dbgfile, -D <file-path>    File path to write debug lines (default is %s).\n", SourceCodeParser.defaultDebugFilePath);
        System.out.println("    --help,    -h                Shows this help menu and exists.");
        System.out.println();
        StandardExitCodes.showMessage();
    }
}