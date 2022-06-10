import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Description: 内存管理
 * @Author hym
 * @Date 2022/5/25 13:52
 */
public class MainMemory {
    private int Memory;
    private int OSMemory;
    private CopyOnWriteArrayList<Zone> zones;


    class Zone{
        private int pid;//指明分配给哪个进程
        private int startLocation;//起址
        private int size;//大小
        private MemoryBlockState state;//分区状态

        public Zone(int pid, int startLocation, int size, MemoryBlockState state) {
            this.pid = pid;
            this.startLocation = startLocation;
            this.size = size;
            this.state = state;
        }

        public int getPid() {
            return pid;
        }

        public int getStartLocation() {
            return startLocation;
        }

        public int getSize() {
            return size;
        }

        public MemoryBlockState getState() {
            return state;
        }

        public String zoneToString(){
            String info = "";
            if(state == MemoryBlockState.OS_ASSIGNED){
                info = "操作系统";
            }else if(state == MemoryBlockState.UNASSIGNED){
                info = "未分配";
            }else{
                info = "进程" + pid;
            }
            return info;
        }
    }

    public MainMemory() {
        zones = new CopyOnWriteArrayList<>();
        Memory = OSConfig.Memory;
        OSMemory = OSConfig.OSMemory;
        Zone OSZone = new Zone(-1,0,OSMemory,MemoryBlockState.OS_ASSIGNED);
        Zone free = new Zone(-2,OSMemory,Memory-OSMemory,MemoryBlockState.UNASSIGNED);
        zones.add(OSZone);
        zones.add(free);
    }

    //检查是否可分配空间，如果可分配则返回这块分区，否则返回null
    public Zone checkAssignable(PCB pcb){
        for(Zone z: zones){
            if(z.state == MemoryBlockState.UNASSIGNED && z.size >= pcb.getRam()){
                return z;
            }
        }
        return null;
    }

    //分配占用内存资源
    public void insertPCB(PCB pcb){
        //检查是否有空间分配
        Zone zone = checkAssignable(pcb);
        pcb.setState(PCBState.ACTIVE_READY);
        if(zone.size == pcb.getRam()){
            //内存长度刚好相等
            zone.pid = pcb.getPid();
        }else{
            int index = zones.indexOf(zone);
            Zone new_zone = new Zone(-2,zone.startLocation+pcb.getRam(), zone.size-pcb.getRam(),MemoryBlockState.UNASSIGNED);
            pcb.setLocation(new_zone.startLocation);
            zones.add(index+1,new_zone);
            zone.pid = pcb.getPid();
            zone.size = pcb.getRam();
        }
        zone.state = MemoryBlockState.ASSIGNED;
        pcb.setLocation(zone.startLocation);
    }


    //释放内存资源，并合并空闲分区
    public void removePCB(PCB pcb){
        for(Zone z: zones){
            if(z.pid == pcb.getPid()){
                z.pid = -2;
                z.state = MemoryBlockState.UNASSIGNED;
            }
        }
        pcb.setLocation(-100);
        mergeMemory();
    }

    //合并空闲分区
    public void mergeMemory(){
        boolean flag = false;
        List<Zone> MemoryBlockToMergeList = Collections.synchronizedList(new ArrayList<>()); //记录要合并的第二块及其后内存块的位置
        for(Zone z: zones){
            if(z.pid == -2){    //检测到未分配的内存空间（-2）
                if(!flag) //第一次检测到未分配的内存空间（-2）
                    flag = true;
                else    //不是第一次检测到未分配的内存空间（-2），即有连续的未分配空间
                    MemoryBlockToMergeList.add(z);
            }else{  //检测到已分配的内存空间（!=-2）
                flag = false;
            }
        }
        if(!MemoryBlockToMergeList.isEmpty()){
            for(int i = MemoryBlockToMergeList.size() - 1; i >= 0; i--){
                Zone temp = MemoryBlockToMergeList.get(i);
                int before = zones.indexOf(temp)-1;
                zones.get(before).size += temp.size;
                zones.remove(temp);
            }
        }
    }


    public String MemoryToString(){
        String info = String.format("%s%16s%15s","起址","大小","状态");
        info += "\r\n";
        for(Zone zone: zones){
            info += String.format("%s", String.format("%03d",zone.startLocation));
            info += String.format("%18s",String.format("%03d",zone.size));
            if(zone.state == MemoryBlockState.OS_ASSIGNED){
                info += String.format("%18s","操作系统");
            }else if(zone.state == MemoryBlockState.ASSIGNED){
                info += String.format("%25s","已分配，属于进程：" +String.format("%02d",zone.pid));
            }else{
                info += String.format("%17s","未分配");
            }
            info += "\r\n";
        }
        return info;
    }

    public CopyOnWriteArrayList<Zone> getZones() {
        return zones;
    }
}
