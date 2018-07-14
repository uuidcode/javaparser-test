import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;

import jdk.nashorn.internal.codegen.CompileUnit;

public class Parse {
    @Test
    public void test() throws Exception {
        CompilationUnit compilationUnit = JavaParser.parse("class A {}");
        Optional<ClassOrInterfaceDeclaration> classA = compilationUnit.getClassByName("A");
    }
    
    @Test
    public void analyze() {
        String source = Stream.<String>builder()
            .add("class A {")
            .add("public static int number;")
            .add("public int line;")
            .add("protected int age;")
            .add("}")
            .build()
            .collect(Collectors.joining("\n"));

        CompilationUnit compilationUnit = JavaParser.parse(source);
        compilationUnit.findAll(FieldDeclaration.class)
            .stream()
            .filter(f -> f.isPublic() && !f.isStatic())
            .forEach(f -> {
                Integer line = f.getRange().map(r -> r.begin.line).orElse(0);
                System.out.println("check field at line " + line);
            });

    }

    @Test
    public void transform() {
        String source = Stream.<String>builder()
            .add("abstract class Test {")
            .add("}")
            .build()
            .collect(Collectors.joining("\n"));

        CompilationUnit compilationUnit = JavaParser.parse(source);
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
            .stream()
            .filter(c -> !c.isInterface())
            .filter(c -> c.isAbstract())
            .filter(c -> !c.getNameAsString().startsWith("Abstract"))
            .forEach(c -> {
                String oldName = c.getNameAsString();
                String newName = "Abstract" + oldName;
                c.setName(newName);
            });

        String newSource = compilationUnit.toString();

        System.out.println(newSource);
    }

    @Test
    public void generate() {
        CompilationUnit compilationUnit = new CompilationUnit();
        ClassOrInterfaceDeclaration myClass =
            compilationUnit.addClass("MyClass")
            .setPublic(true);

        myClass.addField(int.class, "age", Modifier.PUBLIC, Modifier.STATIC);
        myClass.addField(String.class, "name", Modifier.PRIVATE);

        Parameter parameter = new Parameter(new TypeParameter("String"), "name");
        BlockStmt blockStmt = new BlockStmt();
        blockStmt.addStatement("this.name = name;");
        blockStmt.addStatement("return this;");
        MethodDeclaration methodDeclaration = myClass.addMethod("setName", Modifier.PUBLIC);
        methodDeclaration.addParameter(parameter);
        methodDeclaration.setBody(blockStmt);

        System.out.println(myClass.toString());
    }

    @Test
    public void addField() throws Exception {
        CompilationUnit compilationUnit = JavaParser.parse(new File("src/test/java/MyClass.java"));
        ClassOrInterfaceDeclaration targetClass = compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
            .stream()
            .findFirst()
            .get();

        targetClass.addPrivateField(String.class, "uuid");

        System.out.println(compilationUnit.toString());
    }
}
