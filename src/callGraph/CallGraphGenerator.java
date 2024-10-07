package callGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import parser.Parser;
import utils.ColorHelper;
import visitor.MethodDeclarationVisitor;
import visitor.MethodInvocationVisitor;

public class CallGraphGenerator {

    public static String projectSourcePath;

    public static void generateCallGraph(String projectPath) throws IOException {
        projectSourcePath = projectPath;

        final File sourceFolder = new File(projectSourcePath);
        ArrayList<File> javaFiles = Parser.getJavaFilesForFolder(sourceFolder);

        Set<String> edges = new HashSet<>();

        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, "UTF-8");
            CompilationUnit parse = Parser.parse(content.toCharArray());

            MethodDeclarationVisitor methodVisitor = new MethodDeclarationVisitor();
            parse.accept(methodVisitor);

            for (MethodDeclaration method : methodVisitor.getMethods()) {
                MethodInvocationVisitor invocationVisitor = new MethodInvocationVisitor();
                method.accept(invocationVisitor);

                for (MethodInvocation invocation : invocationVisitor.getMethods()) {
                    String caller = method.getName().toString();
                    String callee = invocation.getName().toString();
                    edges.add("\"" + caller + "\" -> \"" + callee + "\";");
                }
            }
        }

        writeDotFile(edges);
    }

    private static void writeDotFile(Set<String> edges) throws IOException {
        File dotFile = new File("resources/graphs/callgraph.dot");
        dotFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(dotFile)) {
            writer.write("digraph CallGraph {\n");
            writer.write("node [shape=rectangle, style=filled];\n");

            String classColor = "lightblue";
            String methodColor = "lightgreen";

            Set<String> nodes = new HashSet<>();

            for (String edge : edges) {
                writer.write(edge + "\n");
                String[] nodesInEdge = edge.replace("\"", "").split(" -> ");
                nodes.add(nodesInEdge[0]);
                nodes.add(nodesInEdge[1]);
            }

            for (String node : nodes) {
                if (Character.isUpperCase(node.charAt(0))) {
                    writer.write("\"" + node + "\" [fillcolor=" + classColor + ", style=filled];\n");
                } else {
                    writer.write("\"" + node + "\" [fillcolor=" + methodColor + ", style=filled];\n");
                }
            }

            addLegend(writer, classColor, methodColor);

            writer.write("}\n");
        }

        generateGraphImage(dotFile);
    }


    private static void addLegend(FileWriter writer, String classColor, String methodColor) throws IOException {
        writer.write("subgraph cluster_legend {\n");
        writer.write("label=\"Légende\";\n");
        writer.write("key [label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">\n");
        writer.write("<TR><TD BGCOLOR=\"" + classColor + "\">Classe</TD></TR>\n");
        writer.write("<TR><TD BGCOLOR=\"" + methodColor + "\">Méthode</TD></TR>\n");
        writer.write("</TABLE>>, shape=plaintext]\n");
        writer.write("}\n");
    }


    private static void generateGraphImage(File dotFile) throws IOException {
        String outputImagePath = "resources/graphs/callgraph.png";
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile.getAbsolutePath(), "-o", outputImagePath);
        pb.inheritIO();
        Process process = pb.start();

        try {
            process.waitFor();
            System.out.println("\n Graphe d'appel généré: " + ColorHelper.info(outputImagePath));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
