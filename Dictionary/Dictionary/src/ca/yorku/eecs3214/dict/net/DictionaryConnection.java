package ca.yorku.eecs3214.dict.net;

import ca.yorku.eecs3214.dict.model.Database;
import ca.yorku.eecs3214.dict.model.Definition;
import ca.yorku.eecs3214.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;
    
    Socket socket;
    BufferedReader in;
    PrintWriter out;
    
    // don't really need to put these here but i figure its easier instead of 
    // just declaring in each method
  	String userInput;
  	List<String> x;
    /**
     * Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages. This constructor does not send any request for additional data.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the welcome
     *                                 messages are not successful.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {

        // TODO Add your code here
    	
     try {
    	this.socket = new Socket(host, port);    //sets the socket
	   	out = new PrintWriter(socket.getOutputStream(), true); //sets the writer
	   	in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //sets the reader
	   	System.out.println("Welcome!");
     
	   	Status status = Status.readStatus(in); //reads the first line which contains the status code
	   	if (status.getStatusCode() != 220) { //status code needs to be 220 
	   		throw new DictConnectionException("Error.  ");
	   	}
		 
	 }
   	  catch (IOException e) {
			throw new DictConnectionException("Error.  ");
		}
     }


    /**
     * Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the welcome
     *                                 messages are not successful.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /**
     * Sends the final QUIT message, waits for its reply, and closes the connection with the server. This function
     * ignores any exception that may happen while sending the message, receiving its reply, or closing the connection.
     */
    public synchronized void close() {

    	out.println("QUIT"); //exit writer
		 try {
			socket.close(); //close socket

		 }
		 catch (IOException e) {
			 
		} 
    }

    /**
     * Requests and retrieves a map of database name to an equivalent database object for all valid databases used in
     * the server.
     *
     * @return A map linking database names to Database objects for all databases supported by the server, or an empty
     * map if no databases are available.
     * @throws DictConnectionException If the connection is interrupted or the messages don't match their expected
     *                                 value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();
        

      	try {
      		
      		
		   	 out.println("SHOW DB"); // show list of databases
			 
			 Status status = Status.readStatus(in);  //reads status code
			 if (status.getStatusCode() == 554) { //if status code 554 means no databases, so return empty
				 return databaseMap;
			 }
			 else if (status.getStatusCode() != 110) { //needs to be 110 code
				 throw new DictConnectionException("Error.  ");
			 }
			 
			 userInput = in.readLine(); // reading the first line
			 x = new ArrayList<String>(); //makes a list so can we split the lines into singular words
		    
			 while (  !userInput.equals(".")) {  //every command ends with "." so check if its "."
			     x = DictStringParser.splitAtoms(userInput);  //splits the name and description
		         databaseMap.put(x.get(0), new Database(x.get(0), x.get(1))); //populate
		         userInput = in.readLine(); // next line
	     	 }

	     	 Status end = Status.readStatus(in); //check the status at the end
	     	 if (end.getStatusCode() != 250){ //needs to be 250
	 			throw new DictConnectionException("Error.  ");
	     	 }
	     	 

      	} catch (IOException e) {

			throw new DictConnectionException("Error.  ");
		}
      	
        return databaseMap;
    }

    /**
     * Requests and retrieves a list of all valid matching strategies supported by the server. Matching strategies are
     * used in getMatchList() to identify how to suggest words that match a specific pattern. For example, the "prefix"
     * strategy suggests words that start with a specific pattern.
     *
     * @return A set of MatchingStrategy objects supported by the server, or an empty set if no strategies are
     * supported.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected
     *                                 value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();

      	try {
      		
		   	out.println("SHOW STRAT"); // show list of strategies 
		   	Status status = Status.readStatus(in); //reads the status
			if (status.getStatusCode() == 555) { //code 55 means no strategies, return empty.
				 return set;
			 }
			else if (status.getStatusCode() != 111) { //needs to be 111
				throw new DictConnectionException("Error.  ");
		   	}

		   	userInput = in.readLine(); // reads first line
		   	x = new ArrayList<String>(); // list for words
	     	while (  !userInput.equals(".")) { //check until "."
		         x = DictStringParser.splitAtoms(userInput);  //splits the name and description
		         set.add(new MatchingStrategy(x.get(0), x.get(1))); //populate
	     		 userInput = in.readLine(); // next line
	     	 }
	     	
	     	 Status end = Status.readStatus(in); //check status  
	     	 if (end.getStatusCode() != 250) { //needs to be 250
	 			throw new DictConnectionException("Error.  ");
	     	 }
	     	 	     	 
      	} catch (IOException e) {

			throw new DictConnectionException("Error.  ");
		}
        

        return set;
    }

    /**
     * Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param pattern  The pattern to use to identify word matches.
     * @param strategy The strategy to be used to compare the list of matches.
     * @param database The database where matches are to be found. Special databases like Database.DATABASE_ANY or
     *                 Database.DATABASE_FIRST_MATCH are supported.
     * @return A set of word matches returned by the server based on the word pattern, or an empty set if no matches
     * were found.
     * @throws DictConnectionException If the connection was interrupted, the messages don't match their expected value,
     *                                 or the database or strategy are not supported by the server.
     */
    public synchronized Set<String> getMatchList(String pattern, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();
        
      	try {
      		
		   	out.println("MATCH "  + database.getName() + " " + strategy.getName()+ " " + "\"" + pattern + "\""); // shows the list
		   	Status status = Status.readStatus(in); //read status code
		   	switch (status.getStatusCode()) {
            case 550: // 550 means invalid database
                throw new DictConnectionException("Invalid database"); 
            case 551: //551 means invalid strategy
                throw new DictConnectionException("Invalid strategy");
            case 552: // 552 means no match
                return set;                
            case 152: // means matches, so continue with code
            	break;
            case 250: // idk what this one means i think its optional
            	break;
            default:
            	break;
		   	}
		   	
		   	userInput = in.readLine(); // reads line
		   	x = new ArrayList<String>(); // array for words
	     	 while (  !userInput.equals(".")) {
		         x = DictStringParser.splitAtoms(userInput);  //splits the words
		         set.add(x.get(1)); // i think it should be .get(0) but idk what going on well over here ----------- note it
	     		 userInput = in.readLine(); // reads next line
	     	 }

	     	 
	     	 Status end = Status.readStatus(in); //exit code
	     	 if (end.getStatusCode() != 250) { //needs to be 250
	 			throw new DictConnectionException("Error.  ");
	     	 }
	     	 
		   	} catch (IOException e) {

			throw new DictConnectionException("Error.  ");
		}
        return set;
    }

    /**
     * Requests and retrieves all definitions for a specific word.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. Special databases like Database.DATABASE_ANY
     *                 or Database.DATABASE_FIRST_MATCH are supported.
     * @return A collection of Definition objects containing all definitions returned by the server, or an empty
     * collection if no definitions were available.
     * @throws DictConnectionException If the connection was interrupted, the messages don't match their expected value,
     *                                 or the database is not supported by the server.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();

      	try {
      		
      		Definition definition; // make a Definition
		   	out.println("DEFINE " + database.getName() + " " + "\"" + word + "\""); // show definitions
		   			   	
		   	Status status = Status.readStatus(in); //reads status code
		   	
		   	switch (status.getStatusCode()) {
            case 550://550 means invalid database
                throw new DictConnectionException("Invalid database, use \"SHOW DB\" for list of databases");
            case 552://552 means no match
                return set;
            case 150: //150 means matches found so proceed with code
            	break;
            case 250: //idk what one is for, i think its optional
            	break;
            default:
            	break;
		   	}
		   	int amount = Integer.parseInt(DictStringParser.splitAtoms(status.getDetails()).get(0));
		   	for ( int i = 0 ; i <amount; i++  ) {
		   		
		   		Status book = Status.readStatus(in); //reads status code of the definition.
		   		
		   		//checks if correct status code
		   		if (book.getStatusCode() != 151) {
					throw new DictConnectionException("Error.  ");
		   		}		   		
		   		
		   		//sets the word and database for that name
		   		definition = new Definition(DictStringParser.splitAtoms(book.getDetails()).get(0), DictStringParser.splitAtoms(book.getDetails()).get(1));
		   		
		   		
		        userInput = in.readLine(); //reads line
		     	 while (  !userInput.equals(".")) { //stop at "."
			         definition.appendDefinition(userInput); // adds information to the definition 
		     		 userInput = in.readLine(); // next line
		     	 }
		     	 
		     	 set.add(definition); //add definition to set
		     	 
		   	}
	     	 Status end = Status.readStatus(in); // read status
	     	 // idk if this method needs more end status cases. I don't think there is more.
			   	if ( end.getStatusCode() != 250) { //needs to be 250
					throw new DictConnectionException("Error.  ");
			   	}
			   	
			 
      	} catch (IOException e) {

			throw new DictConnectionException("Error.  ");
		}

        return set;
    }

}