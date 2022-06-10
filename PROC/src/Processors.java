import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @Author hym
 * @Date 2022/5/25 14:17
 */
public class Processors extends Thread{
    static volatile MainMemory m = new MainMemory();

//    static volatile ArrayList<PCB> backupQueue = new ArrayList<>(); //后备队列（应该放作业，为了简化程序，写为pcb）
//    static volatile PriorityQueue<PCB> readyQueue = new PriorityQueue<>();   //就绪队列
//    static volatile ArrayList<PCB> suspendQueue = new ArrayList<>(); //挂起队列
//    static volatile ArrayList<PCB> finishedQueue = new ArrayList<>();    //完成队列
//    static volatile ArrayList<PCB> waitingQueue = new ArrayList<>(); //等待队列

    static volatile List<PCB> backupQueue = Collections.synchronizedList(new ArrayList<>());
    static volatile PriorityBlockingQueue<PCB> readyQueue = new PriorityBlockingQueue<>();   //就绪队列
    static volatile List<PCB> suspendQueue = Collections.synchronizedList(new ArrayList<>());
    static volatile List<PCB> finishedQueue = Collections.synchronizedList(new ArrayList<>());
    static volatile List<PCB> waitingQueue = Collections.synchronizedList(new ArrayList<>());

    static int mutex_backup = 1;    //后备队列访问互斥锁
    static int mutex_ready = 1; //就绪队列访问互斥锁
    static int mutex_suspend = 1;   //挂起队列互斥访问锁
    static int mutex_finish = 1;    //完成队列访问互斥锁
    static int mutex_wait = 1;  //等待队列访问互斥锁
    static int mutex_memory = 1;    //内存区互斥访问锁

    private String processorID;
    private static int task_cnt = 8;  //道数

    public Processors(String name) {
        super(name);
    }

    public String getProcessorID() {
        return processorID;
    }

    public void setProcessorID(String processorID) {
        this.processorID = processorID;
    }

    //作业调度  先进先出，如果内存不够，则将作业调到队尾
    public void AdvancedScheduling(){
        //优先考虑等待队列的进程放入就绪队列中
        if(!waitingQueue.isEmpty() && readyQueue.size() < task_cnt){
            for(PCB pcb: waitingQueue){
                if(pcb.getProp() == Property.SYNCHRONIZED && foundPCB(pcb)){
                    P(mutex_wait);
                    waitingQueue.remove(pcb);
                    V(mutex_wait);
                    P(mutex_ready);
                    if(readyQueue.size() < task_cnt){
                        pcb.setState(PCBState.ACTIVE_READY);
                        readyQueue.add(pcb);
                        P(mutex_memory);
                        m.insertPCB(pcb);
                        V(mutex_memory);
                        V(mutex_ready);
                    }else{
                        P(mutex_wait);
                        waitingQueue.add(pcb);
                        V(mutex_wait);
                        V(mutex_ready);
                    }

                }
            }
        }
        if(!backupQueue.isEmpty() && readyQueue.size() < task_cnt){
            P(mutex_backup);
            if(m.checkAssignable(backupQueue.get(0)) != null){
                PCB p = backupQueue.get(0);
                backupQueue.remove(0);
                V(mutex_backup);
                P(mutex_ready);
                p.setState(PCBState.ACTIVE_READY);
                readyQueue.add(p);
                P(mutex_memory);
                m.insertPCB(p); //分配内存
                V(mutex_memory);
                V(mutex_ready);
            }else{
                PCB pt = backupQueue.get(0);
                backupQueue.add(pt);
                backupQueue.remove(0);
                V(mutex_backup);
            }
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
                P(mutex_finish);
                for(PCB p: finishedQueue){
                    if(i == p.getPid()){
                        flag = true;
                        break;
                    }
                }
                V(mutex_finish);
                if(!flag) return false;
            }
        }
        return true;
    }


    //抢占式优先权 每个时间片重新选优先权最高的运行就可以了
    public void ShortScheduling(){
        //先把就绪队列中同步进程中前驱没有执行完的进程放入等待队列中
        P(mutex_ready);
        if(!readyQueue.isEmpty()){
            for(PCB pcb: readyQueue){
                if(pcb.getProp() == Property.SYNCHRONIZED && !foundPCB(pcb)){
                    readyQueue.remove(pcb);
                    pcb.setState(PCBState.WAITING);
                    P(mutex_memory);
                    m.removePCB(pcb);
                    V(mutex_memory);
                    P(mutex_wait);
                    waitingQueue.add(pcb);
                    V(mutex_wait);
                }
            }
        }
        V(mutex_ready);
//        MySceneController.refreshTable();
        //调度
        PCB cur_PCB;
        P(mutex_ready);
        if(!readyQueue.isEmpty()){
            for(PCB pcb: readyQueue){
                if(pcb.getState() != PCBState.RUNNING){
                    cur_PCB = readyQueue.peek();
                    V(mutex_ready);
                    if(cur_PCB != null){
                        cur_PCB.on_running();
                        if(cur_PCB.getState() == PCBState.EXIT){
                            P(mutex_ready);
                            readyQueue.remove(cur_PCB);
                            V(mutex_ready);
                            P(mutex_finish);
                            finishedQueue.add(cur_PCB);
                            P(mutex_memory);
                            m.removePCB(cur_PCB);
                            V(mutex_memory);
                            V(mutex_finish);
                        }else{
                            P(mutex_ready);
                            cur_PCB.setState(PCBState.ACTIVE_READY);
                            V(mutex_ready);
                        }
                    }
                    break;
                }
            }
        }else{
            V(mutex_ready);
        }
    }

    //进程挂起
    public void SuspendPCB(PCB pcb){
        P(mutex_ready);
        readyQueue.remove(pcb);
        V(mutex_ready);
        pcb.setState(PCBState.SUSPENDING);  //设置挂起状态
        P(mutex_memory);
        m.removePCB(pcb);   //释放内存
        V(mutex_memory);
        P(mutex_suspend);
        suspendQueue.add(pcb);
        V(mutex_suspend);
    }

    //进程解挂
    public boolean ReleasePCB(PCB pcb){
        if(readyQueue.size() < 5){
            P(mutex_suspend);
            suspendQueue.remove(pcb);
            V(mutex_suspend);
            P(mutex_ready);
            readyQueue.add(pcb);
            P(mutex_memory);
            m.insertPCB(pcb);
            V(mutex_memory);
            pcb.setState(PCBState.ACTIVE_READY);    //设置活动就绪状态
            V(mutex_ready);
            return true;
        }
        return false;
    }

    public synchronized void P(int mutex){
        mutex--;
        if(mutex < 0){
            try{
                this.wait();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public synchronized void V(int mutex){
        mutex++;
        if(mutex <= 0){
            this.notify();
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
