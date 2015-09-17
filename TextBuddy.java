/*
 * This class is used to read commands from users and manipulate the text 
 * according to commands. It stores commands in an arrayList, at the same time, executes
 * the command and does the changes to file. 
 * In this program I choose to write command using bufferedWriter. In this way,
 * the program immediately writes into file. To achieve the behavior in the example, 
 * this program may be time consuming when there is large amount of commands and 
 * the user wants to delete one of them. In the for loop for deleting, the program
 * will iterate through the whole commands and print out to expected file.
 * In the example, we do not see the consequence of entering a wrong command, so I 
 * added as one of the cases.
 * Also, the example does not indicate the order of commands. If there is a delete command
 * before add command, this program would crash. But we assume, people are wise enough to 
 * enter add rather than delete at the first place.
 * The example command is given below:
  			Welcome to TextBuddy. mytextfile.txt is ready for use
     		command: add little brown fox
			added to mytextfile.txt: “little brown fox”
			command: display
			1. little brown fox
			command: add jumped over the moon
			added to mytextfile.txt: “jumped over the moon”
			command: display
			1. little brown fox
			2. jumped over the moon
			command: delete 2
			deleted from mytextfile.txt: “jumped over the moon”
			command: display
			1. little brown fox
			command: clear
			all content deleted from mytextfile.txt
			command: display
			mytextfile.txt is empty
			command: exit
 
 * @author Jingjing Wang 
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


public class TextBuddy {
	// This is a list to store commands.
	public static ArrayList<String> commandList = new ArrayList<String>();
	
	// This command will be used and changed through out the program.
	public static String command;
	
	
	public static void main (String[] args) throws IOException{
		
		File file = new File("fileTest.txt");
		System.out.println("Welcome to TextBuddy. " + file + " is ready for use");
		executeCommand(file);
	}	
	
	/* 
	 * This method is the core of this program which takes the file
	 * as parameter and repeatedly scan from terminal. Each command 
	 * can be categorized into several cases. Then execute each case.
	 */
	public static void executeCommand (File file) throws IOException{
		Scanner scan = new Scanner(System.in);
		
		// Use BufferedWirter is more efficient.
		BufferedWriter write = new BufferedWriter(new FileWriter(file));
		
		// This boolean value is for detecting whether to exit program or not.
		boolean isExit = true;
		
		// While the command is not "exit" do the iteration.
		while (isExit){
			
			System.out.print("Command: ");
			command = scan.nextLine();
			
			if (!command.equals("exit")){
				
				// Read the command.
				Scanner read = new Scanner(command);
				// Take the first word as command.
				String instruct = read.next();
				// Take the rest of command as content.
				String content = "";	
				
				// Those are several cases for commands.
				if(instruct.equals("add")){					
					addToFile(write, read, file, content);					
				} else if (instruct.equals("delete")){
					// This action is used to clean up the file.
					write = new BufferedWriter(new FileWriter(file.getName(), false));
					deleteFromFile(write, read, file);					
				} else if (instruct.equals("display")){					
					display(file);					
				} else if (instruct.equals("clear")){				
					write = new BufferedWriter(new FileWriter(file.getName(), false));
					clear(file);					
				} else{					
					System.out.println("Wrong command");					
				}				
			} else{
				write.close();
				
				// Should be write.flush();
				// Since enter into "exit" case, end the loop.
				isExit = false;				
			}
		}		
	}
	
	/*
	 *  Delete the command from arrayList, and refresh the file by cleaning 
	 *  up file and rewrite in it.
	 */
	public static void deleteFromFile(BufferedWriter write, Scanner read, File file) throws IOException{
		int num = read.nextInt();		
		System.out.println("deleted from " + file.getName() + " “" + commandList.get(num-1) + "”");
		// Remove the command from temporary list, since the index starts from 0 it should be num-1.
		commandList.remove(num-1);
		// Rewrtie all commands from arrayList into file.
		for (int i = 0; i < commandList.size(); i++){
			write.write(i + 1 + "." + commandList.get(i) + "\n");
		}
	
	}
	
	/*
	 * Add command to arrayList and then write into the file.
	 */
	public static void addToFile(BufferedWriter write, Scanner read, File file, String content) throws IOException{
		content = read.nextLine();
		commandList.add(content);
		System.out.println("added to " + file.getName() + " “" + content + "”");
		write.write(commandList.size()+". " +commandList.get(commandList.size()-1) + "\n"); // Get write the most added command to file.
	}
	
	/*
	 * Iterate through the commandList and display on terminal.
	 */
	public static void display(File file){
		if (commandList.isEmpty()){
			System.out.println(file.getName() + " is empty");
		} else{
			for (int i = 0; i < commandList.size(); i++){
				System.out.println( i + 1 + "." + commandList.get(i));
			}
		}
	}
	
	/*
	 * Clean up the file and also clean up the commandList.
	 */
	public static void clear(File file){
		commandList.clear();
		System.out.println("all content deleted from " + file.getName());
	}
}
	








