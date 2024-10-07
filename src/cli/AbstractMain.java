package cli;

import java.io.BufferedReader;
import java.io.IOException;

public abstract class AbstractMain {
	public static final String QUIT = "0";
	
	protected void setTestServiceUrl(BufferedReader inputReader)
			throws IOException {
	}
	
	protected abstract void menu();
}
