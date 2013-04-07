/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *
 * John Gilmer
 */
package hw2;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PCKYNoMods is an application that reads a probabilistic grammar and uses
 * it to parse a set of sentences with the PCKY algorithm.
 */
public class PCKYNoMods {
    
    //hashmap for binary productions and their probabilities
    private static Map<String,Double> bGrammar = new HashMap<String,Double>();
    
    //hashmap for lexical productions and their probabilities
    private static Map<String,Double> lGrammar = new HashMap<String,Double>();
    
    //set of all the non-terminals in the grammar
    private static Set<String> nonTerminals = new HashSet<String>();
    
    //optimization hashmap from string of non-terminal to integer representation
    private static Map<String,Integer> ntToInt = new HashMap<String,Integer>();
    
    //optimization hashmap from integer representation of non-terminal to string representation
    private static Map<Integer,String> ntToString = new HashMap<Integer,String>();
    
    //list of the parses returned for the input sentences
    private static List<String> parses = new ArrayList<String>();
    
    
    /**
     * main takes filePaths to the probabilistic grammar file and to the file for the
     * input sentences
     **/
    public static void main(String args[]) throws FileNotFoundException, IOException, Exception{
        
        
        ///read probabilistic grammar file
        readData(args[0]);
        
        //initialize optimization non-terminal maps
        initializeNTMaps();
        
        //parse the senstences in the file at args[1]
        parseSentences(args[1]);
    }

    /**
     * readData reads a probabilistic grammar file and stores each production with its
     * probability in a hashmap for the binary productions and a hashmap for the lexical productions.
     * As it parses the file it also builds the set of non-terminals
     * @param filename String for file path to the probabilistic grammar file
    **/
    private static void readData(String filename) throws FileNotFoundException,IOException, Exception {
        BufferedReader in = new BufferedReader(new FileReader(filename));
        
       
        String next = in.readLine();
        String[] items;
        String production;
        Double prob;
        
        //parse grammar file
        while(next !=null){
            items = next.split("\\s+");
            
            //get probability of grammar production
            prob = Double.parseDouble(items[items.length-1]);
            
            //get grammar production
            production = items[0];
            
            
            // if it is a lexical production
            if(items.length == 4){
               
                //build string representation of lexical grammar production
                for(int i=1; i< items.length -1; i++){
                    production = production + " " + items[i];
                }
                //put production in the lexical grammar hashmap
                lGrammar.put(production, prob);
            }
            
            //if it is a binary production
            else if(items.length ==5){
                
                //build string representation of binary grammar production
               for(int i=1; i< items.length -1; i++){
                    production = production + " " + items[i];
                }
                //put production in the binary grammar hashmap
                bGrammar.put(production, prob);
            }
            else{
                throw new Exception("Input Error");
            }
            
            //keep track of non-terminal ins the grammar
            if(!nonTerminals.contains(items[0])){
                nonTerminals.add(items[0]);
            }
            next = in.readLine();
        }
        in.close();
    }


