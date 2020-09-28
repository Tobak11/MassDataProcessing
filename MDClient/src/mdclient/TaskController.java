/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mdclient;

import interfaces.TaskControllerInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tobak11
 */
public class TaskController implements TaskControllerInterface {
    //Task related variables
    private String taskClassName;
    private String taskClassString;
    
    //Input data
    private int from;
    private int to;
    
    //Thread multiplier constant
    private final int threadMultiplier;
    
    public TaskController(String taskClassName, int threadMultiplier, int from, int to){
        this.taskClassName = taskClassName;
        this.threadMultiplier = threadMultiplier;
        this.from = from;
        this.to = to;
    }
    
    @Override
    public void readTaskClass(){
        try {
            Scanner fileScan = new Scanner(new File("./src/mdclient/" + taskClassName + ".java"));
            
            StringBuilder sb = new StringBuilder();
            
            while(fileScan.hasNext()){
                sb.append(fileScan.nextLine()).append("\n");
            }
            fileScan.close();
            
            taskClassString = sb.toString();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //If data import is needed
    @Override
    public void importData(){}
    
    @Override
    public void distributeData(ArrayList<ServerController> serverControllerArray, int serverCount){
        boolean allReadyFlag = false;
        
        while(!allReadyFlag){
            int countReadyElements = 0;
            for(int i=0;i<serverControllerArray.size();i++){
                if(serverControllerArray.get(i).getBenchScore() != 0){
                    countReadyElements++;
                }
            }
            
            if(countReadyElements == serverCount){
                allReadyFlag = true;
            }else{
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        double benchSum = 0;
        
        int distributable = to-(from-1);
        
        int lastTo = 0;
        
        for(int i=0;i<serverControllerArray.size();i++){
            benchSum+=serverControllerArray.get(i).getBenchScore();
        }
        
        for(int i=0;i<serverControllerArray.size();i++){
            for(int j=i+1;j<serverControllerArray.size();j++){
                if(serverControllerArray.get(i).getBenchScore()>serverControllerArray.get(j).getBenchScore()){
                    ServerController temp = serverControllerArray.get(i);
                    serverControllerArray.set(i, serverControllerArray.get(j));
                    serverControllerArray.set(j, temp);
                }
            }
        }
        
        for(int i=0;i<serverControllerArray.size()-1;i++){
            int currTo = (int)Math.floor(lastTo + serverControllerArray.get(i).getBenchScore()/benchSum*distributable);
            
            String tempOutputString = (lastTo+1) + ":" + currTo + ":" + threadMultiplier;
            serverControllerArray.get(i).setOutputData(tempOutputString);
            
            lastTo = currTo;
        }
        
        String tempOutputString = (lastTo+1) + ":" + to + ":" + threadMultiplier;
        serverControllerArray.get(serverControllerArray.size()-1).setOutputData(tempOutputString);
    }
    
    @Override
    public void assembleResult(ArrayList<ServerController> serverControllerArray, int serverCount){
        boolean allReadyFlag = false;
        
        while(!allReadyFlag){
            int countReadyElements = 0;
            for(int i=0;i<serverControllerArray.size();i++){
                if(serverControllerArray.get(i).getResultData() != null){
                    countReadyElements++;
                }
            }
            
            if(countReadyElements == serverCount){
                allReadyFlag = true;
            }else{
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        ExecutorService assembleExecutor = Executors.newFixedThreadPool(serverCount);
        
        Future<BigInteger>[] resultParts = new Future[serverCount*4];
        
        for(int i=0;i<serverControllerArray.size();i++){  
            String[] bigServerResultParts = serverControllerArray.get(i).getResultData().split("/");
            ArrayList<String[]> serverResultParts = new ArrayList<>();
            
            for(int j=0;j<bigServerResultParts.length;j++){
                serverResultParts.add(bigServerResultParts[j].split(":"));
            }
            
            for(int j=0;j<serverResultParts.size();j++){
                final int finalJ = j;
                
                resultParts[i*4 + j] = assembleExecutor.submit(new Callable(){                

                    @Override
                    public BigInteger call() throws Exception {
                        BigInteger resultPart = new BigInteger("1");

                        for(int k=0;k<serverResultParts.get(finalJ).length;k++){
                            resultPart = resultPart.multiply(new BigInteger(serverResultParts.get(finalJ)[k]));
                        }

                        return resultPart;
                    }
                });
            }  
        }
        
        BigInteger result = new BigInteger("1");
        
        for(int i=0;i<serverCount*4;i++){
            try {
                result = result.multiply(resultParts[i].get());
            } catch (InterruptedException ex) {
                Logger.getLogger(TaskController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TaskController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        //System.out.println(result);
        
        assembleExecutor.shutdown();
    }

    public String getTaskClassName() {
        return taskClassName;
    }

    public String getTaskClassString() {
        return taskClassString;
    }
}
