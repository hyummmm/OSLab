import java.util.ArrayList;


/**
 * @Description: PCB类的设计：保存进程的状态信息，可以设置进程状态，添加后继进程，运行进程。
 * @Author hym
 * @Date 2022/5/22 15:31
 */

//PCB内容包括：进程名/PID；要求运行时间（单位时间）；优先权；状态；进程属性：独立进程、同步进程（前趋、后继）。
public class PCB implements Comparable<PCB>{
    private int pid;//进程号
    private int time;//剩余运行时间（单位时间）
    private int original_time;//原始运行时间即运行时间总长
    private int location;//在内存中的起始位置
    private int ram;//所需内存空间
    private int priority;//优先权
    private PCBState state;//进程状态
    private Property prop;//进程属性：独立进程、同步进程（前趋、后继）。
    private ArrayList<Integer> precursor;//前驱进程PID  为了简化实验，设置为只能有一个前驱进程
    private ArrayList<Integer> successor;//后继进程PID集合

    public PCB(int pid,int time, int ram, int priority, Property prop, ArrayList<Integer> precursor) {
        this.pid = pid;
        this.time = time;
        this.original_time = time;
        this.ram = ram;
        this.priority = priority;
        this.prop = prop;
        this.precursor = precursor;
        this.successor = new ArrayList<>();
        this.state = PCBState.CREATE;
    }

    public String PCBtoString(int k){
        String info = "";
        info += String.format("%s",String.format("%02d",pid));
        info += String.format("%11s", priority);
        info += String.format("%13s",String.format("%02d",time));
        info += String.format("%9s", ram);
        if(k == 1){
            info += String.format("%10s",String.format("%03d",location));
        }
        info += " ";
        if(prop == Property.INDEPENDENT){
            info += String.format("%12s","独立进程");
            info += String.format("%8s","无");
        }else{
            info += String.format("%12s","同步进程");
            String temp = "";
            for(Integer i: precursor){
                temp += String.valueOf(i);
                if(precursor.indexOf(i) != precursor.size()-1){
                    temp += "/";
                }
            }
            info += String.format("%8s",temp);;
        }
        return info;
    }

    public int getOriginal_time() {
        return original_time;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getPid() {
        return pid;
    }

    public int getTime() {
        return time;
    }

    public int getLocation() {
        return location;
    }

    public int getRam() {
        return ram;
    }

    public int getPriority() {
        return priority;
    }

    public PCBState getState() {
        return state;
    }

    public Property getProp() {
        return prop;
    }

    public ArrayList<Integer> getPrecursor() {
        return precursor;
    }

    public void setState(PCBState state) {
        this.state = state;
    }

    public void addSuccessor(int pid){
        successor.add(pid);
    }

    public void on_running(){
        state = PCBState.RUNNING;
        if(time == 0){
            state = PCBState.EXIT;
            return;
        }else{
            time --;
        }
        try{
            Thread.sleep(1500);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

    }

    public void setLocation(int location) {
        this.location = location;
    }

    @Override
    public int compareTo(PCB o) {
        return o.priority - priority;
    }

}