    /**
     * parseSentences reads sentences from a file and sends them to the method
     * parse which parses a sentence and stores in a list of parses. Finally this method
     * prints all of those parses stored in the list
     **/
    private static void parseSentences(String filename) throws FileNotFoundException, IOException {
         BufferedReader in = new BufferedReader(new FileReader(filename));  
         
         //read sentences from file
         String next = in.readLine();
         while(next != null){
           //parse sentence
           parse(next);
             
           next = in.readLine();    
         }
         
         
         in.close();
         
         //print parses
         for(String parse : parses){
             System.out.println(parse);
         }
    }
    
    
    /**
     * PCKY algorithm implementation
     * */
    private static void parse(String s){
        //split words in the sentence
        String[] words = s.split("\\s+");
        
        //initialize variables
        Double probNT1 =0.0;
        Double probNT2 =0.0;
        Double nextProb =0.0;
        String rootNode = "TOP";
                
        Integer NT0;
        Integer NT1;
        Integer NT2;
        StringBuilder parseString = new StringBuilder();
        
        //table stores probabilties of productions from [i][j][NT]
        double[][][] table = new double[words.length+1][words.length+1][nonTerminals.size()];
        //backtrace 4-d array stores backtraces for given NTs spanning a given range in the sentence
        int[][][][] backTrace = new int[words.length+1][words.length+1][nonTerminals.size()][3];

        
        for(int j = 1; j < (words.length+1) ; j++){
            //store all lexical production probabilties in table for word at j
            for(String prod : lGrammar.keySet()){
                if(prod.split("\\s+")[2].equals(words[j-1])){
                    table[j-1][j][nonTerminalToInt(prod.split("\\s+")[0])] = lGrammar.get(prod);
                }
            }
            
            for(int i = (j-2); i > -1; i --){
                for(int k = (i+1); k < j; k++){
                    for(String prod : bGrammar.keySet()){
                        NT1 = nonTerminalToInt(prod.split("\\s+")[2]);
                        NT2 = nonTerminalToInt(prod.split("\\s+")[3]);
                        probNT1 = table[i][k][NT1];
                        probNT2 = table[k][j][NT2];
                        if( (probNT1 > 0.0) &&
                            (probNT2 > 0.0) ){
                            nextProb = probNT1 * probNT2 * bGrammar.get(prod);
                            NT0 = nonTerminalToInt(prod.split("\\s")[0]);
                            if( table[i][j][NT0]  < nextProb){
                                //store most probable production to this cell in table
                                //store back trace in backtrace
                                table[i][j][NT0] = nextProb;
                                backTrace[i][j][NT0][0] = k;
                                backTrace[i][j][NT0][1] = NT1;
                                backTrace[i][j][NT0][2] = NT2;
                            }   
                        } 
                    }
                }
            }
        }
        
        //return BuildTree
        
        int[] backTraceValues = new int[3];
        int[] emptyList = {0,0,0};
        //get backtrace values spanning the whole sentence
        backTraceValues = backTrace[0][words.length][nonTerminalToInt(rootNode)];
        if(!Arrays.equals(backTraceValues, emptyList)){
          parseString.append("(").append(rootNode).append(" ");
          //call buildTree method
          buildTree(0,words.length,backTraceValues[0],backTraceValues[1],backTraceValues[2],parseString,backTrace,words);
          parseString.append(")");
        }
        else{
          parseString.append(" ");
        }
        //store parseString in list of parses
        parses.add(parseString.toString());
    }

    private static Integer nonTerminalToInt(String key) {
        
        return ntToInt.get(key);
    }

    /**
     * for each of the non-terminals it creates an integer representation 
     * and stores it in both optimization hashmaps
     **/
    private static void initializeNTMaps() {
        Integer key;
        for(String nt : nonTerminals){
            if(!ntToInt.containsKey(nt)){
                key = ntToInt.size();
                ntToInt.put(nt, key);
                ntToString.put(key, nt);
            }
        }
    }

    /**
     * Reconstructs the parse by looking through the backtrace table
     * */
    private static void buildTree(int i, int j, Integer k, Integer B, Integer C,StringBuilder parseString,
            int[][][][] backTrace, String[] input) {
        
          int[] backB = backTrace[i][k][B];
          int[] backC = backTrace[k][j][C];
          int[] empty = {0,0,0};
          
          //first handle B 
          parseString.append("(").append(ntToString.get(B)).append( " ");
          if( !Arrays.equals(backB,empty)){
              buildTree(i,k,backB[0],backB[1],backB[2],parseString,backTrace,input);
              parseString.append(") ");
          }
          else{
              parseString.append(input[k-1]).append(") ");
          }
          
          //handle C
          parseString.append("(").append(ntToString.get(C)).append( " ");
          if(!Arrays.equals(backC,empty)){
              buildTree(k,j,backC[0],backC[1],backC[2],parseString,backTrace,input);
              parseString.append(")");
          }
          else{
              parseString.append(input[k]).append(")");
          }
        
    }
}
