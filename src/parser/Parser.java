package parser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import utils.ColorHelper;
import visitor.*;

import static org.fusesource.jansi.Ansi.ansi;

public class Parser {

	public static String projectSourcePath;
	public static final String jrePath = System.getProperty("java.home") + "/lib/jrt-fs.jar"; // Chemin dynamique

	public static void main(String[] args) throws IOException {
		projectSourcePath = args[0];

		final File sourceFolder = new File(projectSourcePath);
		ArrayList<File> javaFiles = getJavaFilesForFolder(sourceFolder);

		// Variables pour les statistiques
		int amountClass = 0;
		int amountLines = 0;
		double amountMethods = 0;
		Set<String> packages = new HashSet<String>();
		double amountLinesByMethods = 0;
		double amountAttributs = 0;
		double maxParamAmount = 0;

		// Maps pour les méthodes et attributs des classes
		Map<String, Integer> classesMethods = new LinkedHashMap<>();
		Map<String, Integer> classesAttributes = new LinkedHashMap<>();

		// Traitement de chaque fichier Java
		for (File fileEntry : javaFiles) {
			String content = FileUtils.readFileToString(fileEntry, "UTF-8"); // Assurez-vous d'utiliser l'encodage correct

			CompilationUnit parse = parse(content.toCharArray());

			amountClass += getNumberOfClasses(parse);
			amountLines += getNumberOfLines(parse);
			amountMethods += getNumberOfMethods(parse);
			packages.addAll(getPackages(parse));
			amountLinesByMethods += getNumberOfLinesByMethods(parse);
			amountAttributs += getNumberOfAttributs(parse);
			addClassMethods(parse, classesMethods);
			addClassAttributes(parse, classesAttributes);
			maxParamAmount = Math.max(getMethodParams(parse), maxParamAmount);
		}

		// Affichage des statistiques
		displayStatistics(amountClass, amountLines, amountMethods, packages, amountLinesByMethods, amountAttributs, maxParamAmount);
	}

	// Créer l'AST
	public static CompilationUnit parse(char[] classSource) {
		ASTParser parser = ASTParser.newParser(AST.JLS4); // Utiliser la version la plus récente
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);

		Map<String, String> options = JavaCore.getOptions();
		parser.setCompilerOptions(options);

		// Chemins d'accès aux sources et à la bibliothèque JRE
		String[] sources = { projectSourcePath };
		String[] classpath = { jrePath }; // Chemin vers jrt-fs.jar

		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);
		parser.setSource(classSource);

		return (CompilationUnit) parser.createAST(null); // Créer et analyser
	}

	// Méthodes pour les statistiques (à ajouter)
	public static void displayStatistics(int amountClass, int amountLines, double amountMethods, Set<String> packages, double amountLinesByMethods, double amountAttributs, double maxParamAmount) {
		System.out.println("\nStatistiques:\n");
		System.out.println("[⚫] Nombre de classes de l’application: " + ColorHelper.info(String.valueOf(amountClass)));
		System.out.println("[⚫] Nombre de lignes de code de l’application: " + ColorHelper.info(String.valueOf(amountLines)));
		System.out.println("[⚫] Nombre total de méthodes de l’application: " + ColorHelper.info(String.valueOf(amountMethods)));
		System.out.println("[⚫] Nombre total de packages de l’application: " + ColorHelper.info(String.valueOf(packages.size())));
		System.out.println("[⚫] Nombre moyen de méthodes par classe: " + ColorHelper.info(String.valueOf(Math.ceil(amountMethods / amountClass))));
		System.out.println("[⚫] Nombre moyen de lignes de code par méthode: " + ColorHelper.info(String.valueOf(Math.ceil(amountLinesByMethods / amountMethods))));
		System.out.println("[⚫] Nombre moyen d’attributs par classe: " + ColorHelper.info(String.valueOf(Math.ceil(amountAttributs / amountClass))));
		System.out.println("[⚫] Nombre maximal de paramètres par rapport à toutes les méthodes de l’application: " + ColorHelper.info(String.valueOf(maxParamAmount)));
	}



