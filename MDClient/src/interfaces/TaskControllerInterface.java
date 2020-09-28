/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interfaces;

import java.util.ArrayList;
import mdclient.ServerController;

/**
 *
 * @author tobak11
 */
public interface TaskControllerInterface {
    void readTaskClass();
    void importData();
    void distributeData(ArrayList<ServerController> serverControllerArray, int serverCount);
    void assembleResult(ArrayList<ServerController> serverControllerArray, int serverCount);
}
