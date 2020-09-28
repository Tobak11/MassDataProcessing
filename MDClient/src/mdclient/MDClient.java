/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mdclient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author tobak11
 */
public class MDClient {
    
    //Server related data
    private static ArrayList<String> serverAddressArray;
    private static ArrayList<ServerController> serverControllerArray;
    
    //Task controller
    private static TaskController factorialTaskController;
    
    public static void main(String[] args) {
        start();
    }
    
    private static void readServerAddresses(){
        try {
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document file = builder.parse("serverAddresses.xml");
            NodeList addressList = file.getElementsByTagName("address");
            
            for(int i=0;i<addressList.getLength();i++)
                serverAddressArray.add(addressList.item(i).getTextContent());
            
        } catch (SAXException ex) {
            Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
        }   
    }
    
    private static void startConnections(ExecutorService serverExecutor){
        
        for(int i=0;i<serverAddressArray.size();i++){
            final int finalI = i;
            serverExecutor.submit(new Runnable(){
                @Override
                public void run() {
                    serverControllerArray.add(new ServerController(serverAddressArray.get(finalI)));
                }   
            });
        }
    }
    
    private static void startTasks(ExecutorService serverExecutor) {
        
        for(int i=0;i<serverAddressArray.size();i++){
            final int finalI = i;
            serverExecutor.submit(new Runnable(){
                @Override
                public void run() {
                    serverControllerArray.get(finalI).sendRequest(factorialTaskController.getTaskClassName(), factorialTaskController.getTaskClassString());
                }   
            });
        }
    }
    
    private static void start(){
        double start = System.nanoTime();
        serverAddressArray = new ArrayList<>();
        serverControllerArray = new ArrayList<>();
        
        readServerAddresses();
        
        factorialTaskController = new TaskController("FactorialTask", 12, 1, 1000000);
        ExecutorService serverExecutor =  Executors.newFixedThreadPool(serverAddressArray.size());
        
        factorialTaskController.readTaskClass();               
        startConnections(serverExecutor);
        factorialTaskController.distributeData(serverControllerArray,serverAddressArray.size());
        startTasks(serverExecutor);
        factorialTaskController.assembleResult(serverControllerArray,serverAddressArray.size());
        
        serverExecutor.shutdown();
        
        double end = System.nanoTime();
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Time: " + (end-start)/Math.pow(10, 9) + "s");
    }
    
}