// read all java files from specific folder
	public static ArrayList<File> getJavaFilesForFolder(final File folder) {
		ArrayList<File> javaFiles = new ArrayList<File>();
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				javaFiles.addAll(getJavaFilesForFolder(fileEntry));
			} else if (fileEntry.getName().contains(".java")) {
				javaFiles.add(fileEntry);
			}
		}

		return javaFiles;
	}

	// navigate method information
	public static void printMethodInfo(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);

		for (MethodDeclaration method : visitor.getMethods()) {
			System.out.println("Method name: " + method.getName()
					+ " Return type: " + method.getReturnType2());
		}

	}

	// navigate variables inside method
	public static void printVariableInfo(CompilationUnit parse) {

		MethodDeclarationVisitor visitor1 = new MethodDeclarationVisitor();
		parse.accept(visitor1);
		for (MethodDeclaration method : visitor1.getMethods()) {

			VariableDeclarationFragmentVisitor visitor2 = new VariableDeclarationFragmentVisitor();
			method.accept(visitor2);

			for (VariableDeclarationFragment variableDeclarationFragment : visitor2
					.getVariables()) {
				System.out.println("variable name: "
						+ variableDeclarationFragment.getName()
						+ " variable Initializer: "
						+ variableDeclarationFragment.getInitializer());
			}

		}
	}

	// navigate method invocations inside method
	public static void printMethodInvocationInfo(CompilationUnit parse) {

		MethodDeclarationVisitor visitor1 = new MethodDeclarationVisitor();
		parse.accept(visitor1);
		for (MethodDeclaration method : visitor1.getMethods()) {

			MethodInvocationVisitor visitor2 = new MethodInvocationVisitor();
			method.accept(visitor2);

			for (MethodInvocation methodInvocation : visitor2.getMethods()) {
				System.out.println("method " + method.getName() + " invoc method "
						+ methodInvocation.getName());
			}

		}
	}

	// Return the number of class
	public static int getNumberOfClasses(CompilationUnit parse) {
		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		parse.accept(visitor);

		return visitor.getTypes().size();
	}

	// Return the number of methods
	public static int getNumberOfMethods(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);

		return visitor.getMethods().size();
	}

	// Return the number of lines
	public static int getNumberOfLines(CompilationUnit parse) {
		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		parse.accept(visitor);
		return parse.getLineNumber(visitor.getLength());
	}

	// Return the number of packages
	public static List<String> getPackages(CompilationUnit parse) {
		PackageDeclarationVisitor visitor = new PackageDeclarationVisitor();
		parse.accept(visitor);

		return visitor.getPackages().stream().map(p -> p.toString()).collect(Collectors.toList());
	}

	// Return the number of lines by methods
	public static int getNumberOfLinesByMethods(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);

		int totalLines = 0;
		for(MethodDeclaration method : visitor.getMethods()) {
			int start = method.getStartPosition(); // first character position
			int end = start + method.getLength(); // last character position

			int lineStart = parse.getLineNumber(start);
			int lineEnd = parse.getLineNumber(end);
			totalLines += lineEnd - lineStart;
		}

		return totalLines;
	}

	public static int getNumberOfAttributs(CompilationUnit parse) {
		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		parse.accept(visitor);

		return visitor.getAttribus().size();
	}

	// methods to add
	// add KEY: ClassName - VALUE: nbMethods in a Map
	public static void addClassMethods(CompilationUnit parse, Map<String, Integer> classesMethods) {
		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		parse.accept(visitor);
		for (TypeDeclaration type : visitor.getTypes()) {
			classesMethods.put(String.valueOf(type.getName()), type.getMethods().length);
		}
	}

	public static void addClassAttributes(CompilationUnit parse, Map<String, Integer> classesAttributes) {
		TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
		parse.accept(visitor);
		for (TypeDeclaration type : visitor.getTypes()) {
			// System.out.println("########## type name : " + type.getName() + " " + type.getFields().length);
			classesAttributes.put(String.valueOf(type.getName()), type.getFields().length);
		}
	}

	public static int getMethodParams(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);

		int totalParams = 0;
		for(MethodDeclaration method : visitor.getMethods()) {
			int numParams = method.parameters().size(); // Récupérer le nombre de paramètres
			totalParams += numParams;
		}

		return totalParams;
	}


	// print 10% classes with more methods
	public static void classesWithMoreMethods(List<String> listClassesWithMoreMethods, float percent, Map<String, Integer> classesMethods) {
		int cmSize = classesMethods.size();
		int nbToKeepM = (int) Math.ceil(cmSize*(percent/100));

		while (listClassesWithMoreMethods.size() < nbToKeepM) {
			int max = Collections.max(classesMethods.values());
			listClassesWithMoreMethods.addAll(
					classesMethods.entrySet().stream()
							.filter(entry -> entry.getValue() == max)
							.map(entry -> entry.getKey())
							.collect(Collectors.toList())
			);
			for (String type : listClassesWithMoreMethods) {
				classesMethods.remove(type);
			}
		}
		System.out.print("[⚫] " + percent + "% de classes avec le plus de méthodes: ");
		for (int i = 0; i < listClassesWithMoreMethods.size(); i++) {
			System.out.print(ColorHelper.info(String.valueOf(listClassesWithMoreMethods.get(i) + " ")));
		}
		System.out.println();
	}

	// print classes with more than X methods
	public static void classesWithMoreThanXMethods(List<String> listClassesWithMoreThanXMethods, int x, Map<String, Integer> classesMethods) {
		listClassesWithMoreThanXMethods.addAll(
				classesMethods.entrySet().stream()
						.filter(entry -> entry.getValue() > x)
						.map(entry -> entry.getKey())
						.collect(Collectors.toList())
		);
		System.out.print("[⚫] Classes qui possèdent plus de " + x + " methodes: ");
		for (int i = 0; i < listClassesWithMoreThanXMethods.size(); i++) {
			System.out.print(ColorHelper.info(String.valueOf(listClassesWithMoreThanXMethods.get(i) + " ")));
		}
		System.out.println();
	}

	// print 10% classes with more attributes
	public static void classesWithPercentAttributes(List<String> listClassesWithMoreAttributes, float percent, Map<String, Integer> classesAttributes) {
		int caSize = classesAttributes.size();
		int nbToKeepA = (int) Math.ceil(caSize*(percent/100));
		while (listClassesWithMoreAttributes.size() < nbToKeepA) {
			int max = Collections.max(classesAttributes.values());
			listClassesWithMoreAttributes.addAll(
					classesAttributes.entrySet().stream()
							.filter(entry -> entry.getValue() == max)
							.map(entry -> entry.getKey())
							.collect(Collectors.toList())
			);
			for (String type : listClassesWithMoreAttributes) {
				classesAttributes.remove(type);
			}
		}
		System.out.print("[⚫] " + percent + "% de classes avec le plus d'attributs: ");
		for (int i = 0; i < listClassesWithMoreAttributes.size(); i++) {
			System.out.print(ColorHelper.info(String.valueOf(listClassesWithMoreAttributes.get(i) + " ")));
		}
		System.out.println();
	}

	public static void inBoth(List<String> listClassesWithMoreMethods, List<String> listClassesWithMoreAttributes) {
		List<String> listClassesInBoth = new ArrayList<>();
		// we go through the shortest list
		if (listClassesWithMoreMethods.size() <= listClassesWithMoreAttributes.size()) {
			for (int i = 0; i < listClassesWithMoreMethods.size(); i++) {
				if (listClassesWithMoreAttributes.contains(listClassesWithMoreMethods.get(i))) {
					listClassesInBoth.add(listClassesWithMoreMethods.get(i));
				}
			}
		} else {
			for (int i = 0; i < listClassesWithMoreAttributes.size(); i++) {
				if (listClassesWithMoreMethods.contains(listClassesWithMoreAttributes.get(i))) {
					listClassesInBoth.add(listClassesWithMoreAttributes.get(i));
				}
			}
		}
		System.out.print("[⚫] Classes qui sont dans les deux catégories: ");
		for (int i = 0; i < listClassesInBoth.size(); i++) {
			System.out.print(ColorHelper.info(String.valueOf(listClassesInBoth.get(i) + " ")));
		}
		System.out.println("\n");
	}

}
