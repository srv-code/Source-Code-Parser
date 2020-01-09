# Source Code Parser
## Synopsis
A utility program parse a given source file and strip off the source and comment lines from the file.  
Standard C-style comments are considered i.e. a single line comment starts with a double slash ('//') string sequence running till the end of line mark and a multi-line comment starts with a slash and asterix ('/*') string sequence and ends with a asterix and slash ('*/') string sequence.

## Features
- Option to enable debug mode, i.e. to show the information about each major step being carried out.
- Option to receive the file path where the striiped of source lines needs to be stored.
- Option to receive the file path where the striiped of comment lines needs to be stored.
- Option to receive the file path where the debugging information needs to be stored.

### Default behavior
- The debug mode is disabled.
- If debug mode is enabled then the debug information will be stored in a file named 'debug.txt' under the current directory.
- The source and the comment lines are written to the standard output stream.