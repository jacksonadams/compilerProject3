package scanner;

import scanner.Token.TokenType;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        // Get the input file that has the C- code
        File inputFile = new File("scanner/input.txt");
        FileReader codeFile = new FileReader(inputFile);
        BufferedReader inputReader = new BufferedReader(codeFile);
        
        // Scan through the file for tokens
        CMinusScanner myScanner = new CMinusScanner(inputReader);
       
        // Get the output file to print into
        FileWriter outputFile = new FileWriter("scanner/output.txt"); 
        
        Token next = myScanner.getNextToken();
        while(next.getType() != TokenType.EOF_TOKEN){
            outputFile.write(next.toString());
            next = myScanner.getNextToken();
        }
        
        // Close output file
        outputFile.close();
    }
}