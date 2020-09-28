/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mdclient;

import interfaces.ServerControllerInterface;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tobak11
 */
public class ServerController implements ServerControllerInterface {
    private Socket socket;    
    private DataInputStream in;
    private DataOutputStream out;
    
    //Benchmark score, needed to efficiently distribute the source data (Bigger = higher processing capacity)
    private double benchScore;
    
    //Output data to be sent to the server
    private String outputData;
    
    //Result data
    private String resultData;
    
    public ServerController(String address){  
        try {
            socket = new Socket(address, 50000);
            
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            calcBenchScore();
        } catch (IOException ex) {
            Logger.getLogger(ServerController.class.getName()).log(Level.SEVERE, null, ex);
        }   
    }

    @Override
    public void calcBenchScore() {
        try {
            String benchTime = in.readUTF();
            benchScore = 1/Double.valueOf(benchTime);
        } catch (IOException ex) {
            Logger.getLogger(ServerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void handleInputData(){
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
            
            resultData = tempSb.toString();
            
        } catch (IOException ex) {
            Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void handleOutputData() {
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
                Logger.getLogger(MDClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void sendRequest(String taskClassName, String taskClassString) {
        try {
            out.writeUTF(taskClassName);
            out.writeUTF(taskClassString);
            
            handleOutputData();
            handleInputData();
            
        } catch (IOException ex) {
            Logger.getLogger(ServerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void closeConnection() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
    public void setOutputData(String outputData) {
        this.outputData = outputData;
    }
    
    public double getBenchScore(){
        return this.benchScore;
    }
    
    public String getResultData() {
        return resultData;
    }
}
