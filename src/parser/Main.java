/* 
    Main Class

    Test the functionality of the parser by opening an 
    input file containing C- code using the Scanner from 
    Project 1, creating an abstract syntax tree, and then 
    printing that abstract syntax tree to a file.

    Design the output format so that it can be easily 
    understood and easily read back in.
*/

package parser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, IOException, Exception {

        // Get the output file to print into
        FileWriter outputFile = new FileWriter("parser/output.ast"); 

        // Create parser
        CMinusParser myParser = new CMinusParser("parser/input.txt");
        myParser.printTree(outputFile);

        // Close output file
        outputFile.close();
    }
}
