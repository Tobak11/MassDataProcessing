/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mdserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
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
public class MDServer {
    //Task file
    private static File taskFile;
    
    //Network related variables
    private static Socket clientSocket; 
    private static ServerSocket serverSocket;
    private static DataInputStream in;
    private static DataOutputStream out;
    
    //Input variables
    private static String className;
    private static String classImpl;
    private static String inputData;
    
    //Output variables
    private static String outputData;
    
    public static void main(String[] args) {
        start();   
    }
    
    private static void initServer(){
        try {
            serverSocket = new ServerSocket(50000);
            clientSocket = serverSocket.accept();

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void runBenchmark(){
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int nTask = 128;
        
        Future<BigInteger>[] futureArray = new Future[nTask]; 
        ExecutorService exServ = Executors.newFixedThreadPool(availableProcessors);
        
        double startTime = System.nanoTime();
        
        for(int i=0;i<nTask;i++){
            futureArray[i] = exServ.submit(new Callable<BigInteger>() {
                @Override
                public BigInteger call() throws Exception {
                    BigInteger result = new BigInteger("1");
                    for(int j=4096;j>0;j--){
                        result.multiply(new BigInteger(String.valueOf(j)));
                    }
                    
                    return result;
                };
            });
        }
        
        exServ.shutdown();
        
        BigInteger fullResult = new BigInteger("1");
        for(Future<BigInteger> future : futureArray){
            try {
                fullResult.multiply(future.get());
            } catch (InterruptedException ex) {
                Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        double endTime = System.nanoTime();
        
        String benchTime = String.valueOf( (endTime-startTime)/Math.pow(10, 9) );
        
        try {
            out.writeUTF(benchTime);
            System.out.println("Benchtime: " + benchTime + "s");
        } catch (IOException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void createTaskClass(){
        try {
            className = in.readUTF();
            classImpl = in.readUTF();
        } catch (IOException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try(PrintWriter writeClass = new PrintWriter(taskFile = new File("./src/mdserver/" + className + ".java"))){
            classImpl = classImpl.replaceAll("mdclient", "mdserver");
            writeClass.println(classImpl);
            
            writeClass.close();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void executeRequest(){
        try {
            
            Class c = null;
            while(c == null){
                try {
                    c = Class.forName(new MDServer().getClass().getPackage().getName() + "." + className);
                } catch (ClassNotFoundException ex) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex1) {
                        Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
            
            Object o = c.getDeclaredConstructor(String.class).newInstance(inputData);
            
            c.getDeclaredMethod("execute").invoke(o);
            outputData = (String)c.getDeclaredMethod("getResult").invoke(o);
            
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void removeTaskClass() {
        taskFile.delete();
    }
    
    private static void handleInputData(){
        String tempString;
        StringBuilder tempSb = new StringBuilder();
        
        try {
            
            String signal = in.readUTF();
            
            if(signal.equals("start")){
                tempString = in.readUTF();
                
                while(tempString.length()==65535){
                    tempSb.append(tempString);
                    
                    tempString = in.readUTF();
                }
                tempSb.append(tempString);
            }
            
            inputData = tempSb.toString();
            
        } catch (IOException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void handleOutputData() {
        try {
            out.writeUTF("start");
            
            if(outputData.length() < 65536){
                out.writeUTF(outputData);
            }else{
                int packetCount = (int)Math.floor(outputData.length()/65535);
                
                for(int i=0;i<packetCount;i++){
                    out.writeUTF(outputData.substring( i*65535, ((i+1)*65536-(i+1)) ));
                }
                
                out.writeUTF(outputData.substring( packetCount*65535, outputData.length() ));
            }
            
        } catch (IOException ex) {
                Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void shutdownServer(){
        try {
            clientSocket.close();
            serverSocket.close();
            in.close();
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(MDServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void start(){
        while(true){
            initServer();

            runBenchmark();

            createTaskClass();
            handleInputData();
            executeRequest();
            removeTaskClass();
            handleOutputData();

            shutdownServer();
        } 
    }
}
