package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lowlevel.*;
import scanner.CMinusScanner;
import scanner.Token;
import scanner.Token.TokenType;

public class CMinusParser implements Parser {
    /*
        This is a recursive descent parser.
        Given a program written in the C- language,
        Create an Abstract Syntax Tree (AST).

        Use the Scanner from Project #1 to scan tokens.

        Classes
        1. Program
        2. Param
        3. Decl
        4. VarDecl
        5. FunDecl
        6. Statement
        7. ExpressionStmt
        8. CompoundStmt
        9. SelectionStmt
        10. IterationStmt
        11. ReturnStmt
        12. Expression
        13. AssignExpression
        14. BinaryExpression
        15. CallExpression
        16. NumExpression
        17. VarExpression

        Parse Functions
        1. parseProgram
        2. parseDecl
        3. parseDecl2
        4. parseVarDecl
        5. parseFunDecl
        6. parseParams
        7. parseParamList
        8. parseParam
        9. parseCompoundStmt
        10. parseLocalDecl
        11. parseStatementList
        12. parseStatement
        13. parseExpressionStmt
        14. parseSelectionStmt
        15. parseIterationStmt
        16. parseReturnStmt
        17. parseExpression
        18. parseExpression2
        19. parseExpression3
        20. parseSimpleExpr2
        21. parseAdditiveExpr
        22. parseAdditiveExpr2
        23. parseTerm
        24. parseTerm2
        25. parseFactor
        26. parseVarCall
        27. parseArgs
        28. parseArgList

        Extra Functions
        1. matchToken() - check and advance
        2. advanceToken() - just advance
        3. checkToken() - just check
     */

    /* Constructor */
    private CMinusScanner scanner;
    public Program program;
    public HashMap < TokenType, String > ops = new HashMap < TokenType, String > ();
    public String INDENT = "    ";
    public FileWriter outputFile;
    public HashMap<String, Integer> symbolTable = new HashMap<String, Integer>();

    public CMinusParser(String fileName) throws Exception {
        File inputFile = new File(fileName);
        FileReader codeFile = new FileReader(inputFile);
        BufferedReader inputReader = new BufferedReader(codeFile);
        scanner = new CMinusScanner(inputReader);

        // program = parse();

        // Add keys and values (TokenType, Character)
        ops.put(TokenType.PLUS_TOKEN, "+");
        ops.put(TokenType.MINUS_TOKEN, "-");
        ops.put(TokenType.MULT_TOKEN, "*");
        ops.put(TokenType.DIVIDE_TOKEN, "/");
        ops.put(TokenType.NOT_EQUAL_TOKEN, "!=");
        ops.put(TokenType.EQUAL_TOKEN, "==");
        ops.put(TokenType.GREATER_EQUAL_TOKEN, ">=");
        ops.put(TokenType.GREATER_TOKEN, ">");
        ops.put(TokenType.LESS_EQUAL_TOKEN, "<=");
        ops.put(TokenType.LESS_TOKEN, "<");
    }

    /* Helper functions */
    public Boolean checkToken(TokenType token) {
        return (scanner.viewNextToken().getType() == token);
    }
    public Token advanceToken() {
        return scanner.getNextToken();
    }
    public Token matchToken(TokenType token) throws Exception {
        Token nextToken = scanner.getNextToken();
        if (nextToken.getType() != token) {
            throw new Exception("Error: Unexpected token. Was expecting " + token.toString() + " but got " + nextToken.toString());
        }
        return nextToken;
    }

    /* 17 classes */
    public class Program {
        public ArrayList<Decl> decls;

        public Program(ArrayList<Decl> decls) {
            this.decls = decls;
        }

        // Filler code just to let the program compile
        public CodeItem genLLCode() throws Exception {
            CodeItem headItem = null;

            // Loop through our array list of decls and categorize
            // them into Data (var_decl) or functions (fun_decl)
            if(decls.size() > 0){
                // Get the first decl
                Decl firstDecl = decls.get(0);

                headItem = firstDecl.genLLCode();
                CodeItem lastDecl = headItem;

                if(firstDecl instanceof VarDecl){
                    VarDecl firstVar = (VarDecl) firstDecl;
                    symbolTable.put(firstVar.name.var, symbolTable.size());
                }
                
                // Get the remaining decls
                for(int i = 1; i < decls.size(); i++){
                    Decl curDecl = decls.get(i);

                    // Check if it's a global variable then add it in the symbol table
                    if(decls.get(i) instanceof VarDecl){
                        VarDecl curVar = (VarDecl)curDecl;
                        symbolTable.put(curVar.name.var, symbolTable.size());
                    }

                    CodeItem nextItem = curDecl.genLLCode();
                    lastDecl.setNextItem(nextItem);
                    lastDecl = nextItem;
                }
            }

            return headItem;
        }

        void print() throws IOException {
            outputFile.write("Program {\n");
            for (int i = 0; i < decls.size(); i++) {
                if (decls.get(i) != null) {
                    decls.get(i).print("");
                }
            }
            outputFile.write("}\n");
        }
    }

    public class Param {
        // example: int x
        public VarExpression name;
        private int regNum;

        public Param(VarExpression name) {
            this.name = name;
        }

        public int getRegNum(){
            return regNum;
        }
        public void setRegNum(int num){
            regNum = num;
        }

        void genLLCode(Function func){
            this.setRegNum(func.getNewRegNum());
            func.getTable().put(name.var, this.getRegNum());
        }

        void print(String parentSpace) throws IOException {
            this.name.print(parentSpace + INDENT + "int ");
        }
    }

    abstract class Decl {
        // abstract, will be one of the other two decls
        abstract CodeItem genLLCode() throws Exception;
        abstract void print(String parentSpace) throws IOException;
    }

    public class VarDecl extends Decl {
        // example: int x; or int x[10];
        public VarExpression name;

        public VarDecl(VarExpression name) {
            this.name = name;
        }

        public CodeItem genLLCode(){
            Data data = new Data(Data.TYPE_INT, name.var);
            

            return data;
        }

        void print(String parentSpace) throws IOException {
            outputFile.write(parentSpace + INDENT + "int\n");
            this.name.print(parentSpace + INDENT);
        }
    }

    public class FunDecl extends Decl {
        // example: int gcd (int x, int y) { }
        // we need return type, function name, params, and compound statement
        String returnType;
        VarExpression name;
        List < Param > params;
        CompoundStmt content;

        public FunDecl(String returnType, VarExpression name, List < Param > params, CompoundStmt content) {
            this.returnType = returnType;
            this.name = name;
            this.params = params;
            this.content = content;
        }

