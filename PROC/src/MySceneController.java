/**
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @Author hym
 * @Date 2022/5/28 10:26
 */


import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class MySceneController {
    Processors2 processorsJob = new Processors2("JOB");
    Processors2 processorsCPU1 = new Processors2("CPU1");
    Processors2 processorsCPU2 = new Processors2("CPU2");

    @FXML
    private Button AddPCBButton;

    @FXML
    private TextField CPU1Info;

    @FXML
    private TextField CPU2Info;


    @FXML
    private TextField PreText;

    @FXML
    private TextField PriText;

    @FXML
    private ChoiceBox<String> PropText;

    @FXML
    private TextArea memoryTable;

    @FXML
    private Button RunProcessorButton;

    @FXML
    private TextField SizeText;

    @FXML
    private TextField TimeText;


    @FXML
    private TextField pidText;


    @FXML
    public TextArea readyTable ;

    @FXML
    public TextArea suspendTable;

    @FXML
    public TextArea waitTable;

    @FXML
    public TextArea backupTable;

    @FXML
    private ProgressBar CPUBar1;

    @FXML
    private ProgressBar CPUBar2;

    @FXML
    private Button releaseButton;

    @FXML
    private TextField releaseList;

    @FXML
    private Button suspendButton;

    @FXML
    private TextField suspendList;

    @FXML
    private Canvas canvas;

    public TextField getPreText() {
        return PreText;
    }

    public TextField getPriText() {
        return PriText;
    }

    public ChoiceBox<String> getPropText() {
        return PropText;
    }

    public TextField getSizeText() {
        return SizeText;
    }

    public TextField getTimeText() {
        return TimeText;
    }

    public TextField getPidText() {
        return pidText;
    }


    @FXML
    private void initialize(){
        PropText.getItems().addAll("独立进程","同步进程");
        PropText.setValue("独立进程");
        //设置不可编辑
        readyTable.setEditable(false);
        suspendTable.setEditable(false);
        waitTable.setEditable(false);
        backupTable.setEditable(false);
        //设置提示
        suspendList.setPromptText("请输入");
        releaseList.setPromptText("请输入");
        //初始化15个PCB样例
        Random r = new Random();
        for(int i = 0; i < 15; i++){
            PCB pcb = new PCB(i+1,r.nextInt(11)+10,r.nextInt(11)+10,r.nextInt(9)+1,Property.INDEPENDENT,new ArrayList<>());
            Processors2.backupQueue.add(pcb);
        }
        refreshTable();
    }
    @FXML
    void RunProcessor(ActionEvent event) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                refreshTable();
            }
        },1,10);
        processorsJob.start();
        processorsCPU1.start();
        processorsCPU2.start();

    }

    @FXML
    void addPCB(ActionEvent event) {
        int pid = Integer.parseInt(getPidText().getText());
        int time = Integer.parseInt(getTimeText().getText());
        int ram = Integer.parseInt(getSizeText().getText());
        int priority = Integer.parseInt(getPriText().getText());
        Property property;
        ArrayList<Integer> pre = new ArrayList<>();
        if(getPropText().getValue().equals("独立进程")){
            property = Property.INDEPENDENT;
        }else{
            property = Property.SYNCHRONIZED;
            String temp = getPreText().getText();
            String[] split = temp.split("/");
            for(String s: split){
                if(Integer.parseInt(s) >= pid){
                    Alert a = new Alert(null,"前驱进程错误");
                    a.showAndWait();
                    return;
                }
                pre.add(Integer.parseInt(s));
            }
        }
        if(pid == 0 || time == 0 || ram == 0 || priority == 0 || pre.size() == 0 && property == Property.SYNCHRONIZED){
            Alert a = new Alert(null,"请输入合法进程信息！");
            a.showAndWait();
            return;
        }
        PCB pcb = new PCB(pid,time,ram,priority,property,pre);
        Processors2.backupQueue.add(pcb);
        pidText.clear();
        TimeText.clear();
        SizeText.clear();
        PriText.clear();
        PreText.clear();

    }

    public void refreshTable(){

        String prompt = String.format("%s%6s%6s%6s%8s%16s\r\n","进程号","优先权","时间","内存","属性","前驱");
        //就绪队列
        String ready = String.format("%s%6s%6s%6s%6s%10s%16s\r\n","进程号","优先权","时间","内存","起址","属性","前驱");;
        if(!Processors2.readyQueue.isEmpty()){
            for(PCB p: Processors2.readyQueue){
                int k = Processors2.readyQueue.indexOf(p);
                if( k != 0 && k != 1){
                    ready += p.PCBtoString(1);
                    ready += "\r\n";
                }
            }
        }
        String finalReady = ready;
        Platform.runLater(()->{
            readyTable.setText(finalReady);
        });
        //后备队列
        String backup = prompt;
        if(!Processors2.backupQueue.isEmpty()){
            for(PCB p: Processors2.backupQueue){
                backup += p.PCBtoString(0);
                backup += "\r\n";
            }
        }
        String finalBackup = backup;
        Platform.runLater(()->{backupTable.setText(finalBackup);});

        //挂起队列
        String suspend = prompt;
        if(!Processors2.suspendQueue.isEmpty()){
            for(PCB p: Processors2.suspendQueue){
                suspend += p.PCBtoString(0);
                suspend += "\r\n";
            }
        }
        String finalSuspend = suspend;
        Platform.runLater(()->{suspendTable.setText(finalSuspend);});
        //等待队列
        String wait = prompt;
        if(!Processors2.waitingQueue.isEmpty()){
            for(PCB p: Processors2.waitingQueue){
                wait += p.PCBtoString(0);
                wait += "\r\n";
            }
        }
        String finalWait = wait;
        Platform.runLater(()->{waitTable.setText(finalWait);});
        //分区表
        Platform.runLater(()->{memoryTable.setText(Processors2.m.MemoryToString());});
        //CPU运行图表
        PCB temp_pcb1,temp_pcb2;
        temp_pcb1 = Processors2.getNow_PCB1();
        temp_pcb2 = Processors2.getNow_PCB2();
        if(temp_pcb1 != null){
            String CPU1Prompt = "进程号：" + temp_pcb1.getPid() + "  优先权：" + temp_pcb1.getPriority() + "  起址：" + temp_pcb1.getLocation();
            Platform.runLater(()->{
                CPU1Info.setText(CPU1Prompt);
                CPUBar1.setProgress((double) (temp_pcb1.getOriginal_time()-temp_pcb1.getTime())/temp_pcb1.getOriginal_time());
            });
        }
        if(temp_pcb2 != null){
            String CPU2Prompt = "进程号：" + temp_pcb2.getPid() + "  优先权：" + temp_pcb2.getPriority() + "  起址：" + temp_pcb2.getLocation();
            Platform.runLater(()->{
                CPU2Info.setText(CPU2Prompt);
                CPUBar2.setProgress((double) (temp_pcb2.getOriginal_time()- temp_pcb2.getTime())/ temp_pcb2.getOriginal_time());
            });
        }

    }


    @FXML
    void releaseButton(ActionEvent event) {
        String pid = releaseList.getText();
        Alert a  = new Alert(null,"请输入有效的进程号！");
        boolean flag = false;
        if(pid != null && !Processors2.suspendQueue.isEmpty()){
            for(PCB pcb: Processors2.suspendQueue){
                if(String.valueOf(pcb.getPid()).equals(pid)){
                    flag = true;
                    Processors2.ReleasePCB(pcb);
                    break;
                }
            }
        }
        if(!flag){
            a.showAndWait();
        }
        releaseList.clear();
    }

    @FXML
    void suspendButton(ActionEvent event) {
        String pid = suspendList.getText();
        Alert a  = new Alert(null,"请输入有效的进程号！");
        boolean flag = false;
        if(pid != null && !Processors2.readyQueue.isEmpty()){
            for(PCB pcb: Processors2.readyQueue){
                if(String.valueOf(pcb.getPid()).equals(pid)){
                    flag = true;
                    Processors2.SuspendPCB(pcb);
                    break;
                }
            }
        }
        if(!flag){
            a.showAndWait();
        }
        suspendList.clear();
    }

}

