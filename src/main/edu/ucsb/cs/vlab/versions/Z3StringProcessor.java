package edu.ucsb.cs.vlab.versions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.ucsb.cs.vlab.Z3;
import edu.ucsb.cs.vlab.Z3Interface.ExternalToolException;
import edu.ucsb.cs.vlab.Z3Interface.Processable;
import edu.ucsb.cs.vlab.Z3Interface.Processor;
import edu.ucsb.cs.vlab.modelling.Output;
import edu.ucsb.cs.vlab.modelling.Output.Model;

public class Z3StringProcessor implements Processable {
	final Model model = new Model();
	final StringBuilder currentQuery = new StringBuilder();

	@Override
	public void send(String message, Processor proc) throws IOException {
		currentQuery.append(message + "\n");
	}

	@Override
	public void query(String message, Processor proc) throws IOException {
		currentQuery.append(message + "\n");
		
		Process process = proc.startProcess();
		
		
		try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
			writer.write(currentQuery.toString());
		}
		
		

		Files.write(Paths.get(Z3.getTempFile()), currentQuery.toString().getBytes());
	}

	@Override
	public Output getOutput(Processor proc) throws IOException, RuntimeException, NullPointerException {
		boolean sat = false; 

		final Process process = proc.startProcess();
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line = reader.readLine();
				
				
				sat = line.startsWith("sat");
				
				
				if(sat) {
					line = reader.readLine(); // (model
					
					assert(line.startsWith("(model"));
					
					line = reader.readLine(); // (define-fun name () String
					
					while(line != null && !line.startsWith(")")) {
						String[] tokens = line.trim().split(" ");
						
						assert(tokens[0].equals("(define-fun"));
						
						String name = tokens[1];
						
						line = reader.readLine(); // value)
						
						String trimmed = line.trim();
						
						String value = trimmed.substring(0, trimmed.length() - 1); // remove the final ')'
						
						model.put(name, value);
						
						// next "(define-fun name () String"
						line = reader.readLine();
					}
					
				} else {
					assert(line.startsWith("unsat"));
				}
	
			
		}

		return new Output(sat, assembleModel());
	}

	public void process(String line) {
		final String[] parts = line.split(" -> ");
		final String[] typeAndName = parts[0].split(" : ");

		final String name = typeAndName[0].trim();
		final String type = typeAndName[1].trim();
		final String value = parts[1].trim();

		model.put(name, value);
	}

	public Model assembleModel() {
		return model;
	}

}
