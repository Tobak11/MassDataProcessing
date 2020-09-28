/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interfaces;

/**
 *
 * @author tobak11
 */
public interface ServerControllerInterface {
    void calcBenchScore();
    void handleInputData();
    void handleOutputData();
    void sendRequest(String taskClassName, String taskClassString);
    void closeConnection();
}
