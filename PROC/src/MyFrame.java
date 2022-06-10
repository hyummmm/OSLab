import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @Author hym
 * @Date 2022/5/28 15:27
 */
public class MyFrame extends Application {

    static private Stage primaryStage;
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("多处理器调度模拟");
        Parent root = FXMLLoader.load(getClass().getResource("MySceneController.fxml"));
        Scene scene = new Scene(root,1079,600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
