/**
 * @Description: 枚举类  进程状态、进程属性、内存块分配情况
 * @Author hym
 * @Date 2022/5/22 15:44
 */
enum PCBState{
    CREATE("进程创建"),
    ACTIVE_READY("活动就绪"),
    WAITING("进程等待"),
    RUNNING("进程运行"),
    SUSPENDING("进程挂起"),
    EXIT("进程结束");

    private String desc;//中文描述

    private PCBState(String desc){
        this.desc=desc;
    }

    public String getDesc(){
        return desc;
    }
}

enum Property{
    INDEPENDENT("独立进程"),
    SYNCHRONIZED("同步进程");

    private String desc;//中文描述

    private Property(String desc){
        this.desc=desc;
    }

    public String getDesc(){
        return desc;
    }
}

enum MemoryBlockState{
    UNASSIGNED("未分配"),
    ASSIGNED("已分配"),
    OS_ASSIGNED("操作系统");

    private String desc;//中文描述

    private MemoryBlockState(String desc){
        this.desc=desc;
    }

    public String getDesc(){
        return desc;
    }
}