        public CodeItem genLLCode() throws Exception {
            FuncParam firstParam = null;

            // Get the first parameter
            if(params != null){
                Param param = params.get(0);
                firstParam = new FuncParam(Data.TYPE_INT, param.name.var);
            }

            // Get the first function
            int type = (returnType == "void") ? Data.TYPE_VOID : Data.TYPE_INT;
            Function func = new Function(type, name.var, firstParam);

            if(params != null){
                FuncParam lastParam = firstParam;

                for(int i = 0; i < params.size(); i ++){
                    Param param = params.get(i);
                    param.genLLCode(func);

                    if(i > 0){
                        FuncParam nextParam = new FuncParam(Data.TYPE_INT, param.name.var);
                        lastParam.setNextParam(nextParam);
                        lastParam = nextParam;
                    }
                }
            }

            func.createBlock0();
            BasicBlock block = new BasicBlock(func);

            func.appendBlock(block);
            func.setCurrBlock(block);

            this.content.genLLCode(func);

            func.appendBlock(func.getReturnBlock());

            BasicBlock ucBlock = func.getFirstUnconnectedBlock();
            if(ucBlock != null){
                func.appendBlock(ucBlock);
            }

            return func;
        }

        void print(String parentSpace) throws IOException {
            String mySpace = parentSpace + INDENT;
            outputFile.write(mySpace + "function " + this.returnType + "\n");
            name.print(mySpace);
            outputFile.write(mySpace + INDENT + "Params (\n");
            if (this.params != null) {
                for (int i = 0; i < params.size(); i++) {
                    params.get(i).print(mySpace);
                }
            }
            outputFile.write(mySpace + INDENT + ")\n");
            content.print(mySpace);
        }
    }

    abstract class Statement {
        // abstract, will be one of the other 5 statements
        abstract void print(String parentSpace) throws IOException;
        abstract void genLLCode(Function func) throws Exception;

        private int regNum;
        public void setRegNum(int num){
            regNum = num;
        }
        public int getRegNum(){
            return regNum;
        }
    }

    public class ExpressionStmt extends Statement {
        // example: a + 3;

        public Expression statement;
        public ExpressionStmt(Expression statement) {
            this.statement = statement;
        }

        public void genLLCode(Function func) throws Exception {
            this.statement.genLLCode(func);
        }

        void print(String parentSpace) throws IOException {
            this.statement.print(parentSpace);
        }
    }

    public class CompoundStmt extends Statement {
        // a sequence of other statements inside { }
        // example: { x = 3; y = y + 3; }
        ArrayList < Decl > localDecls = new ArrayList < Decl > ();
        ArrayList < Statement > statements = new ArrayList < Statement > ();
        public CompoundStmt(ArrayList < Decl > localDecls, ArrayList < Statement > statements) {
            this.localDecls = localDecls;
            this.statements = statements;
        }

        void print(String parentSpace) throws IOException {
            String mySpace = parentSpace + INDENT;
            outputFile.write(mySpace + "{\n");
            for (int i = 0; i < localDecls.size(); i++) {
                localDecls.get(i).print(mySpace);
            }
            for (int i = 0; i < statements.size(); i++) {
                statements.get(i).print(mySpace);
            }
            outputFile.write(mySpace + "}\n");
        }

        public void genLLCode(Function func) throws Exception {
            // Get local decls and put them in local symbol table
            HashMap<String, Integer> localTable = func.getTable();
            for(int i = 0; i < localDecls.size(); i++){
                VarDecl curDecl = (VarDecl) localDecls.get(i);
                localTable.put(curDecl.name.var, func.getNewRegNum());
            }

            for(int i = 0; i < statements.size(); i++){
                if(statements.get(i) == null){
                    int zzz = 0;
                }
                statements.get(i).genLLCode(func);
            }
        }
    }

    public class SelectionStmt extends Statement {
        // example: if (statement) { } else { }
        public Expression condition;
        public Statement ifSequence;
        public Statement elseSequence;

        public SelectionStmt(Expression condition, Statement ifSequence, Statement elseSequence) {
            this.condition = condition;
            this.ifSequence = ifSequence;
            this.elseSequence = elseSequence;
        }
        public SelectionStmt(Expression condition, Statement ifSequence) {
            this.condition = condition;
            this.ifSequence = ifSequence;
        }

        public void genLLCode(Function func) throws Exception{
            BasicBlock currBlock = func.getCurrBlock();

            // Generate condition's LL code
            this.condition.genLLCode(func);

            // Create all blocks
            BasicBlock ifBlock = new BasicBlock(func);
            BasicBlock elseBlock = null;
            if(elseSequence != null){
                elseBlock = new BasicBlock(func);
            }
            BasicBlock postBlock = new BasicBlock(func);

            // Create a BEQ block
            // If the condition is false, jump to else block (if it exists) or post block
            Operation branchEqualOp = new Operation(Operation.OperationType.BEQ, currBlock);
            /*if(this.condition instanceof BinaryExpression){
                BinaryExpression binaryExpr = (BinaryExpression) this.condition;
                branchEqualOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, binaryExpr.LHS.getRegNum()));
                branchEqualOp.setSrcOperand(1, new Operand(Operand.OperandType.REGISTER, binaryExpr.RHS.getRegNum()));
            } else {
                branchEqualOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, this.condition.getRegNum()));
                branchEqualOp.setSrcOperand(1, new Operand(Operand.OperandType.INTEGER, 0));
            }*/
            branchEqualOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, this.condition.getRegNum()));
            branchEqualOp.setSrcOperand(1, new Operand(Operand.OperandType.INTEGER, 0));
            branchEqualOp.setSrcOperand(2, new Operand(Operand.OperandType.BLOCK, elseBlock == null ? postBlock.getBlockNum() : elseBlock.getBlockNum()));
            currBlock.appendOper(branchEqualOp);

            func.appendToCurrentBlock(ifBlock);
            func.setCurrBlock(ifBlock);
            this.ifSequence.genLLCode(func);

            func.appendToCurrentBlock(postBlock);
            
            if(this.elseSequence != null){
                func.setCurrBlock(elseBlock);
                this.elseSequence.genLLCode(func);

                Operation jumpOp = new Operation(Operation.OperationType.JMP, elseBlock);
                jumpOp.setSrcOperand(0, new Operand(Operand.OperandType.BLOCK, postBlock.getBlockNum()));
                func.getCurrBlock().appendOper(jumpOp);
                func.appendUnconnectedBlock(elseBlock);
            }

