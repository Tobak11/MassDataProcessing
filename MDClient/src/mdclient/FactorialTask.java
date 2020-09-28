/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mdclient;

import interfaces.TaskInterface;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 *
 * @author tobak11
 */

public class FactorialTask implements TaskInterface {
    
    //Processing power info
    private int availableProcessors;
    private int nTask;
    
    //Input data
    private final int from;
    private final int to;
    private final int threadMultiplier;
    //Distributed data
    private ArrayList<Pair<Integer,Integer>> inputArray;
    
    //Preassembled output data
    private Future<BigInteger>[] futureArray;
    
    //Assembled data
    private static String resultString;
    
    public FactorialTask(String inputData){
        String[] parts = inputData.split(":");
        this.from = Integer.valueOf(parts[0]);
        this.to = Integer.valueOf(parts[1]);
        this.threadMultiplier = Integer.valueOf(parts[2]);
    }
    
    @Override
    public void distributeTasks() {
        inputArray = new ArrayList<>();
        
        int i=0;
        int step = (int)Math.floor( (double)(to-from)/(double)nTask)-1;
        int currMin = from;

        System.out.println("From: " + from + " to: " + to);
        System.out.println("nTask: " + nTask);
        System.out.println("STEP: " + step);
        
        while( i< (nTask-1) ){
            inputArray.add(new Pair<>( currMin, (currMin+step) ));
            currMin = currMin+step+1;
            
            i++;   
        }
        
        inputArray.add(new Pair<>( currMin , to ));
    }
    
    @Override
    public BigInteger executeTask(int nthTask) {
        final Pair<Integer,Integer> currPair = inputArray.get(nthTask);
        
        BigInteger res = new BigInteger("1");
        
        for(int i=currPair.getValue();i>=currPair.getKey();i--){
            res = res.multiply(new BigInteger(String.valueOf(i)));
        }
        
        return res;
    }
    
    @Override
    public void execute() {
        availableProcessors = Runtime.getRuntime().availableProcessors();
        nTask = availableProcessors*threadMultiplier;
        futureArray = new Future[nTask];
        
        distributeTasks();
        
        ExecutorService exServ = Executors.newFixedThreadPool(availableProcessors);
        
        
        for(int i=0;i<inputArray.size();i++){
            final int finalI = i;
            futureArray[i] = exServ.submit(new Callable<BigInteger>() {
                @Override
                public BigInteger call() throws Exception {
                    return executeTask(finalI);
                };
            });
        }

        assembleResult();
        
        exServ.shutdown();
    }

    @Override
    public void assembleResult() {
        StringBuilder resultSb = new StringBuilder();
        
        for (int i=0;i<futureArray.length;i++) {
            try {
                
                resultSb.append(futureArray[i].get().toString());
                
                if(i<futureArray.length-1 && (i==0 || i % (int)Math.floor(futureArray.length/4) != 0) ){
                    resultSb.append(":");
                }
                
                if(i != 0 && i % (int)Math.floor(futureArray.length/4) == 0){
                    resultSb.append("/");
                }
                
            } catch (InterruptedException ex) {
                Logger.getLogger(FactorialTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FactorialTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        resultString = resultSb.toString();
    }

    @Override
    public String getResult() {
        return resultString;
    }
}

