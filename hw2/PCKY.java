/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author jgilme1
 */
public class PCKY {
    
    private static Map<String,Double> bGrammar = new HashMap<String,Double>();
    private static Map<String,Double> lGrammar = new HashMap<String,Double>();
    private static Set<String> nonTerminals = new HashSet<String>();
    private static Set<String> terminals = new HashSet<String>();
    private static Map<String,Integer> ntToInt = new HashMap<String,Integer>();
    private static Map<Integer,String> ntToString = new HashMap<Integer,String>();
    private static List<String> parses = new ArrayList<String>();
    
    public static void main(String args[]) throws FileNotFoundException, IOException, Exception{
        
        
        
        readData(args[0]);
        initializeNTMaps();
        parseSentences(args[1]);
    }

    private static void readData(String filename) throws FileNotFoundException,IOException, Exception {
        BufferedReader in = new BufferedReader(new FileReader(filename));
        
       
        String next = in.readLine();
        String[] items;
        String production;
        Double prob;
        while(next !=null){
            items = next.split("\\s+");
            prob = Double.parseDouble(items[items.length-1]);
            production = items[0];
            // lexical production
            if(items.length == 4){
               
                for(int i=1; i< items.length -1; i++){
                    production = production + " " + items[i];
                }
                if(!items[2].equals("UNK")){
                  terminals.add(items[2]);
                }
//                System.out.println(production);
                lGrammar.put(production, prob);
            }
            
            //binary production
            else if(items.length ==5){
                
               for(int i=1; i< items.length -1; i++){
                    production = production + " " + items[i];
                }
                bGrammar.put(production, prob);
            }
            else{
                throw new Exception("Input Error");
            }
            
            if(!nonTerminals.contains(items[0])){
                nonTerminals.add(items[0]);
            }
            next = in.readLine();
        }
        in.close();
    }

    private static void parseSentences(String filename) throws FileNotFoundException, IOException {
         BufferedReader in = new BufferedReader(new FileReader(filename));  
         
         String next = in.readLine();
         while(next != null){
           parse(next);
             
           next = in.readLine();    
         }
         
         
         in.close();
         
         //print parses
         for(String parse : parses){
             System.out.println(parse);
         }
    }
    
    
    private static void parse(String s){
        String[] words = s.split("\\s+");
        Double probNT1 =0.0;
        Double probNT2 =0.0;
        Double nextProb =0.0;
        String rootNode = "TOP";
                
        Integer NT0;
        Integer NT1;
        Integer NT2;
        StringBuilder parseString = new StringBuilder();
        
        //table
        double[][][] table = new double[words.length+1][words.length+1][nonTerminals.size()];
        int[][][][] backTrace = new int[words.length+1][words.length+1][nonTerminals.size()][3];
        String terminalWord;
        
        for(int j = 1; j < (words.length+1) ; j++){
            if(!terminals.contains(words[j-1])){
                
                terminalWord = "UNK";
            }
            else{
                terminalWord = words[j-1];
            }
            for(String prod : lGrammar.keySet()){
                if(prod.split("\\s+")[2].equals(terminalWord)){
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
                            //System.out.println(nextProb);
                            if( table[i][j][NT0]  < nextProb){
                                //System.out.println(i +" " + j + " " + prod.split("\\s")[0]);
                                table[i][j][NT0] = nextProb;
                                backTrace[i][j][NT0][0] = k;
                                backTrace[i][j][NT0][1] = NT1;
                                backTrace[i][j][NT0][2] = NT2;
                            }   
                        } 
                    }
                }
            }
            
            //return buildTree
            
        }
        
        //return BuildTree
        int[] backTraceValues = new int[3];
        int[] emptyList = {0,0,0};
        backTraceValues = backTrace[0][words.length][nonTerminalToInt(rootNode)];
        if(!Arrays.equals(backTraceValues, emptyList)){
          parseString.append("(").append(rootNode).append(" ");
          buildTree(0,words.length,backTraceValues[0],backTraceValues[1],backTraceValues[2],parseString,backTrace,words);
          parseString.append(")");
        }
        else{
          parseString.append(" ");
        }
        parses.add(parseString.toString());
    }

    private static Integer nonTerminalToInt(String key) {
        
        return ntToInt.get(key);
    }

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
