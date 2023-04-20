package parser;
import java.io.FileWriter;
import java.io.IOException;

import parser.CMinusParser.Program;

public interface Parser {
    public Program parse() throws Exception;
    public void printTree(FileWriter outputProgram) throws IOException;
    public void printAST(Program root) throws IOException;
}

