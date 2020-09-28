/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interfaces;

import java.util.concurrent.Future;

/**
 *
 * @author tobak11
 */
public interface TaskInterface {
    void distributeTasks();
    Object executeTask(int nthTask);
    void execute();
    void assembleResult();
    String getResult();
}
