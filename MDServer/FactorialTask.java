/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mdserver;

import clientinterfaces.TaskInterface;
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
    private BigInteger result;
    
    public FactorialTask(String inputData){
        String[] parts = inputData.split(":");
        this.from = Integer.valueOf(parts[0]);
        this.to = Integer.valueOf(parts[1]);
        this.threadMultiplier = Integer.valueOf(parts[2]);
        System.out.println(parts[0] + " " + parts[1] + " " + parts[2]);
    }
    
    @Override
    public void distributeTasks() {
        inputArray = new ArrayList<>();
        
        int i=0;
        int step = (int)Math.floor( (double)to/(double)nTask);
        int currMax = from;
        
        System.out.println(from);
        System.out.println(step);

        while( i< (nTask-1) ){
            currMax = (i+1)*step;
            inputArray.add(new Pair<>( (from+i*step) , currMax ));
            System.out.println((from+i*step) + " " + currMax);
            
            i++;   
        } 
        inputArray.add(new Pair<>( currMax+1 , to ));
        
        System.out.println(currMax+1 + " " + to);
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
        
        double start = System.nanoTime();
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
        
        double end = System.nanoTime();
        System.out.println((end-start)/(Math.pow(10, 9))); 
        
        exServ.shutdown();
    }

    @Override
    public void assembleResult() {
        result = new BigInteger("1"); 
        for (Future<BigInteger> futureArray1 : futureArray) {
            try {
                result = result.multiply(new BigInteger(futureArray1.get().toString()));
            }catch (InterruptedException ex) {
                Logger.getLogger(FactorialTask.class.getName()).log(Level.SEVERE, null, ex);
            }catch (ExecutionException ex) {
                Logger.getLogger(FactorialTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public String getResult() {
        return result.toString();
    }
}

