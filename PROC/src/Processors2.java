import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @Author hym
 * @Date 2022/5/25 14:17
 */
public class Processors2 extends Thread{
    static volatile MainMemory m = new MainMemory();

    static volatile CopyOnWriteArrayList<PCB> backupQueue = new CopyOnWriteArrayList<>();
    static volatile CopyOnWriteArrayList<PCB> readyQueue = new CopyOnWriteArrayList<>();
    static volatile CopyOnWriteArrayList<PCB> suspendQueue = new CopyOnWriteArrayList<>();
    static volatile CopyOnWriteArrayList<PCB> finishedQueue = new CopyOnWriteArrayList<>();
    static volatile CopyOnWriteArrayList<PCB> waitingQueue = new CopyOnWriteArrayList<>();


    private String processorID;
    private static int task_cnt = 8;  //道数

    private static volatile PCB now_PCB1;
    private static volatile PCB now_PCB2;

    public Processors2(String name) {
        super(name);
    }

    public String getProcessorID() {
        return processorID;
    }

    public void setProcessorID(String processorID) {
        this.processorID = processorID;
    }

    public static PCB getNow_PCB1() {
        return now_PCB1;
    }

    public static PCB getNow_PCB2() {
        return now_PCB2;
    }

    //作业调度  先进先出，如果内存不够，则将作业调到队尾
    public void AdvancedScheduling(){
        //优先考虑等待队列的进程放入就绪队列中
        if(!waitingQueue.isEmpty() && readyQueue.size() < task_cnt){
            for(PCB pcb: waitingQueue){
                if(pcb.getProp() == Property.SYNCHRONIZED && foundPCB(pcb) || pcb.getProp() == Property.INDEPENDENT){
                    waitingQueue.remove(pcb);
                    if(readyQueue.size() < task_cnt){
                        pcb.setState(PCBState.ACTIVE_READY);
                        readyQueue.add(pcb);
                        m.insertPCB(pcb);
                    }else{
                        waitingQueue.add(pcb);
                    }
                }
            }
            Collections.sort(readyQueue);
        }
        if(!backupQueue.isEmpty() && readyQueue.size() < task_cnt){
            if(m.checkAssignable(backupQueue.get(0)) != null){
                PCB p = backupQueue.get(0);
                backupQueue.remove(0);
                p.setState(PCBState.ACTIVE_READY);
                readyQueue.add(p);
                m.insertPCB(p); //分配内存
            }else{
                PCB pt = backupQueue.get(0);
                backupQueue.add(pt);
                backupQueue.remove(0);
            }
            Collections.sort(readyQueue);
        }
        try{
            Thread.sleep(100);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    //查询前驱进程是否全部完成
    public boolean foundPCB(PCB pcb){
        boolean flag;
        if(!pcb.getPrecursor().isEmpty()){
            for(Integer i: pcb.getPrecursor()){
                flag = false;
                for(PCB p: finishedQueue){
                    if(i == p.getPid()){
                        flag = true;
                        break;
                    }
                }
                if(!flag) return false;
            }
        }
        return true;
    }


    //抢占式优先权 每个时间片重新选优先权最高的运行就可以了
    public void ShortScheduling(){
        //先把就绪队列中同步进程中前驱没有执行完的进程放入等待队列中
        if(!readyQueue.isEmpty()){
            Collections.sort(readyQueue);
            for(PCB pcb: readyQueue){
                if(pcb.getProp() == Property.SYNCHRONIZED && !foundPCB(pcb)){
                    readyQueue.remove(pcb);
                    pcb.setState(PCBState.WAITING);
                    m.removePCB(pcb);
                    waitingQueue.add(pcb);
                }
            }
        }
        //调度
        PCB cur_PCB;
        if(!readyQueue.isEmpty()){
            Collections.sort(readyQueue);
            for(PCB pcb: readyQueue){
                if(pcb.getState() != PCBState.RUNNING && pcb.getState() != PCBState.EXIT){
                    cur_PCB = pcb;
                    synchronized (this){
                        if(Thread.currentThread().getName().equals("CPU1")){
                            now_PCB1 = cur_PCB;
                        }else if(Thread.currentThread().getName().equals("CPU2")){
                            now_PCB2 = cur_PCB;
                        }
                    }
                    cur_PCB.on_running();
                    if(cur_PCB.getState() == PCBState.EXIT){
                        readyQueue.remove(cur_PCB);
                        finishedQueue.add(cur_PCB);
                        m.removePCB(cur_PCB);
                        synchronized (this){
                            if(Thread.currentThread().getName().equals("CPU1")){
                                now_PCB1 = null;
                            }else if(Thread.currentThread().getName().equals("CPU2")){
                                now_PCB2 = null;
                            }
                        }
                    }else{
                        cur_PCB.setState(PCBState.ACTIVE_READY);
                    }
                    break;
                }
            }
            Collections.sort(readyQueue);
        }
    }

    //进程挂起
    public static void SuspendPCB(PCB pcb){
        readyQueue.remove(pcb);
        pcb.setState(PCBState.SUSPENDING);  //设置挂起状态
        m.removePCB(pcb);   //释放内存
        suspendQueue.add(pcb);
    }

    //进程解挂，暂时解不了挂，放进等待队列
    public static void ReleasePCB(PCB pcb){
        if(readyQueue.size() < 5){
            suspendQueue.remove(pcb);
            readyQueue.add(pcb);
            m.insertPCB(pcb);
            pcb.setState(PCBState.ACTIVE_READY);    //设置活动就绪状态
        }else{
            suspendQueue.remove(pcb);
            waitingQueue.add(pcb);
            pcb.setState(PCBState.WAITING);
        }

    }


    @Override
    public void run(){
        while(true){
            if(Thread.currentThread().getName().equals("JOB")){
                AdvancedScheduling();
            }else{
                ShortScheduling();
            }
        }
    }

}
