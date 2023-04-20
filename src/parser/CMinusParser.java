package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public CMinusParser(String fileName) throws Exception {
        File inputFile = new File(fileName);
        FileReader codeFile = new FileReader(inputFile);
        BufferedReader inputReader = new BufferedReader(codeFile);
        scanner = new CMinusScanner(inputReader);

        program = parse();

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

        public Param(VarExpression name) {
            this.name = name;
        }

        void print(String parentSpace) throws IOException {
            this.name.print(parentSpace + INDENT + "int ");
        }
    }

    abstract class Decl {
        // abstract, will be one of the other two decls
        abstract void print(String parentSpace) throws IOException;
    }

    public class VarDecl extends Decl {
        // example: int x; or int x[10];
        public VarExpression name;

        public VarDecl(VarExpression name) {
            this.name = name;
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
    }

    public class ExpressionStmt extends Statement {
        // example: a + 3;

        public Expression statement;
        public ExpressionStmt(Expression statement) {
            this.statement = statement;
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
        // example: 3
        int num;
        public NumExpression(int num) {
            this.num = num;
        }

        void print(String parentSpace) throws IOException {
            outputFile.write(INDENT + parentSpace + this.num + "\n");
        }
    }

    public class VarExpression extends Expression {
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
        } else if (!checkToken(TokenType.SEMI_TOKEN)){
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