            func.setCurrBlock(postBlock);
        }

        void print(String parentSpace) throws IOException {
            String mySpace = INDENT + parentSpace;
            outputFile.write(mySpace + "if (\n");
            this.condition.print(mySpace);
            outputFile.write(mySpace + ")\n");
            this.ifSequence.print(mySpace);
            if (this.elseSequence != null) {
                outputFile.write(mySpace + "else\n");
                this.elseSequence.print(mySpace);
            }
        }
    }

    public class IterationStmt extends Statement {
        // example: while (x > 0) { }
        Expression condition;
        Statement sequence;
        public IterationStmt(Expression condition, Statement sequence) {
            this.condition = condition;
            this.sequence = sequence;
        }

        public void genLLCode(Function func) throws Exception {
            //BasicBlock conditionBlock = new BasicBlock(func);
            BasicBlock postBlock = new BasicBlock(func);
            //func.appendToCurrentBlock(conditionBlock);
            //func.setCurrBlock(conditionBlock);

            this.condition.genLLCode(func);

            Operation branchEqualOp = new Operation(Operation.OperationType.BEQ, postBlock);
            branchEqualOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, this.condition.getRegNum()));
            branchEqualOp.setSrcOperand(1, new Operand(Operand.OperandType.INTEGER, 0));
            branchEqualOp.setSrcOperand(2, new Operand(Operand.OperandType.BLOCK, postBlock.getBlockNum()));
            func.getCurrBlock().appendOper(branchEqualOp);

            BasicBlock sequenceBlock = new BasicBlock(func);
            func.appendToCurrentBlock(sequenceBlock);
            func.setCurrBlock(sequenceBlock);
            this.sequence.genLLCode(func);

            this.condition.genLLCode(func);
            Operation jumpOp = new Operation(Operation.OperationType.BNE, sequenceBlock);
            jumpOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, this.condition.getRegNum()));
            jumpOp.setSrcOperand(1, new Operand(Operand.OperandType.INTEGER, 0));
            jumpOp.setSrcOperand(2, new Operand(Operand.OperandType.BLOCK, sequenceBlock.getBlockNum()));
            func.getCurrBlock().appendOper(jumpOp);

            func.appendToCurrentBlock(postBlock);
            func.setCurrBlock(postBlock);
        }

        void print(String parentSpace) throws IOException {
            String mySpace = INDENT + parentSpace;
            outputFile.write(mySpace + "while\n");
            outputFile.write(mySpace + "(\n");
            this.condition.print(mySpace);
            outputFile.write(mySpace + ")\n");
            this.sequence.print(mySpace);
        }
    }

    public class ReturnStmt extends Statement {
        // example: return x;
        // could also be blank: return;
        Expression LHS;
        public ReturnStmt() {}
        public ReturnStmt(Expression LHS) {
            this.LHS = LHS;
        }

        public void genLLCode(Function func) throws Exception {
            BasicBlock currBlock = func.getCurrBlock();

            if(LHS != null){
                LHS.genLLCode(func);
                Operation returnOp = new Operation(Operation.OperationType.ASSIGN, currBlock);
                returnOp.setDestOperand(0, new Operand(Operand.OperandType.MACRO, "RetReg"));
                returnOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, LHS.getRegNum()));
                currBlock.appendOper(returnOp);
            }

            Operation jumpOp = new Operation(Operation.OperationType.JMP, currBlock);
            jumpOp.setSrcOperand(0, new Operand(Operand.OperandType.BLOCK, func.getReturnBlock().getBlockNum()));
            currBlock.appendOper(jumpOp);
        }

        void print(String parentSpace) throws IOException {
            String mySpace = INDENT + parentSpace;
            outputFile.write(mySpace + "return\n");
            if (this.LHS != null) {
                this.LHS.print(mySpace);
            }
        }
    }

    abstract class Expression {
        // abstract expression, will be one of the other 5
        abstract void print(String parentSpace) throws IOException;
        abstract void genLLCode(Function func) throws Exception;

        // every expression should have a regnum field
        private int regNum;

        void setRegNum(int num){
            regNum = num;
        }
        int getRegNum(){
            return regNum;
        }
    }

    public class AssignExpression extends Expression {
        // example: x = y, x = 3
        // has to be a var on the left
        VarExpression LHS;
        Expression RHS;

        public AssignExpression(VarExpression LHS, Expression RHS) {
            this.LHS = LHS;
            this.RHS = RHS;
        }

        public void genLLCode(Function func) throws Exception{
            // if local, make assign oper, lhs.reg = rhs.reg (move)
            // annotate with lhs.reg
            // if global, if a = R3, store R3 (source 0) into a (source 1)
            // annotate with R3
            // COME BACK TO THIS bc it's basically just turning the LHS into a pointer
            // this.LHS.genLLCode(func);

            this.RHS.genLLCode(func);

            // Add assign operation
            BasicBlock currBlock = func.getCurrBlock();

            // Left hand side is in local table
            if(func.getTable().containsKey(this.LHS.var)){
                int reg = func.getTable().get(this.LHS.var);
                Operation assignOp = new Operation(Operation.OperationType.ASSIGN, currBlock);
                assignOp.setDestOperand(0, new Operand(Operand.OperandType.REGISTER, reg));
                assignOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, this.RHS.getRegNum()));
                currBlock.appendOper(assignOp);
                setRegNum(reg);
            } else {
                Operation storeOp = new Operation(Operation.OperationType.STORE_I, currBlock);
                storeOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, this.RHS.getRegNum()));
                storeOp.setSrcOperand(1, new Operand(Operand.OperandType.STRING, this.LHS.var));
                currBlock.appendOper(storeOp);
                setRegNum(RHS.getRegNum());
            }

            //this.LHS.setRegNum(this.RHS.getRegNum());
        }

        void print(String parentSpace) throws IOException {
            String mySpace = INDENT + parentSpace;
            outputFile.write(mySpace + "=\n");
            this.LHS.print(mySpace);
            this.RHS.print(mySpace);
        }
    }

    public class BinaryExpression extends Expression {
        // example: 3 + 4, a + b
        Expression LHS;
        TokenType op;
        Expression RHS;

        public BinaryExpression(Expression LHS, TokenType op, Expression RHS) {
            this.LHS = LHS;
            this.op = op;
            this.RHS = RHS;
        }

        public void genLLCode(Function func) throws Exception {
            // rNew = a + b
            // 
            this.LHS.genLLCode(func);
            this.RHS.genLLCode(func);

            this.setRegNum(func.getNewRegNum());

            Operation newOper = null;
            BasicBlock currBlock = func.getCurrBlock();
            switch(this.op){
                case PLUS_TOKEN:
                    newOper = new Operation(Operation.OperationType.ADD_I, currBlock);
                    break;
                case MINUS_TOKEN:
                    newOper = new Operation(Operation.OperationType.SUB_I, currBlock);
                    break;
                case MULT_TOKEN:
                    newOper = new Operation(Operation.OperationType.MUL_I, currBlock);
                    break;
                case DIVIDE_TOKEN:
                    newOper = new Operation(Operation.OperationType.DIV_I, currBlock);
                    break;
                case LESS_TOKEN:
                    newOper = new Operation(Operation.OperationType.LT, currBlock);
                    break;
                case LESS_EQUAL_TOKEN:
                    newOper = new Operation(Operation.OperationType.LTE, currBlock);
                    break;
                case GREATER_TOKEN:
                    newOper = new Operation(Operation.OperationType.GT, currBlock);
                    break;
                case GREATER_EQUAL_TOKEN:
                    newOper = new Operation(Operation.OperationType.GTE, currBlock);
                    break;
                case EQUAL_TOKEN:
                    newOper = new Operation(Operation.OperationType.EQUAL, currBlock);
                    break;
                case NOT_EQUAL_TOKEN:
                    newOper = new Operation(Operation.OperationType.NOT_EQUAL, currBlock);
                    break;
                default:
                    break;
            }
            newOper.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, this.LHS.getRegNum()));
            newOper.setSrcOperand(1, new Operand(Operand.OperandType.REGISTER, this.RHS.getRegNum()));
            newOper.setDestOperand(0, new Operand(Operand.OperandType.REGISTER, this.getRegNum()));
            currBlock.appendOper(newOper);
        }

        void print(String parentSpace) throws IOException {
            String mySpace = parentSpace + INDENT;
            outputFile.write(mySpace + ops.get(this.op) + "\n");
            this.LHS.print(mySpace);
            this.RHS.print(mySpace);
        }
    }

    public class CallExpression extends Expression {
        // example: gcd(3, 4)
        VarExpression LHS;
        ArrayList < Expression > args;

        public CallExpression(VarExpression LHS, ArrayList < Expression > args) {
            this.LHS = LHS;
            this.args = args;
        }

        public void genLLCode(Function func) throws Exception {
            // pass, pass, rNew = retReg
            // retReg holds return value, register number of call expression

            // Get current block
            BasicBlock currBlock = func.getCurrBlock();

            // Generate code on each argument
            if(args != null){
                for(int i = 0; i < args.size(); i ++){
                    Expression arg = args.get(i);

                    arg.genLLCode(func);
                    Operation passOp = new Operation(Operation.OperationType.PASS, currBlock);
                    passOp.setSrcOperand(0, new Operand(Operand.OperandType.REGISTER, arg.getRegNum()));
                    passOp.addAttribute(new Attribute("PARAM_NUM", i + ""));
                    currBlock.appendOper(passOp);
                }

            }

            Operation callOp = new Operation(Operation.OperationType.CALL, currBlock);
            callOp.setSrcOperand(0, new Operand(Operand.OperandType.STRING, this.LHS.var));
            callOp.addAttribute(new Attribute("numParams", args.size() + ""));

            currBlock.appendOper(callOp);

            int register = func.getNewRegNum();
            Operation assignOp = new Operation(Operation.OperationType.ASSIGN, func.getCurrBlock());
            Operand dest = new Operand(Operand.OperandType.REGISTER, register);
            Operand src = new Operand(Operand.OperandType.MACRO, "RetReg");
            assignOp.setDestOperand(0, dest);
            assignOp.setSrcOperand(0, src);
            func.getCurrBlock().appendOper(assignOp);
            this.setRegNum(register);
        }

        void print(String parentSpace) throws IOException {
            String mySpace = INDENT + parentSpace;
            this.LHS.print(parentSpace);
            outputFile.write(mySpace + "(\n");
            for (int i = 0; i < args.size(); i++) {
                args.get(i).print(mySpace);
            }
            outputFile.write(mySpace + ")\n");
        }
    }

    public class NumExpression extends Expression {
        // new register = 1
        // annotate with register number
        // example: 3
        int num;
        public NumExpression(int num) {
            this.num = num;
        }

        public void genLLCode(Function func){
            // note - duplicate numbers may cause problems
            // HashMap<String, Integer> localTable = func.getTable();
            int register = func.getNewRegNum();
            // localTable.put(num + "", register);
            Operation assignOp = new Operation(Operation.OperationType.ASSIGN, func.getCurrBlock());
            Operand dest = new Operand(Operand.OperandType.REGISTER, register);
            Operand src = new Operand(Operand.OperandType.INTEGER, num);
            assignOp.setDestOperand(0, dest);
            assignOp.setSrcOperand(0, src);
            func.getCurrBlock().appendOper(assignOp);
            this.setRegNum(register);
        }

        void print(String parentSpace) throws IOException {
            outputFile.write(INDENT + parentSpace + this.num + "\n");
        }
    }

    public class VarExpression extends Expression {
        // if in local table, annotate yourself with register number in local table
        // if in global table, rNew = load A, annotate with rNew

        // example: x or x[10] or x[]
        String var;
        Expression num;
        Boolean blankArray = false;

        public VarExpression(String var) {
            this.var = var;
        }
        public VarExpression(String var, Expression num) {
            this.var = var;
            this.num = num;
        }
        public VarExpression(String var, Boolean blankArray) {
            this.var = var;
            this.blankArray = blankArray;
        }

        public void genLLCode(Function func) throws Exception{

            HashMap<String, Integer> localTable = func.getTable();
            BasicBlock currentBlock = func.getCurrBlock();
            int register = -1;

            // check for global variables
            if(localTable.containsKey(var)){
                register = localTable.get(var);
                this.setRegNum(register);
            } else {
                int rNew = func.getNewRegNum();
                register = symbolTable.get(var);
                Operation loadOp = new Operation(Operation.OperationType.LOAD_I, currentBlock);
                loadOp.setSrcOperand(0, new Operand(Operand.OperandType.STRING, this.var));
                loadOp.setDestOperand(0, new Operand(Operand.OperandType.REGISTER, rNew));
                currentBlock.appendOper(loadOp);
                this.setRegNum(rNew);
            }

            // throw error if variable doesn't exist
            if(register == -1){
                throw new Exception("Variable " + this.var + " doesn't exist.");
            }
        }

        void print(String parentSpace) throws IOException {
            if (this.blankArray){
                outputFile.write(INDENT + parentSpace + this.var + "[]\n");
            } else if (this.num == null) {
                outputFile.write(INDENT + parentSpace + this.var + "\n");
            } else {
                outputFile.write(INDENT + parentSpace + this.var + " [\n");
                this.num.print(INDENT + parentSpace);
                outputFile.write(INDENT + parentSpace + "]\n");
            }
        }
    }

    /* Parse Functions */
    public Program parse() throws Exception {
        /* program -> decl {decl}
         * first(program) = { void, int }
         * follow(program) = { $ }
         */

        ArrayList < Decl > declList = new ArrayList < Decl > ();

        // Parse the first decl
        Decl nextDecl = parseDecl();
        declList.add(nextDecl);

        // Loop through any other decls
        while (checkToken(TokenType.INT_TOKEN) 
            || checkToken(TokenType.VOID_TOKEN)){
            nextDecl = parseDecl();
            declList.add(nextDecl);
        }

        // if we're no longer in the first set, check if we're in the follow set - if yes, continue, if not, error
        if (!checkToken(TokenType.EOF_TOKEN)) {
            throw new Exception("Parse error in parseProgram(): expected end of file.");
        }

        return new Program(declList);
    }

    private Decl parseDecl() throws Exception {
        /* decl -> void ID fun-decl | int ID decl'
         * first(decl) = { void, int }
         * follow(decl) = { $, int, void }
         */

        Decl decl = null;

        if (checkToken(TokenType.VOID_TOKEN)) {
            matchToken(TokenType.VOID_TOKEN);
            String returnType = "void";

            Token temp = matchToken(TokenType.IDENT_TOKEN);
            VarExpression name = new VarExpression((String) temp.getData());

            decl = parseFunDecl(returnType, name);
        } 
        else if (checkToken(TokenType.INT_TOKEN)) {
            String returnType = "int";
            matchToken(TokenType.INT_TOKEN);

            
            Token temp = matchToken(TokenType.IDENT_TOKEN);
            String name = (String) temp.getData();
            decl = parseDecl2(returnType, name);
        } 
        else {
            throw new Exception("Error: parseDecl() expects int or void.");
        }

        return decl;
    }

    private Decl parseFunDecl(String returnType, VarExpression name) throws Exception {
        /* fun-decl → “(” params “)” compound-stmt
         * First(fun-decl) → { ( }
         * Follow(fun-decl) → { $, void, int }
         */

        Decl funDecl = null;

        matchToken(TokenType.LEFT_PAREN_TOKEN);
        ArrayList < Param > paramList = parseParams();
        matchToken(TokenType.RIGHT_PAREN_TOKEN);
        CompoundStmt content = parseCompoundStmt();
        funDecl = new FunDecl(returnType, name, paramList, content);

        return funDecl;
    }

    private Decl parseDecl2(String returnType, String name) throws Exception {
        /* decl’ → var-decl | fun-decl
         * First(decl') → { ;, [, ( }
         * Follow(decl') → { $, void, int }
         */

        Decl decl2 = null;

        if (checkToken(TokenType.LEFT_BRACKET_TOKEN) 
        || checkToken(TokenType.SEMI_TOKEN)) {
            decl2 = parseVarDecl(name);
        }  
        else if (checkToken(TokenType.LEFT_PAREN_TOKEN)) {
            VarExpression var = new VarExpression(name);
            decl2 = parseFunDecl(returnType, var);
        } 
        else {
            throw new Exception("Error: parseDecl2 expects ; [ or (");
        }

        return decl2;
    }

    private Decl parseVarDecl(String name) throws Exception {
        /* var-decl → [ “[“ NUM “]” ] ;
         * First(var-decl) → { ;, [ }
         * Follow(var-decl) → { int, “}”, ;, ID, NUM, (, *, /, +, -, ;, {, if, while, return }
         */
        Decl varDecl = null;
        Token temp;

        if (checkToken(TokenType.LEFT_BRACKET_TOKEN)) {
            matchToken(TokenType.LEFT_BRACKET_TOKEN);

            temp = matchToken(TokenType.NUM_TOKEN);
            Expression index = new NumExpression((int) temp.getData());
            VarExpression var = new VarExpression(name, index);
            varDecl = new VarDecl(var);

            matchToken(TokenType.RIGHT_BRACKET_TOKEN);
            matchToken(TokenType.SEMI_TOKEN);
        } 
        else if (checkToken(TokenType.SEMI_TOKEN)) {
            VarExpression var = new VarExpression(name);
            varDecl = new VarDecl(var);
            matchToken(TokenType.SEMI_TOKEN);
        }
        else {
            throw new Exception("Error: parseVarDecl expects ; or [");
        }

        return varDecl;
    }
    private ArrayList<Param> parseParams() throws Exception {
        /* params → param-list | void
         * First(params) → { int, void }
         * Follow(params) → { ) }
         */
        ArrayList<Param> params = null;

        if (checkToken(TokenType.INT_TOKEN)) {
            params = parseParamList();
        } 
        else if (checkToken(TokenType.VOID_TOKEN)) {
            matchToken(TokenType.VOID_TOKEN);
        } 
        else if(checkToken(TokenType.RIGHT_PAREN_TOKEN)){
            // Do nothing
        }
        else {
            throw new Exception("Error: parseParams expects int or void");
        }

        return params;
    }

    private ArrayList<Param> parseParamList() throws Exception {
        /* param-list → param {, param}
         * First(params-list) → { int }
         * Follow(param-list) → { ) }
         */

        ArrayList<Param> paramList = new ArrayList<Param>();

        Param param = parseParam();
        paramList.add(param);

        while (checkToken(TokenType.COMMA_TOKEN)) {
            matchToken(TokenType.COMMA_TOKEN);
            param = parseParam();
            paramList.add(param);
        }

        return paramList;
    }

    private Param parseParam() throws Exception {
        /* param → int ID [“[“ “]”]
         * First(param) → { int }
         * Follow(param) → { “,”, ) }
         */
        Param param = null;
        Token temp;

        matchToken(TokenType.INT_TOKEN);

        temp = matchToken(TokenType.IDENT_TOKEN);
        String name = (String) temp.getData();

        if (checkToken(TokenType.LEFT_BRACKET_TOKEN)) {
            matchToken(TokenType.LEFT_BRACKET_TOKEN);
            matchToken(TokenType.RIGHT_BRACKET_TOKEN);

            VarExpression var = new VarExpression(name, true);
            param = new Param(var);
        } 
        else if (checkToken(TokenType.COMMA_TOKEN) || checkToken(TokenType.RIGHT_PAREN_TOKEN)){
            VarExpression var = new VarExpression(name);
            param = new Param(var);
        }
        else {
            throw new Exception("Error: parseParam expected ( ) or ,");
        }

        return param;
    }

    private CompoundStmt parseCompoundStmt() throws Exception {
        /* compound-stmt → “{“ local-declarations statement-list “}”
         * First(compound-stmt) → { { }
         * Follow(compound-stmt) → { $, void, int, “}”, ID, NUM, (, *, /, +, -, ;, {, if, while, return, else }
         */

        matchToken(TokenType.LEFT_BRACE_TOKEN);
        ArrayList<Decl> localDecls = parseLocalDecls();
        ArrayList<Statement> stmtList = parseStmtList();
        matchToken(TokenType.RIGHT_BRACE_TOKEN);

        return new CompoundStmt(localDecls, stmtList);
    }

    private ArrayList<Decl> parseLocalDecls() throws Exception {
        /* local-declarations → {int ID var-decl}
         * First(local-declarations) → { ε, int }
         * Follow(local-declarations) → { “}”, ID, NUM, (, ;, {, if, while, return }
         */
        ArrayList <Decl> localDecls = new ArrayList < Decl > ();
        Token temp;

        while (checkToken(TokenType.INT_TOKEN)) {
            matchToken(TokenType.INT_TOKEN);
            temp = matchToken(TokenType.IDENT_TOKEN);
            String name = (String) temp.getData();
            Decl decl = parseVarDecl(name);
            localDecls.add(decl);
        }

        return localDecls;
    }
    private ArrayList<Statement> parseStmtList() throws Exception {
        /* statement-list → {statement}
         * First(statement-list) → { ε, ID, NUM, (, ;, {, if, while, return }
         * Follow(statement-list) → { “}” }
         */
        ArrayList<Statement> SL = new ArrayList<Statement>();

        while (checkToken(TokenType.IDENT_TOKEN) 
            || checkToken(TokenType.NUM_TOKEN)
            || checkToken(TokenType.LEFT_PAREN_TOKEN)
            || checkToken(TokenType.SEMI_TOKEN)
            || checkToken(TokenType.IF_TOKEN)
            || checkToken(TokenType.WHILE_TOKEN)
            || checkToken(TokenType.RETURN_TOKEN)) {
            Statement S = parseStatement();
            SL.add(S);
        }

        return SL;
    }
    private Statement parseStatement() throws Exception {
        /* statement → expression-stmt | compound-stmt | selection-stmt | iteration-stmt | return-stmt
         * First(statement) → { ID, NUM, (, ;, {, if, while, return }
         * Follow(statement) → { }, ID, NUM, (, ;, {, if, while, return, else }
         */
        Statement S = null;

        if (checkToken(TokenType.IDENT_TOKEN)
        || checkToken(TokenType.NUM_TOKEN)
        || checkToken(TokenType.LEFT_PAREN_TOKEN)
        || checkToken(TokenType.SEMI_TOKEN)) {
            S = parseExpressionStmt();
        } 
        else if (checkToken(TokenType.LEFT_BRACE_TOKEN)) {
            S = parseCompoundStmt();
        } 
        else if (checkToken(TokenType.IF_TOKEN)) {
            S = parseSelectionStmt();
        } 
        else if (checkToken(TokenType.WHILE_TOKEN)) {
            S = parseIterationStmt();
        } 
        else if (checkToken(TokenType.RETURN_TOKEN)) {
            S = parseReturnStmt();
        } 
        else {
            throw new Exception("Error: parseStatement expects beginning of statement.");
        }

        return S;
    }
    private ExpressionStmt parseExpressionStmt() throws Exception {
        /* expression-stmt → [expression] ;
         * First(expression-stmt) → { ID, NUM, (, ; }
         * Follow(expression-stmt) → { }, ID, NUM, (, ;, {, if, while, return, else }
         */
        ExpressionStmt ES = null;

        if (checkToken(TokenType.IDENT_TOKEN) 
        || checkToken(TokenType.NUM_TOKEN) 
        || checkToken(TokenType.LEFT_PAREN_TOKEN)) {
            Expression E = parseExpression();
            ES = new ExpressionStmt(E);
            matchToken(TokenType.SEMI_TOKEN);
        } 
        else if (checkToken(TokenType.SEMI_TOKEN)) {
            matchToken(TokenType.SEMI_TOKEN);
        } 
        else {
            throw new Exception("Error: parseExpressionStmt expected ID, NUM, (, or ;");
        }

        return ES;
    }
    private SelectionStmt parseSelectionStmt() throws Exception {
        /* selection-stmt → if “(“ expression “)” statement [else statement]
         * First(selection-stmt) → { if }
         * Follow(selection-stmt) → { }, ID, NUM, (, ;, {, if, while, return, else }
         */
        SelectionStmt SS = null;

        matchToken(TokenType.IF_TOKEN);
        matchToken(TokenType.LEFT_PAREN_TOKEN);
        Expression condition = parseExpression();
        matchToken(TokenType.RIGHT_PAREN_TOKEN);
        Statement ifSequence = parseStatement();

        if (checkToken(TokenType.ELSE_TOKEN)) {
            matchToken(TokenType.ELSE_TOKEN);
            Statement elseSequence = parseStatement();
            SS = new SelectionStmt(condition, ifSequence, elseSequence);
        }
        else {
            SS = new SelectionStmt(condition, ifSequence);
        }
        
        return SS;
    }
    private IterationStmt parseIterationStmt() throws Exception {
        /* iteration-stmt → while “(” expression “)” statement
         * First(iteration-stmt) → { while }
         * Follow(iteration-stmt) → { }, ID, NUM, (, ;, {, if, while, return, else }
         */
        IterationStmt IS = null;

        matchToken(TokenType.WHILE_TOKEN);
        matchToken(TokenType.LEFT_PAREN_TOKEN);
        Expression condition = parseExpression();
        matchToken(TokenType.RIGHT_PAREN_TOKEN);
        Statement activity = parseStatement();

        IS = new IterationStmt(condition, activity);

        return IS;
    }
    private ReturnStmt parseReturnStmt() throws Exception {
        /* return-stmt → return [expression] ;
         * First(return-stmt) → { return }
         * Follow(return-stmt) → { }, ID, NUM, (, ;, {, if, while, return, else }
         */

        ReturnStmt RS = null;

        matchToken(TokenType.RETURN_TOKEN);
        
        if (checkToken(TokenType.IDENT_TOKEN) 
        || checkToken(TokenType.NUM_TOKEN) 
        || checkToken(TokenType.LEFT_PAREN_TOKEN)) {
            RS = new ReturnStmt(parseExpression());
        } else if (checkToken(TokenType.SEMI_TOKEN)){
            RS = new ReturnStmt();
        } else {
            throw new Exception("Error: return statement expected ID, NUM, ( or ;");
        }

        matchToken(TokenType.SEMI_TOKEN);

        return RS;
    }

    private Expression parseExpression() throws Exception {
        /* expression → ID expression’ | NUM simple-expression’ | (expression) simple-expression’
         * First(expression) → { ID, NUM, ( }
         * Follow(expression) → { ;, ), ], “,” }
         */

        Expression E = null;

        if (checkToken(TokenType.IDENT_TOKEN)) {
            String ID = (String) scanner.getNextToken().getData();
            E = parseExpression2(ID);
        } 
        else if (checkToken(TokenType.NUM_TOKEN)) {
            int num = (int) scanner.getNextToken().getData();
            E = parseSimpleExpr2(new NumExpression(num));
            if (E == null) {
                E = new NumExpression(num);
            }
        } 
        else if (checkToken(TokenType.LEFT_PAREN_TOKEN)) {
            matchToken(TokenType.LEFT_PAREN_TOKEN);
            E = parseExpression();
            matchToken(TokenType.RIGHT_PAREN_TOKEN);
            E = parseSimpleExpr2(E);
        } 
        else {
            throw new Exception("Syntax error: expression expects ID, NUM, or (.");
        }
        
        return E;
    }
    private Expression parseExpression2(String ID) throws Exception {
        /* expression’ → = expression | "["expression"]" expression’’ | (args) simple-expression’ | simple-expression’
         * First(expression’) → { =, [, (, ε, *, /, +, -, <, <=, >, >=, ==, != }
         * Follow(expression’) → { ;, ), ], “,” }
         */

        Expression E2 = null;

        if (checkToken(TokenType.ASSIGN_TOKEN)) {
            matchToken(TokenType.ASSIGN_TOKEN);
            VarExpression LHS = new VarExpression(ID);
            Expression RHS = parseExpression();
            E2 = new AssignExpression(LHS, RHS);
        } 
        else if (checkToken(TokenType.LEFT_BRACKET_TOKEN)) {
            matchToken(TokenType.LEFT_BRACKET_TOKEN);
            Expression index = parseExpression();
            matchToken(TokenType.RIGHT_BRACKET_TOKEN);

            VarExpression LHS = new VarExpression(ID, index);
            Expression E3 = parseExpression3(LHS);

            E2 = LHS;
            if (E3 != null) {
                E2 = E3;
            }
        } 
        else if (checkToken(TokenType.LEFT_PAREN_TOKEN)) {
            E2 = parseVarCall(new VarExpression(ID));
        } 
        else if (checkToken(TokenType.MULT_TOKEN) 
            || checkToken(TokenType.DIVIDE_TOKEN) 
            || checkToken(TokenType.PLUS_TOKEN) 
            || checkToken(TokenType.MINUS_TOKEN) 
            || checkToken(TokenType.LESS_EQUAL_TOKEN) 
            || checkToken(TokenType.LESS_TOKEN) 
            || checkToken(TokenType.GREATER_TOKEN) 
            || checkToken(TokenType.GREATER_EQUAL_TOKEN) 
            || checkToken(TokenType.EQUAL_TOKEN) 
            || checkToken(TokenType.NOT_EQUAL_TOKEN) 
            || checkToken(TokenType.IDENT_TOKEN) 
            || checkToken(TokenType.NUM_TOKEN)) {
            E2 = parseSimpleExpr2(new VarExpression(ID));
        } 
        else if (checkToken(TokenType.SEMI_TOKEN) 
            || checkToken(TokenType.RIGHT_PAREN_TOKEN) 
            || checkToken(TokenType.COMMA_TOKEN)
            || checkToken(TokenType.RIGHT_BRACKET_TOKEN)) {
            E2 = new VarExpression(ID);
        } 
        else {
            throw new Exception("Syntax error: expression' expects = [ or (.");
        }

        return E2;
    }
    private Expression parseExpression3(VarExpression LHS) throws Exception {
        /* expression’’ → = expression | simple-expression’
         * First(expression’’) → {  =, ε, *, /, +, -, <, <=, >, >=, ==, !=  }
         * Follow(expression’’) → { ;, ), ], “,” }
         */

        Expression E3 = LHS;

        if (checkToken(TokenType.ASSIGN_TOKEN)) {
            matchToken(TokenType.ASSIGN_TOKEN);

            Expression RHS = parseExpression();
            E3 = new AssignExpression(LHS, RHS);
        } 
        else if (checkToken(TokenType.MULT_TOKEN) 
            || checkToken(TokenType.DIVIDE_TOKEN)
            || checkToken(TokenType.PLUS_TOKEN)
            || checkToken(TokenType.MINUS_TOKEN)
            || checkToken(TokenType.LESS_TOKEN)
            || checkToken(TokenType.LESS_EQUAL_TOKEN)
            || checkToken(TokenType.GREATER_TOKEN)
            || checkToken(TokenType.GREATER_EQUAL_TOKEN)
            || checkToken(TokenType.EQUAL_TOKEN)
            || checkToken(TokenType.NOT_EQUAL_TOKEN)) {
            E3 = parseSimpleExpr2(LHS);
        }
        else if (!(checkToken(TokenType.SEMI_TOKEN) 
            || checkToken(TokenType.RIGHT_PAREN_TOKEN) 
            || checkToken(TokenType.COMMA_TOKEN)
            || checkToken(TokenType.RIGHT_BRACKET_TOKEN))) {
            throw new Exception("Error: expression'' expects an operator.");
        }

        return E3;
    }
    private Expression parseSimpleExpr2(Expression LHS) throws Exception {
        /* simple-expression’ → additive-expression’ [relop additive expression]
         * First(simple-expression’) → { ε, *, /, +, -, <, <=, >, >=, ==, != }
         * Follow(simple-expression’) → { ;, ), ], “,” }
         */
        
        Expression SE2 = LHS;
        
        if (checkToken(TokenType.MULT_TOKEN) 
        || checkToken(TokenType.DIVIDE_TOKEN) 
        || checkToken(TokenType.PLUS_TOKEN) 
        || checkToken(TokenType.MINUS_TOKEN)) {
            SE2 = parseAdditiveExpr2(LHS);
        }
        if (checkToken(TokenType.GREATER_EQUAL_TOKEN) 
        || checkToken(TokenType.GREATER_TOKEN) 
        || checkToken(TokenType.EQUAL_TOKEN) 
        || checkToken(TokenType.NOT_EQUAL_TOKEN) 
        || checkToken(TokenType.LESS_EQUAL_TOKEN) 
        || checkToken(TokenType.LESS_TOKEN)) {
            TokenType op = scanner.getNextToken().getType();
            Expression RHS = parseAdditiveExpr();
            SE2 = new BinaryExpression(SE2, op, RHS);
        }

        if(!(checkToken(TokenType.SEMI_TOKEN) 
        || checkToken(TokenType.RIGHT_PAREN_TOKEN) 
        || checkToken(TokenType.RIGHT_BRACKET_TOKEN)
        || checkToken(TokenType.COMMA_TOKEN))){
            throw new Exception("Error: simple-expression' expected ; ) ] or ,");
        }
        
        return SE2;
    }
    private Expression parseAdditiveExpr() throws Exception {
        /* additive-expression → term {addop term}
         * First(additive-expression) → { (, ID, NUM }
         * Follow(additive-expression) → { ;, ), ], “,” }
         */

        Expression LHS = parseTerm();
        
        while (checkToken(TokenType.PLUS_TOKEN) 
            || checkToken(TokenType.MINUS_TOKEN)) {
            TokenType op = scanner.getNextToken().getType();
            Expression RHS = parseTerm();
            LHS = new BinaryExpression(LHS, op, RHS);
        }
        
        return LHS;
    }

    private Expression parseAdditiveExpr2(Expression inLHS) throws Exception {
        /* additive-expression’ → term’ {addop term}
         * First(additive-expression’) → { ε, *, /, +, - }
         * Follow(additive-expression’) → { <, <=, >, >=, ==, !=, ;, ), ], “,” }
         */

        Expression LHS = inLHS;
        Expression newLHS = parseTerm2(LHS);
        
        if (newLHS != null) {
            LHS = newLHS;
        }
        while (checkToken(TokenType.PLUS_TOKEN) 
            || checkToken(TokenType.MINUS_TOKEN)) {
            TokenType op = scanner.getNextToken().getType();
            Expression RHS = parseTerm();
            LHS = new BinaryExpression(LHS, op, RHS);
        }
        
        return LHS;
    }
    private Expression parseTerm() throws Exception {
        /* term → factor {mulop factor}
         * First(term) → { (, ID, NUM }
         * Follow(term) → { +, -, <, <=, >, >=, ==, !=, ;, ), ], “,” }
         */

        Expression LHS = parseFactor();
        
        while (checkToken(TokenType.MULT_TOKEN) 
            || checkToken(TokenType.DIVIDE_TOKEN)) {
            TokenType op = scanner.getNextToken().getType();
            Expression RHS = parseFactor();
            LHS = new BinaryExpression(LHS, op, RHS);
        }
        
        return LHS;
    }
    private Expression parseTerm2(Expression inLHS) throws Exception {
        /* term’ → {mulop factor}
         * First(term’) → { ε. *, / }
         * Follow(term’) → { +, -, <, <=, >, >=, ==, !=, ;, ), ], “,” }
         */

        Expression LHS = inLHS;
        
        while (checkToken(TokenType.MULT_TOKEN) 
            || checkToken(TokenType.DIVIDE_TOKEN)) {
            TokenType op = scanner.getNextToken().getType();
            Expression RHS = parseFactor();
            LHS = new BinaryExpression(LHS, op, RHS);
        }
        
        return LHS;
    }
    private Expression parseFactor() throws Exception {
        /* factor → “(” expression “)” | ID varcall | NUM
         * First(factor) → { (, ID, NUM }
         * Follow(factor) → { *, /, +, -, <, <=, >, >=, ==, !=, ;, ), ], “,” }
         */

        Expression F = null;
        
        if (checkToken(TokenType.LEFT_PAREN_TOKEN)) {
            matchToken(TokenType.LEFT_PAREN_TOKEN);
            F = parseExpression();
            matchToken(TokenType.RIGHT_PAREN_TOKEN);
        } 
        else if (checkToken(TokenType.IDENT_TOKEN)) {
            String ID = (String) scanner.getNextToken().getData();
            F = parseVarCall(new VarExpression(ID));
        } 
        else if (checkToken(TokenType.NUM_TOKEN)) {
            int NUM = (int) scanner.getNextToken().getData();
            F = new NumExpression(NUM);
        }
        else {
            throw new Exception("Error: parseFactor expected ( ID or NUM");
        }
        
        return F;
    }
    private Expression parseVarCall(VarExpression ID) throws Exception {
        /* varcall → “(“ args “)” | “[“ expression “]” | ε
         * First(varcall) → { (, [, ε }
         * Follow(varcall) → { *, /, +, -, <, <=, >, >=, ==, !=, ;, ), ], “,”  }
         */

        Expression varcall = null;
        
        if (checkToken(TokenType.LEFT_PAREN_TOKEN)) {
            matchToken(TokenType.LEFT_PAREN_TOKEN);
            ArrayList < Expression > args = parseArgs();
            matchToken(TokenType.RIGHT_PAREN_TOKEN);
            varcall = new CallExpression(ID, args);
        } 
        else if (checkToken(TokenType.LEFT_BRACKET_TOKEN)) {
            matchToken(TokenType.LEFT_BRACKET_TOKEN);
            varcall = parseExpression();
            matchToken(TokenType.RIGHT_BRACKET_TOKEN);
        } 
        else if (checkToken(TokenType.MULT_TOKEN) 
            || checkToken(TokenType.DIVIDE_TOKEN) 
            || checkToken(TokenType.PLUS_TOKEN) 
            || checkToken(TokenType.MINUS_TOKEN) 
            || checkToken(TokenType.LESS_TOKEN) 
            || checkToken(TokenType.GREATER_TOKEN) 
            || checkToken(TokenType.LESS_EQUAL_TOKEN) 
            || checkToken(TokenType.GREATER_EQUAL_TOKEN) 
            || checkToken(TokenType.EQUAL_TOKEN) 
            || checkToken(TokenType.NOT_EQUAL_TOKEN) 
            || checkToken(TokenType.SEMI_TOKEN) 
            || checkToken(TokenType.RIGHT_PAREN_TOKEN) 
            || checkToken(TokenType.RIGHT_BRACKET_TOKEN) 
            || checkToken(TokenType.COMMA_TOKEN)) {
            varcall = ID;
        }
        else {
            throw new Exception("Error: parseVarCall expected ( [ ] ) ; , or an operator.");
        }
        
        return varcall;
    }
    private ArrayList <Expression> parseArgs() throws Exception {
        /* args → arg-list | ε
         * First(args) → { ID, NUM, (, ε }
         * Follow(args) → { ) }
         */

        ArrayList < Expression > args = new ArrayList < Expression > ();
        
        if (checkToken(TokenType.IDENT_TOKEN) 
        || checkToken(TokenType.NUM_TOKEN) 
        || checkToken(TokenType.LEFT_PAREN_TOKEN)) {
            args = parseArgList();
        } else if (!checkToken(TokenType.RIGHT_PAREN_TOKEN)){
            throw new Exception("Error: parseArgs expects )");
        }
        
        return args;
    }
    private ArrayList<Expression> parseArgList() throws Exception {
        /* arg-list → expression {, expression}
         * First(arg-list) → { ID, NUM, ( }
         * Follow(arg-list) → { ) }
         */

        ArrayList < Expression > argList = new ArrayList < Expression > ();

        Expression nextExp = parseExpression();
        argList.add(nextExp);

        while (checkToken(TokenType.COMMA_TOKEN)) {
            matchToken(TokenType.COMMA_TOKEN);
            nextExp = parseExpression();
            argList.add(nextExp);
        }

        return argList;
    }

    /* Print AST */
    public void printTree(FileWriter file) throws IOException {
        outputFile = file;
        program.print();
    }
    public void printAST(Program root) throws IOException{
        root.print();
    }
}